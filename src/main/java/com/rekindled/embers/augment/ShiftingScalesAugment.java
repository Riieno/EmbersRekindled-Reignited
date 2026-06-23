package com.rekindled.embers.augment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.rekindled.embers.ConfigManager;
import com.rekindled.embers.Embers;
import com.rekindled.embers.api.EmbersAPI;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.event.ScaleEvent;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.network.PacketHandler;
import com.rekindled.embers.network.message.MessageScalesData;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ICapabilitySerializable;
import com.rekindled.embers.compat.legacy.LazyOptional;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class ShiftingScalesAugment extends AugmentBase {

	public static final ResourceLocation TEXTURE_HUD = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "textures/gui/icons.png");
	public static final ResourceLocation SCALES = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "scales");

	//Server
	public static final int COOLDOWN = 33;
	public static final double MOVE_PER_SECOND_THRESHOLD = 0.5;

	public static HashSet<String> unaffectedDamageTypes = new HashSet<>();
	public static HashMap<UUID, Integer> cooldownTicksServer = new HashMap<>();
	public static HashMap<UUID, Vec3> lastPositionServer = new HashMap<>();

	//Client
	public static ArrayList<ShardParticle> shards = new ArrayList<>();
	public static int scales = 0;
	public static int scalesLast = 0;

	public ShiftingScalesAugment(ResourceLocation id) {
		super(id, 0.0);
		NeoForge.EVENT_BUS.register(this);
	}

	public static void setLastPosition(UUID uuid, Vec3 pos) {
		lastPositionServer.put(uuid, pos);
	}

	public static double getMoveDistance(UUID uuid, Vec3 pos) {
		Vec3 lastPos = lastPositionServer.getOrDefault(uuid, pos);
		return lastPos.distanceTo(pos);
	}

	private static void resetEntity(UUID uuid) {
		lastPositionServer.remove(uuid);
		cooldownTicksServer.remove(uuid);
	}

	public static void setCooldown(UUID uuid, int ticks) {
		cooldownTicksServer.put(uuid, ticks);
	}

	public static void setMaxCooldown(UUID uuid, int ticks) {
		cooldownTicksServer.put(uuid, Math.max(ticks, cooldownTicksServer.getOrDefault(uuid, 0)));
	}

	public static boolean hasCooldown(UUID uuid) {
		return cooldownTicksServer.getOrDefault(uuid, 0) > 0;
	}

	public static void sendScalesData(ServerPlayer player) {
		double scales = EmbersAPI.getScales(player);
		PacketHandler.sendToPlayer(player, new MessageScalesData(scales));
	}

	@SubscribeEvent
	public void onServerTick(ServerTickEvent.Pre event) {
		for (UUID uuid : cooldownTicksServer.keySet()) {
			int ticks = cooldownTicksServer.get(uuid) - 1;
			cooldownTicksServer.put(uuid, ticks);
		}
	}

	@SubscribeEvent
	public void onUpdate(EntityTickEvent.Post event) {
		if (!(event.getEntity() instanceof LivingEntity entity)) return;

		if (!entity.level().isClientSide()) {
			UUID uuid = entity.getUUID();
			int scaleLevel = AugmentUtil.getArmorAugmentLevel(entity, this) * 2;
			if (scaleLevel > 0) {
				if (getMoveDistance(uuid, entity.position()) * 20 > MOVE_PER_SECOND_THRESHOLD)
					setMaxCooldown(uuid, COOLDOWN);

				double scales = EmbersAPI.getScales(entity);
				if (!hasCooldown(uuid)) {
					scales += 1;
					setCooldown(uuid, COOLDOWN);
				}
				scales = Math.min(Math.min(scales, scaleLevel * 3), entity.getMaxHealth() * 1.5);
				EmbersAPI.setScales(entity, scales);
				setLastPosition(uuid, entity.position());
			} else {
				EmbersAPI.setScales(entity, 0);
				resetEntity(uuid);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onHit(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof LivingEntity entity)) return;
		DamageSource source = event.getSource();
		if (unaffectedDamageTypes.contains(source.type().msgId()))
			return;
		if (event.getNewDamage() <= 0.0f)
			return;

		int scaleLevel = AugmentUtil.getArmorAugmentLevel(entity, this) * 2;
		if (scaleLevel > 0) {
			if (!entity.level().isClientSide())
				setMaxCooldown(entity.getUUID(), COOLDOWN * 3);
			ScaleEvent scaleEvent = new ScaleEvent(entity, event.getNewDamage(), source, ConfigManager.getScaleDamageRate(source.type().msgId()), ConfigManager.getScaleDamagePass(source.type().msgId()));
			NeoForge.EVENT_BUS.post(scaleEvent);
			double totalDamage = event.getNewDamage();
			double extraDamage = totalDamage * scaleEvent.getScalePassRate();
			totalDamage -= extraDamage;
			double multiplier = scaleEvent.getScaleDamageRate();
			double damage = totalDamage * multiplier;
			double scales = EmbersAPI.getScales(entity);
			double absorbed = Math.min(scales, damage);
			double prevScales = scales;
			scales -= absorbed;
			damage -= absorbed;
			if ((int) scales < (int) prevScales) {
				entity.level().playSound(null, entity, EmbersSounds.SHIFTING_SCALES_BREAK.get(), entity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 10.0f, 1.0f);
			}
			EmbersAPI.setScales(entity, scales);
			event.setNewDamage((float) ((damage == 0 ? 0 : damage / multiplier) + extraDamage));
		}
	}

	public static class ShardParticle {
		double x;
		double y;
		int frame;
		double xSpeed;
		double ySpeed;
		double gravity;

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}

		public int getFrame() {
			return frame;
		}

		public ShardParticle(double x, double y, int frame, double xSpeed, double ySpeed, double gravity) {
			this.x = x;
			this.y = y;
			this.frame = frame;
			this.xSpeed = xSpeed;
			this.ySpeed = ySpeed;
			this.gravity = gravity;
		}

		public void update() {
			x += xSpeed;
			y += ySpeed;

			if (ySpeed < 12)
				ySpeed += gravity;

			frame++;
		}
	}

	public static interface IScalesCapability {
		double getScales();
		void setScales(double scales);
		void writeToNBT(CompoundTag tag);
		void readFromNBT(CompoundTag tag);
	}

	public static class DefaultScalesCapability implements IScalesCapability {
		public double scales = 0;

		@Override
		public double getScales() {
			return scales;
		}

		@Override
		public void setScales(double scales) {
			this.scales = scales;
		}

		@Override
		public void writeToNBT(CompoundTag tag) {
			tag.putDouble("scales", scales);
		}

		@Override
		public void readFromNBT(CompoundTag tag) {
			scales = tag.getDouble("scales");
		}
	}

	public static class ScalesCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
		private IScalesCapability capability;

		public LazyOptional<IScalesCapability> holder = LazyOptional.of(() -> capability);

		public ScalesCapabilityProvider() {
			capability = new DefaultScalesCapability();
			holder = LazyOptional.of(() -> capability);
		}

		public ScalesCapabilityProvider(IScalesCapability capability) {
			this.capability = capability;
			holder = LazyOptional.of(() -> this.capability);
		}

		@Nullable
		public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing) {
			if (EmbersCapabilities.SCALES_CAPABILITY != null && capability == EmbersCapabilities.SCALES_CAPABILITY)
				return EmbersCapabilities.SCALES_CAPABILITY.orEmpty(capability, holder);
			return LazyOptional.empty();
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag compound = new CompoundTag();
			capability.writeToNBT(compound);
			return compound;
		}

		@Override
		public void deserializeNBT(CompoundTag compound) {
			capability.readFromNBT(compound);
		}
	}
}
