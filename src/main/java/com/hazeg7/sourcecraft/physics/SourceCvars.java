package com.hazeg7.sourcecraft.physics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Complete set of Counter-Strike: Source movement console variables (CVars).
 * Pure Java data & validation model shared between client, server, networking, and tests.
 */
public final class SourceCvars {
	public record CvarMeta(String name, double min, double max, double defaultVal, boolean isBoolean, String summary) {
	}

	public double svGravity = 800.0;
	public double svAccelerate = 5.5;
	public double svAirAccelerate = 100.0;
	public double svFriction = 4.0;
	public double svStopspeed = 75.0;
	public double svMaxSpeed = 250.0;
	public double svMaxVelocity = 3500.0;
	public double svAirMaxWishspeed = 30.0;
	public double svJumpImpulse = 302.0;

	public boolean svAutoBunnyhopping = false;
	public boolean svEnableBunnyhopping = true;
	public double svStaminaMax = 0.0;
	public double svStaminaJumpCost = 0.08;
	public double svStaminaLandCost = 0.05;
	public double svStaminaRecoveryRate = 60.0;

	public double svWalkRatio = 0.52;
	public double svDuckRatio = 0.34;
	public double svWaterAccelerate = 10.0;
	public double svWaterFriction = 1.0;
	public double svLadderScaleSpeed = 0.78;

	public boolean svSurfEnabled = true;
	public double svSurfOverbounce = 1.0;
	public boolean svWeaponWeight = false;
	public boolean svCrouchJump = true;
	public boolean svEdgebugEnabled = true;
	public boolean svSurvivalFallDamage = false;

	public double svTickrate = 100.0;
	public double svHuPerBlock = 40.0;
	public int svJumpBufferTicks = 2;

	private static final Map<String, CvarMeta> METADATA = new LinkedHashMap<>();

	static {
		reg("sv_gravity", 50.0, 2400.0, 800.0, false, "World gravity in HU/s^2 (CS:S default 800)");
		reg("sv_accelerate", 1.0, 25.0, 5.5, false, "Ground acceleration multiplier");
		reg("sv_airaccelerate", 1.0, 2000.0, 100.0, false, "Air-strafe turn acceleration (10 comp, 100 bhop, 150 surf)");
		reg("sv_friction", 0.0, 20.0, 4.0, false, "Ground friction coefficient (CS:S default 4)");
		reg("sv_stopspeed", 10.0, 300.0, 75.0, false, "Minimum speed used for ground friction deceleration (CS:S 75)");
		reg("sv_maxspeed", 80.0, 600.0, 250.0, false, "Base running cap in HU/s (250 knife speed)");
		reg("sv_maxvelocity", 500.0, 10000.0, 3500.0, false, "Global per-axis velocity clamp in HU/s");
		reg("sv_air_max_wishspeed", 5.0, 150.0, 30.0, false, "Air wishspeed clamp driving Source air-strafe gain (30)");
		reg("sv_jump_impulse", 120.0, 600.0, 302.0, false, "Vertical jump takeoff velocity in HU/s (~302 = 57 HU jump)");
		reg("sv_autobunnyhopping", 0.0, 1.0, 0.0, true, "Hold Space to auto-jump on ground contact");
		reg("sv_enablebunnyhopping", 0.0, 1.0, 1.0, true, "Uncapped bhop speed (0 enforces Valve 1.1x soft cap)");
		reg("sv_staminamax", 0.0, 100.0, 0.0, false, "Maximum CS:S stamina pool (80 comp, 0 bhop)");
		reg("sv_staminajumpcost", 0.0, 0.5, 0.08, false, "Stamina fraction added on each jump");
		reg("sv_staminalandcost", 0.0, 0.5, 0.05, false, "Stamina fraction added on landing");
		reg("sv_staminarecoveryrate", 5.0, 200.0, 60.0, false, "Stamina recovery rate in units/sec");
		reg("sv_walk_ratio", 0.2, 1.0, 0.52, false, "Slow-walk max speed ratio (0.52 = 130 u/s)");
		reg("sv_duck_ratio", 0.2, 1.0, 0.34, false, "Crouch max speed ratio (0.34 = 85 u/s)");
		reg("sv_wateraccelerate", 1.0, 30.0, 10.0, false, "Swimming acceleration multiplier");
		reg("sv_waterfriction", 0.0, 10.0, 1.0, false, "Water drag coefficient");
		reg("sv_ladder_scale_speed", 0.3, 1.5, 0.78, false, "Ladder climb speed ratio (~0.78 CS:S; sqrt(2) fast ladder)");
		reg("sv_surf_enabled", 0.0, 1.0, 1.0, true, "Enable ClipVelocity surfing on ramps, stairs, slabs, and snow");
		reg("sv_surf_overbounce", 1.0, 1.25, 1.0, false, "ClipVelocity plane bounce multiplier (1.0 standard surf)");
		reg("sv_weapon_weight", 0.0, 1.0, 0.0, true, "Scale max speed by held item weight (CS:S weapon mobility)");
		reg("sv_crouch_jump", 0.0, 1.0, 1.0, true, "Enable CS:S air crouch-jump foot-pull & 54 HU crouch hull");
		reg("sv_edgebug_enabled", 0.0, 1.0, 1.0, true, "Cancel fall damage when skimming a solid block edge (edgebug)");
		reg("sv_survival_fall_damage", 0.0, 1.0, 0.0, true, "Apply standard fall damage on hard drops (edgebugs still save)");
		reg("sv_tickrate", 20.0, 128.0, 100.0, false, "Internal physics sub-step frequency in Hz (66 or 100 recommended)");
		reg("sv_hu_per_block", 32.0, 48.0, 40.0, false, "Hammer Units per Minecraft block (40 aligns 72 HU = 1.8m)");
		reg("sv_jump_buffer_ticks", 0.0, 6.0, 2.0, false, "Pre-landing scroll/jump input buffer window in MC ticks");
	}

