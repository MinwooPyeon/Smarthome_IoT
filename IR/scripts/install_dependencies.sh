#!/bin/bash

# Raspberry Pi IR Remote Control System - 의존성 설치 스크립트
# 일반 가전제품 제어를 위한 모든 필요한 패키지를 설치합니다.

set -e  # 오류 발생 시 스크립트 중단

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수들
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 제목 출력
echo -e "${BLUE}"
echo "=================================================="
echo "  Raspberry Pi IR Remote Control System"
echo "  의존성 설치 스크립트"
echo "=================================================="
echo -e "${NC}"

# 시스템 체크
log_info "시스템 정보 확인 중..."
log_info "OS: $(lsb_release -d | cut -f2)"
log_info "Architecture: $(uname -m)"
log_info "Kernel: $(uname -r)"

# 루트 권한 확인
if [[ $EUID -ne 0 ]]; then
   log_error "이 스크립트는 루트 권한으로 실행해야 합니다."
   log_info "sudo $0 명령어로 실행하세요."
   exit 1
fi

# 시스템 업데이트
log_info "시스템 패키지 업데이트 중..."
apt update && apt upgrade -y
log_success "시스템 업데이트 완료"

# 기본 도구 설치
log_info "기본 도구 설치 중..."
apt install -y \
    build-essential \
    cmake \
    pkg-config \
    git \
    curl \
    wget \
    vim \
    htop \
    tree
log_success "기본 도구 설치 완료"

# C++ 개발 도구 설치
log_info "C++ 개발 도구 설치 중..."
apt install -y \
    g++ \
    gcc \
    make \
    libtool \
    autoconf \
    automake
log_success "C++ 개발 도구 설치 완료"

# 필수 라이브러리 설치
log_info "필수 라이브러리 설치 중..."
apt install -y \
    nlohmann-json3-dev \
    libjsoncpp-dev \
    libmosquitto-dev \
    libssl-dev \
    libboost-all-dev \
    libpthread-stubs0-dev \
    libcurl4-openssl-dev
log_success "필수 라이브러리 설치 완료"

# CrowCpp 설치 (C++ 웹 프레임워크)
log_info "CrowCpp 웹 프레임워크 설치 중..."
if ! pkg-config --exists crow; then
    log_info "CrowCpp를 소스에서 빌드 중..."
    cd /tmp
    git clone https://github.com/CrowCpp/Crow.git
    cd Crow
    mkdir build && cd build
    cmake .. -DCROW_BUILD_EXAMPLES=OFF -DCROW_BUILD_TESTS=OFF
    make -j$(nproc)
    make install
    ldconfig
    cd /
    rm -rf /tmp/Crow
    log_success "CrowCpp 설치 완료"
else
    log_success "CrowCpp가 이미 설치되어 있습니다."
fi

# LIRC 설치 (Linux Infrared Remote Control)
log_info "LIRC 설치 중..."
apt install -y \
    lirc \
    lirc-x \
    lirc-extra \
    ir-keytable
log_success "LIRC 설치 완료"

# LIRC 커널 모듈 활성화
log_info "LIRC 커널 모듈 설정 중..."
if ! grep -q "dtoverlay=gpio-ir" /boot/config.txt; then
    echo "dtoverlay=gpio-ir-tx,gpio_pin=23" >> /boot/config.txt
    echo "dtoverlay=gpio-ir,gpio_pin=22" >> /boot/config.txt
    log_success "LIRC GPIO 설정이 /boot/config.txt에 추가되었습니다."
    log_warning "재부팅이 필요합니다."
else
    log_success "LIRC GPIO 설정이 이미 구성되어 있습니다."
fi

# MQTT 브로커 설치 (선택사항)
log_info "MQTT 브로커 설치 중..."
apt install -y mosquitto mosquitto-clients
systemctl enable mosquitto
systemctl start mosquitto
log_success "MQTT 브로커 설치 및 시작 완료"

# 방화벽 설정
log_info "방화벽 설정 중..."
if command -v ufw &> /dev/null; then
    ufw allow 22/tcp    # SSH
    ufw allow 80/tcp    # HTTP
    ufw allow 443/tcp   # HTTPS
    ufw allow 1883/tcp  # MQTT
    ufw allow 9090/tcp  # API
    ufw allow 8080/tcp  # Web UI
    log_success "방화벽 규칙 설정 완료"
else
    log_warning "ufw가 설치되어 있지 않습니다. 방화벽 설정을 수동으로 해주세요."
fi

# 필요한 디렉토리 생성
log_info "필요한 디렉토리 생성 중..."
mkdir -p /var/log/irremote
mkdir -p /etc/irremote
mkdir -p /var/lib/irremote
chown -R pi:pi /var/log/irremote /etc/irremote /var/lib/irremote 2>/dev/null || true
log_success "디렉토리 생성 완료"

