#!/usr/bin/env python3
"""
ESP32 IR Remote Controller
USB-C to USB-C 연결을 통해 ESP32를 제어하는 Python 클라이언트
"""

import serial
import json
import time
import sys
import threading
from typing import Dict, Any, Optional, Callable

class ESP32Controller:
    def __init__(self, port: str = None, baud_rate: int = 115200):
        """
        ESP32 컨트롤러 초기화
        
        Args:
            port: 시리얼 포트 (None이면 자동 검색)
            baud_rate: 시리얼 통신 속도
        """
        self.port = port
        self.baud_rate = baud_rate
        self.serial_conn: Optional[serial.Serial] = None
        self.connected = False
        self.response_callbacks: Dict[str, Callable] = {}
        self.running = False
        self.receive_thread: Optional[threading.Thread] = None
        
    def find_esp32_port(self) -> Optional[str]:
        """ESP32 포트 자동 검색"""
        import serial.tools.list_ports
        
        for port in serial.tools.list_ports.comports():
            # ESP32는 보통 USB-SERIAL CH340, CP2102, 또는 Silicon Labs 드라이버 사용
            if any(keyword in port.description.lower() for keyword in 
                   ['ch340', 'cp2102', 'silicon labs', 'usb serial', 'esp32']):
                print(f"ESP32 포트 발견: {port.device} - {port.description}")
                return port.device
        
        # 포트를 찾지 못한 경우 일반적인 포트들 확인
        common_ports = ['COM3', 'COM4', 'COM5', 'COM6', '/dev/ttyUSB0', '/dev/ttyACM0']
        for port in common_ports:
            try:
                test_conn = serial.Serial(port, self.baud_rate, timeout=1)
                test_conn.close()
                print(f"사용 가능한 포트 발견: {port}")
                return port
            except:
                continue
                
        return None
    
    def connect(self) -> bool:
        """ESP32에 연결"""
        if self.connected:
            return True
            
        if not self.port:
            self.port = self.find_esp32_port()
            if not self.port:
                print("ESP32 포트를 찾을 수 없습니다.")
                return False
        
        try:
            self.serial_conn = serial.Serial(
                self.port, 
                self.baud_rate, 
                timeout=1,
                write_timeout=1
            )
            
            # 연결 대기
            time.sleep(2)
            
            # 초기화 메시지 확인
            init_message = self.serial_conn.readline().decode('utf-8').strip()
            if init_message:
                try:
                    init_data = json.loads(init_message)
                    if init_data.get('type') == 'init':
                        print(f"ESP32 연결 성공: {init_data.get('device_id', 'Unknown')}")
                        self.connected = True
                        self.start_receive_thread()
                        return True
                except json.JSONDecodeError:
                    pass
            
            print("ESP32 초기화 메시지를 받지 못했습니다.")
            return False
            
        except Exception as e:
            print(f"ESP32 연결 실패: {e}")
            return False
    
    def disconnect(self):
        """ESP32 연결 해제"""
        self.running = False
        if self.receive_thread:
            self.receive_thread.join(timeout=2)
        
        if self.serial_conn and self.serial_conn.is_open:
            self.serial_conn.close()
        
        self.connected = False
        print("ESP32 연결 해제됨")
    
    def start_receive_thread(self):
        """수신 스레드 시작"""
        self.running = True
        self.receive_thread = threading.Thread(target=self._receive_loop, daemon=True)
        self.receive_thread.start()
    
    def _receive_loop(self):
        """수신 루프 (별도 스레드에서 실행)"""
        while self.running and self.connected:
            try:
                if self.serial_conn and self.serial_conn.in_waiting:
                    line = self.serial_conn.readline().decode('utf-8').strip()
                    if line:
                        self._handle_response(line)
                time.sleep(0.01)
            except Exception as e:
                print(f"수신 오류: {e}")
                break
    
    def _handle_response(self, response: str):
        """응답 처리"""
        try:
            data = json.loads(response)
            msg_type = data.get('type', '')
            
            if msg_type == 'response':
                command = data.get('command', '')
                result = data.get('result', '')
                print(f"[{command}] {result}")
                
            elif msg_type == 'error':
                error_code = data.get('error_code', '')
                message = data.get('message', '')
                print(f"[오류] {error_code}: {message}")
                
            elif msg_type == 'ir_received':
                ir_code = data.get('ir_code', '')
                timestamp = data.get('timestamp', '')
                print(f"[IR 수신] 코드: {ir_code}, 시간: {timestamp}")
                
            elif msg_type == 'status':
                status_data = data.get('data', {})
                print(f"[상태] {status_data}")
                
            else:
                print(f"[수신] {response}")
                
        except json.JSONDecodeError:
            print(f"[원시 데이터] {response}")
    
    def send_command(self, command: str, params: Dict[str, Any] = None) -> bool:
        """명령 전송"""
        if not self.connected or not self.serial_conn:
            print("ESP32에 연결되지 않았습니다.")
            return False
        
        try:
            message = {
                "command": command,
                "params": params or {}
            }
            
            json_str = json.dumps(message, ensure_ascii=False)
            self.serial_conn.write((json_str + '\n').encode('utf-8'))
            self.serial_conn.flush()
            return True
            
        except Exception as e:
            print(f"명령 전송 실패: {e}")
            return False
    
    def ping(self) -> bool:
        """핑 테스트"""
        return self.send_command("ping")
    
    def get_status(self) -> bool:
        """상태 정보 요청"""
        return self.send_command("status")
    
    def get_wifi_info(self) -> bool:
        """WiFi 정보 요청"""
        return self.send_command("wifi_info")
    
    def get_device_list(self) -> bool:
        """가전기기 목록 요청"""
        return self.send_command("device_list")
    
    def send_ir_command(self, device_id: str, action: str) -> bool:
        """IR 명령 전송"""
        params = {
            "device_id": device_id,
            "action": action
        }
        return self.send_command("ir_send", params)
    
    def get_ir_receive_status(self) -> bool:
        """IR 수신 상태 요청"""
        return self.send_command("ir_receive")
    
    def get_mqtt_status(self) -> bool:
        """MQTT 상태 요청"""
        return self.send_command("mqtt_status")
    
    def restart_esp32(self) -> bool:
        """ESP32 재시작"""
        return self.send_command("restart")
    
    def get_help(self) -> bool:
        """도움말 요청"""
        return self.send_command("help")

