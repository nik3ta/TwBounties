package ru.twbounties;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public class BountyListener implements Listener {

    private final TwBounties plugin;

    public BountyListener(TwBounties plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            if (plugin.getBountyManager().hasBounty(victim.getUniqueId())) {
                plugin.getBountyManager().completeBounty(killer, victim);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(plugin.getBountyManager().getGuiTitle())) {
            event.setCancelled(true);
        }
    }
}