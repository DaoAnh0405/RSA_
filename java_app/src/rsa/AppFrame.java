package rsa;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

public class AppFrame extends JFrame {

    private final KeyPanel     keyPanel     = new KeyPanel();
    private final EncryptPanel encryptPanel = new EncryptPanel();
    private final DecryptPanel decryptPanel = new DecryptPanel();

    // Store tab buttons directly
    private JButton btnTab1, btnTab2, btnTab3;

    public AppFrame() {
        setTitle("RSA EnDe - Nhóm 1");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        keyPanel.setKeyReadyListener((pub, priv) -> {
            encryptPanel.setKeys(pub, priv);
            decryptPanel.setPrivateKey(priv);
        });

        JPanel tabBar = buildTabBar();

        CardLayout cards = new CardLayout();
        JPanel contentArea = new JPanel(cards);
        contentArea.add(keyPanel,     "key");
        contentArea.add(encryptPanel, "enc");
        contentArea.add(decryptPanel, "dec");

        Color activeColor   = new Color(37, 99, 235);
        Color inactiveColor = new Color(55, 60, 75);

        Runnable select1 = () -> { cards.show(contentArea,"key"); styleTab(btnTab1,btnTab2,btnTab3,0,activeColor,inactiveColor); };
        Runnable select2 = () -> { cards.show(contentArea,"enc"); styleTab(btnTab1,btnTab2,btnTab3,1,activeColor,inactiveColor); };
        Runnable select3 = () -> { cards.show(contentArea,"dec"); styleTab(btnTab1,btnTab2,btnTab3,2,activeColor,inactiveColor); };

        btnTab1.addActionListener(e -> select1.run());
        btnTab2.addActionListener(e -> select2.run());
        btnTab3.addActionListener(e -> select3.run());

        wireTransferButton(encryptPanel, select3);

        select1.run();

        JPanel statusBar = buildStatusBar();

        setLayout(new BorderLayout());
        add(tabBar,     BorderLayout.NORTH);
        add(contentArea,BorderLayout.CENTER);
        add(statusBar,  BorderLayout.SOUTH);

        Timer check = new Timer(1500, e -> updateServerStatus(statusBar));
        check.setRepeats(true);
        check.start();
        updateServerStatus(statusBar);
    }

    private void wireTransferButton(EncryptPanel ep, Runnable goTab3) {
        findBtn(ep, "btnTransfer", btn -> {
            btn.addActionListener(e -> {
                String c = ep.getCiphertext();
                if (!c.isEmpty()) decryptPanel.setCipher(c);
                goTab3.run();
            });
        });
    }

    private void findBtn(Container cont, String name, java.util.function.Consumer<JButton> action) {
        for (Component c : cont.getComponents()) {
            if (c instanceof JButton && name.equals(c.getName())) {
                action.accept((JButton)c);
            } else if (c instanceof Container) {
                findBtn((Container)c, name, action);
            }
        }
    }

    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(22, 25, 35));

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabs.setBackground(new Color(22, 25, 35));

        // Create and store buttons as fields
        btnTab1 = tabBtn("1. Quản Lý Khóa");
        btnTab2 = tabBtn("2. Mã Hóa");
        btnTab3 = tabBtn("3. Giải Mã");
        tabs.add(btnTab1);
        tabs.add(btnTab2);
        tabs.add(btnTab3);

        JLabel appTitle = new JLabel("RSA System");
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        appTitle.setForeground(new Color(130,140,160));
        bar.add(tabs, BorderLayout.WEST);
        bar.add(appTitle, BorderLayout.EAST);
        return bar;
    }

    private JButton tabBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setForeground(new Color(180,185,200));
        b.setBackground(new Color(22,25,35));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBorder(new EmptyBorder(12, 22, 12, 22));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleTab(JButton b1, JButton b2, JButton b3, int active, Color ac, Color ic) {
        JButton[] all = {b1,b2,b3};
        for (int i = 0; i < all.length; i++) {
            if (i == active) {
                all[i].setBackground(ac);
                all[i].setForeground(Color.WHITE);
            } else {
                all[i].setBackground(new Color(22,25,35));
                all[i].setForeground(new Color(180,185,200));
            }
        }
    }

    private JLabel lblServerStatus;

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(15,17,25));
        bar.setBorder(new EmptyBorder(5,14,5,14));
        lblServerStatus = new JLabel("● Đang kiểm tra C++ server...");
        lblServerStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblServerStatus.setForeground(new Color(160,165,180));
        JLabel info = new JLabel("Java " + System.getProperty("java.version") + "  |  C++ RSA Server: port 9000");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        info.setForeground(new Color(90,95,110));
        bar.add(lblServerStatus, BorderLayout.WEST);
        bar.add(info, BorderLayout.EAST);
        return bar;
    }

    private void updateServerStatus(JPanel bar) {
        SwingWorker<Boolean,Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() { return CppBridge.isAlive(); }
            @Override protected void done() {
                try {
                    if (get()) {
                        lblServerStatus.setText("C++ Server: ĐANG CHẠY (port 9000)");
                        lblServerStatus.setForeground(new Color(34,197,94));
                    } else {
                        lblServerStatus.setText("C++ Server: KHÔNG KẾT NỐI — Hãy chạy rsa_server.exe");
                        lblServerStatus.setForeground(new Color(239,68,68));
                    }
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}