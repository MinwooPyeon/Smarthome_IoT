#include "MsgBuffer.h"


template<typename T>
void MsgBuffer<T>::push(const T& msg){
	std::lock_guard<std::mutex> lock(mtx);
	buffer.push_back(msg);
}

template<typename T>
std::vector<T> MsgBuffer<T>::flush()
{
	return std::vector<T>();
}

template<typename T>
size_t MsgBuffer<T>::size() const{
	std::lock_guard<std::mutex> lock(mtx);
	return buffer.size();
}

template<typename T>
void MsgBuffer<T>::clear() {
	std::lock_guard<std::mutex> lock(mtx);
	buffer.clear();
}
