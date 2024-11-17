//
// Created by qiah on 2024/11/16.
//

#include <jni.h>
#include "oboe/include/Oboe.h"
#include "SyncPlayer.h"

#ifdef __cplusplus
extern "C" {
#endif
using namespace oboe;
static SyncPlayer player;

JNIEXPORT void JNICALL Java_com_example_syncplayer_SyncPlayer_setFilePath(
        JNIEnv *env,
        jobject thiz,
        jstring filePath
) {
    const char *path = env->GetStringUTFChars(filePath, nullptr);
    debug("cpp get %s", path);
    auto result = player.open(path);
    debug("%d", result == Result::OK);
}

JNIEXPORT void JNICALL Java_com_example_syncplayer_SyncPlayer_start(
        JNIEnv *env,
        jobject thiz
) {
    player.start();
}

JNIEXPORT void JNICALL Java_com_example_syncplayer_SyncPlayer_stop(
        JNIEnv *env,
        jobject thiz
) {
    player.stop();
}

}
