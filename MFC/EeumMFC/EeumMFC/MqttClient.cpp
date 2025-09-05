#include "pch.h"
#include "MqttClient.h"

MqttClient::MqttClient(const std::string& id, const std::string& host, int port)
	: mosquittopp(id.c_str()){
	connect_async(host.c_str(), port, 60);
	loop_start();
}

void MqttClient::on_connect(int rc)
{
	if (rc == 0) {
		subscribe(nullptr, "hub/+/env");
		subscribe(nullptr, "hub/+/irsignal");
	}
}

void MqttClient::on_message(const mosquitto_message* m)
{
	if (!m || !m->payload) return;
	if (onMessage)
		onMessage(m->topic, std::string((char*)m->payload, m->payloadlen));
}
