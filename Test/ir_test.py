#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Ubuntu + Raspberry Pi + VS1838B IR quick test (pigpio-based)
- Logs GPIO edges and computes pulse/space durations in microseconds.
- No kernel overlays or /dev/lirc0 required.
"""

import argparse
import time
import pigpio
from collections import deque

def main():
    ap = argparse.ArgumentParser(description="VS1838B IR receiver tester (pigpio)")
    ap.add_argument("--gpio", type=int, default=17, help="BCM GPIO number for VS1838B OUT (default: 17)")
    ap.add_argument("--glitch", type=int, default=50, help="glitch filter in microseconds (default: 50us)")
    ap.add_argument("--frame_gap", type=int, default=8000, help="gap threshold(us) to separate frames (default: 8000)")
    ap.add_argument("--show_frames", action="store_true", help="print pulse/space list when a full frame is captured")
    ap.add_argument("--run", type=int, default=0, help="auto-exit after N seconds (0=manual Ctrl+C)")
    args = ap.parse_args()

    pi = pigpio.pi()
    if not pi.connected:
        print("ERROR: pigpio daemon not running. Start with: sudo pigpiod")
        return

    pin = args.gpio
    pi.set_mode(pin, pigpio.INPUT)
    # most IR demodulators idle HIGH; enable pull-up just in case
    pi.set_pull_up_down(pin, pigpio.PUD_UP)
    # small deglitch to drop <50us noise
    if args.glitch > 0:
        pi.set_glitch_filter(pin, args.glitch)

    print(f"[OK] Monitoring GPIO{pin} (glitch>={args.glitch}us). Press your remote buttons…")
    print("Tips:")
    print(" - Ensure VS1838B VCC=3.3V, GND correct, OUT -> this GPIO.")
    print(" - Try moving away from strong lighting if noise occurs.")

    # Track edges to compute pulse/space durations
    last_tick = None
    last_level = None
    durations = []  # us list alternating pulse/space
    frame_buf = deque(maxlen=1024)

    def cbf(gpio, level, tick):
        nonlocal last_tick, last_level, durations, frame_buf

        # pigpio levels: 0=LOW, 1=HIGH, 2=NOISE
        if level == 2:
            return

        if last_tick is None:
            last_tick = tick
            last_level = level
            return

        dt = pigpio.tickDiff(last_tick, tick)  # microseconds
        # IR demod output: LOW = carrier burst (pulse), HIGH = silence (space)
        kind = "pulse" if last_level == 0 else "space"
        durations.append((kind, dt))
        frame_buf.append((kind, dt))

        # Print streaming edges (compact)
        if dt >= args.frame_gap and len(durations) > 0:
            # big gap -> treat as end of frame
            if args.show_frames:
                # pretty print one line
                ps = " ".join([f"{k[0]}:{k[1]}" for k in durations])
                print(f"\n<FRAME len={len(durations)}>: {ps}\n")
            else:
                print(f"\n<FRAME> {len(durations)} segments (gap {dt}us)\n")

            durations = []

        else:
            # lightweight live indicator (every few edges)
            if len(durations) % 10 == 0:
                print(".", end="", flush=True)

        last_tick = tick
        last_level = level

    cb = pi.callback(pin, pigpio.EITHER_EDGE, cbf)

    try:
        if args.run > 0:
            time.sleep(args.run)
        else:
            while True:
                time.sleep(0.2)
    except KeyboardInterrupt:
        pass
    finally:
        cb.cancel()
        pi.set_glitch_filter(pin, 0)
        pi.stop()
        print("\n[EXIT] Stopped.")

if __name__ == "__main__":
    main()

