package io.github.mcengine.common.premium.database.mysql;

import io.github.mcengine.common.premium.database.IMCEnginePremiumDB;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL implementation of the Premium Common database using plugin configuration.
 */
public class MCEnginePremiumMySQL implements IMCEnginePremiumDB {

    /**
     * Active SQL database connection instance.
     */
    private Connection connection;

    /**
     * Initializes MySQL connection using plugin configuration.
     *
     * Required config paths in plugin.yml:
     * - database.mysql.host
     * - database.mysql.port
     * - database.mysql.database
     * - database.mysql.user
     * - database.mysql.password
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEnginePremiumMySQL(Plugin plugin) {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        String port = plugin.getConfig().getString("database.mysql.port", "3306");
        String database = plugin.getConfig().getString("database.mysql.database", "mcengine");
        String user = plugin.getConfig().getString("database.mysql.user", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        String ssl = plugin.getConfig().getString("database.mysql.ssl", "false");

        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl;
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL: " + e.getMessage());
        }
    }

    /**
     * Returns the current MySQL database connection.
     *
     * @return active MySQL connection
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
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
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
            "WHERE table_schema = DATABASE() AND table_name LIKE 'premium_rank\\_%' ESCAPE '\\'";
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
     *
     * @param rankType Type of rank (e.g., vip, vvip)
     */
    @Override
    public void createPremiumRank(String rankType) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(String.format("""
                CREATE TABLE IF NOT EXISTS premium_rank_%s (
                    uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                    rank INT NOT NULL
                );
            """, rankType.toLowerCase()));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the rank of a player for the specified rank type from the database.
     *
     * @param uuid     Player UUID
     * @param rankType Rank type (e.g., vip, vvip)
     * @return Integer rank value, or -1 if not found
     */
    @Override
    public int getPremiumRank(String uuid, String rankType) {
        String query = String.format("SELECT rank FROM premium_rank_%s WHERE uuid = ?", rankType.toLowerCase());
        try (var pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, uuid);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // not found
    }

    /**
     * Upgrades the rank of a user by 1. If user not exists, it inserts with rank = 1.
     *
     * @param uuid     Player UUID
     * @param rankType Rank type (e.g., vip, vvip)
     */
    @Override
    public void upgradePremiumRank(String uuid, String rankType) {
        String selectQuery = String.format("SELECT rank FROM premium_rank_%s WHERE uuid = ?", rankType.toLowerCase());
        String insertQuery = String.format("INSERT INTO premium_rank_%s (uuid, rank) VALUES (?, ?)", rankType.toLowerCase());
        String updateQuery = String.format("UPDATE premium_rank_%s SET rank = rank + 1 WHERE uuid = ?", rankType.toLowerCase());

        try (var selectStmt = connection.prepareStatement(selectQuery)) {
            selectStmt.setString(1, uuid);
            var rs = selectStmt.executeQuery();

            if (rs.next()) {
                // Update rank
                try (var updateStmt = connection.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, uuid);
                    updateStmt.executeUpdate();
                }
            } else {
                // Insert new
                try (var insertStmt = connection.prepareStatement(insertQuery)) {
                    insertStmt.setString(1, uuid);
                    insertStmt.setInt(2, 1);
                    insertStmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the MySQL database connection if open.
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
}
