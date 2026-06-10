#include <iostream>
#include <string>
#include <sstream>
#include <cstring>
#include <openssl/rsa.h>
#include <openssl/pem.h>
#include <openssl/bn.h>
#include <openssl/err.h>
#include <openssl/evp.h>

#ifdef _WIN32
  #include <winsock2.h>
  #include <ws2tcpip.h>
  #pragma comment(lib,"ws2_32.lib")
  typedef int socklen_t;
#else
  #include <sys/socket.h>
  #include <netinet/in.h>
  #include <unistd.h>
  #define closesocket close
#endif

#include <vector>
#include <stdexcept>

static const std::string B64 =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

std::string base64_encode(const unsigned char* data, size_t len) {
    std::string out;
    for (size_t i = 0; i < len; i += 3) {
        unsigned char b0 = data[i];
        unsigned char b1 = (i+1 < len) ? data[i+1] : 0;
        unsigned char b2 = (i+2 < len) ? data[i+2] : 0;
        out += B64[b0 >> 2];
        out += B64[((b0 & 3) << 4) | (b1 >> 4)];
        out += (i+1 < len) ? B64[((b1 & 0xf) << 2) | (b2 >> 6)] : '=';
        out += (i+2 < len) ? B64[b2 & 0x3f] : '=';
    }
    return out;
}

std::vector<unsigned char> base64_decode(const std::string& s) {
    auto val = [](char c) -> int {
        if (c >= 'A' && c <= 'Z') return c - 'A';
        if (c >= 'a' && c <= 'z') return c - 'a' + 26;
        if (c >= '0' && c <= '9') return c - '0' + 52;
        if (c == '+') return 62;
        if (c == '/') return 63;
        return -1;
    };
    std::vector<unsigned char> out;
    for (size_t i = 0; i + 3 < s.size(); i += 4) {
        int v0 = val(s[i]), v1 = val(s[i+1]);
        int v2 = val(s[i+2]), v3 = val(s[i+3]);
        out.push_back((v0 << 2) | (v1 >> 4));
        if (s[i+2] != '=') out.push_back(((v1 & 0xf) << 4) | (v2 >> 2));
        if (s[i+3] != '=') out.push_back(((v2 & 0x3) << 6) | v3);
    }
    return out;
}

std::string json_get(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\"";
    size_t pos = json.find(search);
    if (pos == std::string::npos) return "";
    pos = json.find(':', pos);
    if (pos == std::string::npos) return "";
    pos++;
    while (pos < json.size() && json[pos] == ' ') pos++;
    if (json[pos] == '"') {
        size_t start = pos + 1;
        size_t end = json.find('"', start);
        while (end != std::string::npos && json[end-1] == '\\') end = json.find('"', end+1);
        return json.substr(start, end - start);
    }
    size_t start = pos;
    size_t end = json.find_first_of(",}\n", start);
    std::string v = json.substr(start, end - start);
    while (!v.empty() && (v.back() == ' ' || v.back() == '\r')) v.pop_back();
    return v;
}

std::string json_escape(const std::string& s) {
    std::string out;
    for (char c : s) {
        if (c == '"') out += "\\\"";
        else if (c == '\\') out += "\\\\";
        else if (c == '\n') out += "\\n";
        else if (c == '\r') out += "\\r";
        else out += c;
    }
    return out;
}

std::string openssl_error() {
    char buf[256];
    ERR_error_string_n(ERR_get_error(), buf, sizeof(buf));
    return std::string(buf);
}

// Đọc toàn bộ nội dung BIO vào string
static std::string bio_to_string(BIO* bio) {
    std::string result;
    char buf[1024];
    int n;
    while ((n = BIO_read(bio, buf, sizeof(buf))) > 0) {
        result.append(buf, n);
    }
    return result;
}

bool send_all(int fd, const char* buf, size_t len) {
    size_t sent = 0;
    while (sent < len) {
        int n = send(fd, buf + sent, (int)(len - sent), 0);
        if (n <= 0) return false;
        sent += n;
    }
    return true;
}

