package com.hazeg7.sourcecraft;

import static org.junit.jupiter.api.Assertions.*;

import com.hazeg7.sourcecraft.physics.ItemWeightTable;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourceMoveInput;
import com.hazeg7.sourcecraft.physics.SourceMoveState;
import com.hazeg7.sourcecraft.physics.SourceMovementEngine;
import com.hazeg7.sourcecraft.physics.SourceMovementEngine.PreviewPoint;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.hazeg7.sourcecraft.physics.SourceStepTelemetry;
import java.util.List;
import org.junit.jupiter.api.Test;

class SourcePhysicsMathTest {
	@Test
	void groundFrictionDecaysAuthentically() {
		SourceCvars cv = SourcePreset.CSS_CLASSIC.buildCvars();
		SourceMoveState st = new SourceMoveState();
		st.vz = 250.0;

		// 10 sub-steps at 100 Hz = 0.1s of friction from 250 u/s (sv_friction=4, sv_stopspeed=75)
		for (int i = 0; i < 10; i++) {
			SourceMovementEngine.applyFriction(st, cv, 0.01, 1.0);
		}
		assertTrue(st.vz < 180.0 && st.vz > 155.0, "Expected ~167 u/s after 0.1s CS:S friction, got " + st.vz);

		// Below stopspeed (75), friction uses control=stopspeed so player comes to clean zero stop
		for (int i = 0; i < 100; i++) {
			SourceMovementEngine.applyFriction(st, cv, 0.01, 1.0);
		}
		assertEquals(0.0, st.vz, 1e-6);
	}

	@Test
	void groundAccelerateReaches250AndCounterStrafesFast() {
		SourceCvars cv = SourcePreset.CSS_CLASSIC.buildCvars();
		SourceMoveState st = new SourceMoveState();

		for (int t = 0; t < 30; t++) {
			SourceMoveInput in = new SourceMoveInput();
			in.forwardMove = 1.0;
			in.onGround = true;
			SourceMovementEngine.stepTick(st, in, cv);
		}
		assertEquals(250.0, st.horizontalSpeedHu(), 2.5);

		// Instant counter-strafe (press S while moving forward at 250 u/s)
		double initialVz = st.vz;
		SourceMoveInput counter = new SourceMoveInput();
		counter.forwardMove = -1.0;
		counter.onGround = true;
		SourceMovementEngine.stepTick(st, counter, cv);
		assertTrue(st.vz < initialVz * 0.72, "Counter-strafe should drop forward velocity sharply");
	}

	@Test
	void airAccelerateGainsSpeedWithMouseTurnAndZeroWhenStraight() {
		SourceCvars cv = SourcePreset.CSS_CLASSIC.buildCvars(); // aa=100, wish=30

		// 1) Holding straight W in air with initial speed 250 u/s yields 0 gain
		SourceMoveState straight = new SourceMoveState();
		straight.vz = 250.0;
		straight.vy = 200.0;
		SourceMoveInput inStraight = new SourceMoveInput();
		inStraight.forwardMove = 1.0;
		inStraight.onGround = false;
		SourceMovementEngine.stepTick(straight, inStraight, cv);
		assertEquals(250.0, straight.horizontalSpeedHu(), 1e-3);

		// 2) Smooth air-strafe with sub-tick yaw samples builds authentic Source speed (~358 u/s at 1s, >440 u/s at 2s)
		SourceMoveState strafer = new SourceMoveState();
		strafer.vz = 250.0;
		strafer.vy = 302.0;
		float yaw = 0.0f;
		for (int t = 0; t < 40; t++) {
			boolean right = (t / 10) % 2 == 0;
			float delta = right ? 16.0f : -16.0f;
			float[] samples = new float[5];
			for (int s = 0; s < 5; s++) {
				samples[s] = yaw + delta * (s + 1) / 5.0f;
			}
			yaw = samples[4];

			SourceMoveInput in = new SourceMoveInput();
			in.sideMove = right ? 1.0 : -1.0;
			in.yawSamples = samples;
			in.currentYaw = yaw;
			in.onGround = false;
			SourceMovementEngine.stepTick(strafer, in, cv);
			if (strafer.vy < -280.0) {
				strafer.vy = 302.0;
			}
			if (t == 19) {
				assertTrue(strafer.horizontalSpeedHu() > 345.0,
					"Expected 1s CS:S air-strafe speed > 345 u/s, got " + strafer.horizontalSpeedHu());
			}
		}
		assertTrue(strafer.horizontalSpeedHu() > 440.0,
			"Expected 2s CS:S air-strafe speed > 440 u/s, got " + strafer.horizontalSpeedHu());
	}

	@Test
	void airAccelerate100OutgainsCompetitive10() {
		List<PreviewPoint> classic = SourceMovementEngine.simulateAirStrafePreview(SourcePreset.CSS_CLASSIC.buildCvars(), 60);
		List<PreviewPoint> comp = SourceMovementEngine.simulateAirStrafePreview(SourcePreset.CSS_COMP.buildCvars(), 60);

		double maxClassic = classic.stream().mapToDouble(PreviewPoint::speedHu).max().orElse(0);
		double maxComp = comp.stream().mapToDouble(PreviewPoint::speedHu).max().orElse(0);
		assertTrue(maxClassic > maxComp + 80.0,
			"Classic AA100 (" + maxClassic + ") should substantially out-accelerate Comp AA10 (" + maxComp + ")");
	}