	private static void reg(String name, double min, double max, double def, boolean isBool, String summary) {
		METADATA.put(name, new CvarMeta(name, min, max, def, isBool, summary));
	}

	public static SourceCvars createDefault() {
		return new SourceCvars();
	}

	public SourceCvars copy() {
		SourceCvars c = new SourceCvars();
		c.copyFrom(this);
		return c;
	}

	public void copyFrom(SourceCvars o) {
		this.svGravity = o.svGravity;
		this.svAccelerate = o.svAccelerate;
		this.svAirAccelerate = o.svAirAccelerate;
		this.svFriction = o.svFriction;
		this.svStopspeed = o.svStopspeed;
		this.svMaxSpeed = o.svMaxSpeed;
		this.svMaxVelocity = o.svMaxVelocity;
		this.svAirMaxWishspeed = o.svAirMaxWishspeed;
		this.svJumpImpulse = o.svJumpImpulse;
		this.svAutoBunnyhopping = o.svAutoBunnyhopping;
		this.svEnableBunnyhopping = o.svEnableBunnyhopping;
		this.svStaminaMax = o.svStaminaMax;
		this.svStaminaJumpCost = o.svStaminaJumpCost;
		this.svStaminaLandCost = o.svStaminaLandCost;
		this.svStaminaRecoveryRate = o.svStaminaRecoveryRate;
		this.svWalkRatio = o.svWalkRatio;
		this.svDuckRatio = o.svDuckRatio;
		this.svWaterAccelerate = o.svWaterAccelerate;
		this.svWaterFriction = o.svWaterFriction;
		this.svLadderScaleSpeed = o.svLadderScaleSpeed;
		this.svSurfEnabled = o.svSurfEnabled;
		this.svSurfOverbounce = o.svSurfOverbounce;
		this.svWeaponWeight = o.svWeaponWeight;
		this.svCrouchJump = o.svCrouchJump;
		this.svEdgebugEnabled = o.svEdgebugEnabled;
		this.svSurvivalFallDamage = o.svSurvivalFallDamage;
		this.svTickrate = o.svTickrate;
		this.svHuPerBlock = o.svHuPerBlock;
		this.svJumpBufferTicks = o.svJumpBufferTicks;
		this.sanitize();
	}

