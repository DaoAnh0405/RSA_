#include "FilePathHandler.h"
#include <iostream>
#include <fstream>
#include <sstream>

// Yêu cầu người dùng nhập đường dẫn file đầu vào, kiểm tra tồn tại
std::string FilePathHandler::getInputFilePath() {
    std::string path;
    while (true) {
        std::cout << "Nhap duong dan file dau vao: ";
        std::getline(std::cin, path);

        // Loại bỏ dấu nháy nếu người dùng kéo thả file vào terminal
        if (!path.empty() && path.front() == '"' && path.back() == '"') {
            path = path.substr(1, path.size() - 2);
        }

        if (fileExists(path)) {
            std::cout << "[OK] Da tim thay file: " << path << std::endl;
            return path;
        } else {
            std::cout << "[LOI] Khong tim thay file. Vui long thu lai.\n";
        }
    }
}

// Yêu cầu người dùng nhập đường dẫn file đầu ra
std::string FilePathHandler::getOutputFilePath() {
    std::string path;
    std::cout << "Nhap duong dan file dau ra (vd: output.txt): ";
    std::getline(std::cin, path);

    if (!path.empty() && path.front() == '"' && path.back() == '"') {
        path = path.substr(1, path.size() - 2);
    }

    return path;
}

// Kiểm tra file có tồn tại không
bool FilePathHandler::fileExists(const std::string& path) {
    std::ifstream f(path);
    return f.good();
}

// Đọc toàn bộ nội dung file thành chuỗi
std::string FilePathHandler::readFile(const std::string& path) {
    std::ifstream file(path);
    if (!file.is_open()) {
        std::cerr << "[LOI] Khong mo duoc file: " << path << std::endl;
        return "";
    }
    std::ostringstream ss;
    ss << file.rdbuf();
    return ss.str();
}

// Ghi chuỗi ra file, trả về true nếu thành công
bool FilePathHandler::writeFile(const std::string& path, const std::string& content) {
    std::ofstream file(path);
    if (!file.is_open()) {
        std::cerr << "[LOI] Khong ghi duoc file: " << path << std::endl;
        return false;
    }
    file << content;
    std::cout << "[OK] Da ghi ket qua vao: " << path << std::endl;
    return true;
}
