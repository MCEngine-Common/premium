package io.github.mcengine.common.premium.database.sqlite;

import io.github.mcengine.common.premium.database.IMCEnginePremiumDB;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite implementation of the Premium Common database using plugin configuration.
 */
public class MCEnginePremiumSQLite implements IMCEnginePremiumDB {

    /**
     * Active SQL database connection instance.
     */
    private Connection connection;

    /**
     * Initializes SQLite connection using plugin configuration.
     *
     * Config path:
     * - database.sqlite.path (default: "premium.db")
     *
     * @param plugin Bukkit plugin instance
     */
    public MCEnginePremiumSQLite(Plugin plugin) {
        String dbPath = plugin.getConfig().getString("database.sqlite.path", "premium.db");
        File dbFile = new File(plugin.getDataFolder(), dbPath);

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite: " + e.getMessage());
        }
    }

    /**
     * Returns the current SQLite database connection.
     *
     * @return active SQLite connection
     */
    @Override
    public Connection getConnection() {
        return connection;
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
                    uuid TEXT NOT NULL PRIMARY KEY,
                    rank INTEGER NOT NULL
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
     * Closes the SQLite database connection if open.
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
