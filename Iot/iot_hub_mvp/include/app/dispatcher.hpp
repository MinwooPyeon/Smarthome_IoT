#pragma once

#include <functional>
#include <mutex>
#include <vector>
#include <typeindex>
#include <unordered_map>

namespace app
{
    class Dispatcher
    {
    public:
        template <typename T>
        using Handler = std::function<void(const &T)>;

        template <typename T>
        void subscribe(Handler<T> h)
        {
            std::lock_guard<std::mutex> lk(mu_);
            auto &vec = slots_[std::type_index(typeid(T))];
            vec.push_back([fn = std::move(h)](const void *p)
                          { fn(*static_cast<const T *>(p)); });
        }

        template <typename T>
        void publish(const T &ev) const
        {
            std::vector<Thunk> copy;
            {
                std::lock_guard<std::mutex> lk(mu_);
                auto it = slots_.find(std::type_index(typeid(T)));
                if (it == slots_.end())
                    return;
                copy = it->second; // 락 오래 잡지 않도록 복사
            }
            for (auto &f : copy)
                f(&ev);
        }

    private:
        using Thunk = std::function<void(const void *)>;
        mutable std::mutex mu_;
        std::unordered_map<std::type_index, std::vector<Thunk>> slots_;
    };
}