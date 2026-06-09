#include "RSACipher.h"
#include <sstream>
#include <stdexcept>

// Fast modular exponentiation: tính (base^exp) % mod hiệu quả
long long RSACipher::modPow(long long base, long long exp, long long mod) {
    long long result = 1;
    base %= mod;
    while (exp > 0) {
        if (exp % 2 == 1) result = result * base % mod;
        base = base * base % mod;
        exp /= 2;
    }
    return result;
}

// Mã hóa: mỗi ký tự → c = m^e mod n, nối bằng dấu cách
std::string RSACipher::encrypt(const std::string& plaintext, long long e, long long n) {
    std::ostringstream oss;
    for (size_t i = 0; i < plaintext.size(); i++) {
        long long m = (unsigned char)plaintext[i];
        if (m >= n)
            throw std::runtime_error("Ky tu vuot qua gia tri n, can chon so nguyen to lon hon");
        long long c = modPow(m, e, n);
        if (i > 0) oss << " ";
        oss << c;
    }
    return oss.str();
}

// Giải mã: mỗi số c → m = c^d mod n → ký tự ASCII
std::string RSACipher::decrypt(const std::string& ciphertext, long long d, long long n) {
    std::istringstream iss(ciphertext);
    std::string result;
    long long c;
    while (iss >> c) {
        long long m = modPow(c, d, n);
        result += (char)m;
    }
    return result;
}
