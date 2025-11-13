package ru.balance.GenAI.command.subcommands;

import org.bukkit.command.CommandSender;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;
import ru.balance.GenAI.service.TokenService;

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
        return "Manage authentication token";
    }

    @Override
    public String getUsage() {
        return "/genai token <set|clear|info> [token]";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getLocaleManager().getMessage("commands.token.usage"));
            return;
        }

        String token = null;
        if (args.length >= 2) {
            StringBuilder tokenBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) tokenBuilder.append(" ");
                tokenBuilder.append(args[i]);
            }
            token = tokenBuilder.toString();
        }

        switch (args[0].toLowerCase()) {
            case "set":
                if (token == null) {
                    sender.sendMessage(plugin.getLocaleManager().getMessage("commands.token.usage"));
                    return;
                }

                TokenService tokenService = plugin.getTokenService();
                tokenService.setToken(token);

                sender.sendMessage("§aТокен успешно установлен!");
                sender.sendMessage("§7Проверка токена...");

                tokenService.validateToken().thenAccept(isValid -> {
                    if (isValid) {
                        sender.sendMessage("§a✓ Токен действителен и активен!");
                        sender.sendMessage("§eСтатус премиум: " + (tokenService.isPremiumActive() ? "Активен" : "Неактивен"));
                        sender.sendMessage("§eОсталось дней: " + tokenService.getRemainingDays());
                        sender.sendMessage("§eПоследний IP: " + tokenService.getLastIp());
                    } else {
                        sender.sendMessage("§c✗ Токен недействителен или истек!");
                    }
                });
                break;

            case "clear":
                plugin.getTokenService().clearToken();
                sender.sendMessage("§aТокен успешно очищен!");
                break;

            case "info":
                TokenService infoService = plugin.getTokenService();
                if (!infoService.hasToken()) {
                    sender.sendMessage(plugin.getLocaleManager().getMessage("commands.token.invalid-token"));
                    sender.sendMessage("§7Используйте §e/genai token set <токен> §7для установки токена.");
                    return;
                }

                sender.sendMessage("§6=== Информация о токене ===");
                sender.sendMessage("§eТокен: §7" + maskToken(infoService.getToken()));
                sender.sendMessage("§eСтатус премиум: " + (infoService.isPremiumActive() ? "§aАктивен" : "§cНеактивен"));
                sender.sendMessage("§eОсталось дней: §7" + infoService.getRemainingDays());
                sender.sendMessage("§eПоследний IP: §7" + infoService.getLastIp());
                sender.sendMessage("§eМожно перепривязать IP: " + (infoService.canRebindIp() ? "§aДа" : "§cНет"));
                break;

            default:
                sender.sendMessage(plugin.getLocaleManager().getMessage("commands.token.usage"));
                break;
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "****";
        }
        return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
    }
}