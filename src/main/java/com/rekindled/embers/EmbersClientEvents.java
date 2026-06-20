package com.rekindled.embers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL30C;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Axis;
import com.rekindled.embers.api.augment.AugmentUtil;
import com.rekindled.embers.api.augment.IAugment;
import com.rekindled.embers.api.block.IDial;
import com.rekindled.embers.api.capabilities.EmbersCapabilities;
import com.rekindled.embers.api.misc.HammerTarget;
import com.rekindled.embers.api.power.IEmberCapability;
import com.rekindled.embers.api.power.IEmberPacketProducer;
import com.rekindled.embers.api.power.IEmberPacketReceiver;
import com.rekindled.embers.api.tile.IEmberInputHint;
import com.rekindled.embers.api.tile.IExtraCapabilityInformation;
import com.rekindled.embers.api.tile.IUpgradeable;
import com.rekindled.embers.api.upgrades.IUpgradeProxy;
import com.rekindled.embers.blockentity.BeamCannonBlockEntity;
import com.rekindled.embers.blockentity.MechanicalCoreBlockEntity.BlockEntityDirection;
import com.rekindled.embers.blockentity.render.AutomaticHammerBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.EmberBoreBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.EntropicEnumeratorBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.ExcavationBucketsBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.InfernoForgeTopBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.MechanicalPumpBlockEntityRenderer;
import com.rekindled.embers.blockentity.render.StamperBlockEntityRenderer;
import com.rekindled.embers.compat.sublevel.SubLevelCompat;
import com.rekindled.embers.datagen.EmbersItemTags;
import com.rekindled.embers.datagen.EmbersSounds;
import com.rekindled.embers.gui.GuiCodex;
import com.rekindled.embers.render.EmbersRenderTypes;
import com.rekindled.embers.research.ResearchBase;
import com.rekindled.embers.research.ResearchManager;
import com.rekindled.embers.upgrade.ExcavationBucketsUpgrade;
import com.rekindled.embers.util.EmberGenUtil;
import com.rekindled.embers.util.EmbersColors;
import com.rekindled.embers.util.GlowingTextTooltip;
import com.rekindled.embers.util.HeatBarTooltip;
import com.rekindled.embers.util.Misc;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelBakery.ModelBakerImpl;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import com.rekindled.embers.compat.legacy.capabilities.Capability;
import com.rekindled.embers.compat.legacy.capabilities.ForgeCapabilities;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.items.IItemHandler;