# 기본 설정 파일 생성
log_info "기본 설정 파일 생성 중..."
cat > /etc/irremote/config.json << 'EOF'
{
  "token": "your_secret_token_here",
  "port": 9090,
  "webui_port": 8080,
  "mqtt": {
    "enabled": true,
    "broker": "localhost",
    "port": 1883,
    "username": "",
    "password": "",
    "client_id": "irremote_pi"
  },
  "hardware": {
    "ir_tx_pin": 23,
    "ir_rx_pin": 22,
    "pir_pin": 17,
    "pir_sensitivity": 5,
    "pir_threshold": 1000
  },
  "logging": {
    "level": "info",
    "file": "/var/log/irremote/irremote.log",
    "max_size": "10MB",
    "max_files": 5
  }
}
EOF
log_success "기본 설정 파일 생성 완료"

# LIRC 설정 파일 생성
log_info "LIRC 설정 파일 생성 중..."
cat > /etc/lirc/lircd.conf.d/general.conf << 'EOF'
# 일반 가전제품용 LIRC 설정
begin remote
  name  general_appliances
  bits           32
  flags SPACE_ENC|CONST_LENGTH
  eps            30
  aeps          100

  header       9000  4500
  one           562   1688
  zero          562    562
  ptrail        562
  repeat       9000  2250
  pre_data_bits   16
  pre_data       0x20DF
  gap          108000
  toggle_bit_mask 0x0

      begin codes
          KEY_POWER                0x10EF
          KEY_VOLUMEUP            0x40BF
          KEY_VOLUMEDOWN          0xC03F
          KEY_MUTE                0x906F
          KEY_CHANNELUP           0x00FF
          KEY_CHANNELDOWN         0x807F
          KEY_UP                  0x06F9
          KEY_DOWN                0x8679
          KEY_LEFT                0xA659
          KEY_RIGHT               0x469B
          KEY_ENTER               0x16E9
          KEY_RETURN              0x1AE5
          KEY_HOME                0x9E61
          KEY_MENU                0x58A7
          KEY_1                   0x8877
          KEY_2                   0x48B7
          KEY_3                   0xC837
          KEY_4                   0x28D7
          KEY_5                   0xA857
          KEY_6                   0x6897
          KEY_7                   0x18E7
          KEY_8                   0x9867
          KEY_9                   0x58A7
          KEY_0                   0x08F7
      end codes
end remote
EOF
log_success "LIRC 설정 파일 생성 완료"

# 서비스 파일 생성
log_info "systemd 서비스 파일 생성 중..."
cat > /etc/systemd/system/irremote.service << 'EOF'
[Unit]
Description=Raspberry Pi IR Remote Control System
After=network.target lircd.service
Wants=lircd.service

[Service]
Type=simple
User=pi
Group=pi
WorkingDirectory=/home/pi/rpi-ir-remote-master
ExecStart=/home/pi/rpi-ir-remote-master/build/irremote_server -c /etc/irremote/config.json /home/pi/rpi-ir-remote-master/remotes/
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
log_success "systemd 서비스 파일 생성 완료"

# 권한 설정
log_info "권한 설정 중..."
chmod 644 /etc/systemd/system/irremote.service
chmod 644 /etc/irremote/config.json
chmod 644 /etc/lirc/lircd.conf.d/general.conf
log_success "권한 설정 완료"

# 서비스 리로드
log_info "systemd 서비스 리로드 중..."
systemctl daemon-reload
log_success "systemd 서비스 리로드 완료"

# 필수 명령어 확인
log_info "필수 명령어 확인 중..."
local commands=("cmake" "pkg-config" "mosquitto_pub" "irsend")
local missing_commands=()

for cmd in "${commands[@]}"; do
    if ! command -v "$cmd" &> /dev/null; then
        missing_commands+=("$cmd")
    fi
done

if [ ${#missing_commands[@]} -eq 0 ]; then
    log_success "모든 필수 명령어가 설치되었습니다."
else
    log_warning "다음 명령어가 설치되지 않았습니다: ${missing_commands[*]}"
fi

# 라이브러리 확인
log_info "라이브러리 확인 중..."
if pkg-config --exists nlohmann_json; then
    log_success "nlohmann/json 라이브러리 확인됨"
else
    log_error "nlohmann/json 라이브러리를 찾을 수 없습니다."
fi

if pkg-config --exists libmosquitto; then
    log_success "Mosquitto 라이브러리 확인됨"
else
    log_error "Mosquitto 라이브러리를 찾을 수 없습니다."
fi

# 완료 메시지
echo -e "${GREEN}"
echo "=================================================="
echo "  설치 완료!"
echo "=================================================="
echo -e "${NC}"

log_success "Raspberry Pi IR Remote Control System 의존성 설치가 완료되었습니다!"
log_info ""
log_info "다음 단계:"
log_info "1. 프로젝트 빌드: cd rpi-ir-remote-master && mkdir build && cd build && cmake .. && make"
log_info "2. 서비스 활성화: sudo systemctl enable irremote.service"
log_info "3. 서비스 시작: sudo systemctl start irremote.service"
log_info "4. 재부팅 (LIRC 설정 적용을 위해): sudo reboot"
log_info ""
log_info "설정 파일 위치:"
log_info "- 시스템 설정: /etc/irremote/config.json"
log_info "- LIRC 설정: /etc/lirc/lircd.conf.d/"
log_info "- 서비스 파일: /etc/systemd/system/irremote.service"
log_info ""
log_warning "LIRC GPIO 설정을 적용하려면 재부팅이 필요합니다."
log_warning "재부팅 후 irsend 명령어로 테스트해보세요."
