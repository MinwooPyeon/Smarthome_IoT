#include "pch.h"
#include "MqttClient.h"

MqttClient::MqttClient(const std::string& id, const std::string& host, int port)
	: mosquittopp(id.c_str()){
	connect_async(host.c_str(), port, 60);
	loop_start();
}

void MqttClient::on_connect(int rc)
{
	connected_ = (rc == 0);
	if (!connected_) return;

	std::lock_guard<std::mutex> lock(mtx_);
	for (auto& t : topics_) subscribe(nullptr, t.c_str(), 1);
}

void MqttClient::on_disconnect(int rc) {
	connected_ = false;
}

void MqttClient::setTopics(const std::vector<std::string>& topics) {
	std::lock_guard<std::mutex> lock(mtx_);
	if (connected_) {
		for (auto& oldt : topics)
			unsubscribe(nullptr, oldt.c_str());
	}
	topics_ = topics;
	if (connected_) {
		for (auto& t : topics_)
			subscribe(nullptr, t.c_str(), 1);
	}
}

void MqttClient::on_message(const mosquitto_message* m)
{
	if (!m || !m->payload) return;
	if (onMessage)
		onMessage(m->topic, std::string((char*)m->payload, m->payloadlen));
}
