//
// Created by qiah on 2024/11/16.
//
#include <condition_variable>
#include <fstream>
#include <mutex>
#include <queue>
#include "BufferInfo.h"
#include "log.h"

class SafeQueue {
public:
    explicit SafeQueue(const int bufferSize) : bufferSize(bufferSize) {}

    void push(const std::shared_ptr<BufferInfo> &item);

    std::shared_ptr<BufferInfo> pop();

private:
    std::queue<std::shared_ptr<BufferInfo>> queue{};
    std::mutex mtx{};
    std::condition_variable condition{};
    int bufferSize = 20;
};