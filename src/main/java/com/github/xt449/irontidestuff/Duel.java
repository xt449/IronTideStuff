package com.github.xt449.irontidestuff;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author xt449 / BinaryBanana
 */
class Duel {

	final HashMap<UUID, Boolean> participants = new HashMap<>();

	boolean started = false;

	public Duel(UUID creator, UUID... invitations) {
		participants.put(creator, true);
		for(int i = 0; i < invitations.length; i++) {
			participants.put(invitations[i], false);
		}
	}

	boolean isParticipant(Player player) {
		return participants.getOrDefault(player.getUniqueId(), false);
	}
}
