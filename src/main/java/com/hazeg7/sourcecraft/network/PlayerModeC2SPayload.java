package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Reports whether the local player currently has SourceCraft physics active and signals fall-save techs (edgebug/jumpbug).
 */
public record PlayerModeC2SPayload(boolean movementActive, boolean grantFallSave) implements CustomPacketPayload {
	public static final Type<PlayerModeC2SPayload> TYPE = new Type<>(SourceCraftMod.id("player_mode_c2s"));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerModeC2SPayload> CODEC = CustomPacketPayload.codec(
		PlayerModeC2SPayload::write,
		PlayerModeC2SPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(movementActive);
		buf.writeBoolean(grantFallSave);
	}

	private static PlayerModeC2SPayload read(RegistryFriendlyByteBuf buf) {
		return new PlayerModeC2SPayload(buf.readBoolean(), buf.readBoolean());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
