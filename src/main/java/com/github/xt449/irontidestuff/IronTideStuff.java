package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
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
        if(sender instanceof Player) {
            if(command.getName().equals("duel")) {
                final Player player = (Player) sender;
                final UUID playerId = player.getUniqueId();
                Duel duel = duelMap.get(playerId);

                if(duel != null) {
                    sender.sendMessage(ChatColor.RED + "You are already in a duel!");
                    return true;
                }

                if(args.length == 0) {
                    return false;
                }

                final Player target = Bukkit.getPlayer(args[0]);

                if(target == null) {
                    sender.sendMessage(ChatColor.RED + "Invalid username!");
                    return true;
                }

                final UUID targetId = target.getUniqueId();
                duel = duelMap.get(targetId);

                if(duel != null) {
                    sender.sendMessage(ChatColor.RED + "This player is already in a duel or has a pending request!");
                    return true;
                }

                duel = new Duel(playerId, targetId);
                duelMap.put(playerId, duel);
                duelMap.put(targetId, duel);
                player.sendMessage(ChatColor.AQUA + "You have challenged " + target.getName() + " to a duel");
                target.sendMessage(ChatColor.AQUA + "You have be challenged to a duel by " + player.getName() + ChatColor.GREEN + "\nTo accept: /acceptduel " + player.getName() + ChatColor.RED + "\nTo decline: /declineduel " + player.getName());
                return true;
            } else if(command.getName().equals("leaveduel")) {
                final Player player = (Player) sender;
                final UUID playerId = player.getUniqueId();
                Duel duel = duelMap.get(playerId);

                if(duel == null) {
                    sender.sendMessage(ChatColor.RED + "You are not in a duel!");
                    return true;
                }

                duel.participants.remove(playerId);
                duelMap.remove(playerId);
                duel.participants.forEach((id, accepted) -> {
                    if(accepted) {
                        Bukkit.getPlayer(id).sendMessage(ChatColor.RED + player.getName() + " has left the duel");
                    }
                });
                if(duel.participants.size() == 1) {
                    duelMap.remove(duel.participants.keySet().iterator().next());
                }
                sender.sendMessage(ChatColor.AQUA + "You have left the duel");
                return true;
            } else if(command.getName().equals("acceptduel")) {
                final Player player = (Player) sender;
                final UUID playerId = player.getUniqueId();
                Duel duel = duelMap.get(playerId);

                if(duel == null) {
                    sender.sendMessage(ChatColor.RED + "You have no pending requests!");
                    return true;
                }

                duel.participants.forEach((id, accepted) -> {
                    if(accepted) {
                        Bukkit.getPlayer(id).sendMessage(ChatColor.GREEN + player.getName() + " has joined the duel");
                    }
                });
                duel.participants.put(playerId, true);
                sender.sendMessage(ChatColor.AQUA + "You have joined the duel");
                return true;
            } else if(command.getName().equals("declineduel")) {
                final Player player = (Player) sender;
                final UUID playerId = player.getUniqueId();
                Duel duel = duelMap.get(playerId);

                if(duel == null) {
                    sender.sendMessage(ChatColor.RED + "You have no pending requests!");
                    return true;
                }

                duel.participants.remove(playerId);
                duelMap.remove(playerId);
                duel.participants.forEach((id, accepted) -> {
                    if(accepted) {
                        Bukkit.getPlayer(id).sendMessage(ChatColor.RED + player.getName() + " has declined the duel");
                    }
                });
                if(duel.participants.size() == 1) {
                    duelMap.remove(duel.participants.keySet().iterator().next());
                }
                sender.sendMessage(ChatColor.AQUA + "You have declined the duel");
                return true;
            }
        }

        sender.sendMessage("Command must be executed by a player");
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if(!player.hasPlayedBefore()) {
            player.getInventory().addItem(new ItemStack(Material.OAK_BOAT));
        }

        updatePlayerListName(player, player.getWorld().getEnvironment());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerLeave(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final UUID playerId = player.getUniqueId();
        final Duel duel = duelMap.get(playerId);

        if(duel != null) {
            duel.participants.remove(playerId);
            duel.participants.forEach((id, accepted) -> {
                if(accepted) {
                    Bukkit.getPlayer(id).sendMessage(ChatColor.RED + player.getName() + " has left the duel");
                }
            });
            if(duel.participants.size() == 1) {
                duelMap.remove(duel.participants.keySet().iterator().next());
            }
        }
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

            if(duel == null || !duel.isParticipant(target) || !duel.isParticipant(damager)) {
                event.setCancelled(true);
            }
        }
    }
}
