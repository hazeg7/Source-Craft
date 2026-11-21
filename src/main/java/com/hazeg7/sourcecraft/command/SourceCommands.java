package com.hazeg7.sourcecraft.command;

import com.hazeg7.sourcecraft.network.SourceServerManager;
import com.hazeg7.sourcecraft.physics.SourceCvars;
import com.hazeg7.sourcecraft.physics.SourcePreset;
import com.hazeg7.sourcecraft.timer.TimerManager;
import com.hazeg7.sourcecraft.timer.TimerZone;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers Source-style console commands (/css, /cp, /tp, /prevcp, /nextcp, /r).
 */
public final class SourceCommands {
	private static final SuggestionProvider<CommandSourceStack> CVAR_SUGGESTIONS =
		(ctx, builder) -> SharedSuggestionProvider.suggest(SourceCvars.allCvarNames(), builder);

	private static final SuggestionProvider<CommandSourceStack> PRESET_SUGGESTIONS =
		(ctx, builder) -> SharedSuggestionProvider.suggest(
			SourcePreset.all().stream().map(SourcePreset::id), builder
		);

	private SourceCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(Commands.literal("css")
				.then(Commands.literal("info").executes(ctx -> {
					SourceCvars cv = SourceServerManager.getCvars();
					ctx.getSource().sendSuccess(() -> Component.literal(
						"§b[SourceCraft]§f Active: §b" + SourceServerManager.isEnabled()
							+ "§f | Preset: §b" + SourceServerManager.getPresetId()
							+ "§f | tickrate=§3" + cv.formatValue("sv_tickrate")
							+ "§f | aa=§3" + cv.formatValue("sv_airaccelerate")
							+ "§f | autobhop=§3" + cv.formatValue("sv_autobunnyhopping")
							+ "§f | friction=§3" + cv.formatValue("sv_friction")
					), false);
					return 1;
				}))
				.then(Commands.literal("toggle")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> {
						boolean next = !SourceServerManager.isEnabled();
						SourceServerManager.applyServerConfig(
							next,
							SourceServerManager.isServerEnforced(),
							SourceServerManager.getPresetId(),
							SourceServerManager.getCvars(),
							ctx.getSource().getServer()
						);
						ctx.getSource().sendSuccess(() -> Component.literal(
							"§b[SourceCraft]§f CS:S Physics " + (next ? "§aENABLED" : "§cDISABLED")
						), true);
						return 1;
					}))
				.then(Commands.literal("preset")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("presetId", StringArgumentType.word())
						.suggests(PRESET_SUGGESTIONS)
						.executes(ctx -> {
							String id = StringArgumentType.getString(ctx, "presetId");
							SourcePreset preset = SourcePreset.byId(id);
							SourceCvars cv = preset.buildCvars();
							SourceServerManager.applyServerConfig(
								true,
								SourceServerManager.isServerEnforced(),
								preset.id(),
								cv,
								ctx.getSource().getServer()
							);
							ctx.getSource().sendSuccess(() -> Component.literal(
								"§b[SourceCraft]§f Applied preset §b" + preset.name() + " §7(" + preset.badge() + ")"
							), true);
							return 1;
						})))
				.then(Commands.literal("set")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.argument("cvar", StringArgumentType.word())
						.suggests(CVAR_SUGGESTIONS)
						.then(Commands.argument("value", DoubleArgumentType.doubleArg())
							.executes(ctx -> {
								String name = StringArgumentType.getString(ctx, "cvar");
								double val = DoubleArgumentType.getDouble(ctx, "value");
								SourceCvars copy = SourceServerManager.getCvars().copy();
								if (!copy.setByName(name, val)) {
									ctx.getSource().sendFailure(Component.literal("Unknown CVar: " + name));
									return 0;
								}
								SourceServerManager.applyServerConfig(
									SourceServerManager.isEnabled(),
									SourceServerManager.isServerEnforced(),
									SourcePreset.detectPresetId(copy),
									copy,
									ctx.getSource().getServer()
								);
								ctx.getSource().sendSuccess(() -> Component.literal(
									"§b[SourceCraft]§f " + copy.formatSourceConsole(name)
								), true);
								return 1;
							}))))
				.then(Commands.literal("get")
					.executes(ctx -> {
						SourceCvars cv = SourceServerManager.getCvars();
						for (String key : SourceCvars.allCvarNames()) {
							ctx.getSource().sendSuccess(() -> Component.literal("§7" + cv.formatSourceConsole(key)), false);
						}
						return 1;
					})
					.then(Commands.argument("cvar", StringArgumentType.word())
						.suggests(CVAR_SUGGESTIONS)
						.executes(ctx -> {
							String key = StringArgumentType.getString(ctx, "cvar");
							SourceCvars cv = SourceServerManager.getCvars();
							ctx.getSource().sendSuccess(() -> Component.literal("§b" + cv.formatSourceConsole(key)), false);
							return 1;
						})))
				.then(Commands.literal("reset")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> {
						SourcePreset def = SourcePreset.CSS_CLASSIC;
						SourceServerManager.applyServerConfig(
							true, false, def.id(), def.buildCvars(), ctx.getSource().getServer()
						);
						ctx.getSource().sendSuccess(() -> Component.literal(
							"§b[SourceCraft]§f Reset CVars to §b" + def.name()
						), true);
						return 1;
					}))
				.then(Commands.literal("enforce")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> {
						boolean next = !SourceServerManager.isServerEnforced();
						SourceServerManager.applyServerConfig(
							SourceServerManager.isEnabled(),
							next,
							SourceServerManager.getPresetId(),
							SourceServerManager.getCvars(),
							ctx.getSource().getServer()
						);
						ctx.getSource().sendSuccess(() -> Component.literal(
							"§b[SourceCraft]§f Server CVar lock: " + (next ? "§aENFORCED" : "§7CLIENT ALLOWED")
						), true);
						return 1;
					}))
				.then(Commands.literal("zone")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.then(Commands.literal("start").then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos())
							.executes(ctx -> addZoneCmd(ctx.getSource(), TimerZone.ZoneType.START,
								BlockPosArgument.getLoadedBlockPos(ctx, "from"),
								BlockPosArgument.getLoadedBlockPos(ctx, "to"))))))
					.then(Commands.literal("end").then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos())
							.executes(ctx -> addZoneCmd(ctx.getSource(), TimerZone.ZoneType.END,
								BlockPosArgument.getLoadedBlockPos(ctx, "from"),
								BlockPosArgument.getLoadedBlockPos(ctx, "to"))))))
					.then(Commands.literal("stage").then(Commands.argument("from", BlockPosArgument.blockPos())
						.then(Commands.argument("to", BlockPosArgument.blockPos())
							.executes(ctx -> addZoneCmd(ctx.getSource(), TimerZone.ZoneType.STAGE,
								BlockPosArgument.getLoadedBlockPos(ctx, "from"),
								BlockPosArgument.getLoadedBlockPos(ctx, "to"))))))
					.then(Commands.literal("clear").executes(ctx -> {
						int removed = TimerManager.get().clearZones();
						ctx.getSource().sendSuccess(() -> Component.literal("§b[SourceCraft]§f Cleared " + removed + " timer zone(s)."), true);
						return 1;
					}))
					.then(Commands.literal("list").executes(ctx -> {
						var list = TimerManager.get().zones();
						ctx.getSource().sendSuccess(() -> Component.literal("§b[SourceCraft]§f Active zones (" + list.size() + "):"), false);
						for (TimerZone z : list) {
							ctx.getSource().sendSuccess(() -> Component.literal(
								" §7#" + z.id() + " §b" + z.type() + " §7(" + (int) z.minX() + "," + (int) z.minY() + "," + (int) z.minZ() + ")"
							), false);
						}
						return 1;
					})))
			);

			dispatcher.register(Commands.literal("cp").executes(ctx -> {
				ServerPlayer sp = ctx.getSource().getPlayerOrException();
				TimerManager.get().saveCheckpoint(sp, true);
				TimerManager.get().pushTimerState(sp, System.currentTimeMillis());
				return 1;
			}));

			dispatcher.register(Commands.literal("tpcp")
				.executes(ctx -> {
					ServerPlayer sp = ctx.getSource().getPlayerOrException();
					TimerManager.get().teleportCheckpoint(sp, -1);
					TimerManager.get().pushTimerState(sp, System.currentTimeMillis());
					return 1;
				})
				.then(Commands.argument("index", IntegerArgumentType.integer(1, 64)).executes(ctx -> {
					ServerPlayer sp = ctx.getSource().getPlayerOrException();
					int idx = IntegerArgumentType.getInteger(ctx, "index") - 1;
					TimerManager.get().teleportCheckpoint(sp, idx);
					TimerManager.get().pushTimerState(sp, System.currentTimeMillis());
					return 1;
				}))
			);

			dispatcher.register(Commands.literal("prevcp").executes(ctx -> {
				ServerPlayer sp = ctx.getSource().getPlayerOrException();
				TimerManager.get().stepCheckpoint(sp, -1);
				return 1;
			}));

			dispatcher.register(Commands.literal("nextcp").executes(ctx -> {
				ServerPlayer sp = ctx.getSource().getPlayerOrException();
				TimerManager.get().stepCheckpoint(sp, +1);
				return 1;
			}));

			dispatcher.register(Commands.literal("r").executes(ctx -> {
				ServerPlayer sp = ctx.getSource().getPlayerOrException();
				TimerManager.get().restartRun(sp);
				TimerManager.get().pushTimerState(sp, System.currentTimeMillis());
				return 1;
			}));
		});
	}

	private static int addZoneCmd(CommandSourceStack source, TimerZone.ZoneType type, BlockPos from, BlockPos to) {
		String dim = source.getLevel().dimension().identifier().toString();
		TimerZone z = TimerManager.get().addZone(dim, type, from, to);
		source.sendSuccess(() -> Component.literal(
			"§b[SourceCraft]§f Added §b" + type + "§f zone #" + z.id()
		), true);
		return 1;
	}
}
