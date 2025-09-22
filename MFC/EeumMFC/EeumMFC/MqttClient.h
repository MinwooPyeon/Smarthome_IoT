#pragma once
#include <mosquittopp.h>
#include <functional>
#include <string>
#include <mutex>
#include <vector>
#include "Types.h"

class MqttClient : private MosqInitGuard, public mosqpp::mosquittopp {
public:
	explicit MqttClient(const Config& cfg);
	~MqttClient();
	std::function<void(const std::string&, const std::string&)> onMessage;

	void setTopics(const std::vector<std::string>& topics);

	bool publishJson(const std::string& topic, const std::string& json, int qos = 1, bool retain = false);
	bool orderEnv(const std::string& hubId, bool streaming);
protected:
	void on_connect(int rc) override;
	void on_disconnect(int rc) override;
	void on_message(const mosquitto_message* m) override;
private:
	std::mutex mtx_;
	bool connected_ = false;
	std::vector<std::string> topics_;
	Config cfg_;
};