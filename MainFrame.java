package org.example;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame() {
        super("Tierheim Verwaltung");

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setContentPane(tabs);

        // --- Create panels (no args)
        org.example.GreetingPanel greetingPanel = new org.example.GreetingPanel();   // 👈 Begrüßung
        org.example.AnimalsPanel animalsPanel = new org.example.AnimalsPanel();
        org.example.OpeningHoursPanel openingHoursPanel = new org.example.OpeningHoursPanel();
        org.example.FeedingPlanPanel feedingPlanPanel = new org.example.FeedingPlanPanel();

        // --- Add tabs
        tabs.addTab("Welcome", greetingPanel);   // 👈 first tab
        tabs.addTab("Animals", animalsPanel);
        tabs.addTab("Opening hours", openingHoursPanel);
        tabs.addTab("Feeding plan (today)", feedingPlanPanel);

        // --- Optional: reload feeding plan each time tab is selected
        /*
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == feedingPlanPanel) {
                feedingPlanPanel.reloadFromDb();
            }
        });
        */

        // --- Window size & position
        setPreferredSize(new Dimension(900, 600));
        pack();
        setLocationRelativeTo(null);
    }

    // Local launcher (App.java will also work)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}