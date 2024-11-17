//
// Created by qiah on 2024/11/16.
//

#ifndef SYNCPLAYER_SYNCPLAYER_H
#define SYNCPLAYER_SYNCPLAYER_H

#include <utility>

#include "oboe/include/Oboe.h"
#include "reader.h"

class SyncPlayer {
public:
    oboe::Result open(const char *filename);

    oboe::Result start();

    oboe::Result stop();

    oboe::Result close();

    void seekTo(long long offsetMs);

private:
    class MyDataCallBack : public oboe::AudioStreamCallback {
    public:
        explicit MyDataCallBack(std::shared_ptr<SafeQueue> queue, std::ifstream *ios) : ios(ios),
                                                                                        queue(std::move(
                                                                                                queue)) {}

        oboe::DataCallbackResult onAudioReady(
                oboe::AudioStream *audioStream,
                void *audioData,
                int32_t numFrames
        ) override;

    private:
        std::ifstream *ios;
        std::shared_ptr<SafeQueue> queue;
        std::shared_ptr<BufferInfo> bufferInfo = std::make_shared<BufferInfo>(0);
    };

    class MyErrorCallBack : public oboe::AudioStreamErrorCallback {
    public:
        void onErrorAfterClose(oboe::AudioStream *audioStream, oboe::Result error) override;
    };

    std::shared_ptr<oboe::AudioStream> stream;
    std::shared_ptr<MyDataCallBack> dataCall;
    std::shared_ptr<MyErrorCallBack> errorCall;
    std::shared_ptr<FileReader> reader;
    std::shared_ptr<SafeQueue> queue;
};

#endif //SYNCPLAYER_SYNCPLAYER_H