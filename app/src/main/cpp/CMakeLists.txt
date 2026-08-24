cmake_minimum_required(VERSION 3.20)
project(wenmo_sime LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(SIME_ROOT ${CMAKE_CURRENT_SOURCE_DIR}/../../../../require/Sime)
set(SIME_BUILD_TOOLS OFF CACHE BOOL "" FORCE)

add_subdirectory(${SIME_ROOT} ${CMAKE_CURRENT_BINARY_DIR}/sime_build)

add_library(wenmo_sime SHARED
    sime_jni.cc
)

target_include_directories(wenmo_sime PRIVATE ${SIME_ROOT}/include ${SIME_ROOT}/src)
target_link_libraries(wenmo_sime PRIVATE sime_core log)
