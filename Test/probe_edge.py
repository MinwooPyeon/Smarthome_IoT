# dht11_probe_edges_strict.py
import time, pigpio
HOST, PORT, GPIO = '127.0.0.1', 8888, 17

pi = pigpio.pi(HOST, PORT); assert pi.connected
pi.set_mode(GPIO, pigpio.INPUT); pi.set_pull_up_down(GPIO, pigpio.PUD_UP)

edges = []
last_tick = None
def cbf(g, level, tick):
    global last_tick
    if last_tick is None:
        last_tick = tick; return
    dt = pigpio.tickDiff(last_tick, tick)
    last_tick = tick
    edges.append((level, dt))

cb = pi.callback(GPIO, pigpio.EITHER_EDGE, cbf)

# 호스트 스타트: 18ms Low → Input
pi.set_mode(GPIO, pigpio.OUTPUT); pi.write(GPIO, 0); time.sleep(0.018)
pi.set_mode(GPIO, pigpio.INPUT);  pi.set_pull_up_down(GPIO, pigpio.PUD_UP)

time.sleep(0.12)  # 120ms 수집(충분히 늘려서 확인)
cb.cancel()

print("edge count:", len(edges))
print("first 30 edges:", edges[:30])
pi.stop()

