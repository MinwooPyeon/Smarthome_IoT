#include "util.hpp"
#include <algorithm>
#include <cctype>
#include <cerrno>
#include <cstdlib>
#include <cstdio>
#include <sstream>
#include <iomanip>

#if !defined(_WIN32)
#include <time.h>
#endif

static inline void ltrim_inplace(std::string& s) {
    s.erase(s.begin(), std::find_if(s.begin(), s.end(),
           [](unsigned char ch){return !std::isspace(ch);})); }
static inline void rtrim_inplace(std::string& s) {
    s.erase(std::find_if(s.rbegin(), s.rend(),
           [](unsigned char ch){return !std::isspace(ch);}).base(), s.end()); }

std::string escape_field(const std::string& in, const Dialect& d) {
    bool need_quote = false;
    for (char ch : in) if (ch==d.delimiter || ch=='\n' || ch=='\r' || ch==d.quote) { need_quote=true; break; }
    if (!need_quote) return in;
    std::string out; out.reserve(in.size()+2);
    out.push_back(d.quote);
    for (char ch : in) { if (ch==d.quote) out.push_back(d.escape); out.push_back(ch); }
    out.push_back(d.quote);
    return out;
}

bool read_row(std::istream& is, std::vector<std::string>& out, const Dialect& d) {
    out.clear();
    std::string field; field.reserve(64);
    bool in_quotes=false, seen_any=false;

    if (d.allow_bom && is.tellg()==std::streampos(0)) {
        char bom[3]{}; is.read(bom,3);
        if (!((unsigned char)bom[0]==0xEF && (unsigned char)bom[1]==0xBB && (unsigned char)bom[2]==0xBF)) {
            is.clear(); is.seekg(0);
        }
    }

    for (;;) {
        int c = is.get();
        if (c==EOF) {
            if (!seen_any && field.empty()) return false;
            if (d.trim_whitespace && !in_quotes) rtrim_inplace(field);
            out.push_back(std::move(field));
            return true;
        }
        seen_any = true;
        char ch = static_cast<char>(c);

        if (in_quotes) {
            if (ch == d.quote) {
                int next = is.peek();
                if (next == d.quote) { is.get(); field.push_back(d.quote); }
                else { in_quotes = false; }
            } else field.push_back(ch);
        } else {
            if (ch == d.delimiter) {
                if (d.trim_whitespace) rtrim_inplace(field);
                out.push_back(std::move(field)); field.clear();
            } else if (ch == '\n') {
                if (d.trim_whitespace) rtrim_inplace(field);
                out.push_back(std::move(field));
                return true;
            } else if (ch == '\r') {
                int next = is.peek();
                if (next == '\n') is.get();
                if (d.trim_whitespace) rtrim_inplace(field);
                out.push_back(std::move(field));
                return true;
            } else if (ch == d.quote && field.empty()) {
                in_quotes = true;
            } else {
                if (d.trim_whitespace && field.empty() && std::isspace(static_cast<unsigned char>(ch))) {
                    // skip leading space
                } else field.push_back(ch);
            }
        }
    }
}

void write_row(std::ostream& os, const std::vector<std::string>& fields, const Dialect& d) {
    for (size_t i=0;i<fields.size();++i) {
        if (i) os.put(d.delimiter);
        os << escape_field(fields[i], d);
    }
    os << '\n';
}

std::optional<int64_t> to_i64(const std::string& s) {
    if (s.empty()) return std::nullopt;
    char* end=nullptr; errno=0; long long v = std::strtoll(s.c_str(), &end, 10);
    if (errno!=0 || *end!=0) return std::nullopt; return static_cast<int64_t>(v);
}
std::optional<double> to_f64(const std::string& s) {
    if (s.empty()) return std::nullopt;
    char* end=nullptr; errno=0; double v = std::strtod(s.c_str(), &end);
    if (errno!=0 || *end!=0) return std::nullopt; return v;
}
std::optional<bool> to_bool(const std::string& s) {
    if (s.empty()) return std::nullopt;
    std::string t; t.reserve(s.size());
    for (unsigned char c: s) t.push_back(std::tolower(c));
    if (t=="1"||t=="true"||t=="yes"||t=="y") return true;
    if (t=="0"||t=="false"||t=="no" ||t=="n") return false;
    return std::nullopt;
}

std::string to_iso8601(std::chrono::system_clock::time_point tp) {
    using namespace std::chrono;
    std::time_t t = system_clock::to_time_t(tp);
    std::tm tm{};
#if defined(_WIN32)
    gmtime_s(&tm, &t);
#else
    gmtime_r(&t, &tm);
#endif
    char buf[32];
    std::strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", &tm);
    return std::string(buf);
}

std::optional<std::chrono::system_clock::time_point> from_iso8601(const std::string& s) {
    std::tm tm{}; char z='Z';
    if (std::sscanf(s.c_str(), "%d-%d-%dT%d:%d:%d%c",
        &tm.tm_year, &tm.tm_mon, &tm.tm_mday, &tm.tm_hour, &tm.tm_min, &tm.tm_sec, &z) == 7) {
        tm.tm_year -= 1900; tm.tm_mon -= 1; tm.tm_isdst = 0;
#if defined(_WIN32)
        // Windows 대체 필요시 구현
        time_t t = _mkgmtime(&tm);
#else
        time_t t = timegm(&tm);
#endif
        if (t == (time_t)-1) return std::nullopt;
        return std::chrono::system_clock::from_time_t(t);
    }
    return std::nullopt;
}

// ---- JSON-ish helpers (경량) ----
static inline std::string json_escape(const std::string& s){
    std::string o; o.reserve(s.size()+8);
    for(char c: s){
        switch(c){ case '"': o += "\\\""; break; case '\\': o += "\\\\"; break;
            case '\n': o += "\\n"; break; case '\r': o += "\\r"; break; case '\t': o += "\\t"; break;
            default: o += c; }
    } return o;
}
std::string json_encode(const std::vector<int>& v){
    std::string o; o.push_back('[');
    for(size_t i=0;i<v.size();++i){ if(i) o.push_back(','); o += std::to_string(v[i]); }
    o.push_back(']'); return o;
}
std::string json_encode(const std::vector<std::string>& v){
    std::string o; o.push_back('[');
    for(size_t i=0;i<v.size();++i){ if(i) o.push_back(','); o.push_back('"'); o += json_escape(v[i]); o.push_back('"'); }
    o.push_back(']'); return o;
}
std::vector<int> json_parse_int_array(const std::string& s){
    std::vector<int> out; int sign=1; long val=0; bool in_num=false;
    for(char c: s){
        if(c=='-'){ sign=-1; in_num=true; val=0; }
        else if(c>='0'&&c<='9'){ val = val*10 + (c-'0'); in_num=true; }
        else { if(in_num){ out.push_back((int)(sign*val)); in_num=false; sign=1; val=0; } }
    }
    if(in_num) out.push_back((int)(sign*val));
    return out;
}
std::vector<std::string> json_parse_str_array(const std::string& s){
    std::vector<std::string> out; std::string cur; bool in_str=false; bool esc=false;
    for(char c: s){
        if(!in_str){ if(c=='"'){ in_str=true; cur.clear(); } }
        else {
            if(esc){
                switch(c){ case '"': cur+='"'; break; case '\\': cur+='\\'; break;
                    case 'n': cur+='\n'; break; case 'r': cur+='\r'; break; case 't': cur+='\t'; break;
                    default: cur+=c; } esc=false;
            } else if(c=='\\'){ esc=true; }
            else if(c=='"'){ in_str=false; out.push_back(cur); }
            else cur+=c;
        }
    }
    return out;
}