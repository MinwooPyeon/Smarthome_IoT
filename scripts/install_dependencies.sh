#!/bin/bash

# Raspberry Pi IR Remote Control 의존성 설치 스크립트
# 이 스크립트는 필요한 모든 의존성을 설치합니다.

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
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

# 시스템 확인
check_system() {
    log_info "시스템 정보 확인 중..."
    
    # Raspberry Pi 확인
    if [[ ! -f /proc/device-tree/model ]] || ! grep -q "Raspberry Pi" /proc/device-tree/model; then
        log_warning "이 스크립트는 Raspberry Pi용으로 설계되었습니다."
        read -p "계속하시겠습니까? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # OS 확인
    if [[ ! -f /etc/os-release ]]; then
        log_error "OS 정보를 확인할 수 없습니다."
        exit 1
    fi
    
    source /etc/os-release
    log_info "OS: $PRETTY_NAME"
    
    # 아키텍처 확인
    ARCH=$(uname -m)
    log_info "아키텍처: $ARCH"
    
    # 메모리 확인
    MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $2/1024}')
    log_info "메모리: ${MEMORY}GB"
    
    if [[ $MEMORY -lt 1 ]]; then
        log_warning "메모리가 1GB 미만입니다. 성능에 영향을 줄 수 있습니다."
    fi
}

# 패키지 목록 업데이트
update_packages() {
    log_info "패키지 목록 업데이트 중..."
    sudo apt update
    log_success "패키지 목록 업데이트 완료"
}

# 기본 개발 도구 설치
install_basic_tools() {
    log_info "기본 개발 도구 설치 중..."
    
    sudo apt install -y \
        build-essential \
        cmake \
        pkg-config \
        git \
        curl \
        wget \
        unzip \
        vim \
        htop \
        tree
    
    log_success "기본 개발 도구 설치 완료"
}

# C++ 라이브러리 설치
install_cpp_libraries() {
    log_info "C++ 라이브러리 설치 중..."
    
    sudo apt install -y \
        nlohmann-json3-dev \
        libjsoncpp-dev \
        libmosquitto-dev \
        libssl-dev \
        libboost-all-dev \
        libcurl4-openssl-dev \
        libyaml-cpp-dev
    
    log_success "C++ 라이브러리 설치 완료"
}

# Crow 프레임워크 설치
install_crow() {
    log_info "Crow 프레임워크 설치 중..."
    
    if [[ -d "/usr/local/include/crow" ]]; then
        log_info "Crow가 이미 설치되어 있습니다."
        return
    fi
    
    # 임시 디렉토리 생성
    TEMP_DIR=$(mktemp -d)
    cd "$TEMP_DIR"
    
    # Crow 다운로드
    git clone https://github.com/CrowCpp/Crow.git
    cd Crow
    
    # 헤더 파일 복사
    sudo cp -r include/crow /usr/local/include/
    
    # 정리
    cd /
    rm -rf "$TEMP_DIR"
    
    log_success "Crow 프레임워크 설치 완료"
}

# LIRC 설치 및 설정
install_lirc() {
    log_info "LIRC 설치 중..."
    
    sudo apt install -y lirc
    
    log_info "LIRC 설정 중..."
    
    # LIRC 설정 파일 백업
    if [[ -f /etc/lirc/lirc_options.conf ]]; then
        sudo cp /etc/lirc/lirc_options.conf /etc/lirc/lirc_options.conf.backup
    fi
    
    # LIRC 설정 수정
    sudo tee /etc/lirc/lirc_options.conf > /dev/null <<EOF
[lircd]
nodaemon        = False
driver          = default
device          = /dev/lirc0
output          = /var/run/lirc/lircd
pidfile         = /var/run/lirc/lircd.pid
plugindir       = /usr/lib/arm-linux-gnueabihf/lirc/plugins
EOF
    
    log_success "LIRC 설치 및 설정 완료"
}

# 커널 모듈 설정
setup_kernel_modules() {
    log_info "커널 모듈 설정 중..."
    
    # config.txt 백업
    if [[ -f /boot/config.txt ]]; then
        sudo cp /boot/config.txt /boot/config.txt.backup
    fi
    
    # IR 모듈 설정 추가
    if ! grep -q "gpio-ir-tx" /boot/config.txt; then
        echo "" | sudo tee -a /boot/config.txt
        echo "# IR Remote Control 설정" | sudo tee -a /boot/config.txt
        echo "dtoverlay=gpio-ir-tx,gpio_pin=23" | sudo tee -a /boot/config.txt
        echo "dtoverlay=gpio-ir,gpio_pin=22" | sudo tee -a /boot/config.txt
    fi
    
    log_success "커널 모듈 설정 완료"
}

# MQTT 브로커 설치 (선택사항)
install_mqtt_broker() {
    log_info "MQTT 브로커 설치 중..."
    
    sudo apt install -y mosquitto mosquitto-clients
    
    # Mosquitto 설정
    sudo tee /etc/mosquitto/mosquitto.conf > /dev/null <<EOF
# 기본 포트
port 1883

# 로그 설정
log_type all
log_timestamp true

# 보안 설정 (기본적으로 익명 접근 허용)
allow_anonymous true

# 지속성 설정
persistence true
persistence_location /var/lib/mosquitto/

# 로그 파일
log_dest file /var/log/mosquitto/mosquitto.log
EOF
    
    # 로그 디렉토리 생성
    sudo mkdir -p /var/log/mosquitto
    sudo chown mosquitto:mosquitto /var/log/mosquitto
    
    # 서비스 활성화
    sudo systemctl enable mosquitto
    sudo systemctl start mosquitto
    
    log_success "MQTT 브로커 설치 완료"
}

