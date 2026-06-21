package com.rekindled.embers.util;

import com.rekindled.embers.compat.sublevel.SubLevelCompat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class SubLevelParticleUtil {

	private SubLevelParticleUtil() {
	}

	public static void add(BlockEntity source, ParticleOptions particle, double x, double y, double z,
			double velocityX, double velocityY, double velocityZ) {
		Level level = source.getLevel();
		if (level == null) {
			return;
		}
		Vec3 position = SubLevelCompat.toPhysicalPosition(source, new Vec3(x, y, z));
		Vec3 velocity = SubLevelCompat.toPhysicalDirection(source, new Vec3(velocityX, velocityY, velocityZ));
		level.addParticle(particle, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
	}

	public static void send(BlockEntity source, ParticleOptions particle, double x, double y, double z,
			int count, double spreadX, double spreadY, double spreadZ, double speed) {
		if (!(source.getLevel() instanceof ServerLevel level)) {
			return;
		}
		if (!SubLevelCompat.isInSubLevel(source)) {
			Vec3 position = SubLevelCompat.toPhysicalPosition(source, new Vec3(x, y, z));
			level.sendParticles(particle, position.x, position.y, position.z, count, spreadX, spreadY, spreadZ, speed);
			return;
		}
		for (int i = 0; i < count; i++) {
			Vec3 localPosition = new Vec3(
					x + level.random.nextGaussian() * spreadX,
					y + level.random.nextGaussian() * spreadY,
					z + level.random.nextGaussian() * spreadZ);
			Vec3 localVelocity = new Vec3(
					level.random.nextGaussian() * speed,
					level.random.nextGaussian() * speed,
					level.random.nextGaussian() * speed);
			Vec3 position = SubLevelCompat.toPhysicalPosition(source, localPosition);
			Vec3 velocity = SubLevelCompat.toPhysicalDirection(source, localVelocity);
			level.sendParticles(particle, position.x, position.y, position.z, 0, velocity.x, velocity.y, velocity.z, 1.0);
		}
	}
}
