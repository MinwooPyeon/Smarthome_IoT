// file: dht11_test.cpp
#include <pigpio.h>
#include <unistd.h>
#include <iostream>
#include <vector>

#define DHT_PIN 4        // BCM 번호 (모듈 DATA를 GPIO4(물리 7번)에 권장)
#define RETRIES 5
// #define VERBOSE 1

static bool readDHT11_once(int pin, int &temperature, int &humidity) {
    std::vector<int> data(5, 0);

    // 대기 상태: 입력 + 내부 풀업 OFF (모듈 자체 10k 사용)
    gpioSetMode(pin, PI_INPUT);
    gpioSetPullUpDown(pin, PI_PUD_OFF);
    gpioDelay(2000); // 2ms 정도 여유

    // 1) Start signal (보수적으로)
    gpioSetMode(pin, PI_OUTPUT);
    gpioWrite(pin, 0);
    gpioDelay(20000);         // 20ms LOW
    gpioWrite(pin, 1);
    gpioDelay(30);            // 30us HIGH
    gpioSetMode(pin, PI_INPUT);
    gpioSetPullUpDown(pin, PI_PUD_OFF); // 모듈 풀업 사용

    // 2) Sensor response: 80us LOW + 80us HIGH
    // LOW로 떨어질 때까지 대기(타임아웃 가드)
    uint32_t t0 = gpioTick();
    while (gpioRead(pin) == 1) {
        if (gpioTick() - t0 > 200) { // ~200us
#ifdef VERBOSE
            std::cerr << "[resp] no initial LOW\n";
#endif
            return false;
        }
    }
    // LOW 구간 종료 대기
    t0 = gpioTick();
    while (gpioRead(pin) == 0) {
        if (gpioTick() - t0 > 200) {
#ifdef VERBOSE
            std::cerr << "[resp] LOW too long\n";
#endif
            return false;
        }
    }
    // HIGH 구간 종료 대기
    t0 = gpioTick();
    while (gpioRead(pin) == 1) {
        if (gpioTick() - t0 > 200) {
#ifdef VERBOSE
            std::cerr << "[resp] HIGH too long\n";
#endif
            return false;
        }
    }

    // 3) 40비트 수신 (각 비트: ~50us LOW + 26~28us HIGH=0, ~70us HIGH=1)
    for (int i = 0; i < 40; i++) {
        // LOW 구간 기다림
        t0 = gpioTick();
        while (gpioRead(pin) == 0) {
            if (gpioTick() - t0 > 1000) {
#ifdef VERBOSE
                std::cerr << "[bit" << i << "] wait LOW timeout\n";
#endif
                return false;
            }
        }
        // HIGH 길이 측정
        uint32_t startH = gpioTick();
        while (gpioRead(pin) == 1) {
            if (gpioTick() - startH > 120) { // 비정상적으로 긴 HIGH는 에러
#ifdef VERBOSE
                std::cerr << "[bit" << i << "] HIGH too long\n";
#endif
                return false;
            }
        }
        uint32_t highLen = gpioTick() - startH;

        int byteIndex = i / 8;
        data[byteIndex] <<= 1;
        // 임계치 50us: HIGH가 길면 1
        if (highLen > 50) data[byteIndex] |= 1;

#ifdef VERBOSE
        std::cerr << "[bit" << i << "] highLen=" << highLen
                  << " -> " << ((highLen>50)?1:0) << "\n";
#endif
    }

#ifdef VERBOSE
    std::cerr << "raw bytes: "
              << (int)data[0] << " "
              << (int)data[1] << " "
              << (int)data[2] << " "
              << (int)data[3] << " "
              << (int)data[4] << "\n";
#endif

    // 4) 체크섬
    uint8_t sum = (uint8_t)(data[0] + data[1] + data[2] + data[3]);
    if (sum != (uint8_t)data[4]) {
#ifdef VERBOSE
        std::cerr << "[chk] mismatch: " << (int)sum
                  << " != " << (int)data[4] << "\n";
#endif
        return false;
    }

    humidity    = data[0];
    temperature = data[2];
    return true;
}

int main() {
    if (gpioInitialise() < 0) {
        std::cerr << "pigpio init failed\n";
        return 1;
    }

    // 전원 인가 직후 첫 읽기 전 워밍업
    sleep(2);

    int t=0, h=0;
    bool ok = false;
    for (int r = 1; r <= RETRIES; r++) {
        if (readDHT11_once(DHT_PIN, t, h)) { ok = true; break; }
        std::cerr << "read failed (try " << r << "/" << RETRIES << ")\n";
        sleep(2); // DHT11 최소 간격
    }

    if (ok) {
        std::cout << "OK: Temp=" << t << "C Hum=" << h << "%\n";
    } else {
        std::cerr << "FAIL: timeout/no reading\n";
    }

    gpioTerminate();
    return ok ? 0 : 2;
}
