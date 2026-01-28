package org.example;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            org.example.MainFrame frame = new org.example.MainFrame();
            frame.setVisible(true);
        });
    }
}