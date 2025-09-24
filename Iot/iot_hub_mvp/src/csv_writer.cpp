#include "csv_writer.hpp"
#include "util.hpp"

Writer::Writer(std::ostream& os, Dialect d, std::vector<std::string> header)
: os_(&os), d_(d), header_(std::move(header)) {}

void Writer::header(const std::vector<std::string>& names) {
    header_ = names; header_written_ = false;
}

void Writer::write(const std::vector<std::string>& fields) {
    if (d_.write_header && !header_written_ && !header_.empty()) {
        write_row(*os_, header_, d_); header_written_ = true;
    }
    write_row(*os_, fields, d_);
}

