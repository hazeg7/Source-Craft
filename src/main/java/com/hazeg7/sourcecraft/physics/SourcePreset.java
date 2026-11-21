package com.hazeg7.sourcecraft.physics;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Curated CS: Source movement presets matching real community server configs.
 */
public final class SourcePreset {
	private final String id;
	private final String name;
	private final String badge;
	private final String description;
	private final Consumer<SourceCvars> configurator;

	private SourcePreset(String id, String name, String badge, String description, Consumer<SourceCvars> configurator) {
		this.id = id;
		this.name = name;
		this.badge = badge;
		this.description = description;
		this.configurator = configurator;
	}

	public String id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String badge() {
		return badge;
	}

	public String description() {
		return description;
	}

	public SourceCvars buildCvars() {
		SourceCvars c = SourceCvars.createDefault();
		configurator.accept(c);
		c.sanitize();
		return c;
	}

	public void applyTo(SourceCvars target) {
		SourceCvars built = buildCvars();
		target.copyFrom(built);
	}

	public static final SourcePreset CSS_CLASSIC = new SourcePreset(
		"css_classic",
		"CS:S Bhop 100t",
		"100T · AA100",
		"Classic CS: Source 100-tick bhop server rules: sv_airaccelerate 100, friction 4, stopspeed 75, uncapped bhop.",
		c -> {
			c.svTickrate = 100.0;
			c.svAirAccelerate = 100.0;
			c.svAccelerate = 5.5;
			c.svFriction = 4.0;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = false;
			c.svEnableBunnyhopping = true;
			c.svStaminaMax = 0.0;
			c.svSurfEnabled = true;
			c.svSurvivalFallDamage = false;
		}
	);

	public static final SourcePreset CSS_COMP = new SourcePreset(
		"css_comp",
		"CS:S Match 66t",
		"66T · AA10",
		"Authentic Valve 66-tick competitive rules: sv_airaccelerate 10, stamina jump/land drain, soft bhop cap.",
		c -> {
			c.svTickrate = 66.0;
			c.svAirAccelerate = 10.0;
			c.svAccelerate = 5.5;
			c.svFriction = 4.0;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = false;
			c.svEnableBunnyhopping = false;
			c.svStaminaMax = 80.0;
			c.svStaminaJumpCost = 0.08;
			c.svStaminaLandCost = 0.05;
			c.svStaminaRecoveryRate = 60.0;
			c.svWeaponWeight = true;
			c.svSurvivalFallDamage = true;
		}
	);

	public static final SourcePreset CSS_SURF = new SourcePreset(
		"css_surf",
		"CS:S Surf 66t",
		"SURF · AA150",
		"Smooth ramp & stair surfing: sv_airaccelerate 150, auto-bhop enabled, zero fall damage, 3500 u/s maxvelocity.",
		c -> {
			c.svTickrate = 66.0;
			c.svAirAccelerate = 150.0;
			c.svAccelerate = 10.0;
			c.svFriction = 4.0;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = true;
			c.svEnableBunnyhopping = true;
			c.svStaminaMax = 0.0;
			c.svSurfEnabled = true;
			c.svSurvivalFallDamage = false;
		}
	);

	public static final SourcePreset CSS_AUTOBHOP = new SourcePreset(
		"css_autobhop",
		"Auto-Bhop 100t",
		"AUTO · AA1000",
		"Hold Space to chain ultra-tight high-speed air-strafes at 100 Hz with sv_airaccelerate 1000.",
		c -> {
			c.svTickrate = 100.0;
			c.svAirAccelerate = 1000.0;
			c.svAccelerate = 10.0;
			c.svFriction = 4.0;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = true;
			c.svEnableBunnyhopping = true;
			c.svStaminaMax = 0.0;
			c.svSurfEnabled = true;
			c.svSurvivalFallDamage = false;
		}
	);

	public static final SourcePreset CSS_KZ = new SourcePreset(
		"css_kz",
		"KZ / Climb 100t",
		"KZ · AA100",
		"Kreedz climbing & longjump tuning: tight 5.5 ground accel, manual timing, crouch-jump & edgebug enabled.",
		c -> {
			c.svTickrate = 100.0;
			c.svAirAccelerate = 100.0;
			c.svAccelerate = 5.5;
			c.svFriction = 4.0;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = false;
			c.svEnableBunnyhopping = true;
			c.svStaminaMax = 0.0;
			c.svCrouchJump = true;
			c.svEdgebugEnabled = true;
			c.svSurvivalFallDamage = true;
		}
	);

	public static final SourcePreset SURVIVAL_HYBRID = new SourcePreset(
		"survival_hybrid",
		"Survival Hybrid",
		"SMP · AA60",
		"Balanced for normal Minecraft survival worlds: smooth air-strafing, item weight, edgebugs, survival fall damage.",
		c -> {
			c.svTickrate = 100.0;
			c.svAirAccelerate = 60.0;
			c.svAccelerate = 5.5;
			c.svFriction = 4.5;
			c.svStopspeed = 75.0;
			c.svAutoBunnyhopping = false;
			c.svEnableBunnyhopping = true;
			c.svStaminaMax = 40.0;
			c.svStaminaJumpCost = 0.04;
			c.svStaminaLandCost = 0.03;
			c.svWeaponWeight = true;
			c.svSurvivalFallDamage = true;
			c.svEdgebugEnabled = true;
		}
	);

	private static final List<SourcePreset> ALL = List.of(
		CSS_CLASSIC, CSS_COMP, CSS_SURF, CSS_AUTOBHOP, CSS_KZ, SURVIVAL_HYBRID
	);

	public static List<SourcePreset> all() {
		return ALL;
	}

	public static SourcePreset byId(String id) {
		if (id == null) {
			return CSS_CLASSIC;
		}
		String norm = id.trim().toLowerCase(Locale.ROOT);
		for (SourcePreset p : ALL) {
			if (p.id.equals(norm)) {
				return p;
			}
		}
		return CSS_CLASSIC;
	}

	public static String detectPresetId(SourceCvars cvars) {
		if (cvars == null) {
			return "custom";
		}
		for (SourcePreset p : ALL) {
			if (cvars.matches(p.buildCvars())) {
				return p.id;
			}
		}
		return "custom";
	}
}
