package com.rekindled.embers.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.rekindled.embers.Embers;
import com.rekindled.embers.network.message.MessageBeamCannonFX;
import com.rekindled.embers.network.message.MessageCasterOrb;
import com.rekindled.embers.network.message.MessageCrystalCellGrowFX;
import com.rekindled.embers.network.message.MessageDialUpdateRequest;
import com.rekindled.embers.network.message.MessageEmberGenOffset;
import com.rekindled.embers.network.message.MessageEmberRayFX;
import com.rekindled.embers.network.message.MessageItemSound;
import com.rekindled.embers.network.message.MessageResearchData;
import com.rekindled.embers.network.message.MessageResearchTick;
import com.rekindled.embers.network.message.MessageScalesData;
import com.rekindled.embers.network.message.MessageTurngearControl;
import com.rekindled.embers.network.message.MessageWorldSeed;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PacketHandler {

	private static final List<MessageType<?>> MESSAGE_TYPES = new ArrayList<>();
	private static final CustomPacketPayload.Type<Envelope> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "main"));
	private static final StreamCodec<RegistryFriendlyByteBuf, Envelope> CODEC = StreamCodec.of(PacketHandler::encode, PacketHandler::decode);

	static {
		register(MessageDialUpdateRequest.class, MessageDialUpdateRequest::encode, MessageDialUpdateRequest::decode, MessageDialUpdateRequest::handle);
		register(MessageResearchTick.class, MessageResearchTick::encode, MessageResearchTick::decode, MessageResearchTick::handle);
		register(MessageResearchData.class, MessageResearchData::encode, MessageResearchData::decode, MessageResearchData::handle);
		register(MessageWorldSeed.class, MessageWorldSeed::encode, MessageWorldSeed::decode, MessageWorldSeed::handle);
		register(MessageEmberRayFX.class, MessageEmberRayFX::encode, MessageEmberRayFX::decode, MessageEmberRayFX::handle);
		register(MessageItemSound.class, MessageItemSound::encode, MessageItemSound::decode, MessageItemSound::handle);
		register(MessageBeamCannonFX.class, MessageBeamCannonFX::encode, MessageBeamCannonFX::decode, MessageBeamCannonFX::handle);
		register(MessageEmberGenOffset.class, MessageEmberGenOffset::encode, MessageEmberGenOffset::decode, MessageEmberGenOffset::handle);
		register(MessageCasterOrb.class, MessageCasterOrb::encode, MessageCasterOrb::decode, MessageCasterOrb::handle);
		register(MessageScalesData.class, MessageScalesData::encode, MessageScalesData::decode, MessageScalesData::handle);
		register(MessageCrystalCellGrowFX.class, MessageCrystalCellGrowFX::encode, MessageCrystalCellGrowFX::decode, MessageCrystalCellGrowFX::handle);
		register(MessageTurngearControl.class, MessageTurngearControl::encode, MessageTurngearControl::decode, MessageTurngearControl::handle);
	}

	private PacketHandler() {
	}

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playBidirectional(TYPE, CODEC, PacketHandler::handle);
	}

	public static void sendToServer(Object message) {
		PacketDistributor.sendToServer(wrap(message));
	}

	public static void sendToPlayer(ServerPlayer player, Object message) {
		PacketDistributor.sendToPlayer(player, wrap(message));
	}

	public static void sendToAll(Object message) {
		PacketDistributor.sendToAllPlayers(wrap(message));
	}

	public static void sendTrackingEntity(Entity entity, Object message) {
		PacketDistributor.sendToPlayersTrackingEntity(entity, wrap(message));
	}

	public static void sendTrackingEntityAndSelf(Entity entity, Object message) {
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, wrap(message));
	}

	public static void sendTrackingChunk(ServerLevel level, ChunkPos chunk, Object message) {
		PacketDistributor.sendToPlayersTrackingChunk(level, chunk, wrap(message));
	}

	private static <T> void register(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, IPayloadContext> handler) {
		MESSAGE_TYPES.add(new MessageType<>(type, encoder, decoder, handler));
	}

	private static Envelope wrap(Object message) {
		for (int i = 0; i < MESSAGE_TYPES.size(); i++) {
			if (MESSAGE_TYPES.get(i).accepts(message))
				return new Envelope(i, message);
		}
		throw new IllegalArgumentException("Unregistered Embers packet " + message.getClass().getName());
	}

	private static void encode(RegistryFriendlyByteBuf buffer, Envelope envelope) {
		buffer.writeVarInt(envelope.id);
		MESSAGE_TYPES.get(envelope.id).encode(envelope.message, buffer);
	}

	private static Envelope decode(RegistryFriendlyByteBuf buffer) {
		int id = buffer.readVarInt();
		return new Envelope(id, MESSAGE_TYPES.get(id).decode(buffer));
	}

	private static void handle(Envelope envelope, IPayloadContext context) {
		MESSAGE_TYPES.get(envelope.id).handle(envelope.message, context);
	}

	private record Envelope(int id, Object message) implements CustomPacketPayload {
		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	private record MessageType<T>(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder, BiConsumer<T, IPayloadContext> handler) {
		boolean accepts(Object value) {
			return type.isInstance(value);
		}

		void encode(Object value, FriendlyByteBuf buffer) {
			encoder.accept((T) value, buffer);
		}

		Object decode(FriendlyByteBuf buffer) {
			return decoder.apply(buffer);
		}

		void handle(Object value, IPayloadContext context) {
			handler.accept((T) value, context);
		}
	}
}
