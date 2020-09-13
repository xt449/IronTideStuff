package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class IronTideStuff extends JavaPlugin implements Listener {

    private static final String OVERWORLD_PREFIX = ChatColor.GREEN + "Overworld " + ChatColor.WHITE;
    private static final String NETHER_PREFIX = ChatColor.RED + "Nether " + ChatColor.WHITE;
    private static final String END_PREFIX = ChatColor.YELLOW + "End " + ChatColor.WHITE;
    private static final String OTHER_PREFIX = ChatColor.GRAY + "Other " + ChatColor.WHITE;

    @Override
    public void onEnable() {
        double[] temp = null;

        try {
            Object objectDedicatedPlayerList = Bukkit.getServer().getClass().getDeclaredMethod("getHandle").invoke(Bukkit.getServer());
            Object objectDedicatedServer = objectDedicatedPlayerList.getClass().getDeclaredMethod("getServer").invoke(objectDedicatedPlayerList);
            temp = (double[]) objectDedicatedServer.getClass().getField("recentTps").get(objectDedicatedServer);
            //temp = ((CraftServer) Bukkit.getServer()).getHandle().getServer().recentTps;
        } catch(Exception exc) {
            exc.printStackTrace();
        }

        if(temp == null) {
            getLogger().severe("Unable to get TPS from Minecraft Server!");
            getPluginLoader().disablePlugin(this);
        } else {
            final double[] tps = temp;

            Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                for(final Player player : Bukkit.getOnlinePlayers()) {
                    player.setPlayerListHeader(ChatColor.AQUA + "TPS: " + (tps[0] > 20 ? 20 : (int) tps[0]));
                }
            }, 10, 20);
        }

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());
    }

    private void updatePlayerListName(Player player, World.Environment environment) {
        switch(environment) {
            case NORMAL: {
                player.setPlayerListName(OVERWORLD_PREFIX + player.getName());
                break;
            }
            case NETHER: {
                player.setPlayerListName(NETHER_PREFIX + player.getName());
                break;
            }
            case THE_END: {
                player.setPlayerListName(END_PREFIX + player.getName());
                break;
            }
            default: {
                player.setPlayerListName(OTHER_PREFIX + player.getName());
            }
        }
    }
}
