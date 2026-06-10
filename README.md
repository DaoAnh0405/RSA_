# RSA System — Hướng Dẫn Sử Dụng

## Yêu Cầu

- **Java**: JDK 11+
- **C++**: MinGW-w64 (g++), CMake, OpenSSL (MSYS2)
- **MSYS2 OpenSSL**: `pacman -S mingw-w64-x86_64-openssl`

## Chạy Nhanh (Windows)

```bat
double-click run.bat
```
hoặc mở cmd tại file project gõ run.bat

## Chạy Thủ Công

### B1: Build & chạy C++ server
```bat
cd cpp_server
mkdir build && cd build
cmake .. -G "MinGW Makefiles"
mingw32-make
rsa_server.exe
```

### B2: Build & chạy Java
```bat
cd java_app
mkdir bin
javac -encoding UTF-8 -d bin src\rsa\*.java
java -cp bin rsa.AppFrame
```

## Luồng Sử Dụng
1. **Tab 1 — Tạo Khóa**
   - Nhập p, q (số nguyên tố) hoặc nhấn "Random"
   - Nhấn "Tạo Khóa" → C++ server tính n, e, d, phi
   - Tham số hiển thị đầy đủ
   - Khóa tự động chuyển sang Tab 2 và Tab 3

2. **Tab 2 — Mã Hóa**
   - Nhập văn bản hoặc chọn file
   - Nhấn "Tiến Hành Mã Hóa" → C++ RSA PKCS#1 v1.5
   - Lưu bản mã (.enc)
   - Hoặc nhấn "Chuyển sang Tab 3"

3. **Tab 3 — Giải Mã**
   - Nạp file bản mã (.enc)
   - Nạp private key
   - Nhấn "Giải Mã"
   - Kết quả xác thực:
     - Thành công
     - Bản mã bị sửa đổi
     - Khóa sai
     - Cả hai đều sai