import serial
import json
import time
import sys

def send_raw_data_test(ser, raw_data, device_type="test_device", function="test_function"):
    """ESP32로 raw_data 테스트 명령 전송"""
    command = {
        "command": "test_raw_data",
        "params": {
            "raw_data": raw_data,
            "device_type": device_type,
            "function": function
        }
    }

    json_str = json.dumps(command)
    print(f"전송: {json_str}")

    # ESP32로 전송
    ser.write((json_str + '\n').encode())

    # 응답 대기
    time.sleep(2)

    # 응답 읽기
    if ser.in_waiting > 0:
        response = ser.readline().decode().strip()
        print(f"응답: {response}")

        # JSON 응답 파싱
        try:
            response_json = json.loads(response)
            if response_json.get('success'):
                print(f"성공! 소요시간: {response_json.get('duration_ms', 0)}ms")
                print(f"Raw 데이터 개수: {response_json.get('raw_data_count', 0)}")
                print(f"Hex 데이터: {response_json.get('hex_data', '')}")
            else:
                print("실패")
        except json.JSONDecodeError:
            print("JSON 파싱 실패")
    else:
        print("응답 없음")

def main():
    print("=== ESP32 IR Raw Data 테스트 ===")
    print("ESP32가 연결된 시리얼 포트를 입력하세요")
    print("예시: COM3 (Windows), /dev/ttyUSB0 (Linux/Mac)")

    # 시리얼 포트 입력
    port = input("시리얼 포트: ").strip()
    if not port:
        print("포트를 입력해주세요")
        return

    try:
        # 시리얼 연결
        ser = serial.Serial(port, 115200, timeout=1)
        print(f"{port} 포트에 연결됨")
        time.sleep(2)  # ESP32 초기화 대기

        print("\n=== 사용법 ===")
        print("1. Raw data를 쉼표로 구분하여 입력 (예: 9000,4500,560,560)")
        print("2. Device type과 function은 선택사항")
        print("3. 종료하려면 'quit' 입력")
        print("4. 예시 명령어들:")
        print("   - 삼성 TV 전원: 9000,4500,560,560,560,560,560,560,560,560,560,1680")
        print("   - 간단한 테스트: 1000,500,1000,500")

        while True:
            try:
                print("\n" + "="*50)

                # raw_data 입력
                raw_input = input("Raw data 입력: ").strip()
                if raw_input.lower() == 'quit':
                    break

                if not raw_input:
                    print("Raw data를 입력해주세요")
                    continue

                # 쉼표로 구분된 숫자들을 리스트로 변환
                try:
                    raw_data = [int(x.strip()) for x in raw_input.split(',')]
                except ValueError:
                    print("오류: 숫자만 입력하세요 (예: 9000,4500,560,560)")
                    continue

                # device_type 입력
                device_type = input("Device type (기본값: test_device): ").strip()
                if not device_type:
                    device_type = "test_device"

                # function 입력
                function = input("Function (기본값: test_function): ").strip()
                if not function:
                    function = "test_function"

                # 테스트 실행
                print(f"\n테스트 시작...")
                send_raw_data_test(ser, raw_data, device_type, function)

            except KeyboardInterrupt:
                print("\n\n사용자가 중단함")
                break
            except Exception as e:
                print(f"오류 발생: {e}")

    except serial.SerialException as e:
        print(f"시리얼 포트 연결 실패: {e}")
        print("포트 번호를 확인하고 ESP32가 연결되어 있는지 확인하세요")
    except Exception as e:
        print(f"예상치 못한 오류: {e}")
    finally:
        if 'ser' in locals():
            ser.close()
            print("시리얼 연결 종료")
        print("테스트 종료")

if __name__ == "__main__":
    main()
