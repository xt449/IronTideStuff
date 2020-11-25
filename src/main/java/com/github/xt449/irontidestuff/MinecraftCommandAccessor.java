package com.github.xt449.irontidestuff;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class MinecraftCommandAccessor {

	static {
		((CraftServer) Bukkit.getServer()).getHandle().getServer().vanillaCommandDispatcher.a().register(null);
	}
}
