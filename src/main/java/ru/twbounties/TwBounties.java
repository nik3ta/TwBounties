package ru.twbounties;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TwBounties extends JavaPlugin {

    private static Economy econ = null;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] - Vault не найден или нет плагина экономики (например, CMI)!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.bountyManager = new BountyManager(this);
        getCommand("bounty").setExecutor(new BountyCommand(this));
        getServer().getPluginManager().registerEvents(new BountyListener(this), this);

        getLogger().info("TwBounties успешно запущен с поддержкой CMI/Vault!");
    }

    @Override
    public void onDisable() {
        if (bountyManager != null) {
            bountyManager.saveData();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public BountyManager getBountyManager() {
        return bountyManager;
    }
}