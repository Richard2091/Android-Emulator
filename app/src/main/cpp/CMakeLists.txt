cmake_minimum_required(VERSION 3.22.1)

project(retrohall_libretro_host)

add_library(
    retrohall_libretro_host
    SHARED
    libretro_host.cpp
)

find_library(log-lib log)

target_link_libraries(
    retrohall_libretro_host
    ${log-lib}
)
