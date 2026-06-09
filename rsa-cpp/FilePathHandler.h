#ifndef FILE_PATH_HANDLER_H
#define FILE_PATH_HANDLER_H

#include <string>

// Thu thập và kiểm tra đường dẫn file từ người dùng (Tuần 3 - Nguyễn Ngọc Sáng)
class FilePathHandler {
public:
    // Yêu cầu người dùng nhập đường dẫn file đầu vào
    static std::string getInputFilePath();

    // Yêu cầu người dùng nhập đường dẫn file đầu ra
    static std::string getOutputFilePath();

    // Kiểm tra file có tồn tại không
    static bool fileExists(const std::string& path);

    // Đọc nội dung file thành chuỗi
    static std::string readFile(const std::string& path);

    // Ghi chuỗi ra file
    static bool writeFile(const std::string& path, const std::string& content);
};

#endif // FILE_PATH_HANDLER_H
