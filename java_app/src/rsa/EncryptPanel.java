package rsa;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;

public class EncryptPanel extends JPanel {

    private String publicKeyPem  = "";
    private String privateKeyPem = "";

    private final JTextArea taPlain   = textArea("Nhập văn bản cần mã hóa hoặc chọn file...");
    private final JTextArea taCipher  = textArea("");
    private final JTextArea taPreview = textArea("");
    private final JLabel    lblKey    = new JLabel("Chưa có khóa — hãy tạo khóa");
    private final JLabel    lblFile   = new JLabel("Chưa chọn file");
    private final JLabel    lblStatus = new JLabel(" ");
    private final JLabel    lblPreviewInfo = new JLabel("Chọn file để xem preview...");

    public EncryptPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 246, 250));

        // ── Header ──────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 34, 45));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("MÃ HÓA");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title, BorderLayout.WEST);
        JLabel badge = new JLabel("C++ RSA Engine");
        badge.setForeground(new Color(251, 191, 36));
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        header.add(badge, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Key status bar ───────────────────────────────────
        JPanel keyBar = new JPanel(new BorderLayout());
        keyBar.setBackground(new Color(255, 251, 235));
        keyBar.setBorder(new EmptyBorder(6, 18, 6, 18));
        lblKey.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblKey.setForeground(new Color(234, 88, 12));
        keyBar.add(lblKey, BorderLayout.WEST);
        add(keyBar, BorderLayout.SOUTH);

        // ── Main split: LEFT | RIGHT ─────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.5);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setBackground(new Color(245, 246, 250));

        split.setLeftComponent(buildLeftPanel());
        split.setRightComponent(buildRightPanel());

        add(split, BorderLayout.CENTER);

        // Gán tên cho btnTransfer (được wire từ AppFrame)
        // đã set bên trong buildRightPanel()
    }

    // ── LEFT: chọn file + preview + văn bản gốc ─────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 14, 14, 8));

        // File chooser row
        JPanel fileRow = new JPanel(new BorderLayout(8, 0));
        fileRow.setBackground(Color.WHITE);
        fileRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        fileRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnFile = smallBtn("Chọn File (TXT)", new Color(70, 130, 200));
        lblFile.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblFile.setForeground(new Color(100, 105, 120));
        fileRow.add(btnFile, BorderLayout.WEST);
        fileRow.add(lblFile, BorderLayout.CENTER);
        p.add(fileRow);
        p.add(Box.createVerticalStrut(10));

        // Preview box
        JPanel previewBox = new JPanel(new BorderLayout());
        previewBox.setBackground(new Color(248, 249, 252));
        previewBox.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(180, 200, 230), 1, true),
            "Preview file",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11),
            new Color(80, 100, 150)
        ));
        previewBox.setAlignmentX(LEFT_ALIGNMENT);
        previewBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        lblPreviewInfo.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblPreviewInfo.setForeground(new Color(150, 155, 170));
        lblPreviewInfo.setBorder(new EmptyBorder(2, 4, 2, 4));

        taPreview.setEditable(false);
        taPreview.setBackground(new Color(248, 249, 252));
        taPreview.setForeground(new Color(60, 65, 80));
        taPreview.setFont(new Font("Consolas", Font.PLAIN, 11));
        JScrollPane spPreview = new JScrollPane(taPreview);
        spPreview.setBorder(null);

        previewBox.add(lblPreviewInfo, BorderLayout.NORTH);
        previewBox.add(spPreview, BorderLayout.CENTER);
        p.add(previewBox);
        p.add(Box.createVerticalStrut(10));

        // Văn bản gốc
        p.add(sectionLabel("Văn bản gốc:"));
        p.add(Box.createVerticalStrut(4));
        JScrollPane spPlain = scrollOf(taPlain, 0);
        p.add(spPlain);

        btnFile.addActionListener(e -> chooseFile());
        return p;
    }

    // ── RIGHT: encrypt button + kết quả + actions ────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(14, 8, 14, 14));

        // Encrypt button
        JButton btnEncrypt = bigBtn("TIẾN HÀNH MÃ HÓA", new Color(37, 99, 235));
        p.add(btnEncrypt);
        p.add(Box.createVerticalStrut(8));

        // Status
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(100,105,120));
        lblStatus.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblStatus);
        p.add(Box.createVerticalStrut(10));

        // Cipher area
        p.add(sectionLabel("Bản mã (Base64):"));
        p.add(Box.createVerticalStrut(4));
        taCipher.setEditable(false);
        taCipher.setBackground(new Color(248, 249, 252));
        taCipher.setForeground(new Color(30, 80, 160));
        JScrollPane spCipher = scrollOf(taCipher, 0);
        p.add(spCipher);
        p.add(Box.createVerticalStrut(12));

        // Thống kê
        JLabel lblStats = new JLabel(" ");
        lblStats.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblStats.setForeground(new Color(120, 125, 140));
        lblStats.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lblStats);
        p.add(Box.createVerticalStrut(10));

        // Bottom buttons
        JPanel botRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botRow.setBackground(Color.WHITE);
        botRow.setAlignmentX(LEFT_ALIGNMENT);
        JButton btnSave     = smallBtn("Lưu Bản Mã (.enc)", new Color(124, 58, 237));
        JButton btnTransfer = smallBtn("Chuyển sang Tab 3 >>", new Color(16, 185, 129));
        JButton btnCopy     = smallBtn("Sao chép", new Color(59, 130, 246));
        JButton btnNew      = smallBtn("Làm mới", new Color(160, 163, 170));
        botRow.add(btnSave);
        botRow.add(btnTransfer);
        botRow.add(btnCopy);
        botRow.add(btnNew);
        p.add(botRow);

        btnTransfer.setName("btnTransfer");

        // Listeners
        btnEncrypt.addActionListener(e -> doEncrypt(lblStats));
        btnSave.addActionListener(e -> saveCipher());
        btnCopy.addActionListener(e -> {
            String c = taCipher.getText().trim();
            if (!c.isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(c), null);
                showStatus("Đã sao chép bản mã vào clipboard!", new Color(22, 163, 74));
            }
        });
        btnNew.addActionListener(e -> { clearAll(); lblStats.setText(" "); });

        return p;
    }

    public void setKeys(String pubPem, String privPem) {
        this.publicKeyPem  = pubPem;
        this.privateKeyPem = privPem;
        // lblKey.setText("");
        lblKey.setForeground(new Color(22, 163, 74));
        lblKey.getParent().setBackground(new Color(240, 253, 244));
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("TXT, PDF, DOCX", "txt","pdf","docx","doc"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            lblFile.setText(f.getName());
            try {
                String content = new String(Files.readAllBytes(f.toPath()), "UTF-8");
                taPlain.setText(content);

                // Preview: hiện 500 ký tự đầu
                int previewLen = Math.min(content.length(), 500);
                String preview = content.substring(0, previewLen);
                if (content.length() > 500) preview += "\n... (còn " + (content.length()-500) + " ký tự)";
                taPreview.setText(preview);
                lblPreviewInfo.setText("📄 " + f.getName() + "  |  " + f.length() + " bytes  |  " + content.length() + " ký tự");
            } catch (Exception ex) {
                taPlain.setText("[File nhị phân — " + f.length() + " bytes]");
                taPreview.setText("[Không thể đọc nội dung file nhị phân]");
                lblPreviewInfo.setText("📄 " + f.getName() + "  |  " + f.length() + " bytes  |  Binary file");
            }
        }
    }

    private void doEncrypt(JLabel lblStats) {
        if (publicKeyPem.isEmpty()) {
            showStatus("Chưa có khóa!", new Color(220, 38, 38));
            return;
        }

        System.out.println("[ENCRYPT DEBUG] publicKeyPem length = " + publicKeyPem.length());
        System.out.println("[ENCRYPT DEBUG] publicKeyPem = \n" + publicKeyPem);

        String plain = taPlain.getText().trim();
        if (plain.isEmpty()) {
            showStatus("Vui lòng nhập văn bản cần mã hóa.", new Color(234, 88, 12));
            return;
        }
        showStatus("Đang mã hóa...", new Color(99, 102, 241));
        taCipher.setText("");
        lblStats.setText(" ");

        SwingWorker<String, Void> w = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                if (!CppBridge.isAlive())
                    throw new Exception("C++ server chưa chạy!");
                String json = "{\"cmd\":\"encrypt\","
                    + "\"public_key\":\"" + CppBridge.escapeJson(publicKeyPem) + "\","
                    + "\"plaintext\":\"" + CppBridge.escapeJson(plain) + "\"}";
                return CppBridge.send(json);
            }
            @Override protected void done() {
                try {
                    String resp = get();
                    String status = CppBridge.get(resp, "status");
                    if ("ok".equals(status)) {
                        String cipher = CppBridge.get(resp, "ciphertext");
                        taCipher.setText(cipher);
                        showStatus("Mã hóa thành công!", new Color(22, 163, 74));
                        lblStats.setText("Input: " + plain.length() + " ký tự  →  Output: " + cipher.length() + " ký tự Base64");
                    } else {
                        showStatus("❌ " + CppBridge.get(resp, "message"), new Color(220, 38, 38));
                    }
                } catch (Exception ex) {
                    showStatus("❌ " + ex.getMessage(), new Color(220, 38, 38));
                }
            }
        };
        w.execute();
    }

    private void saveCipher() {
        String cipher = taCipher.getText().trim();
        if (cipher.isEmpty()) {
            showStatus("Chưa có bản mã để lưu.", new Color(234, 88, 12)); return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("encrypted.enc"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("-----BEGIN RSA CIPHERTEXT-----\n");
                sb.append(cipher).append("\n");
                sb.append("-----END RSA CIPHERTEXT-----\n");
                sb.append("-----BEGIN PUBLIC KEY-----\n");
                sb.append(publicKeyPem).append("\n");
                sb.append("-----END PUBLIC KEY REFERENCE-----\n");
                Files.write(fc.getSelectedFile().toPath(), sb.toString().getBytes("UTF-8"));
                showStatus("✅ Đã lưu: " + fc.getSelectedFile().getName(), new Color(22, 163, 74));
            } catch (Exception ex) {
                showStatus("❌ Lỗi lưu file: " + ex.getMessage(), new Color(220, 38, 38));
            }
        }
    }

    public String getCiphertext() { return taCipher.getText().trim(); }

    private void clearAll() {
        taPlain.setText(""); taCipher.setText("");
        taPreview.setText("");
        lblFile.setText("Chưa chọn file");
        lblPreviewInfo.setText("Chọn file để xem preview...");
        showStatus(" ", Color.GRAY);
    }

    private void showStatus(String msg, Color c) {
        lblStatus.setText(msg); lblStatus.setForeground(c);
    }

    private static JTextArea textArea(String hint) {
        JTextArea ta = new JTextArea(hint);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }
    private static JScrollPane scrollOf(JTextArea ta, int h) {
        JScrollPane sp = new JScrollPane(ta);
        if (h > 0) {
            sp.setPreferredSize(new Dimension(Integer.MAX_VALUE, h));
            sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        }
        sp.setBorder(new LineBorder(new Color(210, 213, 220), 1, true));
        sp.setAlignmentX(LEFT_ALIGNMENT);
        return sp;
    }
    private static JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(50, 55, 70));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
    private static JButton smallBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setOpaque(true); b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(6, 12, 6, 12));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JButton bigBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setFocusPainted(false); b.setOpaque(true); b.setBorderPainted(false);
        b.setBorder(new EmptyBorder(12, 20, 12, 20));
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}