def main():
    """메인 함수 - 대화형 모드"""
    print("ESP32 IR Remote Controller")
    print("=" * 40)
    
    controller = ESP32Controller()
    
    if not controller.connect():
        print("ESP32 연결에 실패했습니다.")
        return
    
    print("\n사용 가능한 명령어:")
    print("  ping - 연결 테스트")
    print("  status - 시스템 상태")
    print("  wifi - WiFi 정보")
    print("  devices - 가전기기 목록")
    print("  ir <device_id> <action> - IR 명령 전송")
    print("  ir_status - IR 수신 상태")
    print("  mqtt - MQTT 상태")
    print("  restart - ESP32 재시작")
    print("  help - 도움말")
    print("  quit - 종료")
    print()
    
    try:
        while True:
            try:
                command = input("ESP32> ").strip().lower()
                
                if command == "quit" or command == "exit":
                    break
                elif command == "ping":
                    controller.ping()
                elif command == "status":
                    controller.get_status()
                elif command == "wifi":
                    controller.get_wifi_info()
                elif command == "devices":
                    controller.get_device_list()
                elif command.startswith("ir "):
                    parts = command.split()
                    if len(parts) >= 3:
                        device_id = parts[1]
                        action = parts[2]
                        controller.send_ir_command(device_id, action)
                    else:
                        print("사용법: ir <device_id> <action>")
                elif command == "ir_status":
                    controller.get_ir_receive_status()
                elif command == "mqtt":
                    controller.get_mqtt_status()
                elif command == "restart":
                    controller.restart_esp32()
                elif command == "help":
                    controller.get_help()
                else:
                    print("알 수 없는 명령어입니다. 'help'를 입력하세요.")
                    
            except KeyboardInterrupt:
                break
            except Exception as e:
                print(f"오류: {e}")
                
    finally:
        controller.disconnect()

if __name__ == "__main__":
    main()
