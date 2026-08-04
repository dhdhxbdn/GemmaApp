cmake_minimum_required(VERSION 3.22.1)
project("gemmaapp")

# Отключаем лишние компоненты llama.cpp для мобильной сборки
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# Подключаем сабмодуль llama.cpp
add_subdirectory(llama.cpp)

# Собираем наш JNI-мост
add_library(llama_bridge SHARED native-lib.cpp)

find_library(log-lib log)

# Линковка
target_link_libraries(llama_bridge llama ${log-lib})
