package org.example;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class TierTableModel extends AbstractTableModel {

    // Column headers (no "Adopted")
    private final String[] cols = {
            "ID", "Name", "Species", "Age",
            "Inclosure ID", "Sex", "Color", "Arrival Date", "Health Note"
    };

    private List<org.example.Tier> data = new ArrayList<>();

    public void setData(List<org.example.Tier> list) {
        this.data = (list != null) ? list : new ArrayList<>();
        fireTableDataChanged();
    }

    public org.example.Tier getAt(int row) {
        if (row < 0 || row >= data.size()) return null;
        return data.get(row);
    }

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public String getColumnName(int col) { return cols[col]; }

    @Override
    public Object getValueAt(int row, int col) {
        org.example.Tier t = data.get(row);
        return switch (col) {
            case 0 -> t.getId();
            case 1 -> t.getName();
            case 2 -> t.getSpecies();
            case 3 -> t.getAge();            // derived from birthDate
            case 4 -> t.getInclosureId();
            case 5 -> t.getSex();
            case 6 -> t.getColor();
            case 7 -> t.getArrivalDate();
            case 8 -> t.getHealthNote();
            default -> "";
        };
    }
}