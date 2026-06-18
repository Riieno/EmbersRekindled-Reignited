package com.rekindled.embers.render;

import java.util.List;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rekindled.embers.render.FluidCuboid.FluidFace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

//class copy pasted from mantle, that's why it's so well documented
public class FluidRenderer {
	/**
	 * Gets a block sprite from the given location
	 * @param sprite  Sprite name
	 * @return  Sprite location
	 */
	public static TextureAtlasSprite getBlockSprite(ResourceLocation sprite) {
		return Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(sprite);
	}

	/**
	 * Takes the larger light value between combinedLight and the passed block light
	 * @param combinedLight  Sky light/block light lightmap value
	 * @param blockLight     New 0-15 block light value
	 * @return  Updated packed light including the new light value
	 */
	public static int withBlockLight(int combinedLight, int blockLight) {
		// skylight from the combined plus larger block light between combined and parameter
		// not using methods from LightTexture to reduce number of operations
		return (combinedLight & 0xFFFF0000) | Math.max(blockLight << 4, combinedLight & 0xFFFF);
	}

	/* Fluid cuboids */

	/**
	 * Forces the UV to be between 0 and 1
	 * @param value  Original value
	 * @param upper  If true, this is the larger UV. Needed to enforce integer values end up at 1
	 * @return  UV mapped between 0 and 1
	 */
	private static float boundUV(float value, boolean upper) {
		value = value % 1;
		if (value == 0) {
			// if it lands exactly on the 0 bound, map that to 1 instead for the larger UV
			return upper ? 1 : 0;
		}
		// modulo returns a negative result if the input is negative, so add 1 to account for that
		return value < 0 ? (value + 1) : value;
	}

	private static float shrinkUV(float value, float center, float shrink) {
		return value + (center - value) * shrink;
	}

