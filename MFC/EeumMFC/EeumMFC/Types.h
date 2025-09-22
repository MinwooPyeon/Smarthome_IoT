#pragma once
#include <string>
#include <mosquittopp.h>

static std::string CaPathFromExe(const char* name) {
    wchar_t exe[MAX_PATH]{};
    GetModuleFileNameW(nullptr, exe, MAX_PATH);
    std::wstring dir(exe, wcsrchr(exe, L'\\') + 1);
    std::wstring w = dir + std::wstring(name, name + strlen(name));
    std::string  s(w.begin(), w.end()); // ASCII 파일명 가정
    return s;
}

struct EnvSample {
	long long tsMs;
	double t, h, gas;
};

struct IrEvent {
	long long tsMs;
	std::string encoding;
};

struct Metrics {
    // 기존 값
    double tAvg = NAN;     // 평균 온도
    double hAvg = NAN;     // 평균 습도
    double tEwma = NAN;    // EWMA 온도
    double hEwma = NAN;    // EWMA 습도
    double dewPoint = NAN; // 이슬점(°C)
    double heatIndex = NAN;// 체감온도(°C)
    bool   spike = false;

    // 추가: 공조/쾌적 지표
    double absHumidity = NAN; // 절대습도 (g/m^3)
    double wbgt = NAN;        // WBGT (근사, °C)
    double pmv = NAN;         // PMV (-3 ~ +3)
    double ppd = NAN;         // PPD (%)
};

struct Config {
	std::string id = "eeum-mfc";
	std::string host = "43.201.62.254";
	int         port = 8883;
	int         keepalive = 60;

	// 인증
	std::string user = "eeum";
	std::string pass = "ssafy2086eeum";

	// TLS
	std::string caFile = CaPathFromExe("broker_selfsigned_ca.crt");          // 서버 CA(자체서명이라면 서버 cert 자체를 넣어도 됨)
	std::string clientCertFile;  // mTLS 필요 시
	std::string clientKeyFile;   // mTLS 필요 시
	bool        tlsInsecure = true; // 호스트명 검증 off (테스트용)
};

struct MosqInitGuard {
	MosqInitGuard() { mosqpp::lib_init(); }   // 항상 먼저
	~MosqInitGuard() { /* mosqpp::lib_cleanup();  <- 앱 종료에서 호출 */ }
};