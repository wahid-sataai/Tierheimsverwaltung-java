package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDate;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** Animals tab (grid + actions). */
public class AnimalsPanel extends JPanel {

    private static final int MAX_CAPACITY = 100;

    // UI
    private final JTable table = new JTable();
    private final JButton btnAdd = new JButton("Add");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnCheckCapacity = new JButton("Check Capacity");
    private final JLabel capacityLabel = new JLabel("Current: ? / " + MAX_CAPACITY);

    // Your existing service
    private final org.example.TierheimService service = new org.example.TierheimService();

    public AnimalsPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        buildTopBar();
        buildTable();
        wireActions();

        loadData();
        refreshCapacityLabel();
    }

    /* ---------- UI ---------- */

    private void buildTopBar() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        left.add(btnAdd);
        left.add(btnDelete);
        left.add(btnRefresh);
        left.add(btnCheckCapacity);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        right.add(capacityLabel);

        JPanel top = new JPanel(new BorderLayout());
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
    }

    private void buildTable() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[][]{},
                new Object[]{
                        "ID", "Name", "Species", "Age",
                        "Inclosure ID", "Sex", "Color",
                        "Arrival date", "HealthNote"
                }) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table.setModel(model);
        table.setRowHeight(22);
        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /* ---------- Actions ---------- */

    private void wireActions() {
        btnRefresh.addActionListener((ActionEvent e) -> {
            loadData();
            refreshCapacityLabel();
        });
        btnAdd.addActionListener((ActionEvent e) -> add());
        btnDelete.addActionListener((ActionEvent e) -> onDelete());
        btnCheckCapacity.addActionListener((ActionEvent e) -> checkCapacityFromDb());
    }

    /* ---------- Data ops ---------- */

    private void loadData() {
        try {
            DefaultTableModel m = (DefaultTableModel) table.getModel();
            m.setRowCount(0);
            List<org.example.Tier> list = service.findAll();

            for (org.example.Tier a : list) {
                Integer age = a.getAge();
                Integer incId = a.getInclosureId();
                LocalDate arrival = a.getArrivalDate();

                m.addRow(new Object[]{
                        a.getId(),
                        a.getName(),
                        a.getSpecies(),
                        age,
                        incId,
                        a.getSex(),
                        a.getColor(),
                        arrival,
                        a.getHealthNote()
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
        }
    }

    private void refreshCapacityLabel() {
        try {
            int current = service.countALL();
            capacityLabel.setText("Current: " + current + " / " + MAX_CAPACITY);
        } catch (Exception ex) {
            capacityLabel.setText("Current: ? / " + MAX_CAPACITY);
        }
    }

    private void add() {
        org.example.AddAnimalDialog dlg = new org.example.AddAnimalDialog(SwingUtilities.getWindowAncestor(this));
        dlg.setVisible(true);
        if (!dlg.isOK()) return;

        // IMPORTANT: Tier constructor expects Boolean 'adopted' BEFORE inclosureId
        org.example.Tier t = new org.example.Tier(
                0,
                dlg.getNameText(),
                dlg.getSpeciesText(),
                dlg.getAgeValue(),
                false,                    // adopted (Boolean) – set to false by default
                dlg.getInclosureId(),     // Integer
                dlg.getSex(),
                dlg.getColor(),
                dlg.getArrivalDate(),
                dlg.getHealthNote(),
                dlg.getBirthDateResolved() // LocalDate or null
        );

        try {
            service.insert(t);
            loadData();
            refreshCapacityLabel();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Please select a row."); return; }

        Object v = table.getValueAt(row, 0); // ID column
        if (!(v instanceof Number)) { JOptionPane.showMessageDialog(this, "Invalid ID."); return; }
        int id = ((Number) v).intValue();

        int ok = JOptionPane.showConfirmDialog(this,
                "Delete animal ID " + id + "?", "Confirm",
                JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        try {
            service.deleteById(id);
            loadData();
            refreshCapacityLabel();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }

    /* ---------- Capacity check ---------- */

    private void checkCapacityFromDb() {
        try {
            // Prefer enclosure from selected row; if none, prompt.
            Integer encId = getSelectedInclosureIdOrNull();
            if (encId == null) {
                String s = JOptionPane.showInputDialog(this, "enclosure id?");
                if (s == null || s.isBlank()) return;
                try { encId = Integer.valueOf(s.trim()); }
                catch (NumberFormatException nfe) {
                    JOptionPane.showMessageDialog(this, "Invalid enclosure id."); return;
                }
            }

            try (Connection conn = org.example.Database.getConnection()) {
                // 1) capacity from enclosures
                int capacity;
                try (PreparedStatement ps1 =
                             conn.prepareStatement("SELECT capacity FROM enclosures WHERE id = ?")) {
                    ps1.setInt(1, encId);
                    try (ResultSet rs1 = ps1.executeQuery()) {
                        if (!rs1.next()) {
                            JOptionPane.showMessageDialog(this, "Enclosure " + encId + " not found!");
                            return;
                        }
                        capacity = rs1.getInt("capacity");
                    }
                }


                String countSql = "SELECT COUNT(*) AS cnt FROM animals WHERE inclosure_id = ?";
                int current;
                try (PreparedStatement ps2 = conn.prepareStatement(countSql)) {
                    ps2.setInt(1, encId);
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        rs2.next();
                        current = rs2.getInt("cnt");
                    }
                }

                // 3) result
                if (current >= capacity) {
                    JOptionPane.showMessageDialog(this,
                            "Enclosure " + encId + " is FULL!\n" +
                                    "Capacity: " + capacity + " | Current: " + current);
                } else {
                    int free = capacity - current;
                    JOptionPane.showMessageDialog(this,
                            "Enclosure " + encId + " has " + current + " animals.\n" +
                                    "Capacity: " + capacity + "\nFree spots: " + free);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Check failed: " + ex.getMessage());
        }
    }

    // Reads "Inclosure ID" from selected row (column 4). Returns null if none/invalid.
    private Integer getSelectedInclosureIdOrNull() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        Object v = table.getValueAt(row, 4);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.valueOf(v.toString().trim()); }
        catch (Exception e) { return null; }
    }
}