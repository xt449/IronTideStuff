package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class TradeStation {

	private static final int CURRENT_VERSION = 1;

	static final HashMap<UUID, BlockLocation> activeTradesMap = new HashMap<>();

	private final BlockLocation location;
	private final File file;

	private int version;
	List<MerchantRecipe> trades;
	boolean editing = false;

	private final YamlConfiguration config;

	TradeStation(BlockLocation location, File file) {
		this.location = location;
		this.file = file;
		this.config = YamlConfiguration.loadConfiguration(file);
	}

	TradeStation(BlockLocation location) {
		this.location = location;
		this.file = new File(new File(IronTideStuff.instance.innerDataFolder, location.worldName), location.x + "," + location.y + "," + location.z);
		this.config = YamlConfiguration.loadConfiguration(file);
		this.config.set("version", CURRENT_VERSION);
		this.trades = new ArrayList<>();
	}

	void load() {
		if(file.exists()) {
			version = config.getInt("version");
			if(version > 0) {
				if(version < CURRENT_VERSION) {
					IronTideStuff.instance.getLogger().fine("Migrating older data file to new format...");
					migrateAndLoad();
				} else if(version > CURRENT_VERSION) {
					new IOException("Data file '" + file.getName() + "' is saved in newer format. Please update plugin to newer version").printStackTrace();
				} else {
					try {
						trades = config.getList("trades").stream().map(trade -> ((TradeConfigurationSection) trade).trade).collect(Collectors.toList());
					} catch(Exception exc) {
						trades = new ArrayList<>();
					}
					// TODO : Unneeded? - editing = config.getBoolean("editing");
				}
			} else {
				new IOException("Data file '" + file.getName() + "' is saved in unsupported format. Unable to migrate!").printStackTrace();
			}
		} else {
			version = CURRENT_VERSION;
			trades = new ArrayList<>();
		}
	}

	void migrateAndLoad() {
		// TODO
	}

	void save() {
		config.set("trades", trades.stream().map(TradeConfigurationSection::new).collect(Collectors.toList()));

		try {
			file.getParentFile().mkdirs();
			config.save(file);
		} catch(IOException exc) {
			exc.printStackTrace();
		}
	}

	void openMerchant(Player player) {
		final Merchant merchant = Bukkit.createMerchant("Trade Station");

		updateTrades();
		merchant.setRecipes(trades);

		player.openMerchant(merchant, true);
		activeTradesMap.put(player.getUniqueId(), location);
	}

	void updateTrades() {
		final Inventory inventory = ((Container) Bukkit.getWorld(location.worldName).getBlockAt(location.x, location.y, location.z).getState()).getInventory();
		trades.stream().forEach(trade -> {
			if(inventory.containsAtLeast(trade.getResult(), trade.getResult().getAmount()) && IronTideStuff.inventoryHasRoom(inventory, trade.getIngredients().get(0))) {
				trade.setMaxUses(Integer.MAX_VALUE);
			} else {
				trade.setMaxUses(0);
			}
		});
	}
}
