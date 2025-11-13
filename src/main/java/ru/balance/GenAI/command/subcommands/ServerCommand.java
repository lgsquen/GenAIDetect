package ru.balance.GenAI.command.subcommands;

import org.bukkit.command.CommandSender;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ServerCommand extends SubCommand {

    private final GenAI plugin;

    public ServerCommand(GenAI plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "server";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.server.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.server.help-usage");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.server.usage"));
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.reloadPluginConfig();
                sender.sendMessage(plugin.getLocaleManager().getMessage("commands.server.reloaded"));
                break;

            case "stats":
                sender.sendMessage(plugin.getLocaleManager().getMessage("commands.server.stats-generated"));
                break;

            default:
                sender.sendMessage(plugin.getLocaleManager().getMessage("commands.server.invalid-action"));
                break;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList("reload", "stats");
            List<String> result = new ArrayList<>();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(completion);
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}