	public void sanitize() {
		svGravity = clamp("sv_gravity", svGravity);
		svAccelerate = clamp("sv_accelerate", svAccelerate);
		svAirAccelerate = clamp("sv_airaccelerate", svAirAccelerate);
		svFriction = clamp("sv_friction", svFriction);
		svStopspeed = clamp("sv_stopspeed", svStopspeed);
		svMaxSpeed = clamp("sv_maxspeed", svMaxSpeed);
		svMaxVelocity = clamp("sv_maxvelocity", svMaxVelocity);
		svAirMaxWishspeed = clamp("sv_air_max_wishspeed", svAirMaxWishspeed);
		svJumpImpulse = clamp("sv_jump_impulse", svJumpImpulse);
		svStaminaMax = clamp("sv_staminamax", svStaminaMax);
		svStaminaJumpCost = clamp("sv_staminajumpcost", svStaminaJumpCost);
		svStaminaLandCost = clamp("sv_staminalandcost", svStaminaLandCost);
		svStaminaRecoveryRate = clamp("sv_staminarecoveryrate", svStaminaRecoveryRate);
		svWalkRatio = clamp("sv_walk_ratio", svWalkRatio);
		svDuckRatio = clamp("sv_duck_ratio", svDuckRatio);
		svWaterAccelerate = clamp("sv_wateraccelerate", svWaterAccelerate);
		svWaterFriction = clamp("sv_waterfriction", svWaterFriction);
		svLadderScaleSpeed = clamp("sv_ladder_scale_speed", svLadderScaleSpeed);
		svSurfOverbounce = clamp("sv_surf_overbounce", svSurfOverbounce);
		svTickrate = clamp("sv_tickrate", svTickrate);
		svHuPerBlock = clamp("sv_hu_per_block", svHuPerBlock);
		svJumpBufferTicks = (int) Math.round(clamp("sv_jump_buffer_ticks", svJumpBufferTicks));
	}

	private static double clamp(String key, double val) {
		CvarMeta m = METADATA.get(key);
		if (m == null) {
			return val;
		}
		if (!Double.isFinite(val)) {
			return m.defaultVal();
		}
		return Math.max(m.min(), Math.min(m.max(), val));
	}

	public static List<CvarMeta> allMetadata() {
		return List.copyOf(METADATA.values());
	}

	public static List<String> allCvarNames() {
		return List.copyOf(METADATA.keySet());
	}

	public static CvarMeta meta(String name) {
		return name == null ? null : METADATA.get(name.toLowerCase(Locale.ROOT));
	}

	public boolean hasCvar(String name) {
		return meta(name) != null;
	}

