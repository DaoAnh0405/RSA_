package rsa;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.util.Random;

public class KeyPanel extends JPanel {

    private String publicKeyPem  = "";
    private String privateKeyPem = "";

    private final JTextField tfP   = field();
    private final JTextField tfQ   = field();
    private final JTextField tfE   = field();
    private final JTextArea  taN   = paramArea(3);
    private final JTextArea  taD   = paramArea(3);
    private final JTextArea  taPhi = paramArea(2);
    private final JLabel     lblStatus = new JLabel("Chưa tạo khóa");

    public interface KeyReadyListener { void onKeyReady(String pubPem, String privPem); }
    private KeyReadyListener keyReadyListener;
    public void setKeyReadyListener(KeyReadyListener l) { this.keyReadyListener = l; }

    public String getPublicKeyPem()  { return publicKeyPem;  }
    public String getPrivateKeyPem() { return privateKeyPem; }

    public KeyPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 246, 250));

        // ── Header ──────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(30, 34, 45));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));
        JLabel title = new JLabel("QUẢN LÝ KHÓA");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.add(title, BorderLayout.WEST);
        JLabel badge = new JLabel("C++ + Java");
        badge.setForeground(new Color(120, 220, 160));
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        header.add(badge, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Main split: LEFT input | RIGHT params ────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setResizeWeight(0.38);
        split.setDividerSize(6);
        split.setBorder(null);
        split.setLeftComponent(buildInputPanel());
        split.setRightComponent(buildParamsPanel());
        add(split, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(240, 242, 248));
        statusBar.setBorder(new EmptyBorder(6, 18, 6, 18));
        lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatus.setForeground(new Color(120, 120, 130));
        statusBar.add(lblStatus, BorderLayout.WEST);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JPanel buildInputPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 18, 20, 12));

        p.add(sectionLabel("Tạo khóa RSA"));
        p.add(Box.createVerticalStrut(14));

        p.add(inputRow("Số nguyên tố p:", tfP));
        p.add(Box.createVerticalStrut(10));
        p.add(inputRow("Số nguyên tố q:", tfQ));
        p.add(Box.createVerticalStrut(10));
        tfE.setText("65537");
        p.add(inputRow("Số mũ công khai e:", tfE));
        p.add(Box.createVerticalStrut(20));

        JButton btnGen    = makeBtn("Tạo Khóa",      new Color(34, 197, 94));
        JButton btnRandom = makeBtn("Random p, q",   new Color(99, 102, 241));
        JButton btnSave   = makeBtn("Lưu Khóa",      new Color(37, 99, 235));
        JButton btnClear  = makeBtn("Xóa",            new Color(180, 183, 190));

        for (JButton b : new JButton[]{btnGen, btnRandom, btnSave, btnClear}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            b.setAlignmentX(LEFT_ALIGNMENT);
            p.add(b);
            p.add(Box.createVerticalStrut(6));
        }

        btnGen.addActionListener(e -> doKeygen());
        btnRandom.addActionListener(e -> generateRandomPQ());
        btnSave.addActionListener(e -> saveKeys());
        btnClear.addActionListener(e -> clearAll());

        return p;
    }

    private JPanel buildParamsPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 12, 20, 18));

        p.add(sectionLabel("Tham số RSA"));
        p.add(Box.createVerticalStrut(12));
        p.add(paramRow("Modulus n:", taN));
        p.add(Box.createVerticalStrut(8));
        p.add(paramRow("Số mũ bí mật d:", taD));
        p.add(Box.createVerticalStrut(8));
        p.add(paramRow("Euler φ(n):", taPhi));

        return p;
    }

    private void doKeygen() {
        String p = tfP.getText().trim();
        String q = tfQ.getText().trim();
        String eVal = tfE.getText().trim();

        if (p.isEmpty() || q.isEmpty()) {
            showStatus("Vui lòng nhập p và q", new Color(234, 88, 12));
            return;
        }
        showStatus("Đang tính toán (C++ server)...", new Color(99, 102, 241));

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                if (!CppBridge.isAlive())
                    throw new Exception("C++ server chưa chạy!");
                String pHex = toHex(p);
                String qHex = toHex(q);
                String json = "{\"cmd\":\"keygen\","
                    + "\"p\":\"" + pHex + "\","
                    + "\"q\":\"" + qHex + "\","
                    + "\"e\":\"" + CppBridge.escapeJson(eVal.isEmpty() ? "0" : eVal) + "\"}";
                return CppBridge.send(json);
            }
            @Override protected void done() {
                try {
                    String resp = get();
                    if ("ok".equals(CppBridge.get(resp, "status"))) {
                        taN.setText(CppBridge.get(resp, "n"));
                        taD.setText(CppBridge.get(resp, "d"));
                        taPhi.setText(CppBridge.get(resp, "phi"));
                        if (eVal.isEmpty()) tfE.setText(CppBridge.get(resp, "e"));
                        
                        publicKeyPem  = CppBridge.get(resp, "public_key");
                        privateKeyPem = CppBridge.get(resp, "private_key");

                        System.out.println("[DEBUG] pubPem length  = " + publicKeyPem.length());
                        System.out.println("[DEBUG] privPem length = " + privateKeyPem.length());
                        System.out.println("[DEBUG] pubPem preview = " + publicKeyPem.substring(0, Math.min(50, publicKeyPem.length())));
                        
                        // ĐÃ SỬA: Check kỹ xem chuỗi PEM có bị trống không
                        if (publicKeyPem.isEmpty() || privateKeyPem.isEmpty()) {
                            showStatus("Lỗi: C++ tính xong nhưng trả về PEM rỗng!", new Color(220, 38, 38));
                        } else {
                            showStatus("Khóa đã tạo thành công! (C++ tính toán)", new Color(22, 163, 74));
                            if (keyReadyListener != null)
                                keyReadyListener.onKeyReady(publicKeyPem, privateKeyPem);
                        }
                    } else {
                        showStatus("❌ " + CppBridge.get(resp, "message"), new Color(220, 38, 38));
                    }
                } catch (Exception ex) {
                    showStatus("❌ " + ex.getMessage(), new Color(220, 38, 38));
                }
            }
        };
        worker.execute();
    }

    private void saveKeys() {
        // ĐÃ SỬA: Thêm check null an toàn hơn
        if (publicKeyPem == null || privateKeyPem == null || publicKeyPem.trim().isEmpty() || privateKeyPem.trim().isEmpty()) {
            showStatus("⚠ Chưa có khóa để lưu! Hãy tạo khóa trước.", new Color(234, 88, 12));
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("rsa_keys.txt"));
        fc.setDialogTitle("Lưu file khóa RSA");
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("=== RSA PUBLIC KEY ===\n");
                sb.append(publicKeyPem).append("\n");
                sb.append("=== RSA PRIVATE KEY ===\n");
                sb.append(privateKeyPem).append("\n");
                sb.append("=== PARAMETERS ===\n");
                sb.append("n = ").append(taN.getText()).append("\n");
                sb.append("d = ").append(taD.getText()).append("\n");
                sb.append("phi = ").append(taPhi.getText()).append("\n");
                Files.write(fc.getSelectedFile().toPath(), sb.toString().getBytes("UTF-8"));
                showStatus("✅ Đã lưu khóa: " + fc.getSelectedFile().getName(), new Color(22, 163, 74));
            } catch (Exception ex) {
                showStatus("❌ Lỗi lưu file: " + ex.getMessage(), new Color(220, 38, 38));
            }
        }
    }

    private void generateRandomPQ() {
        showStatus("⏳ Đang tạo số nguyên tố ngẫu nhiên...", new Color(99, 102, 241));
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            String pStr, qStr;
            @Override protected Void doInBackground() {
                Random rnd = new Random();
                BigInteger pp = BigInteger.probablePrime(512, rnd);
                BigInteger qq;
                do { qq = BigInteger.probablePrime(512, rnd); } while (qq.equals(pp));
                pStr = pp.toString(); qStr = qq.toString();
                return null;
            }
            @Override protected void done() {
                tfP.setText(pStr); tfQ.setText(qStr);
                showStatus("🎲 Đã tạo ngẫu nhiên p, q 512-bit", new Color(99, 102, 241));
            }
        };
        w.execute();
    }

    private void clearAll() {
        tfP.setText(""); tfQ.setText(""); tfE.setText("65537");
        taN.setText(""); taD.setText(""); taPhi.setText("");
        publicKeyPem = ""; privateKeyPem = "";
        showStatus("Chưa tạo khóa", new Color(120, 120, 130));
    }

    private void showStatus(String msg, Color c) {
        lblStatus.setText(msg); lblStatus.setForeground(c);
    }

    private String toHex(String s) {
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) return s.substring(2).toUpperCase();
        try { return new BigInteger(s).toString(16).toUpperCase(); }
        catch (NumberFormatException e) { return s.toUpperCase(); }
    }

    // ── UI helpers ───────────────────────────────────────────
    private static JTextField field() {
        JTextField tf = new JTextField();
        tf.setFont(new Font("Consolas", Font.PLAIN, 12));
        tf.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(210, 213, 220), 1, true),
            new EmptyBorder(6, 8, 6, 8)));
        return tf;
    }

    private static JTextArea paramArea(int rows) {
        JTextArea ta = new JTextArea(rows, 20);
        ta.setFont(new Font("Consolas", Font.PLAIN, 11));
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBackground(new Color(248, 249, 252));
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }

    private static JPanel inputRow(String label, JTextField tf) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(60, 65, 80));
        p.add(lbl, BorderLayout.NORTH);
        p.add(tf, BorderLayout.CENTER);
        return p;
    }

    private static JPanel paramRow(String label, JTextArea ta) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(Color.WHITE);
        p.setAlignmentX(LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(80, 85, 100));
        p.add(lbl, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(ta);
        sp.setBorder(new LineBorder(new Color(220, 223, 230), 1, true));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(new Color(40, 44, 60));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }
}