	@Test
	void bhopSkipsGroundFrictionAndRespectsEnableBunnyhoppingCap() {
		// Uncapped bhop (sv_enablebunnyhopping = true): landing + jumping same tick preserves >= 99% speed
		SourceCvars uncapped = SourcePreset.CSS_CLASSIC.buildCvars();
		SourceMoveState st = new SourceMoveState();
		st.vz = 460.0;
		st.wasOnGround = false;

		SourceMoveInput hopIn = new SourceMoveInput();
		hopIn.onGround = true;
		hopIn.jumpJustPressed = true;
		hopIn.jumpHeld = true;
		hopIn.sideMove = 1.0;
		SourceStepTelemetry tel = SourceMovementEngine.stepTick(st, hopIn, uncapped);
		assertTrue(tel.jumpedThisTick());
		assertTrue(st.horizontalSpeedHu() >= 455.0, "Uncapped bhop should skip ground friction; got " + st.horizontalSpeedHu());

		// Valve soft-capped bhop (sv_enablebunnyhopping = false): clamps takeoff to 1.1 * maxspeed (275 u/s)
		SourceCvars capped = SourcePreset.CSS_COMP.buildCvars();
		SourceMoveState stCap = new SourceMoveState();
		stCap.vz = 460.0;
		stCap.wasOnGround = false;
		SourceMovementEngine.stepTick(stCap, hopIn, capped);
		assertTrue(stCap.horizontalSpeedHu() <= capped.svMaxSpeed * 1.15 + 1.0,
			"Comp soft cap should limit hop takeoff around 275 u/s; got " + stCap.horizontalSpeedHu());
	}

	@Test
	void clipVelocityProjectsCleanlyOnto30_45_60SurfRamps() {
		for (int deg : new int[] { 30, 45, 60 }) {
			double rad = Math.toRadians(deg);
			double nx = Math.sin(rad);
			double ny = Math.cos(rad);
			double nz = 0.0;

			SourceMoveState st = new SourceMoveState();
			st.vx = -150.0; // pushing inward toward ramp
			st.vy = -300.0; // falling down onto ramp
			st.vz = 500.0;  // surfing forward along Z

			SourceMovementEngine.clipVelocity(st, nx, ny, nz, 1.0);
			double inwardDot = st.vx * nx + st.vy * ny + st.vz * nz;
			assertTrue(inwardDot >= -1e-6, "Inward velocity must not penetrate " + deg + " deg ramp");
			assertEquals(500.0, st.vz, 1e-6, "Tangential along-ramp speed must remain intact");
		}
	}

	@Test
	void fastLadderCrouchJumpEdgebugAndItemWeightsWork() {
		SourceCvars cv = SourcePreset.CSS_CLASSIC.buildCvars();

		// Air crouch-jump pulls feet up
		SourceMoveState cj = new SourceMoveState();
		SourceMoveInput cjIn = new SourceMoveInput();
		cjIn.onGround = false;
		cjIn.crouchHeld = true;
		SourceStepTelemetry cjTel = SourceMovementEngine.stepTick(cj, cjIn, cv);
		assertTrue(cjTel.crouchPulledFeetThisTick());

		// Releasing crouch midair triggers drop back
		cjIn.crouchHeld = false;
		SourceStepTelemetry relTel = SourceMovementEngine.stepTick(cj, cjIn, cv);
		assertTrue(relTel.crouchReleasedDropThisTick());

		// Edgebug cancels fast downward fall & marks fall damage exempt
		SourceMoveState eb = new SourceMoveState();
		eb.vy = -520.0;
		SourceMoveInput ebIn = new SourceMoveInput();
		ebIn.onGround = false;
		ebIn.edgebugCandidate = true;
		SourceStepTelemetry ebTel = SourceMovementEngine.stepTick(eb, ebIn, cv);
		assertTrue(ebTel.edgebugTriggered());
		assertTrue(eb.cancelNextFallDamage);
		assertTrue(eb.vy > -100.0);

		// Fast ladder strafe speed
		SourceMoveState lad = new SourceMoveState();
		SourceMoveInput ladIn = new SourceMoveInput();
		ladIn.onLadder = true;
		ladIn.forwardMove = 1.0;
		ladIn.sideMove = 1.0;
		ladIn.currentPitch = -45.0f;
		ladIn.ladderNormalX = 0.0;
		ladIn.ladderNormalZ = 1.0;
		SourceStepTelemetry ladTel = SourceMovementEngine.stepTick(lad, ladIn, cv);
		assertTrue(ladTel.ladderThisTick());
		assertTrue(lad.speed3dHu() > 40.0);

		// Item weights: empty hand faster than crossbow
		assertTrue(ItemWeightTable.maxSpeedForItemKey("", 250.0) > ItemWeightTable.maxSpeedForItemKey("crossbow", 250.0));
	}
}
