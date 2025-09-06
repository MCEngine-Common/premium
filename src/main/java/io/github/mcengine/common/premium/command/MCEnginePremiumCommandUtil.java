package io.github.mcengine.common.premium.command;

import io.github.mcengine.common.premium.MCEnginePremiumCommon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Utility methods for the {@code /premium} command.
 *
 * <p>Provides permission-gated handlers and common messaging helpers.</p>
 */
public final class MCEnginePremiumCommandUtil {

    /** Permission to create a rank table. */
    public static final String PERM_CREATE = "mcengine.premium.rank.create";
    /** Permission to upgrade own rank. */
    public static final String PERM_UPGRADE = "mcengine.premium.rank.upgrade";
    /** Permission to get own rank. */
    public static final String PERM_GET_SELF = "mcengine.premium.rank.get";
    /** Permission to get other players' ranks. */
    public static final String PERM_GET_OTHERS = "mcengine.premium.rank.get.players";

    private MCEnginePremiumCommandUtil() {}

    /**
     * Sends a concise usage block to the sender, only listing commands they can execute.
     *
     * @param sender the command sender
     * @param label  the command label used (e.g., "premium")
     */
    public static void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Premium commands:");
        if (sender.hasPermission(PERM_CREATE)) {
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " create <rankType>");
        }
        if (sender.hasPermission(PERM_UPGRADE)) {
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " upgrade <rankType>");
        }
        if (sender.hasPermission(PERM_GET_SELF)) {
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " get <rankType>");
        }
        if (sender.hasPermission(PERM_GET_OTHERS)) {
            sender.sendMessage(ChatColor.GRAY + "  /" + label + " get <playerOnline> <rankType>");
        }
    }

    /**
     * Informs the sender that the subcommand is unknown.
     *
     * @param sender sender
     * @param sub    subcommand attempted
     */
    public static void unknownSubcommand(CommandSender sender, String sub) {
        sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + sub);
    }

    /**
     * Handles {@code /premium create <rankType>}.
     *
     * @param sender command sender
     * @param args   arguments
     * @param label  base label
     */
    public static void handleCreate(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission(PERM_CREATE)) {
            noPerm(sender, PERM_CREATE);
            return;
        }
        if (args.length < 2) {
            usage(sender, "/" + label + " create <rankType>");
            return;
        }
        String rankType = args[1];
        MCEnginePremiumCommon.getApi().createPremiumRank(rankType);
        sender.sendMessage(ChatColor.GREEN + "Premium rank table ensured for type: " + ChatColor.AQUA + rankType);
    }

    /**
     * Handles {@code /premium upgrade <rankType>}.
     *
     * @param sender command sender
     * @param args   arguments
     * @param label  base label
     */
    public static void handleUpgrade(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission(PERM_UPGRADE)) {
            noPerm(sender, PERM_UPGRADE);
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run: /" + label + " upgrade <rankType>");
            return;
        }
        if (args.length < 2) {
            usage(sender, "/" + label + " upgrade <rankType>");
            return;
        }
        String rankType = args[1];
        UUID uuid = player.getUniqueId();
        MCEnginePremiumCommon.getApi().upgradePremiumRank(uuid.toString(), rankType);
        int newRank = MCEnginePremiumCommon.getApi().getPremiumRank(uuid.toString(), rankType);
        sender.sendMessage(ChatColor.GREEN + "Your " + ChatColor.AQUA + rankType + ChatColor.GREEN
                + " rank is now: " + ChatColor.GOLD + newRank);
    }

    /**
     * Handles:
     * <ul>
     *   <li>{@code /premium get <rankType>} (self)</li>
     *   <li>{@code /premium get <playerOnline> <rankType>} (others)</li>
     * </ul>
     *
     * @param sender command sender
     * @param args   arguments
     * @param label  base label
     */
    public static void handleGet(CommandSender sender, String[] args, String label) {
        // /premium get <rankType>
        if (args.length == 2) {
            if (!sender.hasPermission(PERM_GET_SELF)) {
                noPerm(sender, PERM_GET_SELF);
                return;
            }
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can run: /" + label + " get <rankType>");
                return;
            }
            String rankType = args[1];
            int rank = MCEnginePremiumCommon.getApi().getPremiumRank(player.getUniqueId().toString(), rankType);
            sender.sendMessage(ChatColor.GREEN + "Your " + ChatColor.AQUA + rankType + ChatColor.GREEN
                    + " rank: " + ChatColor.GOLD + rank);
            return;
        }

        // /premium get <playerOnline> <rankType>
        if (args.length == 3) {
            if (!sender.hasPermission(PERM_GET_OTHERS)) {
                noPerm(sender, PERM_GET_OTHERS);
                return;
            }
            String playerName = args[1];
            String rankType = args[2];

            Player target = Bukkit.getPlayerExact(playerName);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(ChatColor.RED + "Player not found or not online: " + playerName);
                return;
            }
            int rank = MCEnginePremiumCommon.getApi().getPremiumRank(target.getUniqueId().toString(), rankType);
            sender.sendMessage(ChatColor.GREEN + target.getName() + "'s " + ChatColor.AQUA + rankType + ChatColor.GREEN
                    + " rank: " + ChatColor.GOLD + rank);
            return;
        }

        // Wrong arity
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " get <rankType>");
        sender.sendMessage(ChatColor.YELLOW + "  /" + label + " get <playerOnline> <rankType>");
    }

    /* ----------------------------- helpers ----------------------------- */

    private static void usage(CommandSender sender, String usage) {
        sender.sendMessage(ChatColor.YELLOW + "Usage: " + usage);
    }

    private static void noPerm(CommandSender sender, String perm) {
        sender.sendMessage(ChatColor.RED + "You don't have permission: " + perm);
    }
}
