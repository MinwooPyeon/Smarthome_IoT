#include "manager/csv_manager.hpp"
#include <filesystem>
#include <iomanip>
#include <sstream>

namespace fs = std::filesystem;
using namespace csv;

namespace manager {

// ====== 유틸 ======
int CsvManager::yyyymmdd_from_tm(const std::tm& tm) {
    return (tm.tm_year + 1900) * 10000 + (tm.tm_mon + 1) * 100 + tm.tm_mday;
}

std::tm CsvManager::to_utc_tm(const Clock::time_point& tp) {
    using namespace std::chrono;
    std::time_t t = Clock::to_time_t(tp);
    std::tm tm{};
#if defined(_WIN32)
    gmtime_s(&tm, &t);
#else
    gmtime_r(&t, &tm);
#endif
    return tm;
}

std::string CsvManager::yyyymmdd_utc_today() {
    auto now = Clock::now();
    auto tm = to_utc_tm(now);
    char buf[9];
    std::snprintf(buf, sizeof(buf), "%04d%02d%02d", tm.tm_year+1900, tm.tm_mon+1, tm.tm_mday);
    return std::string(buf);
}

// ====== Ctor/Dtor ======
CsvManager::CsvManager(CsvOptions opt)
: opt_(std::move(opt)) {
    // 매퍼/헤더 준비
    metrics_mapper_ = make_metrics_mapper();
    ir_mapper_      = make_ir_signal_mapper();

    env_header_.reserve(metrics_mapper_.columns().size());
    for (auto& c : metrics_mapper_.columns()) env_header_.push_back(c.name);

    ir_header_.reserve(ir_mapper_.columns().size());
    for (auto& c : ir_mapper_.columns()) ir_header_.push_back(c.name);
}

CsvManager::~CsvManager() {
    stop();
}

// ====== Public: start/stop/post ======
void CsvManager::start() {
    if (running_.exchange(true)) return;

    fs::create_directories(opt_.base_dir);
    ensure_open_files_for(Clock::now());

    worker_ = std::thread(&CsvManager::worker_loop, this);
}

void CsvManager::stop() {
    if (!running_.exchange(false)) return;
    {
        std::lock_guard<std::mutex> lk(mu_);
        cv_.notify_all();
    }
    if (worker_.joinable()) worker_.join();

    if (env_ofs_.is_open()) env_ofs_.flush();
    if (ir_ofs_.is_open())  ir_ofs_.flush();
    env_writer_.reset();
    ir_writer_.reset();
    if (env_ofs_.is_open()) env_ofs_.close();
    if (ir_ofs_.is_open())  ir_ofs_.close();
}

void CsvManager::post(const Metrics& m) { post(Event{m}); }
void CsvManager::post(const IrSignalLog& l) { post(Event{l}); }

void CsvManager::post(Event&& ev) {
    std::unique_lock<std::mutex> lk(mu_);
    if (q_.size() >= opt_.max_queue) {
        if (opt_.drop_oldest_on_full) {
            if (!q_.empty()) q_.pop_front();
        } else {
            cv_.wait(lk, [&]{ return q_.size() < opt_.max_queue || !running_.load(); });
        }
    }
    q_.emplace_back(std::move(ev));
    lk.unlock();
    cv_.notify_one();
}

// ====== 워커/플러시 ======
void CsvManager::worker_loop() {
    std::vector<Event> batch;
    batch.reserve(opt_.flush_every_n);

    while (running_.load()) {
        batch.clear();
        std::unique_lock<std::mutex> lk(mu_);
        cv_.wait_for(lk, std::chrono::milliseconds(opt_.flush_interval_ms), [&]{
            return !q_.empty() || !running_.load();
        });
        while (!q_.empty() && batch.size() < opt_.flush_every_n) {
            batch.emplace_back(std::move(q_.front()));
            q_.pop_front();
        }
        lk.unlock();

        if (!batch.empty()) flush_locked(batch);
    }

    // drain
    {
        std::lock_guard<std::mutex> lk(mu_);
        while (!q_.empty()) {
            batch.emplace_back(std::move(q_.front()));
            q_.pop_front();
            if (batch.size() >= opt_.flush_every_n) {
                flush_locked(batch);
                batch.clear();
            }
        }
    }
    if (!batch.empty()) flush_locked(batch);
}

void CsvManager::flush_locked(std::vector<Event>& batch) {
    for (auto& ev : batch) {
        if (std::holds_alternative<Metrics>(ev)) {
            const auto& m = std::get<Metrics>(ev);
            ensure_open_files_for(m.ts);
            write_metrics_row(m);
        } else {
            const auto& l = std::get<IrSignalLog>(ev);
            ensure_open_files_for(l.ts);
            write_ir_row(l);
        }
    }
    if (env_ofs_.is_open()) env_ofs_.flush();
    if (ir_ofs_.is_open())  ir_ofs_.flush();
}

// ====== 파일 오픈/롤링 ======
void CsvManager::ensure_open_files_for(const Clock::time_point& tp) {
    if (!opt_.rotate_daily) {
        if (!env_ofs_.is_open()) {
            env_path_ = opt_.base_dir + "/" + opt_.device_id + "_metrics.csv";
            env_ofs_.open(env_path_, std::ios::binary | std::ios::app);
            // 새 파일이면 헤더 1회
            if (env_ofs_.tellp() == std::streampos(0)) {
                write_row(env_ofs_, env_header_, Dialect{',','"','"'});
            }
            env_writer_ = std::make_unique<Writer>(env_ofs_, Dialect{',','"','"', /*write_header*/ false});
        }
        if (!ir_ofs_.is_open()) {
            ir_path_  = opt_.base_dir + "/" + opt_.device_id + "_ir_log.csv";
            ir_ofs_.open(ir_path_, std::ios::binary | std::ios::app);
            if (ir_ofs_.tellp() == std::streampos(0)) {
                write_row(ir_ofs_, ir_header_, Dialect{',','"','"'});
            }
            ir_writer_  = std::make_unique<Writer>(ir_ofs_,  Dialect{',','"','"', /*write_header*/ false});
        }
        return;
    }

    auto tm = to_utc_tm(tp);
    int ymd = yyyymmdd_from_tm(tm);
    if (ymd == current_yyyymmdd_ && env_ofs_.is_open() && ir_ofs_.is_open()) return;

    if (env_ofs_.is_open()) env_ofs_.close();
    if (ir_ofs_.is_open())  ir_ofs_.close();
    env_writer_.reset();
    ir_writer_.reset();

    open_or_rotate_files(tm);
    current_yyyymmdd_ = ymd;
}

void CsvManager::open_or_rotate_files(const std::tm& tm_utc) {
    std::ostringstream date;
    date << std::setfill('0')
         << std::setw(4) << (tm_utc.tm_year + 1900)
         << std::setw(2) << (tm_utc.tm_mon + 1)
         << std::setw(2) << tm_utc.tm_mday;

    const std::string stem = opt_.base_dir + "/" + opt_.device_id + "_" + date.str();
    env_path_ = stem + "_metrics.csv";
    ir_path_  = stem + "_ir_log.csv";

    env_ofs_.open(env_path_, std::ios::binary | std::ios::app);
    ir_ofs_.open(ir_path_,  std::ios::binary | std::ios::app);

    if (env_ofs_.tellp() == std::streampos(0)) {
        write_row(env_ofs_, env_header_, Dialect{',','"','"'});
    }
    if (ir_ofs_.tellp() == std::streampos(0)) {
        write_row(ir_ofs_, ir_header_, Dialect{',','"','"'});
    }

    env_writer_ = std::make_unique<Writer>(env_ofs_, Dialect{',','"','"', /*write_header*/ false});
    ir_writer_  = std::make_unique<Writer>(ir_ofs_,  Dialect{',','"','"', /*write_header*/ false});
}

// ====== 행 쓰기 ======
void CsvManager::write_metrics_row(const Metrics& m) {
    auto fields = metrics_mapper_.to_fields(m);
    env_writer_->write(fields);
}
void CsvManager::write_ir_row(const IrSignalLog& l) {
    auto fields = ir_mapper_.to_fields(l);
    ir_writer_->write(fields);
}

// ====== 읽기(ALL/범위/마지막 N) ======
std::string CsvManager::resolve_metrics_path_for_today() const {
    if (opt_.rotate_daily) return opt_.base_dir + "/" + opt_.device_id + "_" + yyyymmdd_utc_today() + "_metrics.csv";
    return opt_.base_dir + "/" + opt_.device_id + "_metrics.csv";
}
std::string CsvManager::resolve_ir_path_for_today() const {
    if (opt_.rotate_daily) return opt_.base_dir + "/" + opt_.device_id + "_" + yyyymmdd_utc_today() + "_ir_log.csv";
    return opt_.base_dir + "/" + opt_.device_id + "_ir_log.csv";
}
std::string CsvManager::current_metrics_path() const { return resolve_metrics_path_for_today(); }
std::string CsvManager::current_ir_path() const      { return resolve_ir_path_for_today(); }

std::vector<Metrics> CsvManager::read_metrics_all(std::optional<std::string> path) const {
    const std::string p = path.value_or(resolve_metrics_path_for_today());
    std::ifstream ifs(p, std::ios::binary);
    if (!ifs.good()) return {};
    Reader r(ifs, Dialect{});
    r.maybe_read_header();
    std::vector<Metrics> out; std::vector<std::string> fields;
    while (r.next(fields)) {
        Metrics obj{};
        if (metrics_mapper_.from_fields(fields, obj)) out.push_back(std::move(obj));
    }
    return out;
}
std::vector<IrSignalLog> CsvManager::read_ir_all(std::optional<std::string> path) const {
    const std::string p = path.value_or(resolve_ir_path_for_today());
    std::ifstream ifs(p, std::ios::binary);
    if (!ifs.good()) return {};
    Reader r(ifs, Dialect{});
    r.maybe_read_header();
    std::vector<IrSignalLog> out; std::vector<std::string> fields;
    while (r.next(fields)) {
        IrSignalLog obj{};
        if (ir_mapper_.from_fields(fields, obj)) out.push_back(std::move(obj));
    }
    return out;
}

std::vector<Metrics> CsvManager::read_metrics_range(
    std::chrono::system_clock::time_point start,
    std::chrono::system_clock::time_point end,
    std::optional<std::string> path) const
{
    auto all = read_metrics_all(std::move(path));
    std::vector<Metrics> filtered; filtered.reserve(all.size());
    for (auto& v : all) if (v.ts >= start && v.ts <= end) filtered.push_back(std::move(v));
    return filtered;
}
std::vector<IrSignalLog> CsvManager::read_ir_range(
    std::chrono::system_clock::time_point start,
    std::chrono::system_clock::time_point end,
    std::optional<std::string> path) const
{
    auto all = read_ir_all(std::move(path));
    std::vector<IrSignalLog> filtered; filtered.reserve(all.size());
    for (auto& v : all) if (v.ts >= start && v.ts <= end) filtered.push_back(std::move(v));
    return filtered;
}

std::vector<Metrics> CsvManager::read_metrics_last(size_t n, std::optional<std::string> path) const {
    auto v = read_metrics_all(std::move(path));
    if (n >= v.size()) return v;
    return std::vector<Metrics>(v.end() - static_cast<std::ptrdiff_t>(n), v.end());
}
std::vector<IrSignalLog> CsvManager::read_ir_last(size_t n, std::optional<std::string> path) const {
    auto v = read_ir_all(std::move(path));
    if (n >= v.size()) return v;
    return std::vector<IrSignalLog>(v.end() - static_cast<std::ptrdiff_t>(n), v.end());
}

} // namespace logging
