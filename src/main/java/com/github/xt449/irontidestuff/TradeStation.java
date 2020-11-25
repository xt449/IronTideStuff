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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class TradeStation {

	static final HashMap<UUID, BlockLocation> activeTradesMap = new HashMap<>();

	final BlockLocation location;
	final List<MerchantRecipe> trades;

	boolean editing = false;

	TradeStation(BlockLocation location) {
		this.location = location;
		this.trades = new LinkedList<>();
	}

	TradeStation(BlockLocation location, List<MerchantRecipe> trades) {
		this.location = location;
		this.trades = trades;
	}

	void openMerchant(Player player) {
		final Merchant merchant = Bukkit.createMerchant("Trade Station");
		final Inventory inventory = ((Container) Bukkit.getWorld(location.worldName).getBlockAt(location.x, location.y, location.z).getState()).getInventory();
		merchant.setRecipes(trades.stream().peek(trade -> {
			if(inventory.containsAtLeast(trade.getResult(), trade.getResult().getAmount()) && IronTideStuff.inventoryHasRoom(inventory, trade.getIngredients().get(0))) {
				trade.setMaxUses(Integer.MAX_VALUE);
			} else {
				trade.setMaxUses(0);
			}
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
		return trades.stream().map(IronTideStuff::serializeMerchantRecipe).collect(Collectors.joining("\n"));
	}

	static TradeStation deserialize(BlockLocation location, List<String> lines) {
		return new TradeStation(location, lines.stream().map(IronTideStuff::deserializeMerchantRecipe).collect(Collectors.toList()));
	}
}
