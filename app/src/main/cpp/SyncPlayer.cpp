//
// Created by qiah on 2024/11/16.
//
#include "SyncPlayer.h"
#include "oboe/include/Oboe.h"
#include "log.h"

oboe::Result SyncPlayer::open(const char *filename) {
    std::ifstream ios(filename, std::ios::binary);
    if (!ios.is_open()) {
        debug("file $s not opened", filename);
        return oboe::Result::ErrorInternal;
    }
    queue = std::make_shared<SafeQueue>(20);
    reader = std::make_shared<FileReader>(queue, &ios, 1024);
    dataCall = std::make_shared<MyDataCallBack>(queue, &ios);
    errorCall = std::make_shared<MyErrorCallBack>();
    reader->start();
    oboe::AudioStreamBuilder builder;
    return builder.setSharingMode(oboe::SharingMode::Exclusive)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSampleRate(44100)
            ->setChannelCount(2)
            ->setFormatConversionAllowed(true)
            ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
            ->setFormat(oboe::AudioFormat::Float)
            ->setDataCallback(dataCall)
            ->setErrorCallback(errorCall)
            ->openStream(stream);
}

oboe::Result SyncPlayer::start() {
    return stream->requestStart();
}

oboe::Result SyncPlayer::stop() {
    return stream->requestStop();
}

oboe::Result SyncPlayer::close() {
    return stream->close();
}

void SyncPlayer::seekTo(long long int offsetMs) {
    debug("seek to Input: %lld", offsetMs);
}

long long a = 0;

oboe::DataCallbackResult
SyncPlayer::MyDataCallBack::onAudioReady(oboe::AudioStream *audioStream, void *audioData,
                                         int32_t numFrames) {

    auto output = (float *) audioData;
    auto size = numFrames * 2;
//    ios->read(static_cast<char *>(audioData), size);
    a += size;
    if (bufferInfo->flag == BufferInfo::FLAG_END_STREAM) {
        debug("size: %d", a);
        return oboe::DataCallbackResult::Stop;
    }
    for (int i = 0; i < size; i++) {
        if (bufferInfo->offset >= bufferInfo->size) {
            bufferInfo = queue->pop();

            if (bufferInfo->flag == BufferInfo::FLAG_END_STREAM) {
                return oboe::DataCallbackResult::Stop;
            }
        }

        *output++ = bufferInfo->buffer[bufferInfo->offset++];
    }
    return oboe::DataCallbackResult::Continue;
}

void SyncPlayer::MyErrorCallBack::onErrorAfterClose(oboe::AudioStream *audioStream,
                                                    oboe::Result error) {
    debug("%s() - error = %s",
          __func__,
          oboe::convertToText(error));
}
