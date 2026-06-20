package com.rekindled.embers.network.message;

import java.util.UUID;

import com.rekindled.embers.api.block.IManualAimController;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MessageTurngearControl(BlockPos position, UUID subLevelId, int action, float pitch, float yaw) {
	public static void encode(MessageTurngearControl message, FriendlyByteBuf buffer) {
		buffer.writeBlockPos(message.position);
		buffer.writeBoolean(message.subLevelId != null);
		if (message.subLevelId != null) {
			buffer.writeUUID(message.subLevelId);
		}
		buffer.writeVarInt(message.action);
		buffer.writeFloat(message.pitch);
		buffer.writeFloat(message.yaw);
	}

	public static MessageTurngearControl decode(FriendlyByteBuf buffer) {
		BlockPos position = buffer.readBlockPos();
		UUID subLevelId = buffer.readBoolean() ? buffer.readUUID() : null;
		return new MessageTurngearControl(position, subLevelId, buffer.readVarInt(), buffer.readFloat(), buffer.readFloat());
	}

	public static void handle(MessageTurngearControl message, IPayloadContext context) {
		if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)) {
			return;
		}
		context.enqueueWork(() -> {
			BlockEntity blockEntity = SubLevelCompat.findStoredPosition(player.level(), message.position, message.subLevelId);
			if (blockEntity instanceof IManualAimController controller
					&& Float.isFinite(message.pitch) && Float.isFinite(message.yaw)) {
				controller.handleManualControl(player, message.action, message.pitch, message.yaw);
			}
		});
	}
}
