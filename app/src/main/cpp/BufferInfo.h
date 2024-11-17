#include <cstring>

#include "oboe/include/Oboe.h"

class BufferInfo {
public:
    BufferInfo(const long long fileOffset, int32_t size, int16_t *buffer) : fileOffset(fileOffset),
                                                                            size(size) {
        this->buffer = new float[size / sizeof(int16_t)];
        oboe::convertPcm16ToFloat(buffer, this->buffer, size / sizeof(int16_t));
    }

    explicit BufferInfo(const int flag) : flag((flag)) {}

    ~BufferInfo() {
        delete[] buffer;
    }

    long long fileOffset = 0;
    int offset = 0;
    int size = 0;
    float *buffer = nullptr;
    int flag = 0;
    static constexpr auto FLAG_END_STREAM = -1;
};