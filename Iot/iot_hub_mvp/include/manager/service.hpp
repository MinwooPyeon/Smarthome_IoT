#pragma once
#include <memory>
#include <mutex>
#include <typeindex>
#include <unordered_map>
#include <stdexcept>

namespace manager
{
    class Service
    {
    public:
        template <class T>
        static void register_instance(std::shared_ptr<T> ptr)
        {
            std::lock_guard<std::mutex> lk(mtx());
            auto key = std::type_index(typeid(T));
            store()[key] = std::move(ptr);
        }

        template<class T>
        static std::shared_ptr<T> try_get()
        {
            std::lock_guard<std::mutex> lk(mtx());
            auto key = std::type_index(typeid(T));
            auto it = store().find(key);
            if (it == store().end())
                return nullptr;
            return std::static_pointer_cast<T>(it->second);
        }

        template <class T>
        static T &get()
        {
            auto sp = try_get<T>();
            if (!sp)
                throw std::runtime_error("Service not registered");
            return *sp;
        }

        static void reset()
        {
            std::lock_guard<std::mutex> lk(mtx());
            store().clear();
        }

    private:
        static std::unordered_map<std::type_index, std::shared_ptr<void>> &store()
        {
            static std::unordered_map<std::type_index, std::shared_ptr<void>> s;
            return s;
        }
        static std::mutex &mtx()
        {
            static std::mutex m;
            return m;
        }
    };
}