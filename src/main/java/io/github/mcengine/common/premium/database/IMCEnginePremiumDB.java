package io.github.mcengine.common.premium.database;

import java.sql.Connection;

/**
 * Interface for managing database operations related to MCEngine Premium system.
 */
public interface IMCEnginePremiumDB {

    /**
     * Gets the current database connection.
     *
     * @return a {@link Connection} object to the database.
     */
    Connection getConnection();

    /**
     * Creates the necessary tables or schema for storing premium rank data.
     */
    void createPremiumRank(String rankType);

    /**
     * Retrieves premium rank data from the database.
     */
    int getPremiumRank(String uuid, String rankType);

    /**
     * Upgrades the rank of a user by 1. If user not exists, it inserts with rank = 1.
     *
     * @param uuid     Player UUID
     * @param rankType Rank type (e.g., vip, vvip)
     */
    void upgradePremiumRank(String uuid, String rankType);

    /**
     * Checks whether a rank table exists for the provided {@code rankType}.
     *
     * @param rankType the rank type (e.g., "vip")
     * @return {@code true} if table exists; {@code false} otherwise
     */
    boolean rankTableExists(String rankType);

    /**
     * Closes the current database connection.
     */
    void disConnection();
}