@OnlyIn(Dist.CLIENT)
public class EmbersClientEvents {	public static int ticks = 0;
	private static final int EMBER_SPLINE_TIMEOUT_TICKS = 20 * 20;
	public static double gaugeAngle = 0;
	public static long seed = 0;
	public static BlockPos lastTarget = null;
	public static UUID lastTargetSubLevel = null;
	public static BlockPos lastEmitter = null;
	public static UUID lastEmitterSubLevel = null;
	public static int lastEmitterSeenTick = Integer.MIN_VALUE;
	public static ResourceLocation GAUGE = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "textures/gui/ember_meter_overlay.png"); 
	public static ResourceLocation GAUGE_POINTER = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "textures/gui/ember_meter_pointer.png"); 

	public static void onLevelLoad(LevelEvent.Load event) {
		ticks = 0;
		clearLastEmitter();
	}

	public static void onClientTick(ClientTickEvent.Pre event) {
		{
			Minecraft mc = Minecraft.getInstance();
			if (!mc.isPaused()) {
				ticks++;

				if (mc.hitResult instanceof BlockHitResult result) {
					Level world = mc.level;
					if (result != null && world != null) {
						if (result.getType() == BlockHitResult.Type.BLOCK) {
							BlockState state = world.getBlockState(result.getBlockPos());
							if (state.getBlock() instanceof IDial) {
								((IDial) state.getBlock()).updateBEData(result.getBlockPos(), Math.max(0, (mc.getWindow().getScreenHeight() / 2 - 100) / 11));
							}
						}
					}
				}
			}
		}
	}

	public static void onMovementInput(MovementInputUpdateEvent event) {
		if (event.getEntity().isUsingItem() && !event.getEntity().isPassenger() && event.getEntity().getItemInHand(event.getEntity().getUsedItemHand()).is(EmbersItemTags.NORMAL_WALK_SPEED_TOOL)) {
			event.getInput().forwardImpulse /= 0.2f;
			event.getInput().leftImpulse /= 0.2f;
			if (event.getEntity().isSprinting())
				event.getEntity().setSprinting(false);
		}
	}

	//do not render the normal block highlight when the glowing highlight is drawn
	public static void onBlockHighlight(RenderHighlightEvent.Block event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.options.hideGui)
			return;
		HammerTarget target = Misc.getHammerTarget(mc.player);
		if (target != null && event.getTarget().getBlockPos().equals(target.pos)) {
			event.setCanceled(true);
		}
	}

	public static void onLevelRender(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.options.hideGui)
				return;

			Player player = mc.player;
			boolean drewGlowLines = false;
			RenderType glowLineRenderType = EmbersRenderTypes.glowLines();
			HammerTarget target = Misc.getHammerTarget(player);
			if (target != null) {
				BlockEntity targetTile = SubLevelCompat.findStoredPosition(player.level(), target.pos, target.subLevelId);
				if (!(targetTile instanceof IEmberPacketProducer emitter)) {
					return;
				}
				BlockPos targetPos = targetTile.getBlockPos();
				BlockState state = targetTile.getBlockState();
				if (state.isAir())
					return;
				Direction targetDir = target.face;
				Vec3 camPos = event.getCamera().getPosition();
				VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(glowLineRenderType);
				drewGlowLines = true;
				Vector3f color = Misc.multColor(EmbersColors.EMBER, (float) (Math.sin(Math.toRadians(4.0f*(event.getRenderTick() + event.getPartialTick().getGameTimeDeltaPartialTick(false))))+1.0f) / 2.0f);
				float alpha = 0.8F;
				double x = -camPos.x;
				double y = -camPos.y;
				double z = -camPos.z;
				PoseStack.Pose pose = event.getPoseStack().last();

				Shapes.DoubleLineConsumer lineDrawer = (fromX, fromY, fromZ, toX, toY, toZ) -> {
					float f = (float)(toX - fromX);
					float f1 = (float)(toY - fromY);
					float f2 = (float)(toZ - fromZ);
					float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
					f /= f3;
					f1 /= f3;
					f2 /= f3;
					consumer.addVertex(pose.pose(), (float)(fromX + x), (float)(fromY + y), (float)(fromZ + z)).setColor(color.x, color.y, color.z, alpha).setNormal(pose, f, f1, f2);
					consumer.addVertex(pose.pose(), (float)(toX+ x), (float)(toY + y), (float)(toZ + z)).setColor(color.x, color.y, color.z, alpha).setNormal(pose, f, f1, f2);
				};

				Vec3 motion = emitter.getEmittingDirection(targetDir);

				if (mc.hitResult instanceof BlockHitResult result && result != null && result.getType() == BlockHitResult.Type.BLOCK) {
					BlockEntity receiver = SubLevelCompat.findAtPosition(mc.level, result.getBlockPos());
					if (receiver instanceof IEmberPacketReceiver && receiver != targetTile) {
						lastTarget = receiver.getBlockPos();
						lastTargetSubLevel = SubLevelCompat.getContainingSubLevelId(receiver);
					}
				}

				if (motion != null) {
					Vec3 targetCorner = SubLevelCompat.toPhysicalPosition(targetTile, Vec3.atLowerCornerOf(targetPos));
					Vec3 targetCenter = SubLevelCompat.toPhysicalPosition(targetTile, Vec3.atCenterOf(targetPos));
					motion = SubLevelCompat.toPhysicalDirection(targetTile, motion);
					state.getShape(player.level(), targetPos).forAllEdges((fromX, fromY, fromZ, toX, toY, toZ) ->
							lineDrawer.consume(targetCorner.x + fromX, targetCorner.y + fromY, targetCorner.z + fromZ, targetCorner.x + toX, targetCorner.y + toY, targetCorner.z + toZ));

					if (lastTarget != null) {
						Vec3 hitPos = SubLevelCompat.storedPhysicalPosition(player.level(), lastTarget, lastTargetSubLevel);
						Vec3 oldPos = targetCenter;
						Vec3 newPos = oldPos.add(motion);

						for (int i = 0; i <= 80; ++i) {
							Vec3 targetVector = hitPos.subtract(newPos);
							double length = targetVector.length();
							targetVector = targetVector.scale(0.3 / length);
							double weight = 0;
							if (length <= 3) {
								weight = 0.9 * ((3.0 - length) / 3.0);
								if (length <= 0.2) {
									break;
								}
							}
							motion = new Vec3(
									(0.9 - weight) * motion.x + (0.1 + weight) * targetVector.x,
									(0.9 - weight) * motion.y + (0.1 + weight) * targetVector.y,
									(0.9 - weight) * motion.z + (0.1 + weight) * targetVector.z);
							newPos = oldPos.add(motion);
							lineDrawer.consume(oldPos.x, oldPos.y, oldPos.z, newPos.x, newPos.y, newPos.z);
							oldPos = newPos;
						}
					} else {
						motion = motion.scale(2.0);
						lineDrawer.consume(targetCenter.x, targetCenter.y, targetCenter.z, targetCenter.x + motion.x, targetCenter.y + motion.y, targetCenter.z + motion.z);
					}
				}
			} else {
				lastTarget = null;
				lastTargetSubLevel = null;
			}
			if (Misc.isWearingLens(player)) {
				BlockPos lookedPos = null;
				UUID lookedSubLevel = null;
				if (mc.hitResult instanceof BlockHitResult result && result != null && result.getType() == BlockHitResult.Type.BLOCK) {
					BlockEntity emitter = SubLevelCompat.findAtPosition(mc.level, result.getBlockPos());
					if (emitter != null) {
						lookedPos = emitter.getBlockPos();
						lookedSubLevel = SubLevelCompat.getContainingSubLevelId(emitter);
					}
					if (emitter instanceof IEmberPacketProducer) {
						rememberLastEmitter(emitter);
					}
				}
				Vec3 camPos = event.getCamera().getPosition();
				VertexConsumer consumer = mc.renderBuffers().bufferSource().getBuffer(glowLineRenderType);
				drewGlowLines = true;
				Vector3f color = Misc.multColor(EmbersColors.EMBER, (float) (Math.sin(Math.toRadians(4.0f*(event.getRenderTick() + event.getPartialTick().getGameTimeDeltaPartialTick(false))))+1.0f) / 2.0f);
				float alpha = 0.6F;
				double x = -camPos.x;
				double y = -camPos.y;
				double z = -camPos.z;
				PoseStack.Pose pose = event.getPoseStack().last();

				Shapes.DoubleLineConsumer lineDrawer = (fromX, fromY, fromZ, toX, toY, toZ) -> {
					float f = (float)(toX - fromX);
					float f1 = (float)(toY - fromY);
					float f2 = (float)(toZ - fromZ);
					float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
					f /= f3;
					f1 /= f3;
					f2 /= f3;
					consumer.addVertex(pose.pose(), (float)(fromX + x), (float)(fromY + y), (float)(fromZ + z)).setColor(color.x, color.y, color.z, alpha).setNormal(pose, f, f1, f2);
					consumer.addVertex(pose.pose(), (float)(toX+ x), (float)(toY + y), (float)(toZ + z)).setColor(color.x, color.y, color.z, alpha).setNormal(pose, f, f1, f2);
				};

				HashSet<EmberLineNode> drawnLines = new HashSet<EmberLineNode>();
				HashSet<EmberLineNode> linesToDraw = new HashSet<EmberLineNode>();
				HashSet<EmberLineNode> nextLinesToDraw = new HashSet<EmberLineNode>();
				boolean lookedAtSplineNode = false;
				if (lastEmitter != null) {
					linesToDraw.add(new EmberLineNode(lastEmitter, lastEmitterSubLevel));
				}
				for (int i = 0; i <= 20 && !linesToDraw.isEmpty();) {
					for (EmberLineNode emitterNode : linesToDraw) {
						for (Direction side : Direction.values()) {
							EmberLineNode newTarget = drawEmittingLine(event, player.level(), mc, lineDrawer, emitterNode, side);
							if (newTarget != null) {
								i++;
								if (matchesLineNode(newTarget, lookedPos, lookedSubLevel)) {
									lookedAtSplineNode = true;
								}
								if (!drawnLines.contains(newTarget))
									nextLinesToDraw.add(newTarget);
							}
							drawnLines.add(emitterNode);
						}
					}
					linesToDraw = nextLinesToDraw;
					nextLinesToDraw = new HashSet<EmberLineNode>();
				}
				if (lookedAtSplineNode) {
					lastEmitterSeenTick = ticks;
				}
				if (lastEmitter != null && ticks - lastEmitterSeenTick > EMBER_SPLINE_TIMEOUT_TICKS) {
					clearLastEmitter();
				}
			} else {
				clearLastEmitter();
			}
			if (drewGlowLines) {
				mc.renderBuffers().bufferSource().endBatch(glowLineRenderType);
			}
		}
	}

	private record EmberLineNode(BlockPos pos, UUID subLevelId) {
	}

	private static void rememberLastEmitter(BlockEntity emitter) {
		lastEmitter = emitter.getBlockPos();
		lastEmitterSubLevel = SubLevelCompat.getContainingSubLevelId(emitter);
		lastEmitterSeenTick = ticks;
	}

	private static void clearLastEmitter() {
		lastEmitter = null;
		lastEmitterSubLevel = null;
		lastEmitterSeenTick = Integer.MIN_VALUE;
	}

	private static boolean matchesLineNode(EmberLineNode node, BlockPos pos, UUID subLevelId) {
		return node != null && pos != null && node.pos().equals(pos) && (node.subLevelId() == null ? subLevelId == null : node.subLevelId().equals(subLevelId));
	}

	private static EmberLineNode drawEmittingLine(RenderLevelStageEvent event, Level level, Minecraft mc, Shapes.DoubleLineConsumer lineDrawer, EmberLineNode emitterNode, Direction side) {
		BlockPos target = null;
		if (emitterNode != null && SubLevelCompat.findStoredPosition(level, emitterNode.pos(), emitterNode.subLevelId()) instanceof BlockEntity emitterTile && emitterTile instanceof IEmberPacketProducer emitter) {
			target = emitter.getTarget(side);
			if (target == null)
				return null;
			if (emitterTile instanceof BeamCannonBlockEntity) {
				Vec3 start = SubLevelCompat.toPhysicalPosition(emitterTile, Vec3.atCenterOf(emitterTile.getBlockPos()));
				Vec3 end = SubLevelCompat.toPhysicalPosition(emitterTile, Vec3.atCenterOf(target));
				lineDrawer.consume(start.x, start.y, start.z, end.x, end.y, end.z);
				return null;
			}
			UUID targetSubLevelId = emitter.getTargetSubLevelId(side);
			Vec3 hitPos = SubLevelCompat.linkedTargetPhysicalPosition(emitterTile, target, targetSubLevelId);
			Vec3 motion = SubLevelCompat.toPhysicalDirection(emitterTile, emitter.getEmittingDirection(side));
			Vec3 oldPos = SubLevelCompat.toPhysicalPosition(emitterTile, Vec3.atCenterOf(emitterTile.getBlockPos()));
			Vec3 newPos = oldPos.add(motion);

			for (int i = 0; i <= 80; ++i) {
				Vec3 targetVector = hitPos.subtract(newPos);
				double length = targetVector.length();
				targetVector = targetVector.scale(0.3 / length);
				double weight = 0;
				if (length <= 3) {
					weight = 0.9 * ((3.0 - length) / 3.0);
					if (length <= 0.2) {
						lineDrawer.consume(oldPos.x, oldPos.y, oldPos.z, hitPos.x, hitPos.y, hitPos.z);
						break;
					}
				}
				motion = new Vec3(
						(0.9 - weight) * motion.x + (0.1 + weight) * targetVector.x,
						(0.9 - weight) * motion.y + (0.1 + weight) * targetVector.y,
						(0.9 - weight) * motion.z + (0.1 + weight) * targetVector.z);
				newPos = oldPos.add(motion);
				lineDrawer.consume(oldPos.x, oldPos.y, oldPos.z, newPos.x, newPos.y, newPos.z);
				oldPos = newPos;
			}
			return new EmberLineNode(target, targetSubLevelId);
		}
		return null;
	}

	public static void renderIngameOverlay(Minecraft mc, GuiGraphics graphics, float partialTicks, int width, int height) {
		if (mc.options.hideGui)
			return;

		Player player = mc.player;

		if (mc.hitResult instanceof BlockHitResult result) {
			ClientLevel world = mc.level;
			if (result != null) {
				if (result.getType() == BlockHitResult.Type.BLOCK) {
					BlockPos pos = result.getBlockPos();
					BlockState state = world.getBlockState(pos);
					BlockEntity tileEntity = world.getBlockEntity(result.getBlockPos());
					Direction facing = result.getDirection();
					List<Component> text = new ArrayList<Component>();

					if (tileEntity instanceof IEmberInputHint input && input.shouldShowHintTooltip()) {
						text.add(Component.translatable(Embers.MODID + ".tooltip.craft_lens_0"));
						text.add(Component.translatable(Embers.MODID + ".tooltip.craft_lens_1"));
					}
					if ((player.getMainHandItem().is(EmbersItemTags.ANCIENT_CODEX) || player.getOffhandItem().is(EmbersItemTags.ANCIENT_CODEX)) && ResearchManager.researchByItem.get(state.getBlock().asItem()) != null) {
						text.add(Component.translatable(Embers.MODID + ".tooltip.research.world"));
					}
					if (state.getBlock() instanceof IDial) {
						text.addAll(((IDial) state.getBlock()).getDisplayInfo(world, result.getBlockPos(), state, Math.max(0, (height / 2 - 100) / 11)));
					} else if (state.getBlock() == RegistryManager.ATMOSPHERIC_GAUGE.get() && !player.getMainHandItem().is(EmbersItemTags.GAUGE_OVERLAY) && !player.getOffhandItem().is(EmbersItemTags.GAUGE_OVERLAY)) {
						renderAtmosphericGauge(mc, graphics, player, partialTicks, width, height);
					} else if (Misc.isWearingLens(player)) {
						if (tileEntity != null) {
							addCapabilityInformation(text, state, tileEntity, facing);
						}
					}
					if (!text.isEmpty()) {
						for (int i = 0; i < text.size(); i++) {
							graphics.drawString(mc.font, text.get(i), width / 2 - mc.font.width(text.get(i)) / 2, height / 2 + 40 + 11 * i, 0xFFFFFF);
						}
					}
				}
			}
		}

		if (player.getMainHandItem().getItem() == RegistryManager.ATMOSPHERIC_GAUGE_ITEM.get() || (!player.getMainHandItem().is(EmbersItemTags.GAUGE_OVERLAY) && player.getOffhandItem().getItem() == RegistryManager.ATMOSPHERIC_GAUGE_ITEM.get())) {
			renderAtmosphericGauge(mc, graphics, player, partialTicks, width, height);
		}
	}

	public static void renderAtmosphericGauge(Minecraft mc, GuiGraphics graphics, Player player, float partialTicks, int width, int height) {
		int x = width / 2;
		int y = height / 2;

		graphics.pose().pushPose();

		//int offsetX = 0;

		graphics.blit(GAUGE, x - 16, y - 16, 0, 0, 0, 32, 32, 32, 32);

		//double angle = 195.0;
		//EmberWorldData data = EmberWorldData.get(world);
		if (player != null) {
			//if (data.emberData != null){
			//if (data.emberData.containsKey(""+((int)player.posX) / 16 + " " + ((int)player.posZ) / 16)){
			double ratio = EmberGenUtil.getEmberDensity(seed, player.getBlockX(), player.getBlockZ());
			if (gaugeAngle == 0) {
				gaugeAngle = 165.0 + 210.0 * ratio;
			} else {
				gaugeAngle = gaugeAngle * 0.99 + 0.01 * (165.0 + 210.0 * ratio);
			}
			//}
			//}
		}

		graphics.pose().translate(x, y, 0);
		graphics.pose().mulPose(Axis.ZP.rotationDegrees((float) gaugeAngle));
		graphics.pose().translate(-2.5, -2.5, 0);

		graphics.blit(GAUGE_POINTER, 0, 0, 0, 0, 0, 12, 5, 16, 16);

		graphics.pose().popPose();
	}

	private static void addCapabilityInformation(List<Component> text, BlockState state, BlockEntity tile, Direction facing) {
		addCapabilityItemDescription(text, tile, facing);
		addCapabilityFluidDescription(text, tile, facing);
		addCapabilityEmberDescription(text, tile, facing);
		//if (ConfigManager.isMysticalMechanicsIntegrationEnabled())
		//MysticalMechanicsIntegration.addCapabilityInformation(text, tile, facing);
		if (com.rekindled.embers.util.CapabilityCompat.getCapability(tile, EmbersCapabilities.UPGRADE_PROVIDER_CAPABILITY, facing).isPresent())
			text.add(Component.translatable(Embers.MODID + ".tooltip.goggles.upgrade"));
		boolean proxyable = Misc.isSideProxyable(state, tile, facing);
		if (!proxyable && tile instanceof IUpgradeProxy proxy) {
			BlockEntityDirection multiBlock = proxy.getAttachedMultiblock(ConfigManager.MAX_PROXY_DISTANCE.get() - 1);
			proxyable = multiBlock != null && Misc.isSideProxyable(multiBlock.blockEntity.getLevel().getBlockState(multiBlock.blockEntity.getBlockPos()), multiBlock.blockEntity, multiBlock.direction);
		}
		if (proxyable)
			text.add(Component.translatable(Embers.MODID + ".tooltip.goggles.accessor_slot"));
		if (tile instanceof IUpgradeable upgradeable && upgradeable.isSideUpgradeSlot(facing))
			text.add(Component.translatable(Embers.MODID + ".tooltip.goggles.upgrade_slot"));
		//if (tile instanceof IMechanicallyPowered)
		//	text.add(Component.translatable(Embers.MODID + ".tooltip.goggles.actuator_slot"));
		if (tile instanceof IExtraCapabilityInformation)
			((IExtraCapabilityInformation) tile).addOtherDescription(text, facing);
	}

	public static void addCapabilityItemDescription(List<Component> text, BlockEntity tile, Direction facing) {
		Capability<IItemHandler> capability = ForgeCapabilities.ITEM_HANDLER;
		if (com.rekindled.embers.util.CapabilityCompat.getCapability(tile, capability, facing).isPresent()) {
			IExtraCapabilityInformation.EnumIOType ioType = IExtraCapabilityInformation.EnumIOType.BOTH;
			Component filter = null;
			if (tile instanceof IExtraCapabilityInformation && ((IExtraCapabilityInformation) tile).hasCapabilityDescription(capability)) {
				((IExtraCapabilityInformation) tile).addCapabilityDescription(text, capability, facing);
			} else {
				text.add(IExtraCapabilityInformation.formatCapability(ioType, Embers.MODID + ".tooltip.goggles.item", filter));
			}
		}
	}

	public static void addCapabilityFluidDescription(List<Component> text, BlockEntity tile, Direction facing) {
		Capability<IFluidHandler> capability = ForgeCapabilities.FLUID_HANDLER;
		if (com.rekindled.embers.util.CapabilityCompat.getCapability(tile, capability, facing).isPresent()) {
			IExtraCapabilityInformation.EnumIOType ioType = IExtraCapabilityInformation.EnumIOType.BOTH;
			Component filter = null;
			if (tile instanceof IExtraCapabilityInformation && ((IExtraCapabilityInformation) tile).hasCapabilityDescription(capability)) {
				((IExtraCapabilityInformation) tile).addCapabilityDescription(text, capability, facing);
			} else {
				//fluid handlers no longer tell you if you can insert or remove fluids anymore

				/*IFluidHandler handler = com.rekindled.embers.util.CapabilityCompat.getCapability(tile, capability, facing).orElse(null);
				for (IFluidTankProperties properties : handler.getTankProperties()) {
					boolean input = properties.canFill();
					boolean output = properties.canDrain();
					if (!input && !output)
						ioType = IExtraCapabilityInformation.EnumIOType.NONE;
					else if (input && !output)
						ioType = IExtraCapabilityInformation.EnumIOType.INPUT;
					else if (output && !input)
						ioType = IExtraCapabilityInformation.EnumIOType.OUTPUT;
				}*/
				text.add(IExtraCapabilityInformation.formatCapability(ioType, Embers.MODID + ".tooltip.goggles.fluid", filter));
			}

		}
	}

	public static void addCapabilityEmberDescription(List<Component> text, BlockEntity tile, Direction facing) {
		Capability<IEmberCapability> capability = EmbersCapabilities.EMBER_CAPABILITY;
		if (com.rekindled.embers.util.CapabilityCompat.getCapability(tile, capability, facing).isPresent()) {
			IExtraCapabilityInformation.EnumIOType ioType = IExtraCapabilityInformation.EnumIOType.BOTH;
			if (tile instanceof IExtraCapabilityInformation && ((IExtraCapabilityInformation) tile).hasCapabilityDescription(capability)) {
				((IExtraCapabilityInformation) tile).addCapabilityDescription(text, capability, facing);
			} else {
				text.add(IExtraCapabilityInformation.formatCapability(ioType, Embers.MODID + ".tooltip.goggles.ember", null));
			}
		}
	}

	public static void afterModelBake(ModelEvent.BakingCompleted event) {
		ModelManager modelManager = event.getModelManager();
		EmberBoreBlockEntityRenderer.blades = getModel(modelManager, "ember_bore_blades");
		MechanicalPumpBlockEntityRenderer.pistonBottom = getModel(modelManager, "mechanical_pump_piston_bottom");
		MechanicalPumpBlockEntityRenderer.pistonTop = getModel(modelManager, "mechanical_pump_piston_top");
		StamperBlockEntityRenderer.arm = getModel(modelManager, "stamper_arm");
		AutomaticHammerBlockEntityRenderer.hammer = getModel(modelManager, "automatic_hammer_end");
		InfernoForgeTopBlockEntityRenderer.hatch = getModel(modelManager, "inferno_forge_hatch");
		EntropicEnumeratorBlockEntityRenderer.cubies[0][0][0] = getModel(modelManager, "entropic_enumerator_drf");
		EntropicEnumeratorBlockEntityRenderer.cubies[1][0][0] = getModel(modelManager, "entropic_enumerator_dlf");
		EntropicEnumeratorBlockEntityRenderer.cubies[0][1][0] = getModel(modelManager, "entropic_enumerator_urf");
		EntropicEnumeratorBlockEntityRenderer.cubies[1][1][0] = getModel(modelManager, "entropic_enumerator_ulf");
		EntropicEnumeratorBlockEntityRenderer.cubies[0][0][1] = getModel(modelManager, "entropic_enumerator_drb");
		EntropicEnumeratorBlockEntityRenderer.cubies[1][0][1] = getModel(modelManager, "entropic_enumerator_dlb");
		EntropicEnumeratorBlockEntityRenderer.cubies[0][1][1] = getModel(modelManager, "entropic_enumerator_urb");
		EntropicEnumeratorBlockEntityRenderer.cubies[1][1][1] = getModel(modelManager, "entropic_enumerator_ulb");
		ExcavationBucketsBlockEntityRenderer.wheel = getModel(modelManager, "excavation_buckets_wheel");
		ExcavationBucketsUpgrade.buckets = getModel(modelManager, "ember_bore_excavation_buckets");
	}

	public static BakedModel getModel(ModelManager modelManager, String name) {
		ResourceLocation location = ResourceLocation.fromNamespaceAndPath(Embers.MODID, "block/" + name);
		return modelManager.getModel(ModelResourceLocation.standalone(location));
	}

	public static ItemStack lastHoveredItem = ItemStack.EMPTY;
	public static int tickStartedHoldingCtrl = Integer.MAX_VALUE;

	public static void onTooltip(RenderTooltipEvent.GatherComponents event) {
		addTranslatedDescriptionTooltip(event);

		Minecraft mc = Minecraft.getInstance();
		int codexIndex = -1;
		if (ConfigManager.CODEX_REQUIRED_FOR_LOOKUP.get() && mc.player != null) {
			for (int i = 0; i < Inventory.getSelectionSize(); i++) {
				if (mc.player.getInventory().getItem(i).is(EmbersItemTags.ANCIENT_CODEX)) {
					codexIndex = i;
					break;
				}
			}
		}
		if (codexIndex >= 0 || !ConfigManager.CODEX_REQUIRED_FOR_LOOKUP.get()) {
			ResearchBase research = ResearchManager.researchByItem.get(event.getItemStack().getItem());
			if (research != null) {
				float openProgress = 0;
				if (Screen.hasControlDown()) {
					if (tickStartedHoldingCtrl == Integer.MAX_VALUE) {
						tickStartedHoldingCtrl = ticks;
					}
					openProgress = mc.getTimer().getGameTimeDeltaPartialTick(true) + ticks - tickStartedHoldingCtrl;
				} else {
					tickStartedHoldingCtrl = Integer.MAX_VALUE;
				}
				float intensity = (float) (5.0f * (1 - Math.sqrt(1 - Math.pow(openProgress / ((float) ConfigManager.TICKS_TO_OPEN_CODEX.get()), 2)))) - 1.0f;
				event.getTooltipElements().add(1, Either.right(new GlowingTextTooltip(Component.translatable(Embers.MODID + ".tooltip.research").withStyle(ChatFormatting.GRAY), intensity)));
				if (openProgress >= ConfigManager.TICKS_TO_OPEN_CODEX.get()) {
					if (ConfigManager.CODEX_REQUIRED_FOR_LOOKUP.get())
						mc.player.getInventory().selected = codexIndex;
					GuiCodex.instance.previousScreen = mc.screen;
					GuiCodex.instance.researchPage = research;
					mc.setScreen(GuiCodex.instance);
					ResearchManager.sendCheckmark(research, true);
					mc.level.playSound(mc.player, mc.player, EmbersSounds.CODEX_PAGE_OPEN.get(), SoundSource.MASTER, 0.75f, 1.0f);
				}
			}
		}
		if (AugmentUtil.hasHeat(event.getItemStack())) {
			event.getTooltipElements().add(Either.left(Component.empty()));
			if (AugmentUtil.getLevel(event.getItemStack()) > 0) {
				event.getTooltipElements().add(Either.right(new GlowingTextTooltip(Component.translatable(Embers.MODID + ".tooltip.heat_level").withStyle(ChatFormatting.GRAY), Component.literal("" + AugmentUtil.getLevel(event.getItemStack())))));
				int slots = AugmentUtil.getLevel(event.getItemStack()) - AugmentUtil.getTotalAugmentLevel(event.getItemStack());
				if (slots > 0)
					event.getTooltipElements().add(Either.right(new GlowingTextTooltip(Component.translatable(Embers.MODID + ".tooltip.augment_slots").withStyle(ChatFormatting.GRAY), Component.literal("" + slots))));
			}
			float heat = AugmentUtil.getHeat(event.getItemStack());
			float maxHeat = AugmentUtil.getMaxHeat(event.getItemStack());
			event.getTooltipElements().add(Either.right(new HeatBarTooltip(Component.translatable(Embers.MODID + ".tooltip.heat_amount").withStyle(ChatFormatting.GRAY).getVisualOrderText(), heat, maxHeat)));
			if (mc.options.advancedItemTooltips)
				event.getTooltipElements().add(Either.left(Component.translatable(Embers.MODID + ".tooltip.heat_debug", heat, maxHeat).withStyle(ChatFormatting.DARK_GRAY)));

			List<IAugment> augments = AugmentUtil.getAugments(event.getItemStack()).stream().filter(x -> x.shouldRenderTooltip()).collect(Collectors.toList());
			if (augments.size() > 0) {
				event.getTooltipElements().add(Either.left(Component.translatable(Embers.MODID + ".tooltip.augments").withStyle(ChatFormatting.GRAY)));
				for (IAugment augment : augments) {
					int level = AugmentUtil.getAugmentLevel(event.getItemStack(), augment);
					event.getTooltipElements().add(Either.right(new GlowingTextTooltip(Component.translatable(Embers.MODID + ".tooltip.augment." + augment.getName().toLanguageKey(), Component.translatable(getFormattedModifierLevel(level))))));
				}
			}
		}
	}

	private static void addTranslatedDescriptionTooltip(RenderTooltipEvent.GatherComponents event) {
		if (event.getItemStack().isEmpty()) {
			return;
		}

		String key = event.getItemStack().getDescriptionId() + ".tooltip";
		if (!I18n.exists(key)) {
			return;
		}

		int index = Math.min(1, event.getTooltipElements().size());
		event.getTooltipElements().add(index++, Either.left(Component.translatable(key).withStyle(ChatFormatting.GRAY)));
		for (int line = 1; line < 8; line++) {
			String lineKey = key + "." + line;
			if (!I18n.exists(lineKey)) {
				break;
			}
			event.getTooltipElements().add(index++, Either.left(Component.translatable(lineKey).withStyle(ChatFormatting.GRAY)));
		}
	}

	public static String getFormattedModifierLevel(int level) {
		String key = Embers.MODID + ".tooltip.num" + level;
		if (I18n.exists(key))
			return key;
		else
			return Embers.MODID + ".tooltip.numstop";
	}

	public static RenderTarget depthBuffer;

	public static void onWorldRender(RenderLevelStageEvent event) {
		if (!event.getStage().equals(RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS)) {
			return;
		}

		if (!Minecraft.useFancyGraphics() || EmbersRenderTypes.isShaderPackActive()) {
			releaseDepthBuffer();
			return;
		}

		Minecraft mc = Minecraft.getInstance();

		if (depthBuffer == null) {
			depthBuffer = new TextureTarget(mc.getMainRenderTarget().width, mc.getMainRenderTarget().height, true, Minecraft.ON_OSX);
		}

		if (mc.getMainRenderTarget().isStencilEnabled()) {
			depthBuffer.enableStencil();
		}

		RenderTarget mainRenderTarget = mc.getMainRenderTarget();
		depthBuffer.copyDepthFrom(mainRenderTarget);
		GlStateManager._glBindFramebuffer(GL30C.GL_DRAW_FRAMEBUFFER, mainRenderTarget.frameBufferId);
	}

	private static void releaseDepthBuffer() {
		if (depthBuffer != null) {
			depthBuffer.destroyBuffers();
			depthBuffer = null;
		}
	}
}
