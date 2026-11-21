package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Periodic server-to-client timer, PB, stage split, and checkpoint state update.
 */
public record TimerStateS2CPayload(
	boolean running,
	boolean armed,
	long elapsedMs,
	long personalBestMs,
	long lastDeltaMs,
	boolean proRun,
	int currentStage,
	int checkpointCount,
	int checkpointIndex,
	String calloutText
) implements CustomPacketPayload {
	public static final Type<TimerStateS2CPayload> TYPE = new Type<>(SourceCraftMod.id("timer_state_s2c"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TimerStateS2CPayload> CODEC = CustomPacketPayload.codec(
		TimerStateS2CPayload::write,
		TimerStateS2CPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(running);
		buf.writeBoolean(armed);
		buf.writeVarLong(elapsedMs);
		buf.writeLong(personalBestMs);
		buf.writeLong(lastDeltaMs);
		buf.writeBoolean(proRun);
		buf.writeVarInt(currentStage);
		buf.writeVarInt(checkpointCount);
		buf.writeVarInt(checkpointIndex);
		buf.writeUtf(calloutText != null ? calloutText : "", 96);
	}

	private static TimerStateS2CPayload read(RegistryFriendlyByteBuf buf) {
		return new TimerStateS2CPayload(
			buf.readBoolean(),
			buf.readBoolean(),
			buf.readVarLong(),
			buf.readLong(),
			buf.readLong(),
			buf.readBoolean(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readVarInt(),
			buf.readUtf(96)
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
