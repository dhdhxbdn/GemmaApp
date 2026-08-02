cmake_minimum_required(VERSION 3.22.1)
project("gemmaapp")

# Подключаем папку с исходниками llama.cpp
add_subdirectory(llama.cpp)

add_library(llama_bridge SHARED native-lib.cpp)

find_library(log-lib log)

# Линкуем наш мост с библиотекой llama (которую соберет add_subdirectory)
target_link_libraries(llama_bridge llama ${log-lib})
