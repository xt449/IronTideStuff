package com.github.xt449.irontidestuff;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

/**
 * @author Jonathan Taclott (xt449 / BinaryBanana)
 */
class Duel {

	final HashMap<UUID, Boolean> participants = new HashMap<>();
	//boolean started = false;

	public Duel(UUID creator, UUID... invitations) {
		participants.put(creator, true);
		for(int i = 0; i < invitations.length; i++) {
			participants.put(invitations[i], false);
		}
	}

	boolean isParticipant(Player player) {
		//return participants.getOrDefault(player.getUniqueId(), false);
		final Boolean accepted = participants.get(player.getUniqueId());
		if(accepted == null) {
			return false;
		}
		return accepted;
	}
}
