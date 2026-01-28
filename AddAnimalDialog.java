package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;

/** Dialog to add a new Animal (Tier). Capacity of the selected "Inclosure" is checked before closing OK. */
public class AddAnimalDialog extends JDialog {

    private boolean ok = false;

    // ---- Fields (as in your screenshots) ----
    private final JTextField txtName        = new JTextField(16);
    private final JTextField txtSpecies     = new JTextField(16);
    private final JTextField txtAge         = new JTextField(4);
    private final JTextField txtInclosureId = new JTextField(4);   // spelling "Inclosure" kept
    private final JComboBox<String> cboSex  = new JComboBox<>(new String[] {"M","F","U"});
    private final JTextField txtColor       = new JTextField(12);
    private final JTextField txtArrivalDate = new JTextField(10);  // YYYY-MM-DD
    private final JTextArea  txtHealthNote  = new JTextArea(3, 22);
    private final JTextField txtBirthDate   = new JTextField(10);  // YYYY-MM-DD (optional)

    public AddAnimalDialog(Window owner) {
        super(owner, "Add animal", ModalityType.APPLICATION_MODAL);
        buildUI();
        setLocationRelativeTo(owner);
        pack();
    }

    // Called by your code after setVisible(true)
    public boolean isOK() { return ok; }

    // ---------------- UI ----------------
    private void buildUI() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = gbc(0,0);
        g.insets = new Insets(10, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        form.add(new JLabel("Name:"),                        gbc(0,y)); form.add(txtName,        gbc(1,y++));
        form.add(new JLabel("Species:"),                     gbc(0,y)); form.add(txtSpecies,     gbc(1,y++));
        form.add(new JLabel("Age:"),                         gbc(0,y)); form.add(txtAge,         gbc(1,y++));
        form.add(new JLabel("Inclosure ID:"),                gbc(0,y)); form.add(txtInclosureId, gbc(1,y++));
        form.add(new JLabel("Sex:"),                         gbc(0,y)); form.add(cboSex,         gbc(1,y++));
        form.add(new JLabel("Color:"),                       gbc(0,y)); form.add(txtColor,       gbc(1,y++));
        form.add(new JLabel("Arrival Date (YYYY-MM-DD):"),   gbc(0,y)); form.add(txtArrivalDate, gbc(1,y++));
        form.add(new JLabel("Health Note:"),                 gbc(0,y)); form.add(new JScrollPane(txtHealthNote), gbc(1,y++));
        form.add(new JLabel("Birth Date (YYYY-MM-DD):"),     gbc(0,y)); form.add(txtBirthDate,   gbc(1,y++));

        JButton btnOk = new JButton(new AbstractAction("OK") {
            @Override public void actionPerformed(ActionEvent e) {
                // 1) Validate simple inputs
                if (!validateInputs()) return;

                // 2) Capacity check (DB)
                if (!checkEnclosureHasSpace()) {
                    // message shown inside; do not close dialog
                    return;
                }

                // 3) All good → caller will read values and do the insert
                ok = true;
                dispose();
            }
        });

        JButton btnCancel = new JButton(new AbstractAction("Cancel") {
            @Override public void actionPerformed(ActionEvent e) {
                ok = false;
                dispose();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(btnOk);
        buttons.add(btnCancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private GridBagConstraints gbc(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y;
        g.weightx = (x == 1) ? 1.0 : 0.0;
        g.fill = (x == 1) ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        return g;
    }

    // ---------------- Validation ----------------
    private boolean validateInputs() {
        if (getNameText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name is required."); return false;
        }
        if (getSpeciesText().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Species is required."); return false;
        }
        if (getInclosureId() == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid Inclosure ID (number)."); return false;
        }
        // dates optional but if filled then must parse
        String arr = txtArrivalDate.getText().trim();
        if (!arr.isEmpty() && getArrivalDate() == null) {
            JOptionPane.showMessageDialog(this, "Arrival Date must be YYYY-MM-DD or empty."); return false;
        }
        String birth = txtBirthDate.getText().trim();
        if (!birth.isEmpty() && getBirthDate() == null) {
            JOptionPane.showMessageDialog(this, "Birth Date must be YYYY-MM-DD or empty."); return false;
        }
        return true;
    }

    // ---------------- Capacity check ----------------
    /** Returns false if the enclosure is full (or invalid/not found). */
    private boolean checkEnclosureHasSpace() {
        Integer enclosureId = getInclosureId();
        if (enclosureId == null) return false;

        try (Connection conn = org.example.Database.getConnection()) {
            // 1) get capacity
            int capacity;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT capacity FROM enclosures WHERE id = ?")) {
                ps.setInt(1, enclosureId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Enclosure " + enclosureId + " not found.");
                        return false;
                    }
                    capacity = rs.getInt("capacity");
                }
            }

            // 2) count current animals in this enclosure
            int current;
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "SELECT COUNT(*) AS cnt FROM animals WHERE inclosure_id = ?")) {
                ps2.setInt(1, enclosureId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    rs2.next();
                    current = rs2.getInt("cnt");
                }
            }

            // 3) compare and report
            if (current >= capacity) {
                JOptionPane.showMessageDialog(this,
                        "Enclosure " + enclosureId + " is FULL!\n" +
                                "Capacity: " + capacity + " | Current: " + current);
                return false;
            }
            return true;

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Capacity check error: " + ex.getMessage());
            return false;
        }
    }

    // ---------------- Getters (as in your class) ----------------
    public String getNameText() { return txtName.getText().trim(); }
    public String getSpeciesText() { return txtSpecies.getText().trim(); }

    public Integer getAgeValue() {
        try { return Integer.parseInt(txtAge.getText().trim()); }
        catch (Exception e) { return null; }
    }

    /** Spelling kept to match your existing caller code. */
    public Integer getInclosureId() {
        try { return Integer.parseInt(txtInclosureId.getText().trim()); }
        catch (Exception e) { return null; }
    }

    public String getSex()   { return (String) cboSex.getSelectedItem(); }
    public String getColor() { return txtColor.getText().trim(); }

    public LocalDate getArrivalDate() {
        try { return LocalDate.parse(txtArrivalDate.getText().trim()); }
        catch (Exception e) { return null; }
    }

    public String getHealthNote() { return txtHealthNote.getText().trim(); }

    public LocalDate getBirthDate() {
        try { return LocalDate.parse(txtBirthDate.getText().trim()); }
        catch (Exception e) { return null; }
    }

    /** If birth date is empty, derive from age (today - age). */
    public LocalDate getBirthDateResolved() {
        LocalDate b = getBirthDate();
        if (b != null) return b;
        Integer age = getAgeValue();
        return (age == null) ? null : LocalDate.now().minusYears(age);
    }
}