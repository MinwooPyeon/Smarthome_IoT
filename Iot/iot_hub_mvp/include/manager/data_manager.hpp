#pragma once

#include <deque>
#include <mutex>
#include <vector>
#include <chrono>
#include <optional>

#include "manager/port.hpp"
#include "types.hpp"

namespace manager{
    class DataManager : public IDataStore{
    public:
        explicit DataManager(size_t max_metrics = 10000, size_t max_ir = 10000);

        void add(const Metrics& m) override;
        void add(const IrSignalLog& i) override;
        void add(const IrSendDevice& d) override;

        std::vector<Metrics> snapshot_metrics() const;
        std::vector<IrSignalLog> snapshot_log() const;

        std::vector<Metrics>      last_metrics(size_t n) const override;
        std::vector<IrSignalLog>  last_log(size_t n) const override;

        std::vector<Metrics>      metrics_between(std::chrono::system_clock::time_point start, std::chrono::system_clock::time_point end) const;
        std::vector<IrSignalLog>  log_between(std::chrono::system_clock::time_point start, std::chrono::system_clock::time_point end) const;

        void clear_metrics();
        void clear_log();

    private:
        template<class T>
        static std::vector<T> _last_n_locked(const std::deque<T>& q, size_t n);

        template<class T, class GetTs>
        static std::vector<T> _between_locked(const std::deque<T>& q, GetTs get_ts, std::chrono::system_clock::time_point s, std::chrono::system_clock::time_point e);

    private:
        size_t max_metrics_;
        size_t max_log_;

        mutable std::mutex mu_m_;
        mutable std::mutex mu_i_;
        mutable std::mutex mu_d_;
        std::deque<Metrics>     q_m_;
        std::deque<IrSignalLog> q_i_;
        std::deque<IrSendDevice> q_d_;
    };
}