package com.hazeg7.sourcecraft.item;

import com.hazeg7.sourcecraft.timer.TimerManager;
import com.hazeg7.sourcecraft.timer.TimerZone;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Map-builder tool for defining cuboid KZ / Surf / Bhop Start, End, and Stage zones.
 * Right-click a block to set Pos1/Pos2; Sneak+Right-click to commit zone and cycle type.
 */
public class MapWandItem extends Item {
	private static final class Selection {
		BlockPos pos1;
		BlockPos pos2;
		TimerZone.ZoneType nextType = TimerZone.ZoneType.START;
	}

	private static final Map<UUID, Selection> SELECTIONS = new ConcurrentHashMap<>();

	public MapWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity entity) {
		return false;
	}

	public static void handleLeftClickBlock(Player player, Level level, BlockPos pos) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return;
		}
		Selection sel = SELECTIONS.computeIfAbsent(sp.getUUID(), id -> new Selection());
		sel.pos1 = pos.immutable();
		sp.sendSystemMessage(Component.literal("§b[SourceCraft]§f Zone Pos1 set to §3" + pos.toShortString()));
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (level.isClientSide() || !(context.getPlayer() instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		Selection sel = SELECTIONS.computeIfAbsent(sp.getUUID(), id -> new Selection());
		BlockPos pos = context.getClickedPos().immutable();

		if (sp.isShiftKeyDown()) {
			if (sel.pos1 == null || sel.pos2 == null) {
				sel.nextType = nextType(sel.nextType);
				sp.sendSystemMessage(Component.literal("§b[SourceCraft]§f Zone mode: §b" + sel.nextType.name()));
				return InteractionResult.SUCCESS;
			}
			TimerZone created = TimerManager.get().addZone(
				sp.level().dimension().identifier().toString(),
				sel.nextType,
				sel.pos1,
				sel.pos2
			);
			sp.sendSystemMessage(Component.literal("§a[SourceCraft]§f Created §b" + created.type().name() + "§f zone #" + created.id()));
			sel.nextType = nextType(sel.nextType);
			sel.pos1 = null;
			sel.pos2 = null;
			return InteractionResult.SUCCESS;
		}

		if (sel.pos1 == null) {
			sel.pos1 = pos;
			sp.sendSystemMessage(Component.literal("§b[SourceCraft]§f Pos1 set (" + pos.toShortString() + "). Right-click Pos2."));
		} else {
			sel.pos2 = pos;
			sp.sendSystemMessage(Component.literal("§b[SourceCraft]§f Pos2 set (" + pos.toShortString() + "). Sneak+Right-click to save §b" + sel.nextType.name() + "§f zone."));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide() && player instanceof ServerPlayer sp && sp.isShiftKeyDown()) {
			Selection sel = SELECTIONS.computeIfAbsent(sp.getUUID(), id -> new Selection());
			sel.nextType = nextType(sel.nextType);
			sp.sendSystemMessage(Component.literal("§b[SourceCraft]§f Switched zone brush to §b" + sel.nextType.name()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	private static TimerZone.ZoneType nextType(TimerZone.ZoneType t) {
		return switch (t) {
			case START -> TimerZone.ZoneType.END;
			case END -> TimerZone.ZoneType.STAGE;
			case STAGE -> TimerZone.ZoneType.START;
		};
	}
}
