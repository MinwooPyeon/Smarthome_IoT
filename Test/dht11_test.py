import time
import board
import adafruit_dht

# BCM17(물리 11) 사용. Ubuntu의 Linux SBC에선 pulseio가 없어 use_pulseio=False가 안전
dht = adafruit_dht.DHT11(board.D17, use_pulseio=False)

print("DHT11 (Adafruit) on BCM17, start...")
try:
    for i in range(20):
        try:
            t = dht.temperature   # °C
            h = dht.humidity      # %RH
            # 값이 None일 수 있어 체크
            if t is not None and h is not None:
                print(f"[{i:02d}] T={t:.1f}°C  H={h:.1f}%")
            else:
                print(f"[{i:02d}] Retry (None)")
        except RuntimeError as e:
            # 센서 특성상 자주 나는 예외 → 잠깐 대기 후 재시도
            print(f"[{i:02d}] Retry ({e})")
        time.sleep(2)  # 1초 이상 간격 권장
finally:
    dht.exit()

