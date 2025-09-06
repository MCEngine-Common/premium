package io.github.mcengine.common.premium.database.postgresql;

import io.github.mcengine.common.premium.database.IMCEnginePremiumDB;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.UUID;

/**
 * PostgreSQL implementation of the Premium Common database using plugin configuration.
 */
public class MCEnginePremiumPostgreSQL implements IMCEnginePremiumDB {

    /**
     * Active SQL database connection instance.
     */
    private Connection connection;

    /**
     * Initializes PostgreSQL connection using plugin configuration.
     *
     * Required config paths in plugin.yml (config.yml):
     * - database.postgresql.host
     * - database.postgresql.port
     * - database.postgresql.database
     * - database.postgresql.user
     * - database.postgresql.password
     * - database.postgresql.sslmode   (optional, one of: disable, prefer, require) default: disable
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEnginePremiumPostgreSQL(Plugin plugin) {
        String host = plugin.getConfig().getString("database.postgresql.host", "localhost");
        String port = plugin.getConfig().getString("database.postgresql.port", "5432");
        String database = plugin.getConfig().getString("database.postgresql.database", "mcengine");
        String user = plugin.getConfig().getString("database.postgresql.user", "postgres");
        String password = plugin.getConfig().getString("database.postgresql.password", "");
        String sslmode = plugin.getConfig().getString("database.postgresql.sslmode", "disable"); // disable|prefer|require

        try {
            String url = "jdbc:postgresql://" + host + ":" + port + "/" + database + "?sslmode=" + sslmode;
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to PostgreSQL: " + e.getMessage());
        }
    }

    /**
     * Returns the current PostgreSQL database connection.
     *
     * @return active PostgreSQL connection
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean rankTableExists(String rankType) {
        if (connection == null) return false;
        String safe = (rankType == null ? "default" : rankType).toLowerCase().replaceAll("[^a-z0-9_]", "_");
        String table = "premium_rank_" + safe;
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = ?";
        try (var ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            try (var rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public java.util.List<String> listAvailableRankTypes() {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (connection == null) return out;
        final String prefix = "premium_rank_";
        final String sql =
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = current_schema() AND table_name LIKE 'premium_rank_%'";
        try (var ps = connection.prepareStatement(sql);
            var rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString(1);
                if (name != null && name.toLowerCase().startsWith(prefix)) {
                    out.add(name.substring(prefix.length()));
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    /**
     * Creates a premium rank table for the specified rank type if it does not exist.
     * Uses native UUID and INTEGER types for PostgreSQL.
     *
     * @param rankType Type of rank (e.g., vip, vvip)
     */
    @Override
    public void createPremiumRank(String rankType) {
        String table = tableName(rankType);
        String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" +
                     "  uuid UUID PRIMARY KEY," +
                     "  rank INTEGER NOT NULL" +
                     ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the rank of a player for the specified rank type from the database.
     *
     * @param uuid     Player UUID (string form with dashes)
     * @param rankType Rank type (e.g., vip, vvip)
     * @return Integer rank value, or -1 if not found
     */
    @Override
    public int getPremiumRank(String uuid, String rankType) {
        String table = tableName(rankType);
        String query = "SELECT rank FROM " + table + " WHERE uuid = ?";
        try (var pstmt = connection.prepareStatement(query)) {
            // Use UUID type binding for native uuid column
            pstmt.setObject(1, UUID.fromString(uuid));
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException | IllegalArgumentException e) { // IllegalArgumentException if UUID.fromString fails
            e.printStackTrace();
        }
        return -1; // not found
    }

    /**
     * Upgrades the rank of a user by 1. If user not exists, it inserts with rank = 1.
     *
     * @param uuid     Player UUID (string form with dashes)
     * @param rankType Rank type (e.g., vip, vvip)
     */
    @Override
    public void upgradePremiumRank(String uuid, String rankType) {
        String table = tableName(rankType);

        // Use PostgreSQL upsert for concise logic
        String upsert = "INSERT INTO " + table + " (uuid, rank) VALUES (?, 1) " +
                        "ON CONFLICT (uuid) DO UPDATE SET rank = " + table + ".rank + 1";

        try (var upsertStmt = connection.prepareStatement(upsert)) {
            upsertStmt.setObject(1, UUID.fromString(uuid));
            upsertStmt.executeUpdate();
        } catch (SQLException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the PostgreSQL database connection if open.
     */
    @Override
    public void disConnection() {
        try {
            if (connection != null && !connection.isClosed())
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a safe, lowercased table name for a rank type.
     * Prevents SQL injection through identifiers by restricting to [a-z0-9_].
     * Final form: premium_rank_{sanitizedRank}
     */
    private String tableName(String rankType) {
        String safe = (rankType == null ? "default" : rankType)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        return "premium_rank_" + safe;
    }
}
