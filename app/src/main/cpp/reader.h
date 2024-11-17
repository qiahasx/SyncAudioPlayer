//
// Created by qiah on 2024/11/16.
//

#ifndef SYNCPLAYER_READER_H
#define SYNCPLAYER_READER_H

#include <fstream>
#include <thread>
#include "SafeQueue.h"

class FileReader {
public:
    explicit FileReader(std::shared_ptr<SafeQueue> queue, std::ifstream *ios, int bufferSize)
            : queue(std::move(queue)), ios(ios), bufferSize(bufferSize) {}

    void start() {
        read();
    }

    void read() {
        auto id = std::this_thread::get_id();
        int16_t buffer[1024];
        int32_t bytesRead = 0;
        long long readTotal = 0;

        while (0 <
               (bytesRead = ios->read(reinterpret_cast<char *>(buffer), sizeof(buffer)).gcount())) {
            auto bufferInfo = std::make_shared<BufferInfo>(readTotal, bytesRead, buffer);
            queue->push(bufferInfo);
            readTotal += bytesRead;

        }
        const auto bufferInfo = std::make_shared<BufferInfo>(BufferInfo::FLAG_END_STREAM);
        queue->push(bufferInfo);
        ios->close();
        debug("end after");
    }

    static int16_t *charArrayToInt16Array(const char *charArray, size_t length) {
        size_t shortCount = length / 2;
        auto *int16Array = new int16_t[length];
        for (size_t i = 0; i < length; ++i) {
            if (i < shortCount) {
                // 获取两个字节
                auto low = static_cast<uint8_t>(charArray[i * 2]);
                auto high = static_cast<uint8_t>(charArray[i * 2 + 1]);
                // 合并这两个字节形成一个短整型值，注意这里是小端模式
                auto shortValue = static_cast<int16_t>((high << 8) | low);
                // 存储到int16Array中
                int16Array[i] = shortValue;
            } else {
                int16Array[i] = 0;
            }
        }
        return int16Array;
    }

private:
    std::shared_ptr<SafeQueue> queue;
    std::ifstream *ios;
    int bufferSize;
};

#endif //SYNCPLAYER_READER_H
