package com.github.xt449.irontidestuff;

import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class BlockLocation {

	String worldName;
	int x;
	int y;
	int z;

	BlockLocation(String worldName, int x, int y, int z) {
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public boolean equals(Object other) {
		if(this == other) return true;
		if(other == null || getClass() != other.getClass()) return false;
		BlockLocation that = (BlockLocation) other;
		return worldName.equals(that.worldName) && x == that.x && y == that.y && z == that.z;
	}

	@Override
	public int hashCode() {
		return Objects.hash(worldName, x, y, z);
	}

	static BlockLocation atBlock(Block block) {
		World world = block.getWorld();
		return new BlockLocation(world.getName(), block.getX(), block.getY(), block.getZ());
	}
}
