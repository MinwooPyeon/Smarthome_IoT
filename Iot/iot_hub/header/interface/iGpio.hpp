#pragma once
#include <functional>
#include <string>

namespace sensors{
    enum class PinMode {INPUT, OUTPUT};
    enum class Edge {NONE, RISING, FALLING, BOTH};

    class iGpio{
    public:
        using EdgeCallback = std::function<void(int pin, bool level, uint32_t tick)>;
        virtual ~iGpio() = default;

        virtual bool open(const std::string& chip, int line, PinMode mode, int* err = nullptr) = 0;
        virtual void close() = 0;

        virtual bool read(bool* level, int* err = nullptr) = 0;
        virtual bool write(bool level, int* err = nullptr) = 0;

        virtual bool watch(Edge edge, EdgeCallback cb, int* err = nullptr) = 0;
        virtual void unwatch() = 0;
    };
}