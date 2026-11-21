package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by authorized players (singleplayer owner or server OP) when applying CVars from the Studio GUI.
 */
public record ClientConfigRequestC2SPayload(
	boolean enabled,
	boolean serverEnforced,
	String presetId,
	SourceCvars cvars
) implements CustomPacketPayload {
	public static final Type<ClientConfigRequestC2SPayload> TYPE = new Type<>(SourceCraftMod.id("client_config_c2s"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ClientConfigRequestC2SPayload> CODEC = CustomPacketPayload.codec(
		ClientConfigRequestC2SPayload::write,
		ClientConfigRequestC2SPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeBoolean(enabled);
		buf.writeBoolean(serverEnforced);
		buf.writeUtf(presetId != null ? presetId : "custom", 64);
		CvarCodecHelper.writeCvars(buf, cvars);
	}

	private static ClientConfigRequestC2SPayload read(RegistryFriendlyByteBuf buf) {
		boolean enabled = buf.readBoolean();
		boolean enforced = buf.readBoolean();
		String preset = buf.readUtf(64);
		SourceCvars cvars = CvarCodecHelper.readCvars(buf);
		return new ClientConfigRequestC2SPayload(enabled, enforced, preset, cvars);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
