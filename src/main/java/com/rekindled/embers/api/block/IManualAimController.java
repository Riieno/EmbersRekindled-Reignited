package com.rekindled.embers.api.block;

import net.minecraft.server.level.ServerPlayer;

public interface IManualAimController {
	int AIM = 0;
	int FIRE = 1;

	void handleManualControl(ServerPlayer player, int action, float pitch, float yaw);
}
