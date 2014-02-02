package me.mytl.afkManager;

import java.util.logging.Level;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Core extends JavaPlugin {

    private PlayerListener pManager;

    /**
     * Required onEnable() method.
     */
    public void onEnable() {
        this.saveDefaultConfig();
        Configuration config = this.getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        pManager = new PlayerListener(config, this);
        this.getServer().getPluginManager().registerEvents(pManager, this);

        for (Player p : this.getServer().getOnlinePlayers()) {
            pManager.associatePlayerGroup(p);
            pManager.updatePlayer(p, true);
            getServer().getLogger().log(Level.INFO, p.getName()
                    + " was recognized by the afk manager");
        }
    }

    /**
     * Optional onDisable() method, to dispose
     * all fields.
     */
    public void onDisable() {
        pManager = null;
    }

}
