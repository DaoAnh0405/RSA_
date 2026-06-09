#ifndef RSA_CIPHER_H
#define RSA_CIPHER_H

#include <string>
#include <vector>

// Mã hóa và giải mã RSA phía C++ (Tuần 4 - Nguyễn Ngọc Sáng)
class RSACipher {
public:
    // Mã hóa chuỗi plaintext bằng khóa công khai (e, n)
    // Trả về chuỗi các số nguyên cách nhau bởi dấu cách
    static std::string encrypt(const std::string& plaintext, long long e, long long n);

    // Giải mã chuỗi ciphertext (dạng số nguyên cách nhau bởi dấu cách) bằng khóa bí mật (d, n)
    static std::string decrypt(const std::string& ciphertext, long long d, long long n);

private:
    // Tính (base^exp) % mod bằng fast modular exponentiation
    static long long modPow(long long base, long long exp, long long mod);
};

#endif