std::string handle_keygen(const std::string& req) {
    std::string p_hex = json_get(req, "p");
    std::string q_hex = json_get(req, "q");
    std::string e_str = json_get(req, "e");

    BIGNUM *p = NULL, *q = NULL, *e = BN_new();
    BN_hex2bn(&p, p_hex.c_str());
    BN_hex2bn(&q, q_hex.c_str());

    if (e_str.empty() || e_str == "0") BN_set_word(e, 65537);
    else BN_dec2bn(&e, e_str.c_str());

    BN_CTX* ctx = BN_CTX_new();
    BIGNUM* n = BN_new();
    BN_mul(n, p, q, ctx);

    BIGNUM* p1 = BN_new(); BN_sub(p1, p, BN_value_one());
    BIGNUM* q1 = BN_new(); BN_sub(q1, q, BN_value_one());
    BIGNUM* phi = BN_new(); BN_mul(phi, p1, q1, ctx);

    BIGNUM* d = BN_mod_inverse(NULL, e, phi, ctx);
    if (!d) {
        BN_free(p); BN_free(q); BN_free(e); BN_free(n);
        BN_free(p1); BN_free(q1); BN_free(phi);
        BN_CTX_free(ctx);
        return "{\"status\":\"error\",\"message\":\"e khong hop le hoac khong nguyen to cung nhau voi phi(n)\"}";
    }

    char* n_dec   = BN_bn2dec(n);
    char* e_dec   = BN_bn2dec(e);
    char* d_dec   = BN_bn2dec(d);
    char* phi_dec = BN_bn2dec(phi);

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
    RSA* rsa = RSA_new();
    RSA_set0_key(rsa, BN_dup(n), BN_dup(e), BN_dup(d));
    RSA_set0_factors(rsa, BN_dup(p), BN_dup(q));

    BIGNUM* dmp1 = BN_new();  // d mod (p-1)
    BIGNUM* dmq1 = BN_new();  // d mod (q-1)  
    BIGNUM* iqmp = BN_new();  // q^-1 mod p

    BN_mod(dmp1, d, p1, ctx);
    BN_mod(dmq1, d, q1, ctx);
    BN_mod_inverse(iqmp, q, p, ctx);

    RSA_set0_crt_params(rsa, dmp1, dmq1, iqmp);

    BIO* bio_pub  = BIO_new(BIO_s_mem());
    BIO* bio_priv = BIO_new(BIO_s_mem());

    PEM_write_bio_RSAPublicKey(bio_pub, rsa);
    PEM_write_bio_RSAPrivateKey(bio_priv, rsa, NULL, NULL, 0, NULL, NULL);

    // ── FIX: dùng BIO_read thay vì BIO_get_mem_data ─────────
    // BIO_get_mem_data chỉ trả pointer, không đảm bảo flush hết
    // BIO_read đọc thực sự từng byte ra string
    std::string pub_pem  = bio_to_string(bio_pub);
    std::string priv_pem = bio_to_string(bio_priv);

    std::cout << "[DEBUG] pub_pem  size: " << pub_pem.size()  << std::endl;
    std::cout << "[DEBUG] priv_pem size: " << priv_pem.size() << std::endl;
#pragma GCC diagnostic pop

    std::string result =
        "{\"status\":\"ok\","
        "\"n\":\""   + std::string(n_dec)   + "\","
        "\"e\":\""   + std::string(e_dec)   + "\","
        "\"d\":\""   + std::string(d_dec)   + "\","
        "\"phi\":\"" + std::string(phi_dec) + "\","
        "\"public_key\":\""  + json_escape(pub_pem)  + "\","
        "\"private_key\":\"" + json_escape(priv_pem) + "\"}";

    std::cout << "[DEBUG] Total response size: " << result.size() << " bytes" << std::endl;

    OPENSSL_free(n_dec); OPENSSL_free(e_dec);
    OPENSSL_free(d_dec); OPENSSL_free(phi_dec);
    BIO_free(bio_pub); BIO_free(bio_priv);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
    RSA_free(rsa);
#pragma GCC diagnostic pop
    BN_free(p1); BN_free(q1); BN_free(phi); BN_free(d); BN_free(n);
    BN_CTX_free(ctx);
    return result;
}

std::string handle_encrypt(const std::string& req) {
    std::string pub_pem   = json_get(req, "public_key");
    std::string plaintext = json_get(req, "plaintext");

    std::string pem, pt;
    for (size_t i = 0; i < pub_pem.size(); i++) {
        if (pub_pem[i] == '\\' && i+1 < pub_pem.size()) {
            if      (pub_pem[i+1] == 'n')  { pem += '\n'; i++; }
            else if (pub_pem[i+1] == '"')  { pem += '"';  i++; }
            else pem += pub_pem[i];
        } else pem += pub_pem[i];
    }
    for (size_t i = 0; i < plaintext.size(); i++) {
        if (plaintext[i] == '\\' && i+1 < plaintext.size()) {
            if      (plaintext[i+1] == 'n')  { pt += '\n'; i++; }
            else if (plaintext[i+1] == '"')  { pt += '"';  i++; }
            else pt += plaintext[i];
        } else pt += plaintext[i];
    }

    BIO* bio = BIO_new_mem_buf(pem.c_str(), -1);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"
    RSA* rsa = PEM_read_bio_RSAPublicKey(bio, NULL, NULL, NULL);
    BIO_free(bio);
    if (!rsa) return "{\"status\":\"error\",\"message\":\"Public key khong hop le\"}";

    int rsa_size = RSA_size(rsa);
    std::vector<unsigned char> cipher(rsa_size);
    int ret = RSA_public_encrypt(pt.size(),
        (const unsigned char*)pt.c_str(),
        cipher.data(), rsa, RSA_PKCS1_PADDING);
    RSA_free(rsa);
#pragma GCC diagnostic pop
    if (ret < 0) return "{\"status\":\"error\",\"message\":\"" + openssl_error() + "\"}";

    std::string b64 = base64_encode(cipher.data(), ret);
    return "{\"status\":\"ok\",\"ciphertext\":\"" + b64 + "\"}";
}

std::string process_request(const std::string& req) {
    std::string cmd = json_get(req, "cmd");
    if (cmd == "keygen")  return handle_keygen(req);
    if (cmd == "encrypt") return handle_encrypt(req);
    return "{\"status\":\"error\",\"message\":\"Unknown command\"}";
}

int main() {
#ifdef _WIN32
    WSADATA wsa;
    WSAStartup(MAKEWORD(2,2), &wsa);
#endif
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port        = htons(9000);
    bind(server_fd, (sockaddr*)&addr, sizeof(addr));
    listen(server_fd, 10);

    std::cout << "[RSA C++ Server] Listening on port 9000..." << std::endl;

    while (true) {
        sockaddr_in client{};
        socklen_t clen = sizeof(client);
        int client_fd = accept(server_fd, (sockaddr*)&client, &clen);
        if (client_fd < 0) continue;

        std::string req;
        char buf[65536];
        while (true) {
            int n = recv(client_fd, buf, sizeof(buf)-1, 0);
            if (n <= 0) break;
            buf[n] = 0;
            req += buf;
            if (req.find('\n') != std::string::npos) break;
        }

        std::string resp = process_request(req);
        resp += "\n";
        send_all(client_fd, resp.c_str(), resp.size());
        closesocket(client_fd);
    }

#ifdef _WIN32
    WSACleanup();
#endif
    return 0;
}