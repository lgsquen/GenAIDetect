package ru.balance.GenAI.command.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;

public class AlertsCommand extends SubCommand {

    public AlertsCommand(GenAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "alerts";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.alerts.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.alerts.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("player-only-command"));
            return;
        }
        Player player = (Player) sender;
        boolean newState = plugin.toggleAlerts(player.getUniqueId());
        if (newState) {
            player.sendMessage(plugin.getLocaleManager().getMessage("alerts.toggled-on"));
        } else {
            player.sendMessage(plugin.getLocaleManager().getMessage("alerts.toggled-off"));
        }
    }
}

