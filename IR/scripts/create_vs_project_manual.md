# Visual Studio에서 디버그 설정 가이드

## 방법 1: Visual Studio에서 직접 프로젝트 생성

### 1. Visual Studio 열기

- Visual Studio 2019 또는 2022를 실행합니다.

### 2. 새 프로젝트 생성

- `File` → `New` → `Project`
- `Visual C++` → `Windows Desktop` → `Windows Console Application` 선택
- 프로젝트 이름: `IRRemoteController`
- 위치: 현재 프로젝트 폴더 선택

### 3. 프로젝트 설정

- `Project` → `Properties`
- `Configuration Properties` → `C/C++` → `General`
  - `Additional Include Directories`에 다음 추가:
    ```
    header
    external
    src
    ```
- `Configuration Properties` → `C/C++` → `Preprocessor`
  - `Preprocessor Definitions`에 다음 추가:
    ```
    PLATFORM_WINDOWS
    DEBUG_MODE=1
    VERBOSE_LOGGING=1
    ```

### 4. 소스 파일 추가

- `Solution Explorer`에서 `Source Files` 우클릭
- `Add` → `Existing Item`
- 다음 파일들을 추가:
  ```
  src/config.cpp
  src/irsend.cpp
  src/ir_receiver.cpp
  src/mqtt_client.cpp
  src/mqtt_message.cpp
  src/appliance_controller.cpp
  src/generic_device.cpp
  src/remote.cpp
  src/serial_controller.cpp
  src/ir_learner.cpp
  src/pir_sensor.cpp
  src/matter_client.cpp
  src/esp32_ir_receiver.cpp
  src/esp32_ir_store.cpp
  src/esp32_wifi_mqtt.cpp
  tests/test_simulation.cpp
  ```

### 5. 헤더 파일 추가

- `Solution Explorer`에서 `Header Files` 우클릭
- `Add` → `Existing Item`
- `header` 폴더의 모든 `.h` 파일들을 추가

### 6. 메인 함수 설정

- `tests/test_simulation.cpp`를 메인 소스 파일로 사용
- 또는 `src` 폴더의 파일들 중 하나를 메인으로 설정

## 방법 2: CMake 설치 후 자동 생성

### 1. CMake 설치

- [CMake 공식 사이트](https://cmake.org/download/)에서 다운로드
- Windows Installer 선택하여 설치

### 2. Visual Studio Build Tools 설치

- [Visual Studio Build Tools](https://visualstudio.microsoft.com/downloads/#build-tools-for-visual-studio-2022) 다운로드
- C++ 빌드 도구 포함하여 설치

### 3. 프로젝트 생성

```cmd
mkdir build
cd build
cmake -G "Visual Studio 17 2022" -A x64 ..
```

### 4. Visual Studio에서 열기

- `build/IRRemoteController.sln` 파일을 Visual Studio로 열기

## 디버그 설정

### 1. 환경변수 설정

- `Project` → `Properties` → `Debugging`
- `Environment`에 다음 추가:
  ```
  WIFI_SSID=your_wifi_ssid
  WIFI_PASSWORD=your_wifi_password
  MQTT_BROKER=192.168.1.100
  MQTT_PORT=1883
  MQTT_CLIENT_ID=ir_remote_controller_debug
  ```

### 2. 디버그 시작

- `F5` 키를 눌러 디버그 시작
- 또는 `Debug` → `Start Debugging`

## 테스트 실행

### 1. 단위 테스트

- `unit_tests` 프로젝트를 시작 프로젝트로 설정
- `F5`로 디버그 실행

### 2. 통합 테스트

- `integration_tests` 프로젝트를 시작 프로젝트로 설정
- `F5`로 디버그 실행

## 문제 해결

### 1. 컴파일 오류

- `#include` 경로 확인
- 헤더 파일이 올바른 위치에 있는지 확인

### 2. 링크 오류

- 필요한 라이브러리가 모두 포함되었는지 확인
- Windows에서는 추가 라이브러리가 필요하지 않음 (시뮬레이션 모드)

### 3. 런타임 오류

- 환경변수가 올바르게 설정되었는지 확인
- 디버그 모드에서 실행 중인지 확인
