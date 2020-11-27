package com.github.xt449.irontidestuff;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
public class TradeConfigurationSection implements ConfigurationSerializable {

	final MerchantRecipe trade;

	TradeConfigurationSection(MerchantRecipe trade) {
		this.trade = trade;
	}

	@Override
	public Map<String, Object> serialize() {
		final HashMap<String, Object> map = new HashMap<>();
		map.put("ingredient", trade.getIngredients().get(0).serialize());
		map.put("result", trade.getResult().serialize());
		return map;
	}

	public static TradeConfigurationSection deserialize(Map<String, Object> map) {
		final ItemStack result = ItemStack.deserialize((Map<String, Object>) map.get("result"));
		final ItemStack ingredient = ItemStack.deserialize((Map<String, Object>) map.get("ingredient"));

		final MerchantRecipe trade = new MerchantRecipe(result, 0);
		trade.addIngredient(ingredient);

		return new TradeConfigurationSection(trade);
	}
}
