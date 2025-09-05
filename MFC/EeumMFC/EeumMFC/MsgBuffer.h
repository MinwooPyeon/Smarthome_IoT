#pragma once
#include <vector>
#include <mutex>

template<typename T>
class MsgBuffer {
public:
	void push(const T& msg);

	std::vector<T> flush();

	size_t size() const;

	void clear();

private:
	std::vector<T> buffer;
	std::mutex mtx;
};