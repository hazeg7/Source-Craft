package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Syncs client SourceMoveState velocity when triggered server-side (boost pad or checkpoint teleport).
 */
public record VelocitySyncS2CPayload(
	double vxBlocks,
	double vyBlocks,
	double vzBlocks,
	String calloutTag
) implements CustomPacketPayload {
	public static final Type<VelocitySyncS2CPayload> TYPE = new Type<>(SourceCraftMod.id("velocity_sync_s2c"));

	public static final StreamCodec<RegistryFriendlyByteBuf, VelocitySyncS2CPayload> CODEC = CustomPacketPayload.codec(
		VelocitySyncS2CPayload::write,
		VelocitySyncS2CPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeDouble(vxBlocks);
		buf.writeDouble(vyBlocks);
		buf.writeDouble(vzBlocks);
		buf.writeUtf(calloutTag != null ? calloutTag : "", 64);
	}

	private static VelocitySyncS2CPayload read(RegistryFriendlyByteBuf buf) {
		return new VelocitySyncS2CPayload(
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readUtf(64)
		);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
