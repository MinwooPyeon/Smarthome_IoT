#include "pch.h"
#include "Timer.h"

void Timer::start(double hz, std::function<void()> fn){
	stop();
	run_ = true;
	auto period = std::chrono::milliseconds((int)std::round(1000.0 / hz));

	th_ = std::thread([=] {
		while (run_) {
			auto t0 = std::chrono::steady_clock::now();
			fn();
			auto dt = std::chrono::steady_clock::now() - t0;
			if (dt < period) std::this_thread::sleep_for(period - dt);
		}
	});
}

void Timer::stop() {
	if (run_.exchange(false) && th_.joinable())
		th_.join();
}