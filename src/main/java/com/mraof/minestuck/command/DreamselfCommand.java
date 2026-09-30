package com.mraof.minestuck.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mraof.minestuck.player.dreamself.DreamselfData;
import com.mraof.minestuck.player.dreamself.DreamselfHandler;
import com.mraof.minestuck.player.dreamself.LunarSway;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Debug and admin command for the dreamself system.
 */
public final class DreamselfCommand
{
	public static final String INFO = "commands.minestuck.dreamself.info";
	public static final String INFO_DEAD = "commands.minestuck.dreamself.info_dead";
	public static final String INFO_NONE = "commands.minestuck.dreamself.info_none";
	public static final String SWAP_SUCCESS = "commands.minestuck.dreamself.swap";
	public static final String SWAY_SET = "commands.minestuck.dreamself.sway_set";
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> swayCommand = Commands.literal("sway");
		for(LunarSway sway : LunarSway.values())
			swayCommand.then(Commands.literal(sway.getSerializedName()).executes(context -> setSway(context, sway)));
		
		dispatcher.register(Commands.literal("dreamself").requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(Commands.literal("info").executes(DreamselfCommand::info)).then(Commands.literal("swap").executes(DreamselfCommand::swap)).then(swayCommand));
	}
	
	private static int info(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		DreamselfData data = DreamselfHandler.getData(player);
		if(data == null || data.sway() == null)
		{
			context.getSource().sendSuccess(() -> Component.translatable(INFO_NONE), false);
			return 0;
		}
		
		if(data.isDreamselfDead())
		{
			context.getSource().sendSuccess(() -> Component.translatable(INFO_DEAD, data.sway().getDisplayName()), false);
			return 1;
		}
		
		Component self = Component.translatable(data.isDreaming() ? "commands.minestuck.dreamself.self_dream" : "commands.minestuck.dreamself.self_body");
		context.getSource().sendSuccess(() -> Component.translatable(INFO, data.sway().getDisplayName(), self), false);
		return 1;
	}
	
	private static int swap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		if(!DreamselfHandler.requestSwap(player, null)) return 0;
		context.getSource().sendSuccess(() -> Component.translatable(SWAP_SUCCESS), false);
		return 1;
	}
	
	private static int setSway(CommandContext<CommandSourceStack> context, LunarSway sway) throws CommandSyntaxException
	{
		ServerPlayer player = context.getSource().getPlayerOrException();
		DreamselfData data = DreamselfHandler.getData(player);
		if(data == null) return 0;
		data.setSway(sway);
		context.getSource().sendSuccess(() -> Component.translatable(SWAY_SET, sway.getDisplayName()), true);
		return 1;
	}
}
