#pragma once

#include <string>
#include <functional>
#include <memory>
#include <vector>
#include <map>

class IRSystem {
public:
    enum class Result {
        SUCCESS,
        FAILED,
        INVALID_CODE,
        TRANSMISSION_ERROR,
        TIMEOUT
    };

    struct Command {
        std::string device_type;
        std::string action;
        std::string ir_code;
        int repeat_count;

        Command() : repeat_count(1) {}
        Command(const std::string& type, const std::string& act,
                const std::string& code, int repeat = 1)
            : device_type(type), action(act), ir_code(code), repeat_count(repeat) {}
    };

    struct ControlResult {
        Result result;
        std::string message;
        std::string device_type;
        std::string action;
        double duration_ms;

        ControlResult() : result(Result::FAILED), duration_ms(0.0) {}
        ControlResult(Result r, const std::string& msg,
                     const std::string& dev = "", const std::string& act = "",
                     double dur = 0.0)
            : result(r), message(msg), device_type(dev), action(act), duration_ms(dur) {}
    };

    class ICodeStore {
    public:
        virtual ~ICodeStore() = default;

        virtual bool storeCode(const std::string& device_type,
                              const std::string& action,
                              const std::string& ir_code) = 0;

        virtual std::string getCode(const std::string& device_type,
                                   const std::string& action) const = 0;

        virtual std::vector<std::string> getDeviceTypes() const = 0;

        virtual std::vector<std::string> getActions(const std::string& device_type) const = 0;
    };

    class ITransmitter {
    public:
        virtual ~ITransmitter() = default;

        virtual bool transmit(const std::string& ir_code, int repeat_count = 1) = 0;

        virtual bool isTransmitting() const = 0;

        virtual bool waitForCompletion(int timeout_ms = 1000) = 0;
    };

    class IMQTTClient {
    public:
        virtual ~IMQTTClient() = default;

        virtual bool connect(const std::string& broker, int port = 8883) = 0;

        virtual bool isConnected() const = 0;

        virtual bool subscribe(const std::string& topic) = 0;

        virtual bool publish(const std::string& topic, const std::string& message) = 0;

        virtual void setMessageCallback(std::function<void(const std::string&, const std::string&)> callback) = 0;
    };

    virtual bool initialize() = 0;

    virtual void cleanup() = 0;

    virtual ControlResult executeCommand(const Command& command) = 0;

    virtual std::vector<ControlResult> executeCommands(const std::vector<Command>& commands) = 0;

    virtual bool isReady() const = 0;

    virtual std::string getStatus() const = 0;

protected:
    std::unique_ptr<ICodeStore> code_store_;
    std::unique_ptr<ITransmitter> transmitter_;
    std::unique_ptr<IMQTTClient> mqtt_client_;

    std::function<void(const ControlResult&)> control_callback_;
    std::function<void(const std::string&)> log_callback_;
};
