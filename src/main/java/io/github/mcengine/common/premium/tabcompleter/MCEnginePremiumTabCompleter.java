package io.github.mcengine.common.premium.tabcompleter;

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
 * <p>Only suggests options the sender has permission to execute:</p>
 * <ul>
 *   <li>{@code mcengine.premium.rank.create} → {@code create}</li>
 *   <li>{@code mcengine.premium.rank.upgrade} → {@code upgrade}</li>
 *   <li>{@code mcengine.premium.rank.get} → {@code get}</li>
 *   <li>{@code mcengine.premium.rank.get.players} → player names for {@code get}</li>
 * </ul>
 */
public class MCEnginePremiumTabCompleter implements TabCompleter {

    private static final String PERM_CREATE = "mcengine.premium.rank.create";
    private static final String PERM_UPGRADE = "mcengine.premium.rank.upgrade";
    private static final String PERM_GET_SELF = "mcengine.premium.rank.get";
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

        // /premium get <...>
        if (args.length >= 2 && "get".equalsIgnoreCase(args[0])) {
            // If user can query others, suggest online player names as the 2nd argument.
            if (args.length == 2 && sender.hasPermission(PERM_GET_OTHERS)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    out.add(p.getName());
                }
                // If they only have self-get, and are a player, omit suggestions (rankType freeform).
                return filter(out, args[1]);
            }
            // For args >= 2 beyond player name / rank type, we don't suggest rank types to avoid leaking/guessing.
            return out; // empty list → no suggestions
        }

        // /premium create <rankType> or /premium upgrade <rankType>
        // We intentionally do not suggest rank types here; left freeform.
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
