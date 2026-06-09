#ifndef RSA_KEY_GENERATOR_H
#define RSA_KEY_GENERATOR_H

#include <cstdint>
#include <utility>

// Sinh khóa RSA phía C++ (Tuần 4 - Nguyễn Ngọc Sáng, phối hợp Trần Nhật Nam)
class RSAKeyGenerator {
public:
    struct KeyPair {
        long long e, n; // Khóa công khai
        long long d, n2; // Khóa bí mật (n2 == n)
    };

    // Tạo cặp khóa RSA với số bit tùy chọn (mặc định dùng số nguyên tố nhỏ cho demo)
    static KeyPair generateKeys();

private:
    static bool isPrime(long long n);
    static long long gcd(long long a, long long b);
    static long long modInverse(long long e, long long phi);
    static long long randomPrime(long long min, long long max);
};

#endif
