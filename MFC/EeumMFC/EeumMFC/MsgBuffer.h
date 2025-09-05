#pragma once
#include <vector>
#include <mutex>

template<typename T>
class MsgBuffer {
public:
	void push(const T& msg) {
		std::lock_guard<std::mutex> lock(mtx);
		buffer.push_back(msg);
	}

	std::vector<T> flush() {
		std::lock_guard<std::mutex> lock(mtx);
		std::vector<T> out;
		
		if (!buffer.empty())
			out.swap(buffer);

		return out;
	}

	size_t size() const {
		std::lock_guard<std::mutex> lock(mtx);
		return buffer.size();
	}

	void clear() {
		std::lock_guard<std::mutex> lock(mtx);
		buffer.clear();
	}

private:
	std::vector<T> buffer;
	std::mutex mtx;
};