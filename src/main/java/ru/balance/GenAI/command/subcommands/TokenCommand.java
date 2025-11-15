package ru.balance.GenAI.command.subcommands;

import org.bukkit.command.CommandSender;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;
import ru.balance.GenAI.service.TokenService;

import java.util.Map;

public class TokenCommand extends SubCommand {

    public TokenCommand(GenAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "token";
    }

    @Override
    public String getDescription() {
        return plugin.getLocaleManager().getMessage("commands.token.help-description");
    }

    @Override
    public String getUsage() {
        return plugin.getLocaleManager().getMessage("commands.token.help-usage");
    }

    private String msg(String key) {
        return plugin.getLocaleManager().getMessage(key);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(msg("commands.token.usage"));
            return;
        }

        if (args.length == 1) {
            String first = args[0].toLowerCase();
            if (!"clear".equals(first) && !"info".equals(first) && !"set".equals(first)) {
                handleSetToken(sender, args[0]);
                return;
            }
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "set": {
                if (args.length < 2) {
                    sender.sendMessage(msg("commands.token.usage"));
                    return;
                }
                StringBuilder tokenBuilder = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) {
                        tokenBuilder.append(" ");
                    }
                    tokenBuilder.append(args[i]);
                }
                handleSetToken(sender, tokenBuilder.toString());
                break;
            }

            case "clear":
                plugin.getTokenService().clearToken();
                sender.sendMessage(msg("commands.token.cleared"));
                break;

            case "info":
                handleInfo(sender);
                break;

            default:
                sender.sendMessage(msg("commands.token.usage"));
                break;
        }
    }

    private void handleSetToken(CommandSender sender, String token) {
        TokenService tokenService = plugin.getTokenService();
        tokenService.setToken(token);
        sender.sendMessage(msg("commands.token.saved"));

        tokenService.getSubscriptionInfoAsync().thenAccept(subInfo -> {
            if (subInfo != null && !subInfo.isEmpty()) {
                boolean premiumActive = false;
                int remainingDays = 0;
                String lastIp = "Not bound";

                Object remainingDaysValue = subInfo.get("remaining_days");
                if (remainingDaysValue instanceof Number) {
                    remainingDays = ((Number) remainingDaysValue).intValue();
                    premiumActive = remainingDays > 0 || remainingDays == -1;
                }
                Object lastIpValue = subInfo.get("last_ip");
                if (lastIpValue != null) {
                    lastIp = lastIpValue.toString();
                }

                String yes = msg("commands.token.yes");
                String no = msg("commands.token.no");

                sender.sendMessage(msg("commands.token.valid"));
                sender.sendMessage(msg("commands.token.info-premium")
                        .replace("%value%", premiumActive ? yes : no));
                sender.sendMessage(msg("commands.token.info-days")
                        .replace("%days%", String.valueOf(remainingDays)));
                sender.sendMessage(msg("commands.token.info-ip")
                        .replace("%ip%", lastIp));
            } else {
                sender.sendMessage(msg("commands.token.invalid"));
            }
        });
    }

    private void handleInfo(CommandSender sender) {
        TokenService infoService = plugin.getTokenService();
        if (!infoService.hasToken()) {
            sender.sendMessage(msg("commands.token.invalid-token"));
            sender.sendMessage(msg("commands.token.set-hint"));
            return;
        }

        String maskedToken = maskToken(infoService.getToken());
        sender.sendMessage(msg("commands.token.info-header"));
        sender.sendMessage(msg("commands.token.info-token").replace("%token%", maskedToken));

        infoService.getSubscriptionInfoAsync().thenAccept(subInfo -> {
            boolean premiumActive = false;
            int remainingDays = 0;
            String lastIp = "Not bound";
            boolean canRebind = false;

            if (subInfo != null && !subInfo.isEmpty()) {
                Object remainingDaysValue = subInfo.get("remaining_days");
                if (remainingDaysValue instanceof Number) {
                    remainingDays = ((Number) remainingDaysValue).intValue();
                    premiumActive = remainingDays > 0 || remainingDays == -1;
                }
                Object lastIpValue = subInfo.get("last_ip");
                if (lastIpValue != null) {
                    lastIp = lastIpValue.toString();
                }
                Object canRebindValue = subInfo.get("can_rebind");
                if (canRebindValue instanceof Boolean) {
                    canRebind = (Boolean) canRebindValue;
                }
            }

            String yes = msg("commands.token.yes");
            String no = msg("commands.token.no");

            sender.sendMessage(msg("commands.token.info-premium")
                    .replace("%value%", premiumActive ? yes : no));
            sender.sendMessage(msg("commands.token.info-days")
                    .replace("%days%", String.valueOf(remainingDays)));
            sender.sendMessage(msg("commands.token.info-ip")
                    .replace("%ip%", lastIp));
            sender.sendMessage(msg("commands.token.info-can-rebind")
                    .replace("%value%", canRebind ? yes : no));
        });
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
}
