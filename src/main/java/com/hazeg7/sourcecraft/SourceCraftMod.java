package com.hazeg7.sourcecraft;

import com.hazeg7.sourcecraft.block.SourceBlocks;
import com.hazeg7.sourcecraft.command.SourceCommands;
import com.hazeg7.sourcecraft.item.MapWandItem;
import com.hazeg7.sourcecraft.network.ClientConfigRequestC2SPayload;
import com.hazeg7.sourcecraft.network.PlayerModeC2SPayload;
import com.hazeg7.sourcecraft.network.ServerConfigS2CPayload;
import com.hazeg7.sourcecraft.network.SourceServerManager;
import com.hazeg7.sourcecraft.network.TimerActionC2SPayload;
import com.hazeg7.sourcecraft.network.TimerStateS2CPayload;
import com.hazeg7.sourcecraft.network.VelocitySyncS2CPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceCraftMod implements ModInitializer {
	public static final String MOD_ID = "sourcecraft";
	public static final String MOD_NAME = "SourceCraft";
	public static final int PROTOCOL_VERSION = 1;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		SourceBlocks.init();

		PayloadTypeRegistry.clientboundPlay().register(ServerConfigS2CPayload.TYPE, ServerConfigS2CPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ClientConfigRequestC2SPayload.TYPE, ClientConfigRequestC2SPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(VelocitySyncS2CPayload.TYPE, VelocitySyncS2CPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TimerActionC2SPayload.TYPE, TimerActionC2SPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TimerStateS2CPayload.TYPE, TimerStateS2CPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PlayerModeC2SPayload.TYPE, PlayerModeC2SPayload.CODEC);

		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (player.getItemInHand(hand).is(SourceBlocks.MAP_WAND)) {
				MapWandItem.handleLeftClickBlock(player, world, pos);
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});

		SourceServerManager.init();
		SourceCommands.init();
		LOGGER.info("{} initialized (protocol v{}, CS: Source movement engine ready)", MOD_NAME, PROTOCOL_VERSION);
	}
}