# 방화벽 설정
setup_firewall() {
    log_info "방화벽 설정 중..."
    
    # UFW 설치
    sudo apt install -y ufw
    
    # 기본 정책 설정
    sudo ufw default deny incoming
    sudo ufw default allow outgoing
    
    # SSH 허용
    sudo ufw allow ssh
    
    # 웹 서버 포트 허용
    sudo ufw allow 8080
    
    # MQTT 포트 허용
    sudo ufw allow 1883
    
    # 방화벽 활성화
    sudo ufw --force enable
    
    log_success "방화벽 설정 완료"
}

# 디렉토리 생성
create_directories() {
    log_info "필요한 디렉토리 생성 중..."
    
    sudo mkdir -p /etc/irremote
    sudo mkdir -p /var/log/irremote
    sudo mkdir -p /etc/lirc/lircd.conf.d
    
    # 권한 설정
    sudo chown pi:pi /var/log/irremote
    
    log_success "디렉토리 생성 완료"
}

# 기본 설정 파일 생성
create_default_config() {
    log_info "기본 설정 파일 생성 중..."
    
    sudo tee /etc/irremote/config.json > /dev/null <<EOF
{
  "web_server": {
    "port": 8080,
    "host": "0.0.0.0",
    "enabled": true
  },
  "mqtt": {
    "broker": "localhost",
    "port": 1883,
    "enabled": true,
    "client_id": "irremote_client",
    "topic_prefix": "irremote"
  },
  "security": {
    "api_token": "",
    "api_token_required": false,
    "allowed_origins": ["*"]
  },
  "ir": {
    "device": "/dev/lirc0",
    "timeout_ms": 5000,
    "retry_count": 3
  },
  "logging": {
    "level": "INFO",
    "file": "/var/log/irremote.log",
    "to_file": false
  }
}
EOF
    
    log_success "기본 설정 파일 생성 완료"
}

# 설치 확인
verify_installation() {
    log_info "설치 확인 중..."
    
    # 필수 명령어 확인
    local commands=("cmake" "pkg-config" "mosquitto_pub" "irsend")
    local missing_commands=()
    
    for cmd in "${commands[@]}"; do
        if ! command -v "$cmd" &> /dev/null; then
            missing_commands+=("$cmd")
        fi
    done
    
    if [[ ${#missing_commands[@]} -gt 0 ]]; then
        log_warning "다음 명령어를 찾을 수 없습니다: ${missing_commands[*]}"
    else
        log_success "모든 필수 명령어가 설치되었습니다."
    fi
    
    # 라이브러리 확인
    if pkg-config --exists nlohmann_json; then
        log_success "nlohmann/json 라이브러리 확인됨"
    else
        log_error "nlohmann/json 라이브러리를 찾을 수 없습니다."
    fi
    
    if pkg-config --exists jsoncpp; then
        log_success "JsonCpp 라이브러리 확인됨"
    else
        log_error "JsonCpp 라이브러리를 찾을 수 없습니다."
    fi
    
    if pkg-config --exists libmosquitto; then
        log_success "Mosquitto 라이브러리 확인됨"
    else
        log_error "Mosquitto 라이브러리를 찾을 수 없습니다."
    fi
    
    # 서비스 상태 확인
    if systemctl is-active --quiet mosquitto; then
        log_success "Mosquitto 서비스가 실행 중입니다."
    else
        log_warning "Mosquitto 서비스가 실행되지 않고 있습니다."
    fi
}

# 메인 함수
main() {
    echo "=========================================="
    echo "Raspberry Pi IR Remote Control"
    echo "의존성 설치 스크립트"
    echo "=========================================="
    echo
    
    # 시스템 확인
    check_system
    
    # 패키지 업데이트
    update_packages
    
    # 기본 도구 설치
    install_basic_tools
    
    # C++ 라이브러리 설치
    install_cpp_libraries
    
    # Crow 프레임워크 설치
    install_crow
    
    # LIRC 설치
    install_lirc
    
    # 커널 모듈 설정
    setup_kernel_modules
    
    # MQTT 브로커 설치
    install_mqtt_broker
    
    # 방화벽 설정
    setup_firewall
    
    # 디렉토리 생성
    create_directories
    
    # 기본 설정 파일 생성
    create_default_config
    
    # 설치 확인
    verify_installation
    
    echo
    echo "=========================================="
    log_success "의존성 설치가 완료되었습니다!"
    echo "=========================================="
    echo
    echo "다음 단계:"
    echo "1. 시스템을 재부팅하세요: sudo reboot"
    echo "2. 애플리케이션을 빌드하세요: ./build.sh"
    echo "3. 서비스를 시작하세요: sudo systemctl start irremote.service"
    echo
    echo "재부팅이 필요합니다. 지금 재부팅하시겠습니까? (y/N): "
    read -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        sudo reboot
    fi
}

# 스크립트 실행
main "$@"
