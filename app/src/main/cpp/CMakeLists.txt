cmake_minimum_required(VERSION 3.22.1)
project("gemmaapp")

add_library(
        llama_bridge
        SHARED
        native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(
        llama_bridge
        ${log-lib}
)
