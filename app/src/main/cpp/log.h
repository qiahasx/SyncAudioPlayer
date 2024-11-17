#ifndef LOG_H
#define LOG_H

#include <android/log.h>
#include <cstdarg>

#define TAG "Player_ANDROID"

inline void debug(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_DEBUG, TAG, fmt, args);
    va_end(args);
}

#endif // LOG_H