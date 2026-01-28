package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class FeedingPlanPanel extends JPanel {

    private final org.example.FeedingPlanDao dao = new org.example.FeedingPlanDao();

    private JTable table;
    private org.example.FeedingPlanTableModel model;

    private final JComboBox<String> cbWeekday = new JComboBox<>(new String[]{
            "1 Mon", "2 Tue", "3 Wed", "4 Thu", "5 Fri", "6 Sat", "7 Sun"
    });

    private final JButton btnAdd = new JButton("Add");
    private final JButton btnEdit = new JButton("Edit");
    private final JButton btnDelete = new JButton("Delete");
    private final JButton btnRefresh = new JButton("Refresh");

    public FeedingPlanPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        buildTopBar();
        buildTable();
        selectToday();
        wireActions();
        load(); // initial
    }

    /* ---------- UI ---------- */

    private void buildTopBar() {
        JPanel top = new JPanel(new BorderLayout(6, 6));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JLabel("Weekday:"));
        left.add(cbWeekday);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(btnAdd);
        right.add(btnEdit);
        right.add(btnDelete);
        right.add(btnRefresh);

        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
    }

    private void buildTable() {
        model = new org.example.FeedingPlanTableModel(java.util.Collections.emptyList());
        table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        // optional: a little nicer column sizing
        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Animal
        table.getColumnModel().getColumn(1).setPreferredWidth(120); // Food
        table.getColumnModel().getColumn(2).setPreferredWidth(70);  // Quantity
        table.getColumnModel().getColumn(3).setPreferredWidth(60);  // Unit
        table.getColumnModel().getColumn(4).setPreferredWidth(80);  // Time
        table.getColumnModel().getColumn(5).setPreferredWidth(160); // Notes
        table.getColumnModel().getColumn(6).setPreferredWidth(140); // Medicine

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /* ---------- Actions ---------- */

    private void wireActions() {
        cbWeekday.addActionListener(e -> load());
        btnRefresh.addActionListener(e -> load());
        btnAdd.addActionListener(e -> addNew());
        btnEdit.addActionListener(e -> editSelected());
        btnDelete.addActionListener(e -> deleteSelected());
    }

    private void selectToday() {
        int today = LocalDate.now().getDayOfWeek().getValue(); // 1..7
        cbWeekday.setSelectedIndex(today - 1);
    }

    /* ---------- Data ---------- */

    private int selectedWeekday() {
        // combo items start with "1 Mon", "2 Tue", ...
        String item = (String) cbWeekday.getSelectedItem();
        if (item == null || item.isEmpty()) return DayOfWeek.MONDAY.getValue();
        return Integer.parseInt(item.substring(0, 1));
    }

    private void load() {
        try {
            List<org.example.FeedingPlanRow> rows = dao.listForWeekday(selectedWeekday());
            model = new org.example.FeedingPlanTableModel(rows);
            table.setModel(model);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
        }
    }

    private Frame ownerFrame() {
        Window w = SwingUtilities.getWindowAncestor(this);
        return (w instanceof Frame) ? (Frame) w : null;
    }

    private void addNew() {
        org.example.FeedingPlanEditDialog dlg = new org.example.FeedingPlanEditDialog(ownerFrame(), null);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;

        org.example.FeedingPlanRow row = dlg.getRow();
        row.setWeekday(selectedWeekday());

        try {
            dao.insert(row);
            load();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage());
        }
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row to edit.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        org.example.FeedingPlanRow current = model.getRow(modelRow);

        org.example.FeedingPlanEditDialog dlg = new org.example.FeedingPlanEditDialog(ownerFrame(), current);
        dlg.setVisible(true);
        if (!dlg.isSaved()) return;

        try {
            dao.update(dlg.getRow());
            load();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage());
        }
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this, "Delete selected row?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int modelRow = table.convertRowIndexToModel(viewRow);
        org.example.FeedingPlanRow r = model.getRow(modelRow);

        try {
            dao.deleteById(r.getId());
            load();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage());
        }
    }
}