	public double getByName(String name) {
		String k = name.toLowerCase(Locale.ROOT);
		return switch (k) {
			case "sv_gravity" -> svGravity;
			case "sv_accelerate" -> svAccelerate;
			case "sv_airaccelerate" -> svAirAccelerate;
			case "sv_friction" -> svFriction;
			case "sv_stopspeed" -> svStopspeed;
			case "sv_maxspeed" -> svMaxSpeed;
			case "sv_maxvelocity" -> svMaxVelocity;
			case "sv_air_max_wishspeed" -> svAirMaxWishspeed;
			case "sv_jump_impulse" -> svJumpImpulse;
			case "sv_autobunnyhopping" -> svAutoBunnyhopping ? 1.0 : 0.0;
			case "sv_enablebunnyhopping" -> svEnableBunnyhopping ? 1.0 : 0.0;
			case "sv_staminamax" -> svStaminaMax;
			case "sv_staminajumpcost" -> svStaminaJumpCost;
			case "sv_staminalandcost" -> svStaminaLandCost;
			case "sv_staminarecoveryrate" -> svStaminaRecoveryRate;
			case "sv_walk_ratio" -> svWalkRatio;
			case "sv_duck_ratio" -> svDuckRatio;
			case "sv_wateraccelerate" -> svWaterAccelerate;
			case "sv_waterfriction" -> svWaterFriction;
			case "sv_ladder_scale_speed" -> svLadderScaleSpeed;
			case "sv_surf_enabled" -> svSurfEnabled ? 1.0 : 0.0;
			case "sv_surf_overbounce" -> svSurfOverbounce;
			case "sv_weapon_weight" -> svWeaponWeight ? 1.0 : 0.0;
			case "sv_crouch_jump" -> svCrouchJump ? 1.0 : 0.0;
			case "sv_edgebug_enabled" -> svEdgebugEnabled ? 1.0 : 0.0;
			case "sv_survival_fall_damage" -> svSurvivalFallDamage ? 1.0 : 0.0;
			case "sv_tickrate" -> svTickrate;
			case "sv_hu_per_block" -> svHuPerBlock;
			case "sv_jump_buffer_ticks" -> svJumpBufferTicks;
			default -> Double.NaN;
		};
	}

	public boolean setByName(String name, double rawValue) {
		String k = name.toLowerCase(Locale.ROOT);
		CvarMeta m = METADATA.get(k);
		if (m == null) {
			return false;
		}
		double v = clamp(k, rawValue);
		boolean boolVal = v >= 0.5;
		switch (k) {
			case "sv_gravity" -> svGravity = v;
			case "sv_accelerate" -> svAccelerate = v;
			case "sv_airaccelerate" -> svAirAccelerate = v;
			case "sv_friction" -> svFriction = v;
			case "sv_stopspeed" -> svStopspeed = v;
			case "sv_maxspeed" -> svMaxSpeed = v;
			case "sv_maxvelocity" -> svMaxVelocity = v;
			case "sv_air_max_wishspeed" -> svAirMaxWishspeed = v;
			case "sv_jump_impulse" -> svJumpImpulse = v;
			case "sv_autobunnyhopping" -> svAutoBunnyhopping = boolVal;
			case "sv_enablebunnyhopping" -> svEnableBunnyhopping = boolVal;
			case "sv_staminamax" -> svStaminaMax = v;
			case "sv_staminajumpcost" -> svStaminaJumpCost = v;
			case "sv_staminalandcost" -> svStaminaLandCost = v;
			case "sv_staminarecoveryrate" -> svStaminaRecoveryRate = v;
			case "sv_walk_ratio" -> svWalkRatio = v;
			case "sv_duck_ratio" -> svDuckRatio = v;
			case "sv_wateraccelerate" -> svWaterAccelerate = v;
			case "sv_waterfriction" -> svWaterFriction = v;
			case "sv_ladder_scale_speed" -> svLadderScaleSpeed = v;
			case "sv_surf_enabled" -> svSurfEnabled = boolVal;
			case "sv_surf_overbounce" -> svSurfOverbounce = v;
			case "sv_weapon_weight" -> svWeaponWeight = boolVal;
			case "sv_crouch_jump" -> svCrouchJump = boolVal;
			case "sv_edgebug_enabled" -> svEdgebugEnabled = boolVal;
			case "sv_survival_fall_damage" -> svSurvivalFallDamage = boolVal;
			case "sv_tickrate" -> svTickrate = v;
			case "sv_hu_per_block" -> svHuPerBlock = v;
			case "sv_jump_buffer_ticks" -> svJumpBufferTicks = (int) Math.round(v);
			default -> {
				return false;
			}
		}
		return true;
	}

