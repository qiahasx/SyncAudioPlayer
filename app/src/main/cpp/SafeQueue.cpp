//
// Created by qiah on 2024/11/16.
//
#include "SafeQueue.h"

void SafeQueue::push(const std::shared_ptr<BufferInfo> &item) {
//    std::unique_lock lock(mtx);
//    condition.wait(lock, [this] {
//        return queue.size() <= bufferSize;
//    });
    queue.push(item);
//    condition.notify_one();
}

std::shared_ptr<BufferInfo> SafeQueue::pop() {
//    std::unique_lock lock(mtx);
//    condition.wait(lock, [this] {
//        auto isEmpty = queue.empty();
//        if (isEmpty) {
//            debug("queue is empty be waiting");
//        }
//        return !queue.empty();
//    });
    auto item = queue.front();
    queue.pop();
//    condition.notify_one();
    return item;
}

