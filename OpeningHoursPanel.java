package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public class OpeningHoursPanel extends JPanel {

    private final JLabel todayLabel = new JLabel("Today: ");
    private final JTable table = new JTable();
    private final org.example.OpeningHoursDao dao = new org.example.OpeningHoursDao();

    public OpeningHoursPanel() {
        setLayout(new BorderLayout(8,8));

        // Top: today's hours + actions
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton refresh = new JButton("Refresh");
        JButton edit = new JButton("Edit selected");
        top.add(todayLabel);
        top.add(refresh);
        top.add(edit);
        add(top, BorderLayout.NORTH);

        // Center: weekly table
        table.setModel(new DefaultTableModel(new Object[][]{}, new String[]{
                "Weekday (1-7)", "Status", "Opens", "Closes", "Note"
        }) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        });
        table.setRowHeight(22);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refresh.addActionListener(e -> loadData());
        edit.addActionListener(e -> onEdit());

        loadData(); // initial
    }

    private void loadData() {
        try {
            List<org.example.OpeningHours> list = dao.findAll();
            DefaultTableModel m = (DefaultTableModel) table.getModel();
            m.setRowCount(0);
            for (org.example.OpeningHours oh : list) {
                m.addRow(new Object[]{
                        oh.getWeekday() + " - " + weekdayName(oh.getWeekday()),
                        oh.isOpen() ? "Open" : "Closed",
                        oh.isOpen() && oh.getOpenTime() != null ? oh.getOpenTime().toString().substring(0,5) : "-",
                        oh.isOpen() && oh.getCloseTime() != null ? oh.getCloseTime().toString().substring(0,5) : "-",
                        oh.getNote()
                });
            }

            // today's hours
            int todayIso = DayOfWeek.from(LocalDate.now()).getValue(); // 1..7
            org.example.OpeningHours today = dao.findByWeekday(todayIso);
            if (today == null) {
                todayLabel.setText("Today: (no data)");
            } else if (!today.isOpen()) {
                todayLabel.setText("Today: Closed" + (today.getNote() != null ? " — " + today.getNote() : ""));
            } else {
                String open = today.getOpenTime() == null ? "?" : today.getOpenTime().toString().substring(0,5);
                String close = today.getCloseTime() == null ? "?" : today.getCloseTime().toString().substring(0,5);
                todayLabel.setText("Today: " + open + "–" + close + (today.getNote() != null ? " — " + today.getNote() : ""));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Load opening hours failed: " + ex.getMessage());
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a weekday row first.");
            return;
        }

        // Determine selected weekday from first column ("N - Name")
        String firstCol = table.getValueAt(row, 0).toString();
        int dash = firstCol.indexOf(' ');
        int weekday = Integer.parseInt(dash > 0 ? firstCol.substring(0, dash) : firstCol);

        try {
            org.example.OpeningHours current = dao.findByWeekday(weekday);
            org.example.OpeningHoursEditDialog dlg = new org.example.OpeningHoursEditDialog(SwingUtilities.getWindowAncestor(this), current);
            dlg.setVisible(true);
            if (dlg.isOk()) {
                org.example.OpeningHours updated = dlg.getValue();
                dao.upsert(updated);
                loadData();
                JOptionPane.showMessageDialog(this, "Saved hours for " + weekdayName(updated.getWeekday()));
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid time format. Use HH:mm (e.g., 09:00).");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    private String weekdayName(int iso) {
        return switch (iso) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Day " + iso;
        };
    }
}