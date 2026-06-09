#include "RSAKeyGenerator.h"
#include <cstdlib>
#include <ctime>
#include <vector>
#include <stdexcept>

bool RSAKeyGenerator::isPrime(long long n) {
    if (n < 2) return false;
    if (n == 2) return true;
    if (n % 2 == 0) return false;
    for (long long i = 3; i * i <= n; i += 2)
        if (n % i == 0) return false;
    return true;
}

long long RSAKeyGenerator::gcd(long long a, long long b) {
    while (b != 0) { long long t = b; b = a % b; a = t; }
    return a;
}

// Tìm nghịch đảo modular của e theo mod phi (thuật toán Euclid mở rộng)
long long RSAKeyGenerator::modInverse(long long e, long long phi) {
    long long t = 0, newt = 1, r = phi, newr = e;
    while (newr != 0) {
        long long q = r / newr;
        long long tmp = t - q * newt; t = newt; newt = tmp;
        tmp = r - q * newr; r = newr; newr = tmp;
    }
    if (r > 1) throw std::runtime_error("e khong co nghich dao mod phi");
    if (t < 0) t += phi;
    return t;
}

long long RSAKeyGenerator::randomPrime(long long min, long long max) {
    std::vector<long long> primes;
    for (long long i = min; i <= max; i++)
        if (isPrime(i)) primes.push_back(i);
    if (primes.empty()) throw std::runtime_error("Khong tim duoc so nguyen to trong khoang");
    return primes[rand() % primes.size()];
}

RSAKeyGenerator::KeyPair RSAKeyGenerator::generateKeys() {
    srand((unsigned)time(nullptr));

    // Chọn 2 số nguyên tố p, q khác nhau trong khoảng [50, 200]
    long long p = randomPrime(50, 200);
    long long q;
    do { q = randomPrime(50, 200); } while (q == p);

    long long n = p * q;
    long long phi = (p - 1) * (q - 1);

    // Chọn e: 1 < e < phi và gcd(e, phi) = 1
    long long e = 2;
    while (e < phi && gcd(e, phi) != 1) e++;

    long long d = modInverse(e, phi);

    return {e, n, d, n};
}
