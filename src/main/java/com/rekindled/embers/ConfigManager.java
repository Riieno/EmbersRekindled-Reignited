package com.rekindled.embers;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public class ConfigManager {
	public enum OreGenerationMode {
		NEVER,
		ALWAYS,
		AUTO
	}

	public static ConfigValue<OreGenerationMode> ORE_GENERATION;
	public static ConfigValue<Integer> MAX_PROXY_DISTANCE;
	public static ConfigValue<Double> EMBER_BORE_SPEED_MOD;
	public static ConfigValue<Integer> EMBER_BORE_TIME;
	public static ConfigValue<Double> EMBER_BORE_FUEL_CONSUMPTION;
	public static ConfigValue<Integer> RESERVOIR_CAPACITY;
	public static ConfigValue<Integer> MINI_BOILER_CAPACITY;
	public static ConfigValue<Double> MINI_BOILER_HEAT_MULTIPLIER;
	public static ConfigValue<Boolean> MINI_BOILER_CAN_EXPLODE;
	public static ConfigValue<Integer> INJECTOR_MAX_DISTANCE;
	public static ConfigValue<Boolean> CRYSTAL_SEED_USE_EXPERIMENTAL_SCALING;
	public static ConfigValue<Double> CRYSTAL_SEED_LEVEL_BONUS_SCALAR;
	public static ConfigValue<Integer> MELTER_PROCESS_TIME;
	public static ConfigValue<Double> MELTER_EMBER_COST;
	public static ConfigValue<Integer> MELTER_CAPACITY;
	public static ConfigValue<Integer> GEO_SEPARATOR_CAPACITY;
	public static ConfigValue<Integer> STAMP_BASE_CAPACITY;
	public static ConfigValue<Double> CHARGER_MAX_TRANSFER;
	public static ConfigValue<Integer> FLUID_VESSEL_CAPACITY;
	public static ConfigValue<Double> HEARTH_COIL_EMBER_COST;
	public static ConfigValue<Double> HEARTH_COIL_HEATING_SPEED;
	public static ConfigValue<Double> HEARTH_COIL_COOLING_SPEED;
	public static ConfigValue<Double> HEARTH_COIL_MAX_HEAT;
	public static ConfigValue<Integer> HEARTH_COIL_MIN_COOK_TIME;
	public static ConfigValue<Integer> HEARTH_COIL_MAX_COOK_TIME;
	public static ConfigValue<Double> CREATE_BLAZE_BURNER_EMBER_COST;
	public static ConfigValue<Double> CREATE_BLAZE_BURNER_SUPERHEAT_COST;
	public static ConfigValue<Boolean> ENABLE_EXPERIMENTAL_CREATE_MECHANICS;
	public static ConfigValue<Integer> DAWNSTONE_ANVIL_MAX_HITS;
	public static ConfigValue<Double> BLAZING_RAY_COST;
	public static ConfigValue<Integer> BLAZING_RAY_COOLDOWN;
	public static ConfigValue<Integer> BLAZING_RAY_MAX_CHARGE;
	public static ConfigValue<Double> BLAZING_RAY_DAMAGE;
	public static ConfigValue<Double> BLAZING_RAY_MAX_SPREAD;
	public static ConfigValue<Double> BLAZING_RAY_MAX_DISTANCE;
	public static ConfigValue<Double> CINDER_STAFF_COST;
	public static ConfigValue<Integer> CINDER_STAFF_COOLDOWN;
	public static ConfigValue<Integer> CINDER_STAFF_MAX_CHARGE;
	public static ConfigValue<Double> CINDER_STAFF_DAMAGE;
	public static ConfigValue<Double> CINDER_STAFF_SIZE;
	public static ConfigValue<Double> CINDER_STAFF_AOE_SIZE;
	public static ConfigValue<Integer> CINDER_STAFF_LIFETIME;
	public static ConfigValue<Integer> ASHEN_GOGGLES_SLOTS;
	public static ConfigValue<Integer> ASHEN_CLOAK_SLOTS;
	public static ConfigValue<Integer> ASHEN_LEGGINGS_SLOTS;
	public static ConfigValue<Integer> ASHEN_BOOTS_SLOTS;
	public static ConfigValue<List<? extends String>> SCALE_DAMAGE_PASSES;
	public static ConfigValue<List<? extends String>> SCALE_DAMAGE_RATES;

	public static ConfigValue<Boolean> CODEX_PROGRESSION;
	public static ConfigValue<Boolean> PVP_EVERYBODY_IS_ENEMY;
	public static ConfigValue<Boolean> REDSTONE_CONTROL_INVERTED;
	public static ConfigValue<List<? extends String>> TAG_PREFERENCES;
	public static ConfigValue<List<? extends String>> ITEM_PREFERENCES;

	public static ConfigValue<Boolean> RENDER_FALLBACK;
	public static ConfigValue<Integer> NUGGET_FLUID_VALUE;
	public static ConfigValue<Integer> TICKS_TO_OPEN_CODEX;
	public static ConfigValue<Boolean> CODEX_REQUIRED_FOR_LOOKUP;

	public static double getScaleDamagePass(String type) {
		for (String pass : SCALE_DAMAGE_PASSES.get()) {
			String [] values =  pass.split(":");
			if (type.equals(values[0]))
				return Double.parseDouble(values[1]);
		}
		return 0.0;
	}

	public static double getScaleDamageRate(String type) {
		for (String rate : SCALE_DAMAGE_RATES.get()) {
			String [] values =  rate.split(":");
			if (type.equals(values[0]))
				return Double.parseDouble(values[1]);
		}
		return 1.0;
	}

	public static boolean isRedstoneControlActive(Level level, BlockPos pos) {
		boolean powered = level.hasNeighborSignal(pos);
		return REDSTONE_CONTROL_INVERTED != null && REDSTONE_CONTROL_INVERTED.get() ? !powered : powered;
	}

	public static void register(ModContainer container) {
		registerClientConfigs(container);
		registerCommonConfigs(container);
		//registerServerConfigs();
	}

	public static void registerClientConfigs(ModContainer container) {
		ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

		RENDER_FALLBACK = CLIENT.comment("Use a fallback rendertype for certain objects that normally use custom core shaders. Improves compatibility with shader mods but might not look as good.").define("render_fallback", false);

		NUGGET_FLUID_VALUE = CLIENT.comment("The amount of millibuckets one nugget is worth when molten, only affects fluid tooltips.").define("nugget_fluid_value", 10);

		TICKS_TO_OPEN_CODEX = CLIENT.comment("The amount of ticks it takes to open the codex by holding ctrl while hovering over an item.").define("ticks_to_open_codex", 10);
		CODEX_REQUIRED_FOR_LOOKUP = CLIENT.comment("If the codex is required in the hotbar to open a codex entry from the inventory.").define("codex_required_for_lookup", true);

		container.registerConfig(ModConfig.Type.CLIENT, CLIENT.build());
	}

	public static void registerCommonConfigs(ModContainer container) {
		ModConfigSpec.Builder COMMON = new ModConfigSpec.Builder();

		COMMON.comment("World generation settings")
				.translation(Embers.MODID + ".configuration.worldgen")
				.push("worldgen");

		ORE_GENERATION = COMMON.comment(
				"Controls Embers lead and silver ore generation.",
				"NEVER disables both ores. ALWAYS generates both ores.",
				"AUTO disables each Embers ore when another mod adds a matching ore feature to an Overworld biome.")
				.translation(Embers.MODID + ".configuration.worldgen.generate_ores")
				.defineEnum("generate_ores", OreGenerationMode.AUTO);

		COMMON.pop();

		COMMON.comment("Settings for machine/item/misc parameters").push("parameters");

		MAX_PROXY_DISTANCE = COMMON.comment("The maximum distance that mechanical cores can proxy capabilities and upgrades.").define("mechanical_core.max_distance", 3);

		EMBER_BORE_SPEED_MOD = COMMON.comment("The speed modifier of the Ember Bore before upgrades.").define("emberBore.speedMod", 1.0);
		EMBER_BORE_TIME = COMMON.comment("The time in ticks it takes to try one dig attempt.").define("emberBore.processTime", 200);
		EMBER_BORE_FUEL_CONSUMPTION = COMMON.comment("The amount of fuel consumed each tick.").define("emberBore.fuelCost", 3.0);

		RESERVOIR_CAPACITY = COMMON.comment("How much fluid (in mb) fits into each Caminite Ring on a Reservoir.").define("reservoir.capacity", FluidType.BUCKET_VOLUME * 40);

		MINI_BOILER_CAPACITY = COMMON.comment("How much fluid and gas (in mb) fits into a Mini Boiler.").define("mini_boiler.capacity", FluidType.BUCKET_VOLUME * 16);
		MINI_BOILER_HEAT_MULTIPLIER = COMMON.comment("How much fluid (in mb) a Mini Boiler boils for each ember used/generated.").define("mini_boiler.heat_multiplier", 1.0);
		MINI_BOILER_CAN_EXPLODE = COMMON.comment("Whether the Mini Boiler can explode when overfilled with gas").define("mini_boiler.can_explode", true);

		MELTER_PROCESS_TIME = COMMON.comment("The time in ticks it takes to process one recipe.").define("melter.processTime", 200);
		MELTER_EMBER_COST = COMMON.comment("The ember cost per tick.").define("melter.cost", 1.0);
		MELTER_CAPACITY = COMMON.comment("How much fluid (in mb) fits into a Melter.").define("melter.capacity", FluidType.BUCKET_VOLUME * 4);

		GEO_SEPARATOR_CAPACITY = COMMON.comment("How much fluid (in mb) fits into a Geologic Seperator.").define("geoSeparator.capacity", FluidType.BUCKET_VOLUME);

		STAMP_BASE_CAPACITY = COMMON.comment("How much fluid (in mb) fits into the Stamp Base.").define("stamper.capacity", (FluidType.BUCKET_VOLUME * 3) /2);

		CHARGER_MAX_TRANSFER = COMMON.comment("How much ember is transferred between item and charger per tick.").define("charger.transfer", 10.0);

		FLUID_VESSEL_CAPACITY = COMMON.comment("How much fluid (in mb) fits into the Fluid Vessel.").define("fluidVessel.capacity", FluidType.BUCKET_VOLUME * 16);

		INJECTOR_MAX_DISTANCE = COMMON.comment("The maximum distance that Ember Injectors can be placed from a crystal seed.").define("ember_injector.max_distance", 1);
		CRYSTAL_SEED_USE_EXPERIMENTAL_SCALING = COMMON.comment("If true, crystal seeds use square-root level scaling for extra nugget drop roll segments instead of the original stepped scaling.").define("crystal_seed.use_experimental_scaling", true);
		CRYSTAL_SEED_LEVEL_BONUS_SCALAR = COMMON.comment("Multiplier used by experimental crystal seed scaling. Extra roll segments are floor(sqrt(level * scalar)).").defineInRange("crystal_seed.level_bonus_scalar", 2.0, 0.0, 1024.0);

		HEARTH_COIL_EMBER_COST = COMMON.comment("The amount of ember consumed per tick.").define("hearth_coil.ember_cost", 1.0);
		HEARTH_COIL_HEATING_SPEED = COMMON.comment("The amount of heat gained per tick when consuming ember.").define("hearth_coil.heating_speed", 1.0);
		HEARTH_COIL_COOLING_SPEED = COMMON.comment("The amount of heat lost per tick when not consuming ember.").define("hearth_coil.cooling_speed", 1.0);
		HEARTH_COIL_MAX_HEAT = COMMON.comment("The maximum heat value the hearth coil can reach without upgrades.").define("hearth_coil.max_heat", 280.0);
		HEARTH_COIL_MIN_COOK_TIME = COMMON.comment("The time in ticks it takes to cook 1 item at the highest heat.").define("hearth_coil.min_cook_time", 20);
		HEARTH_COIL_MAX_COOK_TIME = COMMON.comment("The time in ticks it takes to cook 1 item at the lowest heat.").define("hearth_coil.max_cook_time", 300);
		CREATE_BLAZE_BURNER_EMBER_COST = COMMON.comment("The amount of ember a Create blaze burner consumes each tick while sustained by ember.").define("create.blaze_burner.ember_cost", 1.0);
		CREATE_BLAZE_BURNER_SUPERHEAT_COST = COMMON.comment("The amount of ember a Create blaze burner consumes each tick while an Atmospheric Bellows or bellowed Hearth Coil is forcing it to superheat.").define("create.blaze_burner.superheat_cost", 8.0);
		ENABLE_EXPERIMENTAL_CREATE_MECHANICS = COMMON.comment("Enables experimental Create-powered Embers machine upgrades and their recipes. Requires Create to be installed.").define("create.enable_experimental_create_mechanics", true);

		DAWNSTONE_ANVIL_MAX_HITS = COMMON.comment("The amount of hits required to perform one recipe on a dawnstone anvil.").define("dawnstone_anvil.max_hits", 40);

		BLAZING_RAY_COST = COMMON.comment("Ember used up by each shot.").define("blazingRay.cost", 25.0);
		BLAZING_RAY_COOLDOWN = COMMON.comment("Cooldown in ticks between each shot.").define("blazingRay.cooldown", 10);
		BLAZING_RAY_MAX_CHARGE = COMMON.comment("Time in ticks to fully charge.").define("blazingRay.charge", 20);
		BLAZING_RAY_DAMAGE = COMMON.comment("Damage dealt by one shot.").define("blazingRay.damage", 7.0);
		BLAZING_RAY_MAX_SPREAD = COMMON.comment("Maximum spread.").define("blazingRay.spread", 30.0);
		BLAZING_RAY_MAX_DISTANCE = COMMON.comment("Maximum shot distance.").define("blazingRay.distance", 96.0);

		CINDER_STAFF_COST = COMMON.comment("Ember used up by each shot.").define("cinderStaff.cost", 25.0);
		CINDER_STAFF_COOLDOWN = COMMON.comment("Cooldown in ticks between each shot.").define("cinderStaff.cooldown", 10);
		CINDER_STAFF_MAX_CHARGE = COMMON.comment("Time in ticks to fully charge.").define("cinderStaff.charge", 60);
		CINDER_STAFF_DAMAGE = COMMON.comment("Damage dealt by one shot.").define("cinderStaff.damage", 17.0);
		CINDER_STAFF_SIZE = COMMON.comment("Size of the projectile.").define("cinderStaff.size", 17.0);
		CINDER_STAFF_AOE_SIZE = COMMON.comment("Area of Effect on impact.").define("cinderStaff.aoe", 17 * 0.125);
		CINDER_STAFF_LIFETIME = COMMON.comment("Maximum lifetime in ticks of projectile.").define("cinderStaff.lifetime", 160);

		ASHEN_GOGGLES_SLOTS = COMMON.comment("How many inflictor gems can fit in the ashen goggles.").define("ashen.goggles.gem_slots", 2);
		ASHEN_CLOAK_SLOTS = COMMON.comment("How many inflictor gems can fit in the ashen cloak.").define("ashen.cloak.gem_slots", 7);
		ASHEN_LEGGINGS_SLOTS = COMMON.comment("How many inflictor gems can fit in the ashen leggings.").define("ashen.leggings.gem_slots", 5);
		ASHEN_BOOTS_SLOTS = COMMON.comment("How many inflictor gems can fit in the ashen boots.").define("ashen.boots.gem_slots", 3);

		List<String> defaultScaleDamagePasses = new ArrayList<String>();
		defaultScaleDamagePasses.add("drown:1.0");
		defaultScaleDamagePasses.add("starve:1.0");

		List<String> defaultScaleDamageRates = new ArrayList<String>();

		SCALE_DAMAGE_PASSES = COMMON.comment("Syntax is 'damagetype:rate'. Determines which damage types are partially unaffected by the shifting scales augment.").defineList("shiftingScales.damagePasses", defaultScaleDamagePasses, () -> "", a -> true);
		SCALE_DAMAGE_RATES = COMMON.comment("Syntax is 'damagetype:rate'. Specifies a separate damage rate for depleting the scales.").defineList("shiftingScales.damageRates", defaultScaleDamageRates, () -> "", a -> true);

		COMMON.pop();


		COMMON.comment("Miscellaneous settings").push("misc");

		CODEX_PROGRESSION = COMMON.comment("Codex entries need to be completed before the next one unlocks.").define("codexProgression", true);

		PVP_EVERYBODY_IS_ENEMY = COMMON.comment("If true, Embers homing projectiles will go for neutral players.").define("everybodyIsEnemy", false);

		REDSTONE_CONTROL_INVERTED = COMMON.comment("If true, redstone-controlled Embers blocks run without a signal and turn off while powered. If false, they require a redstone signal like classic Embers.").define("redstone.inverted_controls", false);

		List<String> preferences = new ArrayList<String>();
		preferences.add(0, "minecraft");
		preferences.add(1, Embers.MODID);
		TAG_PREFERENCES = COMMON.comment("Which domains are preferred for recipes with dynamic outputs.").defineList("tagPreferences", preferences, () -> "", a -> true);

		List<String> itemPreferences = new ArrayList<String>();
		itemPreferences.add(0, "minecraft:oak_planks");
		itemPreferences.add(1, "minecraft:cobblestone");
		ITEM_PREFERENCES = COMMON.comment("Which items are preferred as the result of breaking down a tool on an anvil.").defineList("itemPreferences", itemPreferences, () -> "", a -> true);

		COMMON.pop();
		container.registerConfig(ModConfig.Type.COMMON, COMMON.build());
	}

	public static void registerServerConfigs(ModContainer container) {
		ModConfigSpec.Builder SERVER = new ModConfigSpec.Builder();

		container.registerConfig(ModConfig.Type.SERVER, SERVER.build());
	}
}
