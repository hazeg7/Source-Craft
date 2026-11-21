package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client-to-server checkpoint & run timer action (hotkeyed C / R or HUD controls).
 */
public record TimerActionC2SPayload(Action action, int indexArg) implements CustomPacketPayload {
	public enum Action {
		SAVE_CP,
		TP_CP,
		PREV_CP,
		NEXT_CP,
		RESTART,
		TOGGLE_RESTORE_VEL,
		CLEAR_CP
	}

	public static final Type<TimerActionC2SPayload> TYPE = new Type<>(SourceCraftMod.id("timer_action_c2s"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TimerActionC2SPayload> CODEC = CustomPacketPayload.codec(
		TimerActionC2SPayload::write,
		TimerActionC2SPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(action.ordinal());
		buf.writeVarInt(indexArg);
	}

	private static TimerActionC2SPayload read(RegistryFriendlyByteBuf buf) {
		Action[] vals = Action.values();
		Action act = vals[Math.floorMod(buf.readVarInt(), vals.length)];
		int idx = buf.readVarInt();
		return new TimerActionC2SPayload(act, idx);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
