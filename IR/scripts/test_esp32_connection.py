#!/usr/bin/env python3
"""
ESP32 연결 테스트 스크립트
USB-C to USB-C 연결을 통해 ESP32와의 통신을 테스트합니다.
"""

import serial
import json
import time
import sys

def test_esp32_connection(port=None, baud_rate=115200):
    """ESP32 연결 테스트"""
    print("ESP32 연결 테스트 시작...")
    print("=" * 40)
    
    # 포트 자동 검색
    if not port:
        import serial.tools.list_ports
        for p in serial.tools.list_ports.comports():
            if any(keyword in p.description.lower() for keyword in 
                   ['ch340', 'cp2102', 'silicon labs', 'usb serial', 'esp32']):
                port = p.device
                print(f"ESP32 포트 발견: {port}")
                break
    
    if not port:
        print("ESP32 포트를 찾을 수 없습니다.")
        return False
    
    try:
        # 시리얼 연결
        print(f"포트 {port}에 연결 중... (속도: {baud_rate} bps)")
        ser = serial.Serial(port, baud_rate, timeout=2)
        time.sleep(2)  # 연결 대기
        
        # 초기화 메시지 확인
        print("초기화 메시지 대기 중...")
        init_msg = ser.readline().decode('utf-8').strip()
        
        if init_msg:
            try:
                init_data = json.loads(init_msg)
                if init_data.get('type') == 'init':
                    print(f"✓ ESP32 연결 성공!")
                    print(f"  디바이스 ID: {init_data.get('device_id', 'Unknown')}")
                    print(f"  버전: {init_data.get('version', 'Unknown')}")
                else:
                    print(f"초기화 메시지 형식 오류: {init_msg}")
                    return False
            except json.JSONDecodeError:
                print(f"JSON 파싱 오류: {init_msg}")
                return False
        else:
            print("초기화 메시지를 받지 못했습니다.")
            return False
        
        # 기본 명령어 테스트
        test_commands = [
            ("ping", {}),
            ("status", {}),
            ("help", {}),
            ("wifi_info", {}),
            ("device_list", {})
        ]
        
        print("\n명령어 테스트 시작...")
        print("-" * 40)
        
        for command, params in test_commands:
            print(f"테스트: {command}")
            
            # 명령 전송
            message = {"command": command, "params": params}
            json_str = json.dumps(message, ensure_ascii=False)
            ser.write((json_str + '\n').encode('utf-8'))
            ser.flush()
            
            # 응답 대기
            response = ser.readline().decode('utf-8').strip()
            if response:
                try:
                    resp_data = json.loads(response)
                    if resp_data.get('type') == 'response':
                        print(f"  ✓ 성공: {resp_data.get('result', 'No result')[:50]}...")
                    elif resp_data.get('type') == 'error':
                        print(f"  ✗ 오류: {resp_data.get('message', 'Unknown error')}")
                    else:
                        print(f"  ? 알 수 없는 응답: {response[:50]}...")
                except json.JSONDecodeError:
                    print(f"  ? JSON 파싱 오류: {response[:50]}...")
            else:
                print(f"  ✗ 응답 없음")
            
            time.sleep(0.5)  # 명령 간 대기
        
        # IR 명령 테스트 (실제 IR 송신은 하지 않음)
        print(f"\nIR 명령 테스트...")
        ir_message = {
            "command": "ir_send",
            "params": {
                "device_id": "samsung_tv",
                "action": "power"
            }
        }
        
        json_str = json.dumps(ir_message, ensure_ascii=False)
        ser.write((json_str + '\n').encode('utf-8'))
        ser.flush()
        
        response = ser.readline().decode('utf-8').strip()
        if response:
            try:
                resp_data = json.loads(response)
                if resp_data.get('type') == 'response':
                    result = json.loads(resp_data.get('result', '{}'))
                    success = result.get('success', False)
                    print(f"  {'✓' if success else '✗'} IR 명령 테스트: {result.get('message', 'No message')}")
                else:
                    print(f"  ? IR 명령 응답 오류: {response[:50]}...")
            except json.JSONDecodeError:
                print(f"  ? IR 명령 JSON 파싱 오류: {response[:50]}...")
        
        ser.close()
        print("\n" + "=" * 40)
        print("✓ ESP32 연결 테스트 완료!")
        return True
        
    except Exception as e:
        print(f"✗ 연결 테스트 실패: {e}")
        return False

def main():
    """메인 함수"""
    port = None
    if len(sys.argv) > 1:
        port = sys.argv[1]
    
    success = test_esp32_connection(port)
    
    if success:
        print("\nESP32가 정상적으로 연결되었습니다!")
        print("이제 다음 명령으로 ESP32를 제어할 수 있습니다:")
        print("  python scripts/esp32_controller.py")
        sys.exit(0)
    else:
        print("\nESP32 연결에 실패했습니다.")
        print("다음을 확인해주세요:")
        print("  1. USB-C to USB-C 케이블 연결")
        print("  2. ESP32 전원 상태")
        print("  3. ESP32 펌웨어 업로드 상태")
        print("  4. 시리얼 포트 드라이버 설치")
        sys.exit(1)

if __name__ == "__main__":
    main()
