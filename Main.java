package org.example;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            org.example.TierheimService service = new org.example.TierheimService();
            org.example.MainFrame frame = new org.example.MainFrame();
            frame.setVisible(true);
        });
    }
}