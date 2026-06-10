cmake_minimum_required(VERSION 3.16)
project(RSAServer LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 17)

# ── OpenSSL ─────────────────────────────────────────────────
# Ưu tiên MSYS2/MinGW, fallback sang Program Files
if(EXISTS "C:/msys64/mingw64/lib/libcrypto.dll.a")
    set(OPENSSL_ROOT_DIR      "C:/msys64/mingw64")
    set(OPENSSL_INCLUDE_DIR   "C:/msys64/mingw64/include")
    set(OPENSSL_CRYPTO_LIBRARY "C:/msys64/mingw64/lib/libcrypto.dll.a")
    set(OPENSSL_SSL_LIBRARY    "C:/msys64/mingw64/lib/libssl.dll.a")
elseif(EXISTS "C:/Program Files/OpenSSL-Win64/lib/VC/x64/MD/libcrypto.lib")
    set(OPENSSL_ROOT_DIR      "C:/Program Files/OpenSSL-Win64")
    set(OPENSSL_INCLUDE_DIR   "C:/Program Files/OpenSSL-Win64/include")
    set(OPENSSL_CRYPTO_LIBRARY "C:/Program Files/OpenSSL-Win64/lib/VC/x64/MD/libcrypto.lib")
    set(OPENSSL_SSL_LIBRARY    "C:/Program Files/OpenSSL-Win64/lib/VC/x64/MD/libssl.lib")
endif()

find_package(OpenSSL REQUIRED)

add_executable(rsa_server rsa_server.cpp)
target_link_libraries(rsa_server OpenSSL::SSL OpenSSL::Crypto)

if(WIN32)
    target_link_libraries(rsa_server ws2_32)
endif()
