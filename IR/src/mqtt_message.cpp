#include "network/mqtt_message.h"
#include <chrono>
#include <random>
#include <sstream>
#include <iomanip>

namespace irremote {

// MessageHeader 구현
MessageHeader::MessageHeader(const std::string& device, const std::string& schema) 
    : deviceId(device), schema(schema) {
    setTimestamp();
    msgId = generateMsgId();
}

void MessageHeader::setTimestamp() {
    auto now = std::chrono::system_clock::now();
    auto duration = now.time_since_epoch();
    ts = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
}

std::string MessageHeader::generateMsgId() {
    static std::random_device rd;
    static std::mt19937 gen(rd());
    static std::uniform_int_distribution<> dis(0, 15);
    
    std::stringstream ss;
    ss << std::hex;
    
    for (int i = 0; i < 8; ++i) {
        ss << std::setw(2) << std::setfill('0') << dis(gen);
        if (i == 3 || i == 5 || i == 7 || i == 9) ss << "-";
    }
    
    return ss.str();
}

nlohmann::json MessageHeader::toJson() const {
    nlohmann::json json;
    json["ts"] = ts;
    json["deviceId"] = deviceId;
    json["msgId"] = msgId;
    json["schema"] = schema;
    return json;
}

void MessageHeader::fromJson(const nlohmann::json& json) {
    if (json.contains("ts")) ts = json["ts"];
    if (json.contains("deviceId")) deviceId = json["deviceId"];
    if (json.contains("msgId")) msgId = json["msgId"];
    if (json.contains("schema")) schema = json["schema"];
}

// IRSignalMessage 구현
IRSignalMessage::IRSignalMessage() 
    : MessageHeader("", "irsignal/1.0"), carrierHz(38000), dutyCycle(0.33), repeat(1), quality(1.0) {
}

nlohmann::json IRSignalMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["encoding"] = encoding;
    json["carrierHz"] = carrierHz;
    json["dutyCycle"] = dutyCycle;
    json["address"] = address;
    json["command"] = command;
    json["timing"] = timing;
    json["rawData"] = rawData;
    json["data"] = data;
    json["repeat"] = repeat;
    json["quality"] = quality;
    if (!remark.empty()) json["remark"] = remark;
    return json;
}

void IRSignalMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("encoding")) encoding = json["encoding"];
    if (json.contains("carrierHz")) carrierHz = json["carrierHz"];
    if (json.contains("dutyCycle")) dutyCycle = json["dutyCycle"];
    if (json.contains("address")) address = json["address"];
    if (json.contains("command")) command = json["command"];
    if (json.contains("timing")) timing = json["timing"];
    if (json.contains("rawData")) rawData = json["rawData"];
    if (json.contains("data")) data = json["data"];
    if (json.contains("repeat")) repeat = json["repeat"];
    if (json.contains("quality")) quality = json["quality"];
    if (json.contains("remark")) remark = json["remark"];
}

// EnvMessage 구현
EnvMessage::EnvMessage() 
    : MessageHeader("", "env/1.1"), temperature(0.0), humidity(0.0), gasDensity(0.0), sampleRateHz(1.0), status("ok") {
}

nlohmann::json EnvMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["temperature"] = temperature;
    json["humidity"] = humidity;
    json["gasDensity"] = gasDensity;
    json["units"] = units;
    json["calib"] = calib;
    json["sampleRateHz"] = sampleRateHz;
    json["status"] = status;
    json["meta"] = meta;
    return json;
}

void EnvMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("temperature")) temperature = json["temperature"];
    if (json.contains("humidity")) humidity = json["humidity"];
    if (json.contains("gasDensity")) gasDensity = json["gasDensity"];
    if (json.contains("units")) units = json["units"];
    if (json.contains("calib")) calib = json["calib"];
    if (json.contains("sampleRateHz")) sampleRateHz = json["sampleRateHz"];
    if (json.contains("status")) status = json["status"];
    if (json.contains("meta")) meta = json["meta"];
}

// OrderMessage 구현
OrderMessage::OrderMessage() 
    : MessageHeader("", "order/1.2"), priority(5), expiresAt(0) {
}

nlohmann::json OrderMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["corrId"] = corrId;
    json["type"] = type;
    json["priority"] = priority;
    json["expiresAt"] = expiresAt;
    json["retry"] = retry;
    json["replyTo"] = replyTo;
    json["payload"] = payload;
    return json;
}

void OrderMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("corrId")) corrId = json["corrId"];
    if (json.contains("type")) type = json["type"];
    if (json.contains("priority")) priority = json["priority"];
    if (json.contains("expiresAt")) expiresAt = json["expiresAt"];
    if (json.contains("retry")) retry = json["retry"];
    if (json.contains("replyTo")) replyTo = json["replyTo"];
    if (json.contains("payload")) payload = json["payload"];
}

bool OrderMessage::isExpired() const {
    if (expiresAt == 0) return false; // 만료시간이 설정되지 않음
    auto now = std::chrono::system_clock::now();
    auto duration = now.time_since_epoch();
    int64_t currentTs = std::chrono::duration_cast<std::chrono::milliseconds>(duration).count();
    return currentTs > expiresAt;
}

bool OrderMessage::shouldRetry(int currentRetries) const {
    if (!retry.contains("max")) return false;
    int maxRetries = retry["max"];
    return currentRetries < maxRetries;
}

// AckMessage 구현
AckMessage::AckMessage() 
    : MessageHeader("", "ack/1.0"), durationMs(0), retries(0) {
}

nlohmann::json AckMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["corrId"] = corrId;
    json["status"] = status;
    json["result"] = result;
    json["durationMs"] = durationMs;
    json["retries"] = retries;
    return json;
}

void AckMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("corrId")) corrId = json["corrId"];
    if (json.contains("status")) status = json["status"];
    if (json.contains("result")) result = json["result"];
    if (json.contains("durationMs")) durationMs = json["durationMs"];
    if (json.contains("retries")) retries = json["retries"];
}

// ErrorMessage 구현
ErrorMessage::ErrorMessage() 
    : MessageHeader("", "error/1.0"), level("INFO") {
}

nlohmann::json ErrorMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["level"] = level;
    json["code"] = code;
    json["detail"] = detail;
    json["ctx"] = ctx;
    return json;
}

void ErrorMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("level")) level = json["level"];
    if (json.contains("code")) code = json["code"];
    if (json.contains("detail")) detail = json["detail"];
    if (json.contains("ctx")) ctx = json["ctx"];
}

// StateMessage 구현
StateMessage::StateMessage() 
    : MessageHeader("", "state/1.0"), status("offline") {
}

nlohmann::json StateMessage::toJson() const {
    nlohmann::json json = MessageHeader::toJson();
    json["status"] = status;
    json["info"] = info;
    return json;
}

void StateMessage::fromJson(const nlohmann::json& json) {
    MessageHeader::fromJson(json);
    if (json.contains("status")) status = json["status"];
    if (json.contains("info")) info = json["info"];
}

} // namespace irremote
