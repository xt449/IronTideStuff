package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author xt449 / BinaryBanana
 */
public final class IronTideStuff extends JavaPlugin implements Listener {

    private static final String OVERWORLD_PREFIX = ChatColor.GREEN + "Overworld " + ChatColor.WHITE;
    private static final String NETHER_PREFIX = ChatColor.RED + "Nether " + ChatColor.WHITE;
    private static final String END_PREFIX = ChatColor.YELLOW + "End " + ChatColor.WHITE;
    private static final String OTHER_PREFIX = ChatColor.GRAY + "Other " + ChatColor.WHITE;

    private final HashMap<UUID, Duel> duelMap = new HashMap<>();

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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());

        // TODO
        //duelMap.put(event.getPlayer().getUniqueId(), new Duel());
        //duelMap.values().forEach(duel -> duel.particpants.addAll(Bukkit.getOnlinePlayers().stream().map(Entity::getUniqueId).collect(Collectors.toSet())));
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

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onPlayerDamage(EntityDamageByEntityEvent event) {
        if(event.getEntity() instanceof Player) {
            final Player target = (Player) event.getEntity();
            final Duel duel = duelMap.get(target.getUniqueId());

            final Player damager;

            if(event.getDamager() instanceof Player) {
                damager = (Player) event.getDamager();
            } else if(event.getDamager() instanceof Projectile) {
                final ProjectileSource source = ((Projectile) event.getDamager()).getShooter();

                if(source instanceof Player) {
                    damager = (Player) source;
                } else {
                    return;
                }
            } else if(event.getDamager() instanceof TNTPrimed) {
                final Entity source = ((TNTPrimed) event.getDamager()).getSource();

                if(source instanceof Player) {
                    damager = (Player) source;
                } else {
                    return;
                }
            } else {
                return;
            }

            if(duel == null || !duel.particpants.contains(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }
}
