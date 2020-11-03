package com.github.xt449.irontidestuff;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
public final class IronTideStuff extends JavaPlugin implements Listener {

	private static final String SIGN_TRADE_HEADER = "[Trade]";
	private static final String OVERWORLD_PREFIX = ChatColor.GREEN + "Overworld " + ChatColor.WHITE;
	private static final String NETHER_PREFIX = ChatColor.RED + "Nether " + ChatColor.WHITE;
	private static final String END_PREFIX = ChatColor.YELLOW + "End " + ChatColor.WHITE;
	private static final String OTHER_PREFIX = ChatColor.GRAY + "Other " + ChatColor.WHITE;

	static File shopsFolder;

	private final HashMap<UUID, Duel> duelMap = new HashMap<>();
	private final HashMap<BlockLocation, TradeStation> tradeDataMap = new HashMap<>();

	private final HashMap<String, Material> friendlyMaterialsMap = new HashMap<>(Arrays.stream(Material.values()).filter(material -> material.isItem() && !material.isLegacy()).collect(Collectors.toMap(material -> material.getKey().getKey(), material -> material)));

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
			}, 20, 10 * 20);
		}

		Bukkit.getPluginManager().registerEvents(this, this);

		IronTideStuff.shopsFolder = new File(getDataFolder(), "shops");
		IronTideStuff.shopsFolder.mkdirs();

		final File[] worldFolders = IronTideStuff.shopsFolder.listFiles();
		for(int i = 0; i < worldFolders.length; i++) {
			if(worldFolders[i].isDirectory()) {
				final File[] shopFiles = worldFolders[i].listFiles();
				for(int j = 0; j < shopFiles.length; j++) {
					final StringBuilder builder = new StringBuilder();
					try(final BufferedReader reader = new BufferedReader(new FileReader(shopFiles[j]))) {
						while(reader.ready()) {
							builder.append(reader.readLine()).append('\n');
						}
						final String[] parts = shopFiles[j].getName().split(",");
						final BlockLocation location = new BlockLocation(worldFolders[i].getName(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
						tradeDataMap.put(location, TradeStation.deserialize(location, builder.toString()));
					} catch(IOException exc) {
						exc.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void onDisable() {
		for(TradeStation tradeStation : tradeDataMap.values()) {
			tradeStation.save();
		}
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
				sender.sendMessage(ChatColor.AQUA + "You have challenged " + target.getName() + " to a duel");
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
			} else if(command.getName().equals("addtrade")) {
				if(args.length < 4) {
					return false;
				}

				final Player player = (Player) sender;
				final Block block = player.getTargetBlockExact(5);
				if(block != null && block.getType() == Material.CHEST) {
					final Block signBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
					if(!Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
						sender.sendMessage(ChatColor.RED + "This is not a valid trade station");
						return true;
					}

					ignoreBlockBreak = true;
					final BlockBreakEvent breakCheck = new BlockBreakEvent(block, player);
					Bukkit.getPluginManager().callEvent(breakCheck);
					ignoreBlockBreak = false;
					// check for protection plugins preventing breaking of this block
					if(breakCheck.isCancelled()) {
						return true;
					}

					final TradeStation tradeStation = tradeDataMap.get(BlockLocation.fromLocation(block.getLocation()));
					if(tradeStation == null) {
						sender.sendMessage(ChatColor.RED + "This is not a valid trade station");
						return true;
					}
					if(!tradeStation.editing) {
						sender.sendMessage(ChatColor.RED + "You must switch to editing mode first");
						return true;
					}

					final Material material1 = friendlyMaterialsMap.get(args[0]);
					if(material1 == null) {
						sender.sendMessage(ChatColor.RED + "Invalid item \"" + args[0] + '\"');
						return true;
					}
					final int amount1;
					try {
						amount1 = Integer.parseInt(args[1]);
						if(amount1 < 1 || amount1 > 64) {
							sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64");
							return true;
						}
					} catch(NumberFormatException exc) {
						sender.sendMessage(ChatColor.RED + "Invalid number \"" + args[1] + '\"');
						return true;
					}
					final Material material2 = friendlyMaterialsMap.get(args[2]);
					if(material2 == null) {
						sender.sendMessage(ChatColor.RED + "Invalid item \"" + args[2] + '\"');
						return true;
					}
					final int amount2;
					try {
						amount2 = Integer.parseInt(args[3]);
						if(amount2 < 1 || amount2 > 64) {
							sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64");
							return true;
						}
					} catch(NumberFormatException exc) {
						sender.sendMessage(ChatColor.RED + "Invalid number \"" + args[3] + '\"');
						return true;
					}

					tradeStation.trades.add(new Trade(new ItemStack(material1, amount1), new ItemStack(material2, amount2)));
					sender.sendMessage(ChatColor.GREEN + "Trade added");
					return true;
				} else {
					sender.sendMessage(ChatColor.RED + "You must be looking at a trade station chest to add a trade");
					return true;
				}
			} else if(command.getName().equals("removetrade")) {
				if(args.length < 1) {
					return false;
				}

				final Player player = (Player) sender;
				final Block block = player.getTargetBlockExact(5);
				if(block != null && block.getType() == Material.CHEST) {
					final Block signBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
					if(!Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
						sender.sendMessage(ChatColor.RED + "This is not a valid trade station");
						return true;
					}

					ignoreBlockBreak = true;
					final BlockBreakEvent breakCheck = new BlockBreakEvent(block, player);
					Bukkit.getPluginManager().callEvent(breakCheck);
					ignoreBlockBreak = false;
					// check for protection plugins preventing breaking of this block
					if(breakCheck.isCancelled()) {
						return true;
					}

					final TradeStation tradeStation = tradeDataMap.get(BlockLocation.fromLocation(block.getLocation()));
					if(tradeStation == null) {
						sender.sendMessage(ChatColor.RED + "This is not a valid trade station");
						return true;
					}
					if(!tradeStation.editing) {
						sender.sendMessage(ChatColor.RED + "You must switch to editing mode first");
						return true;
					}

					final int row;
					try {
						row = Integer.parseInt(args[0]);
					} catch(NumberFormatException exc) {
						sender.sendMessage(ChatColor.RED + "Invalid number \"" + args[0] + '\"');
						return true;
					}

					try {
						tradeStation.trades.remove(row - 1);
						sender.sendMessage(ChatColor.GREEN + "Trade removed");
						return true;
					} catch(IndexOutOfBoundsException exc) {
						sender.sendMessage(ChatColor.RED + "Invalid row number");
						return true;
					}
				} else {
					sender.sendMessage(ChatColor.RED + "You must be looking at a trade station chest to add a trade");
					return true;
				}
			}
		}

		sender.sendMessage("Command must be executed by a player");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if(command.getName().equals("addtrade")) {
			if(args.length == 1 || args.length == 3) {
				final String starting = args[args.length - 1];
				return friendlyMaterialsMap.keySet().stream().filter(key -> key.startsWith(starting)).collect(Collectors.toList());
			} else if(args.length == 2 || args.length == 4) {
				return Collections.emptyList();
			}
		}

		return null;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerLogin(PlayerLoginEvent event) {
		final Player player = event.getPlayer();

		if(!player.hasPlayedBefore()) {
			player.getInventory().addItem(new ItemStack(Material.OAK_BOAT));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerJoin(PlayerLoginEvent event) {
		final Player player = event.getPlayer();

		updatePlayerListName(player, player.getWorld().getEnvironment());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		updatePlayerListName(event.getPlayer(), event.getPlayer().getWorld().getEnvironment());
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

	@EventHandler(priority = EventPriority.HIGHEST)
	private void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			final Block block = event.getClickedBlock();

			if(block == null) {
				return;
			}

			if(block.getType() == Material.CHEST) {
				// Chest

				final Block signBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
				if(!Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
					return;
				}

				final TradeStation tradeStation = tradeDataMap.get(BlockLocation.fromLocation(block.getLocation()));
				if(tradeStation == null) {
					return;
				}

				if(!tradeStation.editing) {
					final Player player = event.getPlayer();
					tradeStation.openMerchant(player);

					event.setUseInteractedBlock(Event.Result.DENY);
					event.setCancelled(true);
				}
			} else if(Tag.WALL_SIGNS.isTagged(block.getType())) {
				// Sign

				final Block chestBlock = block.getRelative(((Directional) block.getBlockData()).getFacing(), -1);
				if(chestBlock.getType() != Material.CHEST) {
					return;
				}

				final TradeStation tradeStation = tradeDataMap.get(BlockLocation.fromLocation(chestBlock.getLocation()));
				if(tradeStation == null) {
					return;
				}

				if(event.useInteractedBlock() != Event.Result.DENY) {
					if(!event.getPlayer().isSneaking()) {
						ignoreBlockBreak = true;
						final BlockBreakEvent breakCheck = new BlockBreakEvent(block, event.getPlayer());
						Bukkit.getPluginManager().callEvent(breakCheck);
						ignoreBlockBreak = false;
						// check for protection plugins preventing breaking of this block
						if(!breakCheck.isCancelled()) {
							tradeStation.editing = !tradeStation.editing;
							final Sign sign = (Sign) block.getState();
							if(tradeStation.editing) {
								sign.setLine(0, "[Editing]");
								sign.update();
								event.getPlayer().sendMessage(ChatColor.AQUA + "Trade station switched to editing mode");
							} else {
								sign.setLine(0, SIGN_TRADE_HEADER);
								sign.update();
								event.getPlayer().sendMessage(ChatColor.AQUA + "Trade station switched to standard mode");
							}

							event.setUseInteractedBlock(Event.Result.DENY);
							event.setCancelled(true);
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onSignChange(SignChangeEvent event) {
		if(event.isCancelled()) {
			return;
		}

		if(SIGN_TRADE_HEADER.equals(event.getLine(0))) {
			final String name = event.getLine(1);
			if(name == null || name.length() == 0) {
				event.getPlayer().sendMessage(ChatColor.RED + "You must specify a name for this trade station");
				return;
			}

			final BlockFace blockFace = ((Directional) event.getBlock().getBlockData()).getFacing();
			final Block chestBlock = event.getBlock().getRelative(blockFace, -1);
			if(chestBlock.getType() != Material.CHEST) {
				return;
			}

			if(((Directional) chestBlock.getBlockData()).getFacing() != blockFace) {
				return;
			}

			final BlockLocation location = BlockLocation.fromLocation(chestBlock.getLocation());
			tradeDataMap.put(location, new TradeStation(name, location));
			event.getPlayer().sendMessage(ChatColor.GREEN + "You created a new trade station");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	private void onInventoryClick(InventoryClickEvent event) {
		if(event.getClickedInventory() instanceof MerchantInventory) {
			final MerchantInventory inventory = (MerchantInventory) event.getClickedInventory();

			if(inventory.getHolder() == null) {
				// custom merchant

				if(event.getSlot() == 2) {
					// clicking result slot

					final MerchantRecipe recipe = inventory.getSelectedRecipe();
					if(recipe != null && recipe.getMaxUses() > 0) {
						// recipe available to be made

						if(event.isShiftClick()) {
							// shift click is spooky
							event.getWhoClicked().sendMessage(ChatColor.GOLD + "Shift-clicking from trade stations is not supported");
							event.setResult(Event.Result.DENY);
							return;
						}

						final BlockLocation location = TradeStation.activeTradesMap.get(event.getWhoClicked().getUniqueId());
						if(location != null) {
							final ItemStack cursor = event.getCursor();
							if(cursor != null && cursor.getType() != Material.AIR) {
								// item is currently being moved on cursor
								final ItemStack slot = event.getCurrentItem();
								if(!cursor.isSimilar(slot)) {
									// item on cursor is unable to stack with item in slot
									return;
								}
								if(cursor.getAmount() + slot.getAmount() > cursor.getMaxStackSize()) {
									// item on cursor and item in slot total amount > max stack size
									return;
								}
							}

							final Block block = event.getWhoClicked().getWorld().getBlockAt(location.x, location.y, location.z);
							if(block.getState() instanceof Container) {
								final Inventory chest = ((Container) block.getState()).getInventory();
								final ItemStack result = recipe.getResult();
								int remaining = result.getAmount();

								final ItemStack[] items = chest.getContents();
								for(int i = 0; i < items.length; i++) {
									if(result.isSimilar(items[i])) {
										int available = Math.min(items[i].getAmount(), remaining);
										items[i].setAmount(items[i].getAmount() - available);
										remaining -= available;
										if(remaining == 0) {
											break;
										}
									}
								}
								chest.addItem(recipe.getIngredients().get(0));

								event.getWhoClicked().sendMessage(ChatColor.GREEN + "Successful trade");

								if(!chest.containsAtLeast(result, result.getAmount())) {
									recipe.setMaxUses(0);
									event.getWhoClicked().sendMessage(ChatColor.GOLD + "This trade is now out of stock");
									// TODO - ((Player) event.getWhoClicked()).updateInventory();
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean ignoreBlockBreak = false;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onBlockBreak(BlockBreakEvent event) {
		if(ignoreBlockBreak) {
			return;
		}

		final Block block = event.getBlock();

		if(block.getType() == Material.CHEST) {
			// Chest

			tradeDataMap.remove(BlockLocation.fromLocation(event.getBlock().getLocation()));
		} else if(Tag.WALL_SIGNS.isTagged(block.getType())) {
			// Sign

			final Block chestBlock = block.getRelative(((Directional) block.getBlockData()).getFacing(), -1);
			if(chestBlock.getType() == Material.CHEST) {
				tradeDataMap.remove(BlockLocation.fromLocation(chestBlock.getLocation()));
			}
		}
	}

	static boolean inventoryHasRoom(Inventory inventory, ItemStack item) {
		final Material material = item.getType();
		int remaining = item.getAmount();

		final ItemStack[] items = inventory.getContents();
		for(int i = 0; i < items.length; i++) {
			if(items[i] == null) {
				remaining -= material.getMaxStackSize();
			} else if(items[i].isSimilar(item)) {
				remaining -= (material.getMaxStackSize() - items[i].getAmount());
			}
			if(remaining <= 0) {
				return true;
			}
		}
		return false;
	}
}
