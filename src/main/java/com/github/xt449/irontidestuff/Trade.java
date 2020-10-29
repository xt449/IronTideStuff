package com.github.xt449.irontidestuff;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.Collections;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class Trade {

	final ItemStack inputItem;
	final ItemStack outputItem;
	final MerchantRecipe recipe;

	Trade(ItemStack inputItem, ItemStack ouputItem) {
		this.inputItem = inputItem;
		this.outputItem = ouputItem;
		this.recipe = new MerchantRecipe(outputItem, 0);
		this.recipe.setIngredients(Collections.singletonList(inputItem));
	}

	String serialize() {
		return inputItem.getType().name() + '\u0000' + inputItem.getAmount() + '\u0000' + outputItem.getType().name() + '\u0000' + outputItem.getAmount();
	}

	static Trade deserialize(String text) throws IllegalArgumentException {
		try {
			String[] parts = text.split("\u0000");

			return new Trade(
					new ItemStack(Material.getMaterial(parts[0]), Integer.parseInt(parts[1])),
					new ItemStack(Material.getMaterial(parts[2]), Integer.parseInt(parts[3]))
			);
		} catch(Exception exc) {
			throw new IllegalArgumentException(exc);
		}
	}
}
