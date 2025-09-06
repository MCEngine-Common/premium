package io.github.mcengine.common.premium.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Handles the {@code /premium} command and delegates logic to {@link MCEnginePremiumCommandUtil}.
 *
 * <p>Subcommands:
 * <ul>
 *   <li>{@code /premium create &lt;rankType&gt;} — requires {@code mcengine.premium.rank.create}</li>
 *   <li>{@code /premium upgrade &lt;rankType&gt;} — requires {@code mcengine.premium.rank.upgrade}</li>
 *   <li>{@code /premium get &lt;rankType&gt;} — requires {@code mcengine.premium.rank.get}</li>
 *   <li>{@code /premium get &lt;playerOnline&gt; &lt;rankType&gt;} — requires {@code mcengine.premium.rank.get.players}</li>
 * </ul>
 * </p>
 */
public class MCEnginePremiumCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MCEnginePremiumCommandUtil.sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> MCEnginePremiumCommandUtil.handleCreate(sender, args, label);
            case "upgrade" -> MCEnginePremiumCommandUtil.handleUpgrade(sender, args, label);
            case "get" -> MCEnginePremiumCommandUtil.handleGet(sender, args, label);
            default -> {
                MCEnginePremiumCommandUtil.unknownSubcommand(sender, sub);
                MCEnginePremiumCommandUtil.sendUsage(sender, label);
            }
        }
        return true;
    }
}
