package io.github.mcengine.common.premium;

import io.github.mcengine.common.premium.database.IMCEnginePremiumDB;
import io.github.mcengine.common.premium.database.mysql.MCEnginePremiumMySQL;
import io.github.mcengine.common.premium.database.postgresql.MCEnginePremiumPostgreSQL;
import io.github.mcengine.common.premium.database.sqlite.MCEnginePremiumSQLite;
import org.bukkit.plugin.Plugin;

/**
 * Central entrypoint for the Premium module's common API.
 * <p>
 * This class selects and initializes the database backend (SQLite/MySQL/PostgreSQL)
 * based on {@code database.type} in the plugin configuration and exposes
 * the active {@link IMCEnginePremiumDB} implementation to the rest of the plugin.
 */
public class MCEnginePremiumCommon {

    /** Singleton instance of the Premium common API. */
    private static MCEnginePremiumCommon instance;

    /** The Bukkit plugin instance that owns this API. */
    private final Plugin plugin;

    /** Database interface used by the Premium module. */
    private final IMCEnginePremiumDB db;

    /**
     * Constructs the Premium common API and wires the configured database backend.
     *
     * <p>Supported values for {@code database.type}: {@code sqlite}, {@code mysql}, {@code postgresql}.</p>
     *
     * @param plugin the owning Bukkit {@link Plugin} instance
     * @throws IllegalArgumentException if {@code database.type} is unsupported
     */
    public MCEnginePremiumCommon(Plugin plugin) {
        instance = this;
        this.plugin = plugin;

        String dbType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();
        switch (dbType) {
            case "sqlite" -> this.db = new MCEnginePremiumSQLite(plugin);
            case "mysql" -> this.db = new MCEnginePremiumMySQL(plugin);
            case "postgresql" -> this.db = new MCEnginePremiumPostgreSQL(plugin);
            default -> throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
    }

    /** Returns the global API singleton instance. */
    public static MCEnginePremiumCommon getApi() { return instance; }

    /** Returns the Bukkit plugin instance. */
    public Plugin getPlugin() { return plugin; }

    /** Returns the database interface used by this module. */
    public IMCEnginePremiumDB getDB() { return db; }

    /**
     * Checks whether a premium rank table exists for the given rank type.
     *
     * @param rankType the rank type (e.g., "vip", "vvip")
     * @return {@code true} if the table exists, {@code false} otherwise
     */
    public boolean rankTableExists(String rankType) {
        return db.rankTableExists(rankType);
    }

    /**
     * Lists available premium rank types by inspecting existing {@code premium_rank_*} tables.
     *
     * @return list of rank type suffixes (e.g., ["vip","vvip"]), never null
     */
    public java.util.List<String> listAvailableRankTypes() {
        return db.listAvailableRankTypes();
    }

    /**
     * Creates the necessary tables or schema for storing premium rank data.
     *
     * @param rankType type of rank (e.g., vip, vvip)
     */
    public void createPremiumRank(String rankType) {
        db.createPremiumRank(rankType);
    }

    /**
     * Retrieves premium rank data from the database.
     *
     * @param uuid     Player UUID
     * @param rankType Rank type (e.g., vip, vvip)
     * @return the rank value, or -1 if not found
     */
    public int getPremiumRank(String uuid, String rankType) {
        return db.getPremiumRank(uuid, rankType);
    }

    /**
     * Upgrades the rank of a user by 1. If the user does not exist, it inserts with rank = 1.
     *
     * @param uuid     Player UUID
     * @param rankType Rank type (e.g., vip, vvip)
     */
    public void upgradePremiumRank(String uuid, String rankType) {
        db.upgradePremiumRank(uuid, rankType);
    }
}
