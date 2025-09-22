#include "pch.h"
#include "MqttClient.h"
#include "MainFrm.h"

static std::string normalizeHub(const std::string& hubIdOrPath){
	// "hub/001" 오면 그대로, "001" 오면 "hub/001"로
	if (hubIdOrPath.rfind("hub/", 0) == 0) return hubIdOrPath;
	return "hub/" + hubIdOrPath;
}
static void LogToPaneOrOutput(const CString& level, const CString& msg)
{
	if (auto* mf = dynamic_cast<CMainFrame*>(AfxGetMainWnd()))
		mf->Log(level, msg);
	else
		::OutputDebugStringW((level + L": " + msg + L"\n"));
}
static void LogMqttConnectRCs(int rc1, int rc2, int rc3, const std::string& host)
{
	CString whost(CA2W(host.c_str())); // std::string → CString(W)

	CString msg;
	msg.Format(L"MQTT r1=%d r2=%d r3=%d host=%s", rc1, rc2, rc3, whost.GetString());
	LogToPaneOrOutput(L"DEBUG", msg);

	if (rc3 != MOSQ_ERR_SUCCESS) {
		// OS 에러 메시지까지 함께 출력
		DWORD wsa = WSAGetLastError();
		wchar_t sys[256]{};
		FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
			nullptr, wsa, 0, sys, 256, nullptr);

		CString em;
		em.Format(L"connect_async FAILED → WSA=%lu (%s)", wsa, sys);
		LogToPaneOrOutput(L"ERROR", em);
	}
}

MqttClient::MqttClient(const Config& cfg)
	: mosquittopp(cfg.id.c_str()), cfg_(cfg){
	int rc1 = username_pw_set(cfg_.user.c_str(), cfg_.pass.c_str());
	int rc2 = tls_set(
		cfg_.caFile.empty() ? nullptr : cfg_.caFile.c_str(),
		nullptr,
		cfg_.clientCertFile.empty() ? nullptr : cfg_.clientCertFile.c_str(),
		cfg_.clientKeyFile.empty() ? nullptr : cfg_.clientKeyFile.c_str()
	);
	tls_insecure_set(cfg_.tlsInsecure);

	int rc3 = connect_async(cfg_.host.c_str(), cfg_.port, cfg_.keepalive);
	loop_start();

	LogMqttConnectRCs(rc1, rc2, rc3, cfg_.host);
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
	try { loop_stop(true /*force*/); }
	catch (...) {}
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
