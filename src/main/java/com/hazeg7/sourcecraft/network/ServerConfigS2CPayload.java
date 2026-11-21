package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.SourceCraftMod;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Authoritative server CVar + preset configuration packet sent on join and CVar updates.
 */
public record ServerConfigS2CPayload(
	int protocolVersion,
	boolean enabled,
	boolean serverEnforced,
	String presetId,
	SourceCvars cvars
) implements CustomPacketPayload {
	public static final Type<ServerConfigS2CPayload> TYPE = new Type<>(SourceCraftMod.id("server_config_s2c"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ServerConfigS2CPayload> CODEC = CustomPacketPayload.codec(
		ServerConfigS2CPayload::write,
		ServerConfigS2CPayload::read
	);

	private void write(RegistryFriendlyByteBuf buf) {
		buf.writeVarInt(protocolVersion);
		buf.writeBoolean(enabled);
		buf.writeBoolean(serverEnforced);
		buf.writeUtf(presetId != null ? presetId : "custom", 64);
		CvarCodecHelper.writeCvars(buf, cvars);
	}

	private static ServerConfigS2CPayload read(RegistryFriendlyByteBuf buf) {
		int proto = buf.readVarInt();
		boolean enabled = buf.readBoolean();
		boolean enforced = buf.readBoolean();
		String preset = buf.readUtf(64);
		SourceCvars cvars = CvarCodecHelper.readCvars(buf);
		return new ServerConfigS2CPayload(proto, enabled, enforced, preset, cvars);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
