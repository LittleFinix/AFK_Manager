package me.mytl.afkManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * The Listener used for afk checking.
 * @author Littlefinix
 *
 */
public class PlayerListener implements Listener {

    /**
     * Stores player process IDs.
     */
    private HashMap<Player, Integer> playerPID;

    /**
     * Stores whether the player is afk or not.
     */
    private HashMap<Player, Boolean> playerAFK;

    /**
     * Stores association groups with players.
     */
    private HashMap<Player, Group> playerGroups;

    /**
     * Stores available groups.
     */
    private List<Group> groups;

    /**
     * Stores configuration.
     */
    private Configuration config;

    /**
     * Stores the plugin.
     */
    private Plugin p;

    /**
     * Initialize the PlayerListener.
     *
     * @param configuration the configuration to load the data from.
     * @param plugin the plugin that initializes the PlayerListener.
     */
    public PlayerListener(final Configuration configuration,
            final Plugin plugin) {
        playerPID = new HashMap<Player, Integer>();
        playerAFK = new HashMap<Player, Boolean>();
        playerGroups = new HashMap<Player, Group>();
        groups = new ArrayList<Group>();
        this.config = configuration;
        this.p = plugin;

        for (String group
                : configuration.getStringList("AFK_Manager.EnabledGroups")) {
            try {
                groups.add(new Group(group, configuration, plugin));
                plugin.getLogger().log(Level.INFO, "Added group " + group);
            } catch (Exception e) { continue; }
        }
    }

    /**
     * Associates a player with a group if available and
     * calls updatePlayer for the first time.
     * @param e PlayerJoinEvent
     */
    @EventHandler
    public final void onPlayerJoin(final PlayerJoinEvent e) {

        associatePlayerGroup(e.getPlayer());

        updatePlayer(e.getPlayer(), true);
        e.getPlayer().getServer().getLogger()
        .log(Level.INFO, e.getPlayer().getName()
                + " was recognized by the AFK Manager");
    }

    /**
     * Associates a player with a group if available.
     * @param player the player to be associated.
     */
    public void associatePlayerGroup(Player player) {
        Group playerGroup = null;

        for (Group g : groups) {
            if (player.hasPermission(g.getPermission())
                    && !player.isOp()) {
                playerGroup = g;
            }
        }

        if (playerGroup != null) {
            player.getServer().getLogger().log(Level.INFO,
                    player.getName()
                    + " was assigned to AFK Manager Group "
                    + playerGroup.getGroupName());

            playerGroups.put(player, playerGroup);
        }
    }

    /**
     * Checks if the player turned and
     * calls updatePlayer if needed.
     * @param e PlayerMoveEvent
     */
    @EventHandler
    public final void onPlayerTurn(final PlayerMoveEvent e) {

        if (e.getPlayer().getLocation().getDirection()
                .equals(e.getTo().getDirection())) {
            return;
        }

        Group g = playerGroups.get(e.getPlayer());

        if (playerAFK.get(e.getPlayer())) {
            if (g == null) {

                e.getPlayer().setDisplayName(e.getPlayer().getDisplayName()
                        .replaceFirst(
                               config.getString("AFK_Manager.AFK.Prefix"), ""));

                if (config.getBoolean("AFK_Manager.Resume.Public")) {
                    e.getPlayer().getServer().broadcastMessage(
                        config.getString("AFK_Manager.Resume.Public_Message")
                            .replaceAll("--p", e.getPlayer().getDisplayName()));
                } else {
                    e.getPlayer().sendMessage(
                            config.getString("AFK_Manager.Resume.Message")
                            .replaceAll("--p", e.getPlayer().getDisplayName()));
                }
            } else {
                g.resumePlayer(e.getPlayer());
            }
        }

        updatePlayer(e.getPlayer());
    }

    /**
     * Removes all instances of the player from the storage.
     * @param e PlayerQuitEvent
     */
    @EventHandler
    public final void onPlayerLeave(final PlayerQuitEvent e) {

        if (playerPID.get(e.getPlayer()) != null) {
            e.getPlayer().getServer().getScheduler()
            .cancelTask(playerPID.get(e.getPlayer()));
        }

        playerAFK.remove(e.getPlayer());
        playerGroups.remove(e.getPlayer());
    }