	public String formatValue(String name) {
		String k = name.toLowerCase(Locale.ROOT);
		CvarMeta m = METADATA.get(k);
		double val = getByName(k);
		if (m == null || Double.isNaN(val)) {
			return "?";
		}
		if (m.isBoolean()) {
			return val >= 0.5 ? "1" : "0";
		}
		if (Math.abs(val - Math.rint(val)) < 1e-6) {
			return Long.toString(Math.round(val));
		}
		return String.format(Locale.ROOT, "%.3f", val).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	public String formatSourceConsole(String name) {
		CvarMeta m = meta(name);
		if (m == null) {
			return "Unknown cvar \"" + name + "\"";
		}
		return String.format(Locale.ROOT, "\"%s\" = \"%s\" (def. \"%s\") - %s",
			m.name(), formatValue(m.name()),
			m.isBoolean() ? (m.defaultVal() >= 0.5 ? "1" : "0")
				: (Math.abs(m.defaultVal() - Math.rint(m.defaultVal())) < 1e-6
					? Long.toString(Math.round(m.defaultVal()))
					: Double.toString(m.defaultVal())),
			m.summary());
	}

	public boolean matches(SourceCvars other) {
		if (other == null) {
			return false;
		}
		for (String key : METADATA.keySet()) {
			if (Math.abs(this.getByName(key) - other.getByName(key)) > 1e-4) {
				return false;
			}
		}
		return true;
	}

	public double[] toWireArray() {
		return new double[] {
			svGravity, svAccelerate, svAirAccelerate, svFriction, svStopspeed,
			svMaxSpeed, svMaxVelocity, svAirMaxWishspeed, svJumpImpulse,
			svAutoBunnyhopping ? 1.0 : 0.0,
			svEnableBunnyhopping ? 1.0 : 0.0,
			svStaminaMax, svStaminaJumpCost, svStaminaLandCost, svStaminaRecoveryRate,
			svWalkRatio, svDuckRatio, svWaterAccelerate, svWaterFriction, svLadderScaleSpeed,
			svSurfEnabled ? 1.0 : 0.0,
			svSurfOverbounce,
			svWeaponWeight ? 1.0 : 0.0,
			svCrouchJump ? 1.0 : 0.0,
			svEdgebugEnabled ? 1.0 : 0.0,
			svSurvivalFallDamage ? 1.0 : 0.0,
			svTickrate, svHuPerBlock, svJumpBufferTicks
		};
	}

	public static SourceCvars fromWireArray(double[] a) {
		SourceCvars c = new SourceCvars();
		if (a == null || a.length < 29) {
			return c;
		}
		c.svGravity = a[0];
		c.svAccelerate = a[1];
		c.svAirAccelerate = a[2];
		c.svFriction = a[3];
		c.svStopspeed = a[4];
		c.svMaxSpeed = a[5];
		c.svMaxVelocity = a[6];
		c.svAirMaxWishspeed = a[7];
		c.svJumpImpulse = a[8];
		c.svAutoBunnyhopping = a[9] >= 0.5;
		c.svEnableBunnyhopping = a[10] >= 0.5;
		c.svStaminaMax = a[11];
		c.svStaminaJumpCost = a[12];
		c.svStaminaLandCost = a[13];
		c.svStaminaRecoveryRate = a[14];
		c.svWalkRatio = a[15];
		c.svDuckRatio = a[16];
		c.svWaterAccelerate = a[17];
		c.svWaterFriction = a[18];
		c.svLadderScaleSpeed = a[19];
		c.svSurfEnabled = a[20] >= 0.5;
		c.svSurfOverbounce = a[21];
		c.svWeaponWeight = a[22] >= 0.5;
		c.svCrouchJump = a[23] >= 0.5;
		c.svEdgebugEnabled = a[24] >= 0.5;
		c.svSurvivalFallDamage = a[25] >= 0.5;
		c.svTickrate = a[26];
		c.svHuPerBlock = a[27];
		c.svJumpBufferTicks = (int) Math.round(a[28]);
		c.sanitize();
		return c;
	}
}
