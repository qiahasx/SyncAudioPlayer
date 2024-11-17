cmake_minimum_required(VERSION 3.22.1)

set(OBOE_DIR ${CMAKE_CURRENT_LIST_DIR}/oboe)

add_subdirectory(${OBOE_DIR} ${CMAKE_CURRENT_BINARY_DIR}/oboe-bin)

set(APP_SOURCES
        BufferInfo.h
        log.h
        native-lib.cpp
        reader.h
        SafeQueue.cpp
        SafeQueue.h
        SyncPalyerJNI.cpp
        SyncPlayer.cpp
        SyncPlayer.h
)

include_directories(
        ${OBOE_DIR}/include
        ${CMAKE_CURRENT_LIST_DIR}
)

add_library(syncPlayer SHARED
        ${APP_SOURCES}
)


target_link_libraries(syncPlayer
        oboe
        log)

target_link_options(syncPlayer PRIVATE "-Wl,-z,max-page-size=16384")
