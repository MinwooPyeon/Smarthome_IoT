#pragma once
#include <string>
#include <deque>
#include <thread>
#include <mutex>
#include <condition_variable>
#include <atomic>
#include <chrono>
#include <fstream>
#include <variant>
#include <vector>
#include <optional>

#include "csv/csv_mapper_func.hpp"
#include "csv/csv_reader.hpp"
#include "csv/csv_writer.hpp"
#include "csv/csv_mapper.hpp"

#include "manager/port.hpp"

#include "util.hpp"
#include "types.hpp"

namespace manager
{
    class CsvManager : public IEventSink
    {
    public:
        using Clock = std::chrono::system_clock;
        using Event = std::variant<Metrics, IrSignalLog, IrSendDevice>;

        explicit CsvManager(CsvOptions opt = {});
        ~CsvManager();

        CsvManager(const CsvManager &) = delete;
        CsvManager &operator=(const CsvManager &) = delete;

        // ---- 쓰기 사이드 ----
        void start() override;
        void stop() override;
        void post(const Metrics &m) override;
        void post(const IrSignalLog &l) override;
        void post(const IrSendDevice &d) override;
        void post(Event &&ev);

        // ---- 읽기 사이드 ----
        std::vector<Metrics> read_metrics_all(std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSignalLog> read_log_all(std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSendDevice> read_device_all(std::optional<std::string> path = std::nullopt) const;

        std::vector<Metrics> read_metrics_range(std::chrono::system_clock::time_point start,
                                                        std::chrono::system_clock::time_point end,
                                                        std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSignalLog> read_log_range(std::chrono::system_clock::time_point start,
                                                       std::chrono::system_clock::time_point end,
                                                       std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSendDevice> read_device_range(std::chrono::system_clock::time_point start,
                                                       std::chrono::system_clock::time_point end,
                                                       std::optional<std::string> path = std::nullopt) const;

        std::vector<Metrics> read_metrics_last(size_t n, std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSignalLog> read_log_last(size_t n, std::optional<std::string> path = std::nullopt) const;
        std::vector<IrSendDevice> read_device_last(size_t n, std::optional<std::string> path = std::nullopt) const;

        std::string current_metrics_path() const;
        std::string current_log_path() const;
        std::string current_device_path() const;

    private:
        // ---- 내부 쓰기 구현 ----
        void worker_loop();
        void flush_locked(std::vector<Event> &batch);

        static std::tm to_utc_tm(const Clock::time_point &tp);
        static int yyyymmdd_from_tm(const std::tm &tm);
        static std::string yyyymmdd_utc_today();

        void ensure_open_files_for(const Clock::time_point &tp);
        void open_or_rotate_files(const std::tm &tm_utc);

        std::string resolve_metrics_path_for_today() const;
        std::string resolve_log_path_for_today() const;
        std::string resolve_device_path_for_today() const;

        void write_metrics_row(const Metrics &m);
        void write_log_row(const IrSignalLog &l);
        void write_device_row(const IrSendDevice &d);

    private:
        // 옵션
        CsvOptions opt_;

        // 큐/워커
        mutable std::mutex mu_;
        std::condition_variable cv_;
        std::deque<Event> q_;
        std::atomic<bool> running_{false};
        std::thread worker_;

        // 파일/라이터
        std::string env_path_;
        std::string log_path_;
        std::string device_path_;
        std::ofstream env_ofs_;
        std::ofstream log_ofs_;
        std::ofstream device_ofs_;
        std::unique_ptr<csv::Writer> env_writer_;
        std::unique_ptr<csv::Writer> log_writer_;
        std::unique_ptr<csv::Writer> device_writer_;
        int current_yyyymmdd_{-1}; // 롤링 기준(UTC)

        // 매퍼 & 헤더
        csv::CsvMapper<Metrics> metrics_mapper_;
        csv::CsvMapper<IrSignalLog> log_mapper_;
        csv::CsvMapper<IrSendDevice> device_mapper_;
        std::vector<std::string> env_header_;
        std::vector<std::string> log_header_;
        std::vector<std::string> device_header_;
    };
}