	/**
	 * Adds a quad to the renderer
	 * @param renderer    Renderer instnace
	 * @param matrix      Render matrix
	 * @param sprite      Sprite to render
	 * @param from        Quad start
	 * @param to          Quad end
	 * @param face        Face to render
	 * @param color       Color to use in rendering
	 * @param brightness  Face brightness
	 * @param flowing     If true, half texture coordinates
	 */
	public static void putTexturedQuad(VertexConsumer renderer, PoseStack matrices, TextureAtlasSprite sprite, Vector3f from, Vector3f to, Direction face, int color, int brightness, int packedOverlay, int rotation, boolean flowing) {
		Matrix4f matrix = matrices.last().pose();
		Matrix3f normal = matrices.last().normal(); 
		// start with texture coordinates
		float x1 = from.x(), y1 = from.y(), z1 = from.z();
		float x2 = to.x(), y2 = to.y(), z2 = to.z();
		// choose UV based on the directions, some need to negate UV due to the direction
		// note that we use -UV instead of 1-UV as its slightly simpler and the later logic deals with negatives
		float u1, u2, v1, v2;
		switch (face) {
		default -> { // DOWN
			u1 = x1; u2 = x2;
			v1 = z2; v2 = z1;
		}
		case UP -> {
			u1 = x1; u2 = x2;
			v1 = -z1; v2 = -z2;
		}
		case NORTH -> {
			u1 = -x1; u2 = -x2;
			v1 = y1; v2 = y2;
		}
		case SOUTH -> {
			u1 = x2; u2 = x1;
			v1 = y1; v2 = y2;
		}
		case WEST -> {
			u1 = z2; u2 = z1;
			v1 = y1; v2 = y2;
		}
		case EAST -> {
			u1 = -z1; u2 = -z2;
			v1 = y1; v2 = y2;
		}
		}

		// flip V when relevant
		if (rotation == 0 || rotation == 270) {
			float temp = v1;
			v1 = -v2;
			v2 = -temp;
		}
		// flip U when relevant
		if (rotation >= 180) {
			float temp = u1;
			u1 = -u2;
			u2 = -temp;
		}

		// bound UV to be between 0 and 1
		boolean reverse = u1 > u2;
		u1 = boundUV(u1, reverse);
		u2 = boundUV(u2, !reverse);
		reverse = v1 > v2;
		v1 = boundUV(v1, reverse);
		v2 = boundUV(v2, !reverse);

		float uvShrink = sprite.uvShrinkRatio();
		float uCenter = (u1 + u2) * 0.5f;
		float vCenter = (v1 + v2) * 0.5f;
		u1 = shrinkUV(u1, uCenter, uvShrink);
		u2 = shrinkUV(u2, uCenter, uvShrink);
		v1 = shrinkUV(v1, vCenter, uvShrink);
		v2 = shrinkUV(v2, vCenter, uvShrink);

		// if rotating by 90 or 270, swap U and V
		float minU, maxU, minV, maxV;
		float uvScale = flowing ? 0.5f : 1.0f;
		if ((rotation % 180) == 90) {
			minU = sprite.getU(v1 * uvScale);
			maxU = sprite.getU(v2 * uvScale);
			minV = sprite.getV(u1 * uvScale);
			maxV = sprite.getV(u2 * uvScale);
		} else {
			minU = sprite.getU(u1 * uvScale);
			maxU = sprite.getU(u2 * uvScale);
			minV = sprite.getV(v1 * uvScale);
			maxV = sprite.getV(v2 * uvScale);
		}
		// based on rotation, put coords into place
		float u3, u4, v3, v4;
		switch(rotation) {
		default -> { // 0
			u1 = minU; v1 = maxV;
			u2 = minU; v2 = minV;
			u3 = maxU; v3 = minV;
			u4 = maxU; v4 = maxV;
		}
		case 90 -> {
			u1 = minU; v1 = minV;
			u2 = maxU; v2 = minV;
			u3 = maxU; v3 = maxV;
			u4 = minU; v4 = maxV;
		}
		case 180 -> {
			u1 = maxU; v1 = minV;
			u2 = maxU; v2 = maxV;
			u3 = minU; v3 = maxV;
			u4 = minU; v4 = minV;
		}
		case 270 -> {
			u1 = maxU; v1 = maxV;
			u2 = minU; v2 = maxV;
			u3 = minU; v3 = minV;
			u4 = maxU; v4 = minV;
		}
		}
		// add quads
		int light1 = brightness & 0xFFFF;
		int light2 = brightness >> 0x10 & 0xFFFF;
		int a = color >> 24 & 0xFF;
		int r = color >> 16 & 0xFF;
		int g = color >> 8 & 0xFF;
		int b = color & 0xFF;
		switch (face) {
		case DOWN -> {
			renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, -1, 0);
			renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, -1, 0);
			renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, -1, 0);
			renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, -1, 0);
		}
		case UP -> {
			renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 1, 0);
			renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 1, 0);
			renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 1, 0);
			renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 1, 0);
		}
		case NORTH -> {
			renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, -1);
			renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, -1);
			renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, -1);
			renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, -1);
		}
		case SOUTH -> {
			renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, 1);
			renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, 1);
			renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, 1);
			renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(0, 0, 1);
		}
		case WEST -> {
			renderer.addVertex(matrix, x1, y1, z2).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(-1, 0, 0);
			renderer.addVertex(matrix, x1, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(-1, 0, 0);
			renderer.addVertex(matrix, x1, y2, z1).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(-1, 0, 0);
			renderer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(-1, 0, 0);
		}
		case EAST -> {
			renderer.addVertex(matrix, x2, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(1, 0, 0);
			renderer.addVertex(matrix, x2, y2, z1).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(1, 0, 0);
			renderer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(1, 0, 0);
			renderer.addVertex(matrix, x2, y1, z2).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setUv2(light1, light2).setNormal(1, 0, 0);
		}
		}
	}

	/**
	 * Renders a full fluid cuboid for the given data
	 * @param matrices  Matrix stack instance
	 * @param buffer    Buffer type
	 * @param still     Still sprite
	 * @param flowing   Flowing sprite
	 * @param cube      Fluid cuboid
	 * @param from      Fluid start
	 * @param to        Fluid end
	 * @param color     Fluid color
	 * @param light     Quad lighting
	 * @param isGas     If true, fluid is a gas
	 */
	public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, TextureAtlasSprite still, TextureAtlasSprite flowing, Vector3f from, Vector3f to, int color, int light, int packedOverlay, boolean isGas) {
		int rotation = isGas ? 180 : 0;
		for (Direction dir : Direction.values()) {
			FluidFace face = cube.getFace(dir);
			if (face != null) {
				boolean isFlowing = face.isFlowing();
				int faceRot = (rotation + face.rotation()) % 360;
				putTexturedQuad(buffer, matrices, isFlowing ? flowing : still, from, to, dir, color, light, packedOverlay, faceRot, isFlowing);
			}
		}
	}

	/**
	 * Renders a list of fluid cuboids
	 * @param matrices  Matrix stack instance
	 * @param buffer    Buffer instance
	 * @param cubes     List of cubes to render
	 * @param fluid     Fluid to use in rendering
	 * @param light     Light level from TER
	 */
	public static void renderCuboids(PoseStack matrices, VertexConsumer buffer, List<FluidCuboid> cubes, FluidStack fluid, int light, int packedOverlay) {
		if (fluid.isEmpty()) {
			return;
		}

		// fluid type, fetch once for all fluids to save effort
		FluidType type = fluid.getFluid().getFluidType();
		IClientFluidTypeExtensions clientType = IClientFluidTypeExtensions.of(type);
		TextureAtlasSprite still = getBlockSprite(clientType.getStillTexture(fluid));
		TextureAtlasSprite flowing = getBlockSprite(clientType.getFlowingTexture(fluid));
		boolean isGas = type.isLighterThanAir();
		light = withBlockLight(light, type.getLightLevel(fluid));
		int color = clientType.getTintColor(fluid);

		// render all given cuboids
		for (FluidCuboid cube : cubes) {
			renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, packedOverlay, isGas);
		}
	}

	/**
	 * Renders a fluid cuboid with the given offset, used to manually place cuboids from a list for rendering {@link #renderCuboids(PoseStack, VertexConsumer, List, FluidStack, int)}
	 * @param matrices  Matrix stack instance
	 * @param buffer    Buffer type
	 * @param cube      Fluid cuboid
	 * @param yOffset   Amount to offset the cube in the Y direction, used in faucets for rendering fluid in lower block
	 * @param still     Still sprite
	 * @param flowing   Flowing sprite
	 * @param color     Fluid color
	 * @param light     Quad lighting from TER
	 * @param isGas     If true, fluid is a gas
	 */
	public static void renderCuboid(PoseStack matrices, VertexConsumer buffer, FluidCuboid cube, float yOffset, TextureAtlasSprite still, TextureAtlasSprite flowing, int color, int light, int packedOverlay, boolean isGas) {
		if (yOffset != 0) {
			matrices.pushPose();
			matrices.translate(0, yOffset, 0);
		}
		renderCuboid(matrices, buffer, cube, still, flowing, cube.getFromScaled(), cube.getToScaled(), color, light, packedOverlay, isGas);
		if (yOffset != 0) {
			matrices.popPose();
		}
	}

	/**
	 * Renders a fluid cuboid with partial height based on the capacity
	 * @param matrices  Matrix stack instance
	 * @param buffer    Render type buffer instance
	 * @param fluid     Fluid to render
	 * @param offset    Fluid amount offset, used to animate transitions
	 * @param capacity  Fluid tank capacity, must be above 0
	 * @param light     Quad lighting from TER
	 * @param cube      Fluid cuboid instance
	 * @param flipGas   If true, flips gas cubes
	 */
	public static void renderScaledCuboid(PoseStack matrices, MultiBufferSource buffer, FluidCuboid cube, FluidStack fluid, float offset, int capacity, int light, int packedOverlay, boolean flipGas) {
		// nothing to render
		if (fluid.isEmpty() || capacity <= 0) {
			return;
		}

		// fluid type
		FluidType type = fluid.getFluid().getFluidType();
		IClientFluidTypeExtensions clientType = IClientFluidTypeExtensions.of(type);
		TextureAtlasSprite still = getBlockSprite(clientType.getStillTexture(fluid));
		TextureAtlasSprite flowing = getBlockSprite(clientType.getFlowingTexture(fluid));
		boolean isGas = type.isLighterThanAir();
		light = withBlockLight(light, type.getLightLevel(fluid));

		// determine height based on fluid amount
		Vector3f from = cube.getFromScaled();
		Vector3f to = cube.getToScaled();
		// gas renders upside down
		float minY = from.y();
		float maxY = to.y();
		float height = (fluid.getAmount() - offset) / capacity;
		if (isGas && flipGas) {
			from = new Vector3f(from);
			from.set(from.x(), maxY + (height * (minY - maxY)), from.z());
		} else {
			to = new Vector3f(to);
			to.set(to.x(), minY + (height * (maxY - minY)), to.z());
		}

		// draw cuboid
		renderCuboid(matrices, buffer.getBuffer(EmbersRenderTypes.FLUID), cube, still, flowing, from, to, clientType.getTintColor(fluid), light, packedOverlay, isGas && flipGas);
	}
}
