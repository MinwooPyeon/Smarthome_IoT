#pragma once
#include <thread>
#include <atomic>
#include <chrono>
#include <cmath>
#include <functional>

class Timer {
public:
	
private:
	std::thread th_;
	std::atomic<bool> run_{ false };
public:
	~Timer() { stop(); }
	void start(double hz, std::function<void()> fn);
	void stop();
};