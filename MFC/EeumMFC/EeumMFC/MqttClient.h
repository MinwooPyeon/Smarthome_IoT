#pragma once
#include <mosquittopp.h>
#include <functional>
#include <string>

class MqttClient : public mosqpp::mosquittopp {
public:
	std::function<void(const std::string&, const std::string&)> onMessage;

	MqttClient(const std::string& id, const std::string& host, int port);
	void on_connect(int rc) override;
	void on_message(const mosquitto_message* m) override;
};