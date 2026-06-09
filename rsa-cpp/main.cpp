#include <iostream>
#include <string>
#include "FilePathHandler.h"
#include "RSAKeyGenerator.h"
#include "RSACipher.h"

// Hiển thị menu chính
void showMenu() {
    std::cout << "\n========================================\n";
    std::cout << "    CHUONG TRINH MA HOA / GIAI MA RSA   \n";
    std::cout << "========================================\n";
    std::cout << "1. Ma hoa file\n";
    std::cout << "2. Giai ma file\n";
    std::cout << "3. Thoat\n";
    std::cout << "Chon chuc nang: ";
}

int main() {
    // Sinh cặp khóa RSA một lần khi khởi động
    std::cout << "Dang sinh khoa RSA...\n";
    RSAKeyGenerator::KeyPair keys;
    try {
        keys = RSAKeyGenerator::generateKeys();
    } catch (const std::exception& ex) {
        std::cerr << "[LOI] Sinh khoa that bai: " << ex.what() << std::endl;
        return 1;
    }

    std::cout << "Khoa cong khai (e, n) = (" << keys.e << ", " << keys.n << ")\n";
    std::cout << "Khoa bi mat  (d, n) = (" << keys.d << ", " << keys.n << ")\n";

    int choice;
    while (true) {
        showMenu();
        std::cin >> choice;
        std::cin.ignore(); // Xóa newline còn lại trong buffer

        if (choice == 1) {
            // --- MÃ HÓA ---
            std::string inputPath = FilePathHandler::getInputFilePath();
            std::string plaintext = FilePathHandler::readFile(inputPath);
            if (plaintext.empty()) continue;

            std::string ciphertext;
            try {
                ciphertext = RSACipher::encrypt(plaintext, keys.e, keys.n);
            } catch (const std::exception& ex) {
                std::cerr << "[LOI] Ma hoa that bai: " << ex.what() << std::endl;
                continue;
            }

            std::string outputPath = FilePathHandler::getOutputFilePath();
            FilePathHandler::writeFile(outputPath, ciphertext);

        } else if (choice == 2) {
            // --- GIẢI MÃ ---
            std::string inputPath = FilePathHandler::getInputFilePath();
            std::string ciphertext = FilePathHandler::readFile(inputPath);
            if (ciphertext.empty()) continue;

            std::string plaintext;
            try {
                plaintext = RSACipher::decrypt(ciphertext, keys.d, keys.n);
            } catch (const std::exception& ex) {
                std::cerr << "[LOI] Giai ma that bai: " << ex.what() << std::endl;
                continue;
            }

            std::string outputPath = FilePathHandler::getOutputFilePath();
            FilePathHandler::writeFile(outputPath, plaintext);

        } else if (choice == 3) {
            std::cout << "Tam biet!\n";
            break;
        } else {
            std::cout << "Lua chon khong hop le. Vui long thu lai.\n";
        }
    }

    return 0;
}
