#!/usr/bin/env python3
"""
ESP32 IR Remote Controller 통합 테스트
USB-C 연결을 통한 전체 시스템 기능을 테스트합니다.
"""

import serial
import json
import time
import sys
import threading
from typing import Dict, Any, List

class ESP32IntegrationTest:
    def __init__(self, port=None, baud_rate=115200):
        self.port = port
        self.baud_rate = baud_rate
        self.serial_conn = None
        self.connected = False
        self.test_results = []
        
    def connect(self) -> bool:
        """ESP32에 연결"""
        if not self.port:
            import serial.tools.list_ports
            for p in serial.tools.list_ports.comports():
                if any(keyword in p.description.lower() for keyword in 
                       ['ch340', 'cp2102', 'silicon labs', 'usb serial', 'esp32']):
                    self.port = p.device
                    break
        
        if not self.port:
            print("ESP32 포트를 찾을 수 없습니다.")
            return False
        
        try:
            self.serial_conn = serial.Serial(self.port, self.baud_rate, timeout=2)
            time.sleep(2)
            
            # 초기화 메시지 확인
            init_msg = self.serial_conn.readline().decode('utf-8').strip()
            if init_msg:
                init_data = json.loads(init_msg)
                if init_data.get('type') == 'init':
                    print(f"✓ ESP32 연결 성공: {init_data.get('device_id', 'Unknown')}")
                    self.connected = True
                    return True
            
            print("ESP32 초기화 메시지를 받지 못했습니다.")
            return False
            
        except Exception as e:
            print(f"ESP32 연결 실패: {e}")
            return False
    
    def disconnect(self):
        """연결 해제"""
        if self.serial_conn:
            self.serial_conn.close()
        self.connected = False
    
    def send_command(self, command: str, params: Dict[str, Any] = None) -> Dict[str, Any]:
        """명령 전송 및 응답 수신"""
        if not self.connected:
            return {"error": "Not connected"}
        
        try:
            message = {"command": command, "params": params or {}}
            json_str = json.dumps(message, ensure_ascii=False)
            self.serial_conn.write((json_str + '\n').encode('utf-8'))
            self.serial_conn.flush()
            
            # 응답 대기
            response = self.serial_conn.readline().decode('utf-8').strip()
            if response:
                return json.loads(response)
            else:
                return {"error": "No response"}
                
        except Exception as e:
            return {"error": str(e)}
    
    def run_test(self, test_name: str, command: str, params: Dict[str, Any] = None, expected_type: str = "response") -> bool:
        """개별 테스트 실행"""
        print(f"테스트: {test_name}")
        
        response = self.send_command(command, params)
        
        if "error" in response:
            print(f"  ✗ 실패: {response['error']}")
            self.test_results.append({"test": test_name, "result": "FAIL", "error": response['error']})
            return False
        
        if response.get('type') == expected_type:
            print(f"  ✓ 성공")
            self.test_results.append({"test": test_name, "result": "PASS"})
            return True
        else:
            print(f"  ✗ 실패: 예상 타입 {expected_type}, 실제 타입 {response.get('type')}")
            self.test_results.append({"test": test_name, "result": "FAIL", "error": f"Wrong response type"})
            return False
    
    def test_basic_commands(self) -> bool:
        """기본 명령어 테스트"""
        print("\n=== 기본 명령어 테스트 ===")
        
        tests = [
            ("핑 테스트", "ping", {}),
            ("상태 확인", "status", {}),
            ("도움말", "help", {}),
            ("WiFi 정보", "wifi_info", {}),
            ("가전기기 목록", "device_list", {}),
            ("IR 수신 상태", "ir_receive", {}),
            ("MQTT 상태", "mqtt_status", {})
        ]
        
        success_count = 0
        for test_name, command, params in tests:
            if self.run_test(test_name, command, params):
                success_count += 1
            time.sleep(0.5)
        
        print(f"\n기본 명령어 테스트 결과: {success_count}/{len(tests)} 성공")
        return success_count == len(tests)
    
    def test_ir_commands(self) -> bool:
        """IR 명령어 테스트"""
        print("\n=== IR 명령어 테스트 ===")
        
        # Samsung TV 명령어 테스트
        tv_commands = [
            ("TV 전원", "ir_send", {"device_id": "samsung_tv", "action": "power"}),
            ("TV 볼륨 업", "ir_send", {"device_id": "samsung_tv", "action": "volume_up"}),
            ("TV 볼륨 다운", "ir_send", {"device_id": "samsung_tv", "action": "volume_down"}),
            ("TV 채널 업", "ir_send", {"device_id": "samsung_tv", "action": "channel_up"}),
            ("TV 채널 다운", "ir_send", {"device_id": "samsung_tv", "action": "channel_down"})
        ]
        
        # Samsung AC 명령어 테스트
        ac_commands = [
            ("AC 전원", "ir_send", {"device_id": "samsung_ac", "action": "power"}),
            ("AC 모드", "ir_send", {"device_id": "samsung_ac", "action": "mode"}),
            ("AC 온도 18도", "ir_send", {"device_id": "samsung_ac", "action": "temp_18"}),
            ("AC 온도 업", "ir_send", {"device_id": "samsung_ac", "action": "temp_up"}),
            ("AC 온도 다운", "ir_send", {"device_id": "samsung_ac", "action": "temp_down"})
        ]
        
        all_commands = tv_commands + ac_commands
        success_count = 0
        
        for test_name, command, params in all_commands:
            if self.run_test(test_name, command, params):
                success_count += 1
            time.sleep(1)  # IR 명령 간 충분한 대기 시간
        
        print(f"\nIR 명령어 테스트 결과: {success_count}/{len(all_commands)} 성공")
        return success_count == len(all_commands)
    
    def test_error_handling(self) -> bool:
        """오류 처리 테스트"""
        print("\n=== 오류 처리 테스트 ===")
        
        error_tests = [
            ("잘못된 명령어", "invalid_command", {}),
            ("IR 명령 파라미터 누락", "ir_send", {"device_id": "samsung_tv"}),  # action 누락
            ("존재하지 않는 기기", "ir_send", {"device_id": "nonexistent_device", "action": "power"})
        ]
        
        success_count = 0
        for test_name, command, params in error_tests:
            response = self.send_command(command, params)
            
            if response.get('type') == 'error':
                print(f"  ✓ {test_name}: 오류 올바르게 처리됨")
                success_count += 1
                self.test_results.append({"test": test_name, "result": "PASS"})
            else:
                print(f"  ✗ {test_name}: 오류 처리 실패")
                self.test_results.append({"test": test_name, "result": "FAIL"})
            
            time.sleep(0.5)
        
        print(f"\n오류 처리 테스트 결과: {success_count}/{len(error_tests)} 성공")
        return success_count == len(error_tests)
    
    def test_performance(self) -> bool:
        """성능 테스트"""
        print("\n=== 성능 테스트 ===")
        
        # 연속 명령 전송 테스트
        print("연속 명령 전송 테스트 (10회)...")
        start_time = time.time()
        success_count = 0
        
        for i in range(10):
            response = self.send_command("ping")
            if response.get('type') == 'response':
                success_count += 1
        
        end_time = time.time()
        total_time = end_time - start_time
        avg_time = total_time / 10
        
        print(f"  총 시간: {total_time:.2f}초")
        print(f"  평균 응답 시간: {avg_time:.2f}초")
        print(f"  성공률: {success_count}/10")
        
        performance_ok = success_count >= 8 and avg_time < 1.0
        test_result = "PASS" if performance_ok else "FAIL"
        self.test_results.append({"test": "성능 테스트", "result": test_result})
        
        return performance_ok
    
    def run_all_tests(self) -> bool:
        """모든 테스트 실행"""
        print("ESP32 IR Remote Controller 통합 테스트")
        print("=" * 50)
        
        if not self.connect():
            return False
        
        try:
            # 각 테스트 실행
            basic_ok = self.test_basic_commands()
            ir_ok = self.test_ir_commands()
            error_ok = self.test_error_handling()
            perf_ok = self.test_performance()
            
            # 결과 요약
            print("\n" + "=" * 50)
            print("테스트 결과 요약")
            print("=" * 50)
            
            total_tests = len(self.test_results)
            passed_tests = sum(1 for result in self.test_results if result['result'] == 'PASS')
            
            print(f"총 테스트: {total_tests}")
            print(f"통과: {passed_tests}")
            print(f"실패: {total_tests - passed_tests}")
            print(f"성공률: {(passed_tests/total_tests)*100:.1f}%")
            
            print("\n상세 결과:")
            for result in self.test_results:
                status = "✓" if result['result'] == 'PASS' else "✗"
                print(f"  {status} {result['test']}")
                if 'error' in result:
                    print(f"    오류: {result['error']}")
            
            return passed_tests == total_tests
            
        finally:
            self.disconnect()

def main():
    """메인 함수"""
    port = None
    if len(sys.argv) > 1:
        port = sys.argv[1]
    
    tester = ESP32IntegrationTest(port)
    success = tester.run_all_tests()
    
    if success:
        print("\n🎉 모든 테스트가 통과했습니다!")
        print("ESP32 IR Remote Controller가 정상적으로 작동합니다.")
        sys.exit(0)
    else:
        print("\n❌ 일부 테스트가 실패했습니다.")
        print("ESP32 설정 및 연결을 확인해주세요.")
        sys.exit(1)

if __name__ == "__main__":
    main()
