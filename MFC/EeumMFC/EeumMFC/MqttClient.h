#pragma once
#include <mosquittopp.h>
#include <functional>
#include <string>
#include <mutex>

class MqttClient : public mosqpp::mosquittopp {
public:
	explicit MqttClient(const std::string& id, const std::string& host, int port);
	std::function<void(const std::string&, const std::string&)> onMessage;

	void setTopics(const std::vector<std::string>& topics);
protected:
	void on_connect(int rc) override;
	void on_disconnect(int rc) override;
	void on_message(const mosquitto_message* m) override;
private:
	std::mutex mtx_;
	bool connected_ = false;
	std::vector<std::string> topics_;
};