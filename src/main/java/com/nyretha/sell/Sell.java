package com.nyretha.sell;

import com.nyretha.sell.manager.LevelManager;
import com.nyretha.sell.manager.PlayerDataManager;
import com.nyretha.sell.manager.PriceManager;
import com.nyretha.sell.utils.CurrencyFormatter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Compatibility host for the Sell module's existing managers.
 * The unified Core is the declared Bukkit entry point.
 */
public final class Sell extends JavaPlugin {
    private CurrencyFormatter currencyFormatter;
    private PriceManager priceManager;
    private PlayerDataManager playerDataManager;
    private LevelManager levelManager;
    private Economy economy;

    @Override public void onEnable() {
        currencyFormatter = new CurrencyFormatter(getConfig());
        priceManager = new PriceManager();
        playerDataManager = new PlayerDataManager(this);
        levelManager = new LevelManager(this, currencyFormatter, priceManager);
    }
    public CurrencyFormatter getCurrencyFormatter() {
        if (currencyFormatter == null) currencyFormatter = new CurrencyFormatter(getConfig());
        return currencyFormatter;
    }
    public PriceManager getPriceManager() {
        if (priceManager == null) priceManager = new PriceManager();
        return priceManager;
    }
    public PlayerDataManager getPlayerDataManager() {
        if (playerDataManager == null) playerDataManager = new PlayerDataManager(this);
        return playerDataManager;
    }
    public LevelManager getLevelManager() {
        if (levelManager == null) levelManager = new LevelManager(this, getCurrencyFormatter(), getPriceManager());
        return levelManager;
    }
    public Economy getEconomy() { return economy; }
    public void setEconomy(Economy economy) { this.economy = economy; }
    public boolean isSellMultiEnabled() { return getConfig().getBoolean("sell-multiplier.enabled", false); }
    public boolean isShulkerSupportEnabled() { return getConfig().getBoolean("shulker-support", true); }
    public boolean isPluginEnabled(String name) { return Bukkit.getPluginManager().isPluginEnabled(name); }
    public void runAsync(Runnable task) { Bukkit.getScheduler().runTaskAsynchronously(this, task); }
    public void runTask(Runnable task) { Bukkit.getScheduler().runTask(this, task); }
    public void runTaskLater(Runnable task, long delay) { Bukkit.getScheduler().runTaskLater(this, task, delay); }
    public void runTaskTimer(Runnable task, long delay, long period) { Bukkit.getScheduler().runTaskTimer(this, task, delay, period); }
}
