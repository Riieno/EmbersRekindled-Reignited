package com.rekindled.embers.compat.aeronautics;

import java.util.UUID;

import com.rekindled.embers.api.block.IManualAimController;
import com.rekindled.embers.network.PacketHandler;
import com.rekindled.embers.network.message.MessageTurngearControl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

final class TurngearClientInput {
	private static int aimSyncTicks;

	private TurngearClientInput() {
	}

	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		TurngearAddress address = player == null ? null : getAddress(player);
		if (address == null) {
			aimSyncTicks = 0;
			return;
		}
		if (++aimSyncTicks >= 2) {
			aimSyncTicks = 0;
			send(address, IManualAimController.AIM, player);
		}
	}

	static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isAttack()) {
			return;
		}
		Player player = Minecraft.getInstance().player;
		TurngearAddress address = player == null ? null : getAddress(player);
		if (address == null) {
			return;
		}
		send(address, IManualAimController.FIRE, player);
		event.setSwingHand(false);
		event.setCanceled(true);
	}

	private static void send(TurngearAddress address, int action, Player player) {
		PacketHandler.sendToServer(new MessageTurngearControl(address.position, address.subLevelId,
				action, player.getXRot(), player.getYRot()));
	}

	private static TurngearAddress getAddress(Player player) {
		if (!(player.getVehicle() instanceof ArmorStand mount) || !mount.isInvisible() || mount.getCustomName() == null) {
			return null;
		}
		String value = mount.getCustomName().getString();
		if (!value.startsWith(CaminiteTurngearBlockEntity.MOUNT_NAME_PREFIX)) {
			return null;
		}
		String[] parts = value.split("\\|");
		if (parts.length != 3) {
			return null;
		}
		try {
			UUID subLevelId = "world".equals(parts[1]) ? null : UUID.fromString(parts[1]);
			return new TurngearAddress(BlockPos.of(Long.parseLong(parts[2])), subLevelId);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private record TurngearAddress(BlockPos position, UUID subLevelId) {
	}
}
