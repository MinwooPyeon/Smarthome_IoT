import qrcode
import os

def generate_device_qr(serial_number):
    """시리얼번호만 포함하는 QR 코드 생성"""

    # QR 코드에 시리얼번호만 포함
    qr_text = serial_number

    # QR 코드 생성
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )

    qr.add_data(qr_text)
    qr.make(fit=True)

    # QR 코드 이미지 생성
    img = qr.make_image(fill_color="black", back_color="white")

    # 파일명 생성
    filename = f"qr_code_{serial_number}.png"

    # 이미지 저장
    img.save(filename)

    print(f"✅ QR 코드 생성 완료: {filename}")
    print(f"📱 시리얼 번호: {serial_number}")

    return filename, serial_number

def main():
    """QR 코드 생성 메인 함수"""
    print("=== 시리얼번호 QR 코드 생성기 ===")

    # 시리얼 번호 입력
    serial_number = input("시리얼 번호를 입력하세요 (예: ESP32_001): ").strip()
    if not serial_number:
        serial_number = "ESP32_001"

    # QR 코드 생성
    filename, serial_number = generate_device_qr(serial_number)

    print(f"\n📋 생성된 정보:")
    print(f"   파일명: {filename}")
    print(f"   시리얼 번호: {serial_number}")
    print(f"   QR 코드 내용: {serial_number}")

if __name__ == "__main__":
    try:
        main()
        print(f"\n🎉 완료! QR 코드가 생성되었습니다.")

    except ImportError:
        print("❌ qrcode 라이브러리가 설치되지 않았습니다.")
        print("설치 명령어: pip install qrcode[pil]")
    except Exception as e:
        print(f"❌ 오류 발생: {e}")
