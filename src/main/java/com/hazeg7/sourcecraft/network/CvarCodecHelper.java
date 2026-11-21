package com.hazeg7.sourcecraft.network;

import com.hazeg7.sourcecraft.physics.SourceCvars;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Shared binary codec helpers for {@link SourceCvars}.
 */
public final class CvarCodecHelper {
	private CvarCodecHelper() {
	}

	public static void writeCvars(FriendlyByteBuf buf, SourceCvars cvars) {
		double[] arr = (cvars != null ? cvars : SourceCvars.createDefault()).toWireArray();
		buf.writeVarInt(arr.length);
		for (double v : arr) {
			buf.writeDouble(v);
		}
	}

	public static SourceCvars readCvars(FriendlyByteBuf buf) {
		int len = Math.max(0, Math.min(64, buf.readVarInt()));
		double[] arr = new double[len];
		for (int i = 0; i < len; i++) {
			arr[i] = buf.readDouble();
		}
		return SourceCvars.fromWireArray(arr);
	}
}
