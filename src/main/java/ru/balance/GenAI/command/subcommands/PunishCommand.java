package ru.balance.GenAI.command.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import ru.balance.GenAI.GenAI;
import ru.balance.GenAI.command.SubCommand;
import ru.balance.GenAI.service.PunishmentService;
import java.util.List;
import java.util.stream.Collectors;

public class PunishCommand extends SubCommand {

    public PunishCommand(GenAI plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "punish";
    }

    @Override
    public String getDescription() {
        return "Запускает анимацию наказания для игрока";
    }

    @Override
    public String getUsage() {
        return "/genai punish <игрок>";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            return;
        }

        PunishmentService.playAnimationAndPunish(target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> StringUtil.startsWithIgnoreCase(name, args[0]))
                    .collect(Collectors.toList());
        }
        return super.onTabComplete(sender, args);
    }
}
