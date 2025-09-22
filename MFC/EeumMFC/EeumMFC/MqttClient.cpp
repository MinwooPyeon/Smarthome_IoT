#include "pch.h"
#include "MqttClient.h"
#include "MainFrm.h"
#include <fstream>

static std::string normalizeHub(const std::string& hubIdOrPath) {
	// "hub/001" 오면 그대로, "001" 오면 "hub/001"로
	if (hubIdOrPath.rfind("hub/", 0) == 0) return hubIdOrPath;
	return "hub/" + hubIdOrPath;
}

static void PostLogToMain(const CString& line) {
	if (auto* mf = dynamic_cast<CMainFrame*>(AfxGetMainWnd())) {
		auto* p = new CString(line);
		::PostMessage(mf->GetSafeHwnd(), WM_APP_LOG, 0, (LPARAM)p); // UI 스레드에서 Append
	}
}

MqttClient::MqttClient(const Config& cfg)
	: mosquittopp(cfg.id.c_str()), cfg_(cfg) {
	#ifdef MOSQ_OPT_PROTOCOL_VERSION
		// mosquittopp 1.5+ : opts_set로 프로토콜 버전 지정 가능
		// 값은 MQTT_PROTOCOL_V31 / MQTT_PROTOCOL_V311 / MQTT_PROTOCOL_V5 중 선택
		this->opts_set(MOSQ_OPT_PROTOCOL_VERSION, MQTT_PROTOCOL_V311);
	#endif
	reconnect_delay_set(1, 8, true);

	if (!cfg_.user.empty()) username_pw_set(cfg_.user.c_str(), cfg_.pass.c_str());

	if (!cfg_.caFile.empty()) {
		std::ifstream f(cfg_.caFile);
		// 없으면 로그만, 어쨌든 tls_set 시도
		tls_set(cfg_.caFile.c_str(), nullptr,
			cfg_.clientCertFile.empty() ? nullptr : cfg_.clientCertFile.c_str(),
			cfg_.clientKeyFile.empty() ? nullptr : cfg_.clientKeyFile.c_str());
		tls_insecure_set(cfg_.tlsInsecure);
	}

	// 비동기 connect + 루프
	const int rcConn = connect_async(cfg_.host.c_str(), cfg_.port, cfg_.keepalive);
	const int rcLoop = loop_start();
}

MqttClient::~MqttClient() {
	try {
		// 더 이상 콜백 들어오지 않게
		std::lock_guard<std::mutex> lk(mtx_);
		for (auto& t : topics_) unsubscribe(nullptr, t.c_str());
	}
	catch (...) {}

	try { disconnect(); }
	catch (...) {}
	loop_stop(true);
}

void MqttClient::on_connect(int rc)
{
	connected_ = (rc == 0);
	{
		std::lock_guard<std::mutex> lock(mtx_);
		for (auto& t : topics_) subscribe(nullptr, t.c_str(), 1);
	}
	CString m; m.Format(L"[on_connect] rc=%d", rc);
	PostLogToMain(m);
}

void MqttClient::on_disconnect(int rc) {
	connected_ = false;
	CString m; m.Format(L"[on_disconnect] rc=%d", rc);
	PostLogToMain(m);
}

void MqttClient::on_log(int level, const char* s)
{
	CString m; m.Format(L"[mosq log %d] %S", level, s ? s : "(null)");
	PostLogToMain(m);
}

void MqttClient::setTopics(const std::vector<std::string>& topics) {
	std::lock_guard<std::mutex> lock(mtx_);
	if (connected_) {
		for (auto& oldt : topics_)
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

bool MqttClient::orderEnv(const std::string& hubId, bool streaming) {
	const std::string hub = normalizeHub(hubId);
	const std::string topic = hub + "/order/env";

	const std::string payload = std::string("{\"streaming\":") + (streaming ? "true" : "false") + "}";

	return publishJson(topic, payload, 1, false);
}

void MqttClient::on_message(const mosquitto_message* m)
{
	if (!m || !m->payload) return;
	if (onMessage)
		onMessage(m->topic, std::string((char*)m->payload, m->payloadlen));
}
