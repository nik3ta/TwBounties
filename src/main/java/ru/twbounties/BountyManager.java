package ru.twbounties;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class BountyManager {

    private final TwBounties plugin;
    private final Map<UUID, BountyData> bounties = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final String GUI_TITLE = ChatColor.DARK_RED + "Список жертв";

    public BountyManager(TwBounties plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    public static class BountyData {
        String creatorName;
        double amount;

        public BountyData(String creatorName, double amount) {
            this.creatorName = creatorName;
            this.amount = amount;
        }
    }

    // Метод для красивых денег (100.000€)
    private String formatMoney(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,###", symbols);
        return formatter.format(amount);
    }

    // НОВЫЙ МЕТОД: Принудительная рассылка всем игрокам
    private void sendGlobalMessage(String message) {
        // Отправляем каждому игроку лично (обходит фильтры чата)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(message);
        }
        // И дублируем в консоль, чтобы ты видел логи
        Bukkit.getConsoleSender().sendMessage(message);
    }

    public boolean addBounty(Player creator, OfflinePlayer target, double amount) {
        Economy economy = TwBounties.getEconomy();

        if (!economy.has(creator, amount)) {
            creator.sendMessage(ChatColor.RED + "У вас недостаточно средств! Требуется: " + formatMoney(amount) + "€");
            return false;
        }

        economy.withdrawPlayer(creator, amount);

        bounties.put(target.getUniqueId(), new BountyData(creator.getName(), amount));
        saveData();

        String msg = ChatColor.translateAlternateColorCodes('&',
                "&c&l☠ &4&lИгрок &6" + creator.getName() + " &4&lназначил награду за голову &c" + target.getName() +
                        "&4&l. Кто его убьет получит &6" + formatMoney(amount) + "€");

        // Используем наш новый метод вместо broadcastMessage
        sendGlobalMessage(msg);

        return true;
    }

    public void completeBounty(Player killer, Player victim) {
        BountyData data = bounties.remove(victim.getUniqueId());
        if (data != null) {
            Economy economy = TwBounties.getEconomy();
            economy.depositPlayer(killer, data.amount);

            String msg = ChatColor.translateAlternateColorCodes('&',
                    "&2&l✔ &aИгрок &6" + killer.getName() + " &aубил игрока &c" + victim.getName() +
                            " &aи получил &6" + formatMoney(data.amount) + "€");

            // Используем наш новый метод вместо broadcastMessage
            sendGlobalMessage(msg);

            saveData();
        }
    }

    public boolean hasBounty(UUID targetUUID) {
        return bounties.containsKey(targetUUID);
    }

    public void openGui(Player player) {
        int size = 54;
        if (bounties.size() <= 9) size = 9;
        else if (bounties.size() <= 18) size = 18;
        else if (bounties.size() <= 27) size = 27;
        else if (bounties.size() <= 36) size = 36;
        else if (bounties.size() <= 45) size = 45;

        Inventory gui = Bukkit.createInventory(null, size, GUI_TITLE);

        for (Map.Entry<UUID, BountyData> entry : bounties.entrySet()) {
            if (gui.firstEmpty() == -1) break;

            UUID targetUUID = entry.getKey();
            BountyData data = entry.getValue();
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.setDisplayName(ChatColor.DARK_RED + (target.getName() != null ? target.getName() : "Unknown"));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Заказал: " + ChatColor.GOLD + data.creatorName);
                lore.add(ChatColor.GRAY + "Награда: " + ChatColor.GREEN + formatMoney(data.amount) + "€");
                meta.setLore(lore);

                head.setItemMeta(meta);
            }
            gui.addItem(head);
        }

        player.openInventory(gui);
    }

    public String getGuiTitle() {
        return GUI_TITLE;
    }

    public void saveData() {
        if (dataConfig == null) dataConfig = new YamlConfiguration();

        for (String key : dataConfig.getKeys(false)) {
            dataConfig.set(key, null);
        }

        for (Map.Entry<UUID, BountyData> entry : bounties.entrySet()) {
            String path = entry.getKey().toString();
            dataConfig.set(path + ".creator", entry.getValue().creatorName);
            dataConfig.set(path + ".amount", entry.getValue().amount);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить data.yml!");
            e.printStackTrace();
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Не удалось создать data.yml!");
                e.printStackTrace();
                return;
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        bounties.clear();

        for (String key : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String creator = dataConfig.getString(key + ".creator");
                double amount = dataConfig.getDouble(key + ".amount");
                bounties.put(uuid, new BountyData(creator, amount));
            } catch (Exception e) {
                plugin.getLogger().warning("Ошибка загрузки заказа для UUID: " + key);
            }
        }
    }
}