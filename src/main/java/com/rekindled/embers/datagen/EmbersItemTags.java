package com.rekindled.embers.datagen;

import java.util.concurrent.CompletableFuture;

import com.rekindled.embers.Embers;
import com.rekindled.embers.RegistryManager;
import com.rekindled.embers.RegistryManager.ToolSet;
import com.rekindled.embers.compat.curios.CuriosCompat;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class EmbersItemTags extends ItemTagsProvider {

	public static final TagKey<Item> PIPE_UNCLOGGER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "pipe_uncloggers"));
	public static final TagKey<Item> MATERIA_BLACKLIST = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "materia_repair_blacklist"));
	public static final TagKey<Item> BREAKDOWN_BLACKLIST = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "anvil_breakdown_blacklist"));
	public static final TagKey<Item> REPAIR_BLACKLIST = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "anvil_repair_blacklist"));

	//this tag is only for recipes
	public static final TagKey<Item> TINKER_HAMMER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "tinker_hammer"));

	public static final TagKey<Item> INSCRIBABLE_PAPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "inscribable_paper"));

	public static final TagKey<Item> TOOLS_HAMMERS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/hammers"));

	public static final TagKey<Item> NORMAL_WALK_SPEED_TOOL = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "normal_walk_speed_tool"));

	public static final TagKey<Item> GAUGE_OVERLAY = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "gauge_overlay"));

	public static final TagKey<Item> CINDER_PLINTH_BLACKLIST = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "cinder_plinth_blacklist"));

	public static final TagKey<Item> AUGMENTABLE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables"));
	public static final TagKey<Item> AUGMENTABLE_TOOLS_AND_ARMORS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/tools_armors"));
	public static final TagKey<Item> AUGMENTABLE_TOOLS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/tools"));
	public static final TagKey<Item> AUGMENTABLE_PROJECTILE_WEAPONS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/projectiles"));
	public static final TagKey<Item> AUGMENTABLE_ARMORS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/armors"));
	public static final TagKey<Item> AUGMENTABLE_HELMETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/armors/helmets"));
	public static final TagKey<Item> AUGMENTABLE_CHESTPLATES = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/armors/chestplates"));
	public static final TagKey<Item> AUGMENTABLE_LEGGINGS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/armors/leggings"));
	public static final TagKey<Item> AUGMENTABLE_BOOTS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "augmentables/armors/boots"));

	public static final TagKey<Item> TINKER_LENS_HELMETS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "tinker_lens_helmets"));

	public static final TagKey<Item> ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus"));
	public static final TagKey<Item> IRON_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/iron"));
	public static final TagKey<Item> COPPER_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/copper"));
	public static final TagKey<Item> LEAD_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/lead"));
	public static final TagKey<Item> SILVER_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/silver"));
	public static final TagKey<Item> DAWNSTONE_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/dawnstone"));
	public static final TagKey<Item> MITHRIL_ASPECTUS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "aspectus/dwarven_mithril"));

	public static final TagKey<Item> ASHEN_STONE = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "ashen_stone"));

	public static final TagKey<Item> PLATES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates"));
	public static final TagKey<Item> IRON_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/iron"));
	//public static final TagKey<Item> GOLD_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/gold"));
	public static final TagKey<Item> COPPER_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/copper"));

	public static final TagKey<Item> COPPER_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/copper"));

	public static final TagKey<Item> LEAD_ORE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/lead"));
	public static final TagKey<Item> RAW_LEAD_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_lead"));
	public static final TagKey<Item> LEAD_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lead"));

	public static final TagKey<Item> RAW_LEAD = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "raw_materials/lead"));
	public static final TagKey<Item> LEAD_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/lead"));
	public static final TagKey<Item> LEAD_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/lead"));
	public static final TagKey<Item> LEAD_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/lead"));

	public static final TagKey<Item> SILVER_ORE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ores/silver"));
	public static final TagKey<Item> RAW_SILVER_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/raw_silver"));
	public static final TagKey<Item> SILVER_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/silver"));

	public static final TagKey<Item> RAW_SILVER = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "raw_materials/silver"));
	public static final TagKey<Item> SILVER_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/silver"));
	public static final TagKey<Item> SILVER_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/silver"));
	public static final TagKey<Item> SILVER_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/silver"));

	public static final TagKey<Item> DAWNSTONE_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/dawnstone"));
	public static final TagKey<Item> DAWNSTONE_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/dawnstone"));
	public static final TagKey<Item> DAWNSTONE_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/dawnstone"));
	public static final TagKey<Item> DAWNSTONE_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/dawnstone"));

	public static final TagKey<Item> MITHRIL_BLOCK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/dwarven_mithril"));
	public static final TagKey<Item> MITHRIL_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/dwarven_mithril"));
	public static final TagKey<Item> MITHRIL_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/dwarven_mithril"));
	public static final TagKey<Item> MITHRIL_PLATE = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/dwarven_mithril"));

	public static final TagKey<Item> NICKEL_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/nickel"));
	public static final TagKey<Item> TIN_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/tin"));
	public static final TagKey<Item> ALUMINUM_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/aluminum"));
	public static final TagKey<Item> ZINC_INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/zinc"));

	public static final TagKey<Item> NICKEL_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/nickel"));
	public static final TagKey<Item> TIN_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/tin"));
	public static final TagKey<Item> ALUMINUM_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/aluminum"));
	public static final TagKey<Item> ZINC_NUGGET = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "nuggets/zinc"));

	public static final TagKey<Item> CAMINITE_BRICK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/caminite_brick"));
	public static final TagKey<Item> ARCHAIC_BRICK = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/archaic_brick"));
	public static final TagKey<Item> ASH_DUST = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "dusts/ash"));

	public static final TagKey<Item> ANCIENT_CODEX = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "ancient_codex"));
	public static final TagKey<Item> STAMPS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "stamps"));
	public static final TagKey<Item> DIALS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "dials"));

	public static final TagKey<Item> WORLD_BOTTOM = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "world_bottom"));
	public static final TagKey<Item> SNOW = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "snow"));

	public static final TagKey<Item> PRISTINE_COPPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "pristine_copper"));
	public static final TagKey<Item> EXPOSED_COPPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "exposed_copper"));
	public static final TagKey<Item> WEATHERED_COPPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "weathered_copper"));
	public static final TagKey<Item> OXIDIZED_COPPER = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "oxidized_copper"));

	public static final TagKey<Item> CRYSTAL_SEEDS = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds"));
	public static final TagKey<Item> COPPER_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/copper"));
	public static final TagKey<Item> IRON_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/iron"));
	public static final TagKey<Item> GOLD_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/gold"));
	public static final TagKey<Item> LEAD_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/lead"));
	public static final TagKey<Item> SILVER_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/silver"));
	public static final TagKey<Item> NICKEL_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/nickel"));
	public static final TagKey<Item> TIN_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/tin"));
	public static final TagKey<Item> ALUMINUM_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/aluminum"));
	public static final TagKey<Item> ZINC_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/zinc"));
	public static final TagKey<Item> PLATINUM_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/platinum"));
	public static final TagKey<Item> URANIUM_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/uranium"));
	public static final TagKey<Item> DAWNSTONE_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/dawnstone"));
	public static final TagKey<Item> MITHRIL_SEED = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Embers.MODID, "crystal_seeds/dwarven_mithril"));

	//curios
	public static final TagKey<Item> ANY_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "curio"));
	public static final TagKey<Item> RING_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "ring"));
	public static final TagKey<Item> BELT_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "belt"));
	public static final TagKey<Item> AMULET_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "necklace"));
	public static final TagKey<Item> BODY_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "body"));
	public static final TagKey<Item> CHARM_CURIO = ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"));

	//other compat
	public static final TagKey<Item> ANVIL_METAL = ItemTags.create(ResourceLocation.fromNamespaceAndPath("tconstruct", "anvil_metal"));

	public EmbersItemTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, CompletableFuture<TagLookup<Block>> blockTagProvider, ExistingFileHelper existingFileHelper) {
		super(output, lookupProvider, blockTagProvider, Embers.MODID, existingFileHelper);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void addTags(HolderLookup.Provider provider) {
		tag(PIPE_UNCLOGGER).addTag(Tags.Items.RODS);
		tag(TINKER_HAMMER).add(RegistryManager.TINKER_HAMMER.get());
		tag(INSCRIBABLE_PAPER).add(Items.PAPER);
		tag(NORMAL_WALK_SPEED_TOOL).add(RegistryManager.BLAZING_RAY.get()).add(RegistryManager.CINDER_STAFF.get());
		tag(GAUGE_OVERLAY).add(RegistryManager.ATMOSPHERIC_GAUGE_ITEM.get());
		tag(CINDER_PLINTH_BLACKLIST).add(Items.NETHERITE_INGOT);

		tag(AUGMENTABLE).addTag(AUGMENTABLE_TOOLS).addTag(AUGMENTABLE_PROJECTILE_WEAPONS).addTag(AUGMENTABLE_ARMORS);
		tag(AUGMENTABLE_TOOLS_AND_ARMORS).addTag(AUGMENTABLE_TOOLS).addTag(AUGMENTABLE_ARMORS);
		tag(AUGMENTABLE_ARMORS).addTag(Tags.Items.ARMORS).addTag(AUGMENTABLE_HELMETS).addTag(AUGMENTABLE_CHESTPLATES).addTag(AUGMENTABLE_LEGGINGS).addTag(AUGMENTABLE_BOOTS);

		tag(AUGMENTABLE_TOOLS).addTag(Tags.Items.TOOLS).addTag(Tags.Items.MELEE_WEAPON_TOOLS).addTag(Tags.Items.RANGED_WEAPON_TOOLS);
		tag(AUGMENTABLE_PROJECTILE_WEAPONS).add(RegistryManager.BLAZING_RAY.get()).add(RegistryManager.CINDER_STAFF.get());
		tag(AUGMENTABLE_HELMETS).addTag(ItemTags.HEAD_ARMOR).add(Items.CARVED_PUMPKIN);
		tag(AUGMENTABLE_CHESTPLATES).addTag(ItemTags.CHEST_ARMOR);
		tag(AUGMENTABLE_LEGGINGS).addTag(ItemTags.LEG_ARMOR);
		tag(AUGMENTABLE_BOOTS).addTag(ItemTags.FOOT_ARMOR);

		tag(TINKER_LENS_HELMETS).add(RegistryManager.ASHEN_GOGGLES.get());

		tag(ASPECTUS).addTags(IRON_ASPECTUS, COPPER_ASPECTUS, LEAD_ASPECTUS, SILVER_ASPECTUS, DAWNSTONE_ASPECTUS, MITHRIL_ASPECTUS);
		tag(IRON_ASPECTUS).add(RegistryManager.IRON_ASPECTUS.get());
		tag(COPPER_ASPECTUS).add(RegistryManager.COPPER_ASPECTUS.get());
		tag(LEAD_ASPECTUS).add(RegistryManager.LEAD_ASPECTUS.get());
		tag(SILVER_ASPECTUS).add(RegistryManager.SILVER_ASPECTUS.get());
		tag(DAWNSTONE_ASPECTUS).add(RegistryManager.DAWNSTONE_ASPECTUS.get());
		tag(MITHRIL_ASPECTUS).add(RegistryManager.MITHRIL_ASPECTUS.get());

		tag(ASHEN_STONE).add(RegistryManager.ASHEN_STONE_ITEM.get(), RegistryManager.ASHEN_BRICK_ITEM.get(), RegistryManager.ASHEN_TILE_ITEM.get());

		toolTags(RegistryManager.LEAD_TOOLS);
		toolTags(RegistryManager.SILVER_TOOLS);
		toolTags(RegistryManager.DAWNSTONE_TOOLS);
		tag(ItemTags.SWORDS).add(RegistryManager.TYRFING.get());

		tag(Tags.Items.ORES).addTags(LEAD_ORE, SILVER_ORE);
		tag(LEAD_ORE).add(RegistryManager.LEAD_ORE_ITEM.get()).add(RegistryManager.DEEPSLATE_LEAD_ORE_ITEM.get());
		tag(SILVER_ORE).add(RegistryManager.SILVER_ORE_ITEM.get()).add(RegistryManager.DEEPSLATE_SILVER_ORE_ITEM.get());

		tag(Tags.Items.ORES_IN_GROUND_STONE).add(RegistryManager.LEAD_ORE_ITEM.get());
		tag(Tags.Items.ORES_IN_GROUND_DEEPSLATE).add(RegistryManager.DEEPSLATE_LEAD_ORE_ITEM.get());
		tag(Tags.Items.ORES_IN_GROUND_STONE).add(RegistryManager.SILVER_ORE_ITEM.get());
		tag(Tags.Items.ORES_IN_GROUND_DEEPSLATE).add(RegistryManager.DEEPSLATE_SILVER_ORE_ITEM.get());

		tag(Tags.Items.STORAGE_BLOCKS).addTags(RAW_LEAD_BLOCK, RAW_SILVER_BLOCK);
		tag(RAW_LEAD_BLOCK).add(RegistryManager.RAW_LEAD_BLOCK_ITEM.get());
		tag(RAW_SILVER_BLOCK).add(RegistryManager.RAW_SILVER_BLOCK_ITEM.get());

		tag(Tags.Items.STORAGE_BLOCKS).addTags(LEAD_BLOCK, SILVER_BLOCK, DAWNSTONE_BLOCK, MITHRIL_BLOCK);
		tag(LEAD_BLOCK).add(RegistryManager.LEAD_BLOCK_ITEM.get());
		tag(SILVER_BLOCK).add(RegistryManager.SILVER_BLOCK_ITEM.get());
		tag(DAWNSTONE_BLOCK).add(RegistryManager.DAWNSTONE_BLOCK_ITEM.get());
		tag(MITHRIL_BLOCK).add(RegistryManager.MITHRIL_BLOCK_ITEM.get());

		tag(Tags.Items.RAW_MATERIALS).addTags(RAW_LEAD, RAW_SILVER);
		tag(RAW_LEAD).add(RegistryManager.RAW_LEAD.get());
		tag(RAW_SILVER).add(RegistryManager.RAW_SILVER.get());

		tag(Tags.Items.INGOTS).addTags(LEAD_INGOT, SILVER_INGOT, DAWNSTONE_INGOT, MITHRIL_INGOT);
		tag(LEAD_INGOT).add(RegistryManager.LEAD_INGOT.get());
		tag(SILVER_INGOT).add(RegistryManager.SILVER_INGOT.get());
		tag(DAWNSTONE_INGOT).add(RegistryManager.DAWNSTONE_INGOT.get());
		tag(MITHRIL_INGOT).add(RegistryManager.MITHRIL_INGOT.get());

		tag(Tags.Items.NUGGETS).addTags(COPPER_NUGGET, LEAD_NUGGET, SILVER_NUGGET, DAWNSTONE_NUGGET, MITHRIL_NUGGET);
		tag(COPPER_NUGGET).add(RegistryManager.COPPER_NUGGET.get());
		tag(LEAD_NUGGET).add(RegistryManager.LEAD_NUGGET.get());
		tag(SILVER_NUGGET).add(RegistryManager.SILVER_NUGGET.get());
		tag(DAWNSTONE_NUGGET).add(RegistryManager.DAWNSTONE_NUGGET.get());
		tag(MITHRIL_NUGGET).add(RegistryManager.MITHRIL_NUGGET.get());

		tag(PLATES).addTags(IRON_PLATE, COPPER_PLATE, LEAD_PLATE, SILVER_PLATE, DAWNSTONE_PLATE, MITHRIL_PLATE);
		tag(IRON_PLATE).add(RegistryManager.IRON_PLATE.get());
		//tag(GOLD_PLATE).add(RegistryManager.GOLD_PLATE.get());
		tag(COPPER_PLATE).add(RegistryManager.COPPER_PLATE.get());
		tag(LEAD_PLATE).add(RegistryManager.LEAD_PLATE.get());
		tag(SILVER_PLATE).add(RegistryManager.SILVER_PLATE.get());
		tag(DAWNSTONE_PLATE).add(RegistryManager.DAWNSTONE_PLATE.get());
		tag(MITHRIL_PLATE).add(RegistryManager.MITHRIL_PLATE.get());

		tag(Tags.Items.INGOTS).addTags(CAMINITE_BRICK);
		tag(CAMINITE_BRICK).add(RegistryManager.CAMINITE_BRICK.get());
		tag(Tags.Items.INGOTS).addTags(ARCHAIC_BRICK);
		tag(ARCHAIC_BRICK).add(RegistryManager.ARCHAIC_BRICK.get());

		tag(Tags.Items.DUSTS).addTag(ASH_DUST);
		tag(ASH_DUST).add(RegistryManager.ASH.get());

		tag(ItemTags.PICKAXES).add(RegistryManager.CLOCKWORK_PICKAXE.get());
		tag(ItemTags.AXES).add(RegistryManager.CLOCKWORK_AXE.get());
		tag(Tags.Items.TOOLS).addTag(TOOLS_HAMMERS);
		tag(TOOLS_HAMMERS).add(RegistryManager.TINKER_HAMMER.get(), RegistryManager.GRANDHAMMER.get());

		tag(ItemTags.HEAD_ARMOR).add(RegistryManager.ASHEN_GOGGLES.get());
		tag(ItemTags.CHEST_ARMOR).add(RegistryManager.ASHEN_CLOAK.get());
		tag(ItemTags.LEG_ARMOR).add(RegistryManager.ASHEN_LEGGINGS.get());
		tag(ItemTags.FOOT_ARMOR).add(RegistryManager.ASHEN_BOOTS.get());

		tag(Tags.Items.SLIME_BALLS).add(RegistryManager.ADHESIVE.get());

		tag(Tags.Items.MUSIC_DISCS).add(RegistryManager.MUSIC_DISC_7F_PATTERNS.get());

		tag(ANCIENT_CODEX).add(RegistryManager.ANCIENT_CODEX.get());
		tag(STAMPS).add(RegistryManager.FLAT_STAMP.get(), RegistryManager.INGOT_STAMP.get(), RegistryManager.NUGGET_STAMP.get(), RegistryManager.PLATE_STAMP.get(), RegistryManager.GEAR_STAMP.get());
		tag(DIALS).add(RegistryManager.EMBER_DIAL_ITEM.get(), RegistryManager.ITEM_DIAL_ITEM.get(), RegistryManager.FLUID_DIAL_ITEM.get());

		//tags shared by blocks
		copy(EmbersBlockTags.WORLD_BOTTOM, WORLD_BOTTOM);
		copy(EmbersBlockTags.SNOW, SNOW);

		copy(EmbersBlockTags.PRISTINE_COPPER, PRISTINE_COPPER);
		copy(EmbersBlockTags.EXPOSED_COPPER, EXPOSED_COPPER);
		copy(EmbersBlockTags.WEATHERED_COPPER, WEATHERED_COPPER);
		copy(EmbersBlockTags.OXIDIZED_COPPER, OXIDIZED_COPPER);

		copy(EmbersBlockTags.CRYSTAL_SEEDS, CRYSTAL_SEEDS);
		copy(EmbersBlockTags.COPPER_SEED, COPPER_SEED);
		copy(EmbersBlockTags.IRON_SEED, IRON_SEED);
		copy(EmbersBlockTags.GOLD_SEED, GOLD_SEED);
		copy(EmbersBlockTags.LEAD_SEED, LEAD_SEED);
		copy(EmbersBlockTags.SILVER_SEED, SILVER_SEED);
		copy(EmbersBlockTags.NICKEL_SEED, NICKEL_SEED);
		copy(EmbersBlockTags.TIN_SEED, TIN_SEED);
		copy(EmbersBlockTags.ALUMINUM_SEED, ALUMINUM_SEED);
		copy(EmbersBlockTags.ZINC_SEED, ZINC_SEED);
		copy(EmbersBlockTags.PLATINUM_SEED, PLATINUM_SEED);
		copy(EmbersBlockTags.URANIUM_SEED, URANIUM_SEED);
		copy(EmbersBlockTags.DAWNSTONE_SEED, DAWNSTONE_SEED);
		copy(EmbersBlockTags.MITHRIL_SEED, MITHRIL_SEED);

		copy(EmbersBlockTags.ANVIL_METAL, ANVIL_METAL);

		//compat stuff
		tag(AUGMENTABLE_TOOLS)
		.addOptional(ResourceLocation.fromNamespaceAndPath("weaponmaster", "wm_broadsword"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("weaponmaster", "wm_rapier"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("weaponmaster", "wm_broadswordlarge"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("weaponmaster", "wm_rapierlarge"));

		tag(AUGMENTABLE_TOOLS)
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_sword"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_katana"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_naginata"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_greatsword"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_dagger"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_spear"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_throwing_knife"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_rapier"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_longsword"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_trident"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_scythe"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_sickle"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_shovel"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_pickaxe"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_axe"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_hoe"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_mattock"));
		tag(AUGMENTABLE_HELMETS).addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_helmet"));
		tag(AUGMENTABLE_CHESTPLATES).addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_chestplate")).addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_elytra"));
		tag(AUGMENTABLE_LEGGINGS).addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_leggings"));
		tag(AUGMENTABLE_BOOTS).addOptional(ResourceLocation.fromNamespaceAndPath("miapi", "modular_boots"));

		tag(AUGMENTABLE_TOOLS)
		.addOptional(ResourceLocation.fromNamespaceAndPath("tetra", "modular_single"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("tetra", "modular_double"))
		.addOptional(ResourceLocation.fromNamespaceAndPath("tetra", "modular_sword"));

		tag(ANY_CURIO).addOptional(CuriosCompat.EMBER_BULB.getId());
		tag(RING_CURIO).addOptional(CuriosCompat.EMBER_RING.getId());
		tag(BELT_CURIO).addOptional(CuriosCompat.EMBER_BELT.getId());
		tag(AMULET_CURIO).addOptional(CuriosCompat.EMBER_AMULET.getId()).addOptional(CuriosCompat.ASHEN_AMULET.getId()).addOptional(CuriosCompat.NONBELEIVER_AMULET.getId());
		tag(BODY_CURIO).addOptional(CuriosCompat.DAWNSTONE_MAIL.getId());
		tag(CHARM_CURIO).addOptional(CuriosCompat.EXPLOSION_CHARM.getId());
	}

	public void toolTags(ToolSet set) {
		tag(ItemTags.SWORDS).add(set.SWORD.get());
		tag(ItemTags.SHOVELS).add(set.SHOVEL.get());
		tag(ItemTags.PICKAXES).add(set.PICKAXE.get());
		tag(ItemTags.AXES).add(set.AXE.get());
		tag(ItemTags.HOES).add(set.HOE.get());
		tag(Tags.Items.TOOLS).add(set.SWORD.get(), set.SHOVEL.get(), set.PICKAXE.get(), set.AXE.get(), set.HOE.get());
	}
}
