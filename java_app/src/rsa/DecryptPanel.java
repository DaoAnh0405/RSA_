package rsa;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import javax.crypto.Cipher;

public class DecryptPanel extends JPanel {

    private String loadedCipherB64 = "";
    private String loadedPrivKeyPem = "";

    private final JTextArea taCipher  = textArea(5);
    private final JTextArea taKey     = textArea(6);
    private final JTextArea taResult  = textArea(6);
    private final JLabel    lblCipherFile = new JLabel("Chưa nạp file bản mã");
    private final JLabel    lblKeyFile    = new JLabel("Chưa nạp khóa");
    private final JLabel    lblStatus = statusLabel();

    // Integrity check: store hash of original cipher & key at load time
    private String origCipherHash = "";
    private String origKeyHash    = "";

    public DecryptPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 34, 45));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("GIẢI MÃ");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title, BorderLayout.WEST);
        JLabel badge = new JLabel("Java Decrypt");
        badge.setForeground(new Color(129, 230, 217));
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        header.add(badge, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ── Cipher file ──────────────────────────────────────
        body.add(sectionLabel("Bản mã cần giải:"));
        body.add(Box.createVerticalStrut(4));

        JPanel cRow = new JPanel(new BorderLayout(8, 0));
        cRow.setBackground(Color.WHITE);
        cRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JButton btnLoadCipher = smallBtn("Mở File Bản Mã", new Color(70, 130, 200));
        lblCipherFile.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCipherFile.setForeground(new Color(100,105,120));
        cRow.add(btnLoadCipher, BorderLayout.WEST);
        cRow.add(lblCipherFile, BorderLayout.CENTER);
        body.add(cRow);
        body.add(Box.createVerticalStrut(6));

        taCipher.setEditable(true);
        taCipher.setBackground(new Color(248,249,252));
        body.add(scrollOf(taCipher, 100));
        body.add(Box.createVerticalStrut(10));

        // ── Private key ──────────────────────────────────────
        body.add(sectionLabel("Private Key (PEM):"));
        body.add(Box.createVerticalStrut(4));

        JPanel kRow = new JPanel(new BorderLayout(8, 0));
        kRow.setBackground(Color.WHITE);
        kRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JButton btnLoadKey = smallBtn("Mở File Private Key", new Color(234, 88, 12));
        lblKeyFile.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblKeyFile.setForeground(new Color(100,105,120));
        kRow.add(btnLoadKey, BorderLayout.WEST);
        kRow.add(lblKeyFile, BorderLayout.CENTER);
        body.add(kRow);
        body.add(Box.createVerticalStrut(6));

        taKey.setEditable(true);
        taKey.setBackground(new Color(248,249,252));
        body.add(scrollOf(taKey, 100));
        body.add(Box.createVerticalStrut(12));

        // ── Decrypt button ───────────────────────────────────
        JButton btnDecrypt = bigBtn("GIẢI MÃ", new Color(107, 33, 168));
        body.add(btnDecrypt);
        body.add(Box.createVerticalStrut(8));

        // ── Status badge ─────────────────────────────────────
        body.add(lblStatus);
        body.add(Box.createVerticalStrut(10));

        // ── Result ───────────────────────────────────────────
        body.add(sectionLabel("Kết quả giải mã:"));
        body.add(Box.createVerticalStrut(4));
        taResult.setEditable(false);
        taResult.setBackground(new Color(248,249,252));
        body.add(scrollOf(taResult, 120));
        body.add(Box.createVerticalStrut(10));

        // Bottom buttons
        JPanel botRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botRow.setBackground(Color.WHITE);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // ── Listeners ────────────────────────────────────────
        btnLoadCipher.addActionListener(e -> loadCipherFile());
        btnLoadKey.addActionListener(e -> loadKeyFile());
        btnDecrypt.addActionListener(e -> doDecrypt());
    }

    // ── Load bản mã ─────────────────────────────────────────
    private void loadCipherFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        lblCipherFile.setText(f.getName());
        try {
            String raw = new String(Files.readAllBytes(f.toPath()), "UTF-8");
            String cipher = extractBlock(raw, "-----BEGIN RSA CIPHERTEXT-----", "-----END RSA CIPHERTEXT-----");
            if (cipher.isEmpty()) cipher = raw.trim();
            String cleanedCipher = cipher.replaceAll("\\s+", "");
            taCipher.setText(cleanedCipher);
            loadedCipherB64 = cleanedCipher;
            origCipherHash = sha256(cleanedCipher);
        } catch (Exception ex) {
            showStatus("❌ Lỗi đọc file: " + ex.getMessage(), new Color(220,38,38), "❌");
        }
    }

    // ── Load private key ─────────────────────────────────────
    private void loadKeyFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("PEM/Key files", "pem","key","txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        lblKeyFile.setText(f.getName());
        try {
            String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");

            String pem = extractBlock(content,
                "-----BEGIN RSA PRIVATE KEY-----",
                "-----END RSA PRIVATE KEY-----");

            if (pem.isEmpty()) {
                pem = extractBlock(content,
                    "-----BEGIN PRIVATE KEY-----",
                    "-----END PRIVATE KEY-----");
            }

            if (!pem.isEmpty()) {
                String fullPem;
                if (content.contains("BEGIN RSA PRIVATE KEY")) {
                    fullPem = "-----BEGIN RSA PRIVATE KEY-----\n" + pem + "\n-----END RSA PRIVATE KEY-----\n";
                } else {
                    fullPem = "-----BEGIN PRIVATE KEY-----\n" + pem + "\n-----END PRIVATE KEY-----\n";
                }
                taKey.setText(fullPem);
                loadedPrivKeyPem = fullPem;
            } else {
                taKey.setText(content);
                loadedPrivKeyPem = content;
            }

            // SỬA: chuẩn hóa line endings trước khi hash
            origKeyHash = sha256(normalizeKey(loadedPrivKeyPem));
        } catch (Exception ex) {
            showStatus("Lỗi đọc khóa: " + ex.getMessage(), new Color(220,38,38), "❌");
        }
    }

    // ── Giải mã (Java) ──────────────────────────────────────
    private void doDecrypt() {
        String cipherB64 = taCipher.getText().replaceAll("\\s+", "");
        // SỬA: dùng normalizeKey thay vì trim() để đồng nhất với lúc hash
        String keyPem    = normalizeKey(taKey.getText());

        if (cipherB64.isEmpty() || keyPem.isEmpty()) {
            showStatus("Vui lòng nạp bản mã và khóa.", new Color(234,88,12), "⚠");
            return;
        }

        showStatus("Đang giải mã...", new Color(99,102,241), "⏳");
        taResult.setText("");

        SwingWorker<String, Void> w = new SwingWorker<>() {
            String errorType = "";
            @Override protected String doInBackground() throws Exception {
                boolean cipherModified = !origCipherHash.isEmpty() && !sha256(cipherB64).equals(origCipherHash);
                // SỬA: so sánh hash với keyPem đã được normalizeKey ở trên
                boolean keyModified    = !origKeyHash.isEmpty()    && !sha256(keyPem).equals(origKeyHash);

                if (cipherModified && keyModified) { errorType = "both"; return null; }
                if (cipherModified) { errorType = "cipher"; return null; }
                if (keyModified)    { errorType = "key";    return null; }

                try {
                    PrivateKey privateKey = loadPrivateKey(keyPem);
                    byte[] cipherBytes = Base64.getDecoder().decode(cipherB64);
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                    cipher.init(Cipher.DECRYPT_MODE, privateKey);
                    byte[] plain = cipher.doFinal(cipherBytes);
                    return new String(plain, "UTF-8");
                } catch (InvalidKeyException ex) {
                    errorType = "key"; return null;
                } catch (Exception ex) {
                    String msg = ex.getMessage();
                    if (msg != null && (msg.contains("key") || msg.contains("Key")))
                        errorType = "key";
                    else
                        errorType = "cipher";
                    return null;
                }
            }

            @Override protected void done() {
                try {
                    String result = get();
                    switch (errorType) {
                        case "both":
                            showStatus("KHÓA VÀ BẢN MÃ ĐỀU BỊ SỬA ĐỔI", new Color(153,27,27), "🔴");
                            taResult.setText("[Không thể giải mã — cả khóa lẫn bản mã đều không hợp lệ]");
                            break;
                        case "cipher":
                            showStatus("BẢN MÃ BỊ SỬA ĐỔI", new Color(194,65,12), "🟠");
                            taResult.setText("[Bản mã bị hỏng hoặc đã bị sửa đổi — không thể giải mã]");
                            break;
                        case "key":
                            showStatus("KHÓA KHÔNG ĐÚNG", new Color(161,98,7), "🟡");
                            taResult.setText("[Khóa private không khớp với bản mã — kiểm tra lại khóa]");
                            break;
                        default:
                            if (result != null && !result.isEmpty()) {
                                showStatus("GIẢI MÃ THÀNH CÔNG", new Color(22,163,74), "✅");
                                taResult.setText(result);
                            } else {
                                showStatus("GIẢI MÃ THẤT BẠI", new Color(220,38,38), "❌");
                            }
                    }
                } catch (Exception ex) {
                    showStatus("❌ " + ex.getMessage(), new Color(220,38,38), "❌");
                }
            }
        };
        w.execute();
    }

    // ── Kiểm tra toàn vẹn không giải mã ────────────────────
    private void verifyIntegrity() {
        String currentCipher = taCipher.getText().replaceAll("\\s+", "");
        String currentKey    = normalizeKey(taKey.getText());

        boolean cipherOk = origCipherHash.isEmpty() || sha256(currentCipher).equals(origCipherHash);
        boolean keyOk    = origKeyHash.isEmpty()    || sha256(currentKey).equals(origKeyHash);

        if (cipherOk && keyOk)
            showStatus("Toàn vẹn xác nhận — Bản mã và Khóa nguyên vẹn", new Color(22,163,74), "✅");
        else if (!cipherOk && !keyOk)
            showStatus("Cả BẢN MÃ lẫn KHÓA đều bị thay đổi!", new Color(153,27,27), "🔴");
        else if (!cipherOk)
            showStatus("BẢN MÃ đã bị thay đổi kể từ khi nạp!", new Color(194,65,12), "🟠");
        else
            showStatus("KHÓA đã bị thay đổi kể từ khi nạp!", new Color(161,98,7), "🟡");
    }

    // ── Set cipher từ Tab 2 ──────────────────────────────────
    public void setCipher(String b64) {
        String cleaned = b64.replaceAll("\\s+", "");
        taCipher.setText(cleaned);
        loadedCipherB64 = cleaned;
        origCipherHash = sha256(cleaned);
        lblCipherFile.setText("(từ Tab 2)");
    }

    // ── Set private key từ Tab 1 ─────────────────────────────
    public void setPrivateKey(String pem) {
        taKey.setText(pem);
        loadedPrivKeyPem = pem;
        origKeyHash = sha256(normalizeKey(pem));
        lblKeyFile.setText("(từ Tab 1)");
    }

    private void clearAll() {
        taCipher.setText(""); taKey.setText(""); taResult.setText("");
        lblCipherFile.setText("Chưa nạp file bản mã");
        lblKeyFile.setText("Chưa nạp khóa");
        loadedCipherB64 = ""; loadedPrivKeyPem = "";
        origCipherHash = ""; origKeyHash = "";
        showStatus("", Color.GRAY, "");
    }

    // ── RSA decrypt helpers ──────────────────────────────────
    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String stripped = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(stripped);
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            byte[] pkcs8 = convertPkcs1ToPkcs8(der);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        }
    }

    /** Wrap PKCS#1 DER inside PKCS#8 envelope */
    private byte[] convertPkcs1ToPkcs8(byte[] pkcs1) {
        byte[] oid = {0x30,0x0d,0x06,0x09,0x2a,(byte)0x86,0x48,(byte)0x86,(byte)0xf7,0x0d,0x01,0x01,0x01,0x05,0x00};
        byte[] octet = buildDer(0x04, pkcs1);
        byte[] inner = new byte[oid.length + octet.length];
        System.arraycopy(oid, 0, inner, 0, oid.length);
        System.arraycopy(octet, 0, inner, oid.length, octet.length);
        byte[] version = {0x02,0x01,0x00};
        byte[] fullInner = new byte[version.length + inner.length];
        System.arraycopy(version, 0, fullInner, 0, version.length);
        System.arraycopy(inner, 0, fullInner, version.length, inner.length);
        return buildDer(0x30, fullInner);
    }

    private byte[] buildDer(int tag, byte[] content) {
        int len = content.length;
        byte[] lenBytes;
        if (len < 128) lenBytes = new byte[]{(byte)len};
        else if (len < 256) lenBytes = new byte[]{(byte)0x81,(byte)len};
        else lenBytes = new byte[]{(byte)0x82,(byte)(len>>8),(byte)(len&0xff)};
        byte[] result = new byte[1 + lenBytes.length + content.length];
        result[0] = (byte)tag;
        System.arraycopy(lenBytes,0,result,1,lenBytes.length);
        System.arraycopy(content,0,result,1+lenBytes.length,content.length);
        return result;
    }

    private String extractBlock(String text, String begin, String end) {
        int s = text.indexOf(begin);
        int e = text.indexOf(end);
        if (s < 0 || e < 0) return "";
        return text.substring(s + begin.length(), e).trim();
    }

    // ── Chuẩn hóa line endings để hash nhất quán ────────────
    private String normalizeKey(String pem) {
        return pem.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n").trim();
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return s.hashCode() + ""; }
    }

    private void showStatus(String msg, Color bg, String icon) {
        lblStatus.setText("  " + msg + "  ");
        lblStatus.setBackground(bg.brighter().brighter());
        lblStatus.setForeground(bg.darker().darker());
        lblStatus.setOpaque(true);
    }

    // ── UI helpers ───────────────────────────────────────────
    private static JTextArea textArea(int rows) {
        JTextArea ta = new JTextArea(rows, 20);
        ta.setFont(new Font("Consolas", Font.PLAIN, 11));
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }
    private static JScrollPane scrollOf(JTextArea ta, int h) {
        JScrollPane sp = new JScrollPane(ta);
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        sp.setBorder(new LineBorder(new Color(210,213,220),1,true));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        return sp;
    }
    private static JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(50,55,70));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private static JLabel statusLabel() {
        JLabel l = new JLabel("  Chờ giải mã...  ");
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setBorder(new EmptyBorder(6,10,6,10));
        l.setBackground(new Color(230,230,235));
        l.setForeground(new Color(80,85,100));
        l.setOpaque(true);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private static JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setOpaque(true); b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(6,12,6,12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JButton bigBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setOpaque(true); b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(12,20,12,20));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}