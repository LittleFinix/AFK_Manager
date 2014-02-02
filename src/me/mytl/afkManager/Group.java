package me.mytl.afkManager;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class Group {

    private String permission, groupName,
    dMessage, aMessage, rMessage,
    dPublicMessage, aPublicMessage, rPublicMessage,
    prefix;

    private boolean dPublic, aPublic, rPublic;

    private long dTime, aTime;

    private Plugin p;

    /**
     * @return the permission associated with
     * this group.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * @return the group name.
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * @param name the group name.
     * @param config the configuration that contains this group.
     * @param plugin the plugin associated with this group.
     * @throws Exception if the configuration does not contain this group.
     */
    public Group(String name, Configuration config, Plugin plugin)
            throws Exception {

        groupName = name;
        this.p = plugin;

        if (!config.contains("AFK_Manager.Groups." + groupName)) {
            throw new Exception("Group " + groupName + " is not available!");
        }

        ConfigurationSection gRoot =
            config.getConfigurationSection("AFK_Manager.Groups." + groupName);

        permission = gRoot.getString("Permission");

        aTime = gRoot.getLong("AFK.Time") * 20;
        aMessage = gRoot.getString("AFK.Message");
        aPublic = gRoot.getBoolean("AFK.Public");
        aPublicMessage = gRoot.getString("AFK.Public_Message");
        prefix = gRoot.getString("AFK.Prefix");

        dTime = gRoot.getLong("Disconnect.Time") * 20;
        dMessage = gRoot.getString("Disconnect.Message");
        dPublic = gRoot.getBoolean("Disconnect.Public");
        dPublicMessage = gRoot.getString("Disconnect.Public_Message");

        rMessage = gRoot.getString("Resume.Message");
        rPublic = gRoot.getBoolean("Resume.Public");
        rPublicMessage = gRoot.getString("Resume.Public_Message");
    }

    /**
     * Creates a task that will mark a player afk.
     * @param scheduler the scheduler that will be used to create the task.
     * @param run the Runnable (Task).
     * @param player the player who will be set afk.
     * @return the process ID.
     */
    public Integer createAFKTask(BukkitScheduler scheduler,
            Runnable run, Player player) {

        if (aTime / 20 == -1) {
            return null;
        }

        return scheduler.scheduleSyncRepeatingTask(p, run, aTime, aTime);
    }

    /**
     * Creates a task that will disconnect a player.
     * @param scheduler the scheduler that will be used to create the task.
     * @param run the Runnable (Task).
     * @param player the player who will be disconnected.
     * @return the process ID.
     */
    public Integer createDisconnectTask(BukkitScheduler scheduler,
            Runnable run, Player player) {

        player.setDisplayName(prefix + player.getDisplayName());

        if (dTime / 20 == -1) {
            return null;
        }

        if (aPublic) {
            p.getServer().broadcastMessage(aPublicMessage
                    .replaceAll("--p", player.getDisplayName()));
        } else {
            player.sendMessage(aMessage);
        }

        return scheduler.scheduleSyncRepeatingTask(p, run, dTime, dTime);
    }

    /**
     * Resume a player.
     * @param player the player who will loose the afk mark.
     */
    public void resumePlayer(Player player) {

        player.setDisplayName(player.getDisplayName().replaceFirst(prefix, ""));

        if (rPublic) {
            player.getServer().broadcastMessage(
                    rPublicMessage.replaceAll("--p", player.getDisplayName()));
        } else {
            player.sendMessage(
                    rMessage.replaceAll("--p", player.getDisplayName()));
        }
    }

    /**
     * Disconnect a player.
     * @param player the player who will be disconnected.
     */
    public void disconnectPlayer(Player player) {

        if (dPublic) {
            p.getServer().broadcastMessage(
                    dPublicMessage.replaceAll("--p", player.getDisplayName()));
        }

        player.kickPlayer(dMessage);
    }
}
