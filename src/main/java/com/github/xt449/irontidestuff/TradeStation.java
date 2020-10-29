package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class TradeStation {

	static final HashMap<UUID, BlockLocation> activeTradesMap = new HashMap<>();

	final String name;
	final BlockLocation location;
	final LinkedList<Trade> trades;

	boolean editing = false;

	TradeStation(String name, BlockLocation location) {
		this.name = name;
		this.location = location;
		this.trades = new LinkedList<>();
	}

	TradeStation(String name, BlockLocation location, LinkedList<Trade> trades) {
		this.name = name;
		this.location = location;
		this.trades = trades;
	}

	void openMerchant(Player player) {
		final Merchant merchant = Bukkit.createMerchant(name);
		merchant.setRecipes(trades.stream().map(trade -> {
			final MerchantRecipe recipe = trade.recipe;
			final Inventory inventory = ((Container) Bukkit.getWorld(location.worldName).getBlockAt(location.x, location.y, location.z).getState()).getInventory();
			if(inventory.containsAtLeast(trade.outputItem, trade.outputItem.getAmount()) && IronTideStuff.inventoryHasRoom(inventory, trade.inputItem)) {
				recipe.setMaxUses(Integer.MAX_VALUE);
			} else {
				recipe.setMaxUses(0);
			}
			return recipe;
		}).collect(Collectors.toList()));

		player.openMerchant(merchant, true);
		activeTradesMap.put(player.getUniqueId(), location);
	}

	void save() {
		File file = new File(new File(IronTideStuff.shopsFolder, location.worldName), location.x + "," + location.y + "," + location.z);

		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch(IOException exc) {
			exc.printStackTrace();
		}

		try(final BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
			writer.write(serialize());
		} catch(IOException exc) {
			exc.printStackTrace();
		}
	}

	String serialize() {
		return name + '\n' + trades.stream().map(Trade::serialize).collect(Collectors.joining("\n"));
	}

	static TradeStation deserialize(BlockLocation location, String text) {
		final String[] parts = text.split("\n");

		final LinkedList<Trade> trades = new LinkedList<>();

		for(int i = 1; i < parts.length; i++) {
			try {
				trades.add(Trade.deserialize(parts[i]));
			} catch(IllegalArgumentException exc) {
				Bukkit.getLogger().warning("Error reading trade from data at " + location.toString());
			}
		}

		return new TradeStation(parts[0], location, trades);
	}
}
