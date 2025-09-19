#include "pch.h"
#include "MqttClient.h"

static std::string normalizeHub(const std::string& hubIdOrPath){
	// "hub/001" 오면 그대로, "001" 오면 "hub/001"로
	if (hubIdOrPath.rfind("hub/", 0) == 0) return hubIdOrPath;
	return "hub/" + hubIdOrPath;
}

MqttClient::MqttClient(const Config& cfg)
	: mosquittopp(cfg.id.c_str()), cfg_(cfg){
	if (!cfg_.user.empty())
		username_pw_set(cfg_.user.c_str(), cfg_.pass.c_str());

	if (!cfg_.caFile.empty() || !cfg_.clientCertFile.empty() || !cfg_.clientKeyFile.empty()) {
		tls_set(
			cfg_.caFile.empty() ? nullptr : cfg_.caFile.c_str(),
			nullptr,
			cfg_.clientCertFile.empty() ? nullptr : cfg_.clientCertFile.c_str(),
			cfg_.clientKeyFile.empty() ? nullptr : cfg_.clientKeyFile.c_str()
		);
		
		tls_insecure_set(cfg_.tlsInsecure);
	}
	reconnect_delay_set(2, 30, true);

	connect_async(cfg_.host.c_str(), cfg_.port, cfg_.keepalive);
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

bool MqttClient::publishJson(const std::string& topic, const std::string& json, int qos, bool retain) {
	std::lock_guard<std::mutex> lock(mtx_);
	int rc = publish(nullptr, topic.c_str(), (int)json.size(), json.data(), qos, retain);
	return rc == MOSQ_ERR_SUCCESS;
}

bool MqttClient::orderEnv(const std::string& hubId, bool streaming){
	const std::string hub = normalizeHub(hubId);
	const std::string topic = hub + "/order/env";

	// JSON boolean (true/false). 만약 서버가 "TRUE"/"FALSE" 문자열을 요구하면 아래 줄을 변경:
	// const std::string payload = std::string("{\"streaming\":\"") + (streaming ? "TRUE" : "FALSE") + "\"}";
	const std::string payload = std::string("{\"streaming\":") + (streaming ? "true" : "false") + "}";

	return publishJson(topic, payload, 1, false);
}

void MqttClient::on_message(const mosquitto_message* m)
{
	if (!m || !m->payload) return;
	if (onMessage)
		onMessage(m->topic, std::string((char*)m->payload, m->payloadlen));
}
