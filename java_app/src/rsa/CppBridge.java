package rsa;

import java.io.*;
import java.net.*;

public class CppBridge {
    private static final String HOST = "127.0.0.1";
    private static final int    PORT = 9000;

    public static String send(String json) throws IOException {
        try (Socket s = new Socket(HOST, PORT)) {
            s.setSoTimeout(15000);
            OutputStream os = s.getOutputStream();
            byte[] data = (json + "\n").getBytes("UTF-8");
            os.write(data);
            os.flush();

            InputStream is = s.getInputStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] tmp = new byte[8192];
            int n;
            while ((n = is.read(tmp)) != -1) {
                buf.write(tmp, 0, n);
                byte[] cur = buf.toByteArray();
                if (cur.length > 0 && cur[cur.length - 1] == '\n') break;
            }
            String result = buf.toString("UTF-8").trim();

            // DEBUG: in ra để xem bị cắt ở đâu
            System.out.println("[DEBUG] Raw response length = " + result.length());
            System.out.println("[DEBUG] Last 100 chars: " + result.substring(Math.max(0, result.length()-100)));
            System.out.println("[DEBUG] Contains public_key: " + result.contains("\"public_key\""));
            System.out.println("[DEBUG] Contains private_key: " + result.contains("\"private_key\""));

            return result;
        }
    }

    public static boolean isAlive() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(HOST, PORT), 500);
            return true;
        } catch (IOException e) { return false; }
    }

    /**
     * Parse giá trị string từ JSON thô (đã escape \\n → \n, v.v.)
     * Hỗ trợ value dài nhiều dòng như PEM key.
     */
    public static String get(String json, String key) {
        // Parse dạng string value: "key":"..."
        // Dùng Pattern để tìm chính xác, tránh lỗi khi value rất dài
        java.util.regex.Pattern strPat = java.util.regex.Pattern.compile(
            "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"",
            java.util.regex.Pattern.DOTALL
        );
        java.util.regex.Matcher m = strPat.matcher(json);
        if (m.find()) {
            // Unescape: \" → "   \n → newline   \\ → \
            return m.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\n",  "\n")
                    .replace("\\r",  "\r")
                    .replace("\\\\", "\\");
        }

        // Parse dạng non-string: "key":123
        java.util.regex.Pattern numPat = java.util.regex.Pattern.compile(
            "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*([^,}\\s]+)"
        );
        m = numPat.matcher(json);
        if (m.find()) return m.group(1).trim();

        return "";
    }

    public static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}