    /**
     * Restarts the repeating sync task and
     * sets playerAFK to false.
     * @param player the player that is being processed
     */
    public final void updatePlayer(final Player player) {
        updatePlayer(player, false);
    }

    /**
     * Restarts the repeating sync task and
     * sets playerAFK to false.
     * @param player the player that is being processed
     * @param firstLogin checks whether or not the player
     * logs in for the first time.
     */
    public final void updatePlayer(final Player player, boolean firstLogin) {

        if (!playerPID.containsKey(player) && !firstLogin) {
            player.kickPlayer("You where not recognized by the AFK Manager."
                    + "Please relog.");
            return;
        }

        if (playerPID.get(player) != null) {
            player.getServer().getScheduler().cancelTask(playerPID.get(player));
        }
        playerAFK.put(player, false);

        p.getLogger().log(Level.INFO, "begin player update!");

        Group g = playerGroups.get(player);
        Runnable run = new Runnable() {

            @Override
            public void run() {
                setAFK(player);
            }

        };

        p.getLogger().log(Level.INFO, "player updateing!");

        if (g == null) {
            if (config.getInt("AFK_Manager.AFK.Time") != -1) {
                playerPID.put(player, player.getServer().getScheduler().
                        scheduleSyncRepeatingTask(p, run,
                            config.getInt("AFK_Manager.AFK.Time") * 20,
                            config.getInt("AFK_Manager.AFK.Time") * 20));
            }
        } else {
            playerPID.put(player, g.createAFKTask(
                    player.getServer().getScheduler(), run, player));
        }
        p.getLogger().log(Level.INFO, "player updated!");
    }

    /**
     * Sets the player afk and creates
     * the disconnection task.
     * @param player the player that is afk.
     */
    public void setAFK(final Player player) {

        if (playerPID.get(player) != null) {
            player.getServer().getScheduler().cancelTask(playerPID.get(player));
        }

        Group g = playerGroups.get(player);
        Runnable run = new Runnable() {

            @Override
            public void run() {
                kickPlayer(player);
            }

        };

        if (g == null) {
            if (config.getBoolean("AFK_Manager.AFK.Public")) {
                player.getServer().broadcastMessage(
                        config.getString("AFK_Manager.AFK.Public_Message")
                        .replaceAll("--p", player.getDisplayName()));
            } else {
                player.sendMessage(config.getString("AFK_Manager.AFK.Message"));
            }

            if (config.getInt("AFK_Manager.Disconnect.Time") != -1) {
                playerPID.put(player, player.getServer().getScheduler()
                        .scheduleSyncRepeatingTask(p, run,
                            config.getInt("AFK_Manager.Disconnect.Time") * 20,
                            config.getInt("AFK_Manager.Disconnect.Time") * 20));
            }

            player.setDisplayName(config.getString("AFK_Manager.AFK.Prefix")
                    + player.getDisplayName());
        } else {
            playerPID.put(player, g.createDisconnectTask(
                    player.getServer().getScheduler(), run, player));
        }

        playerAFK.put(player, true);
    }

    /**
     * Disconnects the player.
     * @param player the player that will be disconnected.
     */
    public void kickPlayer(final Player player) {

        Group g = playerGroups.get(player);

        if (playerPID.get(player) != null) {
            player.getServer().getScheduler().cancelTask(playerPID.get(player));
        }

        if (g == null) {
            if (config.getBoolean("AFK_Manager.Disconnection.Public")) {
                player.getServer().broadcastMessage(
                    config.getString("AFK_Manager.Disconnect.Public_Message")
                        .replaceAll("--p", player.getDisplayName()));
            }

            player.kickPlayer(config.getString("AFK_Manager.Disconnect.Message")
                    .replaceAll("--p", player.getDisplayName()));
        } else {
            g.disconnectPlayer(player);
        }
    }
}
