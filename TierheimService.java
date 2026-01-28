package org.example;

import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Service/DAO for MySQL tables: animals + enclosures.
 * - No 'adopted' column.
 * - Age is computed from birth_date.
 * - Insert derives birth_date from age when needed.
 * - Capacity helpers.
 */
public class TierheimService {

    private static final String DB_URL  = org.example.TestConnection.DB_URL;
    private static final String DB_USER = org.example.TestConnection.DB_USER;
    private static final String DB_PASS = org.example.TestConnection.DB_PASS;

    static {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) { throw new RuntimeException("MySQL JDBC Driver not found", e); }
    }

    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /* ---------- helpers: read nullable ints safely ---------- */

    private static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /* ===================== READ ===================== */

    public List<org.example.Tier> findAll() throws SQLException {
        String sql = """
            SELECT
              id, name, species,
              TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) AS computed_age,
              inclosure_id, sex, color, arrival_date, health_note, birth_date
            FROM animals
            ORDER BY id DESC
        """;

        List<org.example.Tier> list = new ArrayList<>();
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String species = rs.getString("species");

                Integer age = getNullableInt(rs, "computed_age");     // SAFE (Integer or null)
                Integer inclosureId = getNullableInt(rs, "inclosure_id");

                String sex = rs.getString("sex");
                String color = rs.getString("color");

                java.sql.Date arr = rs.getDate("arrival_date");
                LocalDate arrival = (arr == null) ? null : arr.toLocalDate();

                String health = rs.getString("health_note");

                java.sql.Date bd = rs.getDate("birth_date");
                LocalDate birthDate = (bd == null) ? null : bd.toLocalDate();

                org.example.Tier t = new org.example.Tier(
                        id, name, species, age, null, inclosureId, sex, color, arrival, health, birthDate
                );
                list.add(t);
            }
        }
        return list;
    }

    /* ===================== INSERT ===================== */

    public int insert(org.example.Tier t) throws SQLException {
        if (t.getInclosureId() != null && !hasSpaceInEnclosure(t.getInclosureId())) {
            throw new SQLException("Enclosure " + t.getInclosureId() + " is full");
        }

        LocalDate birth = t.getBirthDate();
        if (birth == null && t.getAge() != null) {
            birth = LocalDate.now().minusYears(t.getAge());
        }
        if (animalExists(t.getName(), t.getSpecies(), birth)) {
            throw new SQLException("Animal already exists (same name, species, birth_date).");
        }

        Integer ageToStore = (birth == null) ? t.getAge()
                : Period.between(birth, LocalDate.now()).getYears();

        String sql = """
            INSERT INTO animals
              (name, species, age, inclosure_id, sex, color, arrival_date, health_note, birth_date)
            VALUES
              (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, t.getName());
            ps.setString(2, t.getSpecies());

            if (ageToStore == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, ageToStore);

            if (t.getInclosureId() == null) ps.setNull(4, Types.INTEGER);
            else ps.setInt(4, t.getInclosureId());

            ps.setString(5, t.getSex());
            ps.setString(6, t.getColor());

            if (t.getArrivalDate() == null) ps.setNull(7, Types.DATE);
            else ps.setDate(7, java.sql.Date.valueOf(t.getArrivalDate()));

            ps.setString(8, t.getHealthNote());

            if (birth == null) ps.setNull(9, Types.DATE);
            else ps.setDate(9, java.sql.Date.valueOf(birth));

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return 0;
    }

    /* ===================== DELETE ===================== */

    public void deleteById(int id) throws SQLException {
        String sql = "DELETE FROM animals WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /* ================= DUPLICATE CHECK ================= */

    public boolean animalExists(String name, String species, LocalDate birthDate) throws SQLException {
        String sql = """
            SELECT COUNT(*)
            FROM animals
            WHERE name = ? AND species = ?
              AND ((birth_date IS NULL AND ? IS NULL) OR birth_date = ?)
        """;
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, species);

            if (birthDate == null) {
                ps.setNull(3, Types.DATE);
                ps.setNull(4, Types.DATE);
            } else {
                java.sql.Date d = java.sql.Date.valueOf(birthDate);
                ps.setDate(3, d);
                ps.setDate(4, d);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /* ================== CAPACITY HELPERS ================== */

    public int countALL() throws SQLException {
        String sql = "SELECT COUNT(*) FROM animals";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int getEnclosureCapacity(int enclosureId) throws SQLException {
        String sql = "SELECT capacity FROM enclosures WHERE id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enclosureId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public int countByEnclosure(int enclosureId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM animals WHERE inclosure_id = ?";
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, enclosureId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public boolean hasSpaceInEnclosure(Integer enclosureId) throws SQLException {
        if (enclosureId == null) return true;
        int cap = getEnclosureCapacity(enclosureId);
        if (cap <= 0) return false;
        int current = countByEnclosure(enclosureId);
        return current < cap;
    }
}