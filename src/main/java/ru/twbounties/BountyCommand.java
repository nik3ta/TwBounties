package ru.twbounties;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BountyCommand implements CommandExecutor {

    private final TwBounties plugin;

    public BountyCommand(TwBounties plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команда только для игроков.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            plugin.getBountyManager().openGui(player);
            return true;
        }

        if (args.length == 2) {
            String targetName = args[0];
            String amountStr = args[1];

            if (targetName.equalsIgnoreCase(player.getName())) {
                player.sendMessage(ChatColor.RED + "Нельзя заказать самого себя!");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                player.sendMessage(ChatColor.RED + "Игрок " + targetName + " никогда не играл на сервере.");
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Неверный формат суммы. Используйте числа.");
                return true;
            }

            if (amount < 100000) {
                player.sendMessage(ChatColor.RED + "Минимальная сумма заказа: 100.000€.");
                return true;
            }

            if (plugin.getBountyManager().hasBounty(target.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "На этого игрока уже назначен заказ!");
                return true;
            }

            boolean success = plugin.getBountyManager().addBounty(player, target, amount);

            if (success) {
                player.sendMessage(ChatColor.GREEN + "Заказ успешно создан! Сумма списана.");
            }

            return true;
        }

        player.sendMessage(ChatColor.RED + "Использование: /bounty <ник> <сумма> или /bounty gui");
        return true;
    }
}