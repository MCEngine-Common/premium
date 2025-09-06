package io.github.mcengine.common.premium.tabcompleter;

import io.github.mcengine.common.premium.MCEnginePremiumCommon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab completion for {@code /premium}.
 *
 * <p>Only suggests options the sender has permission to execute. Also lists available
 * rank types discovered from existing {@code premium_rank_*} tables via
 * {@link MCEnginePremiumCommon#listAvailableRankTypes()}.</p>
 */
public class MCEnginePremiumTabCompleter implements TabCompleter {

    /** Permission node: allows creating premium rank tables (suggests {@code create}). */
    private static final String PERM_CREATE = "mcengine.premium.rank.create";
    /** Permission node: allows upgrading own premium rank (suggests {@code upgrade}). */
    private static final String PERM_UPGRADE = "mcengine.premium.rank.upgrade";
    /** Permission node: allows checking own premium rank (suggests {@code get}). */
    private static final String PERM_GET_SELF = "mcengine.premium.rank.get";
    /** Permission node: allows checking other players' ranks (suggests player names for {@code get}). */
    private static final String PERM_GET_OTHERS = "mcengine.premium.rank.get.players";

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        // Subcommand suggestions
        if (args.length == 1) {
            if (sender.hasPermission(PERM_CREATE)) out.add("create");
            if (sender.hasPermission(PERM_UPGRADE)) out.add("upgrade");
            if (sender.hasPermission(PERM_GET_SELF) || sender.hasPermission(PERM_GET_OTHERS)) out.add("get");
            return filter(out, args[0]);
        }

        // /premium upgrade <rankType>
        if (args.length == 2 && "upgrade".equalsIgnoreCase(args[0])) {
            if (sender.hasPermission(PERM_UPGRADE)) {
                out.addAll(MCEnginePremiumCommon.getApi().listAvailableRankTypes());
            }
            return filter(out, args[1]);
        }

        // /premium get ...
        if ("get".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                // Suggest available rank types for self-get
                if (sender.hasPermission(PERM_GET_SELF)) {
                    out.addAll(MCEnginePremiumCommon.getApi().listAvailableRankTypes());
                }
                // Also suggest online player names if they can query others
                if (sender.hasPermission(PERM_GET_OTHERS)) {
                    for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
                }
                return filter(out, args[1]);
            }
            if (args.length == 3 && sender.hasPermission(PERM_GET_OTHERS)) {
                // When querying others, suggest rank types as the 3rd arg
                out.addAll(MCEnginePremiumCommon.getApi().listAvailableRankTypes());
                return filter(out, args[2]);
            }
            return out;
        }

        // /premium create <rankType> – keep freeform (do not suggest)
        return out;
    }

    private List<String> filter(List<String> base, String token) {
        if (token == null || token.isEmpty()) return base;
        String lower = token.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String s : base) {
            if (s.toLowerCase().startsWith(lower)) filtered.add(s);
        }
        return filtered;
    }
}
