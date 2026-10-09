package com.mraof.minestuck.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mraof.minestuck.command.argument.TitleArgument;
import com.mraof.minestuck.network.TitleDataPacket;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.PlayerData;
import com.mraof.minestuck.player.Title;
import com.mraof.minestuck.player.godtier.GodTierAscensionHandler;
import com.mraof.minestuck.player.godtier.GodTierReset;
import com.mraof.minestuck.player.godtier.GodTierState;
import com.mraof.minestuck.player.godtier.GodTierTickHandler;
import com.mraof.minestuck.player.godtier.skill.Badge;
import com.mraof.minestuck.player.godtier.skill.GodTierSkills;
import com.mraof.minestuck.player.godtier.skill.Skill;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import com.mraof.minestuck.util.MSAttachments;
import com.mraof.minestuck.world.lands.LandTypePair;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class GodTierCommand
{
	public static final String LOCATE_SUCCESS = "commands.minestuck.godtier.locate.success";
	public static final String LOCATE_WRONG_DIMENSION = "commands.minestuck.godtier.locate.wrong_dimension";
	public static final String LOCATE_NOT_FOUND = "commands.minestuck.godtier.locate.not_found";
	public static final String TITLE_SUCCESS = "commands.minestuck.godtier.title.success";
	public static final String RESET_SUCCESS = "commands.minestuck.godtier.reset.success";
	public static final String RESET_NOT_GOD_TIER = "commands.minestuck.godtier.reset.not_god_tier";
	public static final String MASTER_CONTROL_ON = "commands.minestuck.godtier.mastercontrol.on";
	public static final String MASTER_CONTROL_OFF = "commands.minestuck.godtier.mastercontrol.off";
	public static final String MAX_BADGES_SUCCESS = "commands.minestuck.godtier.maxbadges.success";
	public static final String MAX_BADGES_DEFAULT = "commands.minestuck.godtier.maxbadges.default";
	public static final String LUNAR_SWAY_SUCCESS = "commands.minestuck.godtier.lunarsway.success";
	public static final String ASCEND_SUCCESS = "commands.minestuck.godtier.ascend.success";
	public static final String ASCEND_NO_TITLE = "commands.minestuck.godtier.ascend.no_title";
	public static final String ASCEND_ALREADY = "commands.minestuck.godtier.ascend.already";
	public static final String BADGE_GRANT_SUCCESS = "commands.minestuck.godtier.badge.grant.success";
	public static final String BADGE_GRANT_FAILED = "commands.minestuck.godtier.badge.grant.failed";
	public static final String BADGE_REVOKE_SUCCESS = "commands.minestuck.godtier.badge.revoke.success";
	public static final String BADGE_REVOKE_FAILED = "commands.minestuck.godtier.badge.revoke.failed";
	public static final String BADGE_UNKNOWN = "commands.minestuck.godtier.badge.unknown";
	public static final String NO_PLAYER_DATA = "commands.minestuck.godtier.no_player_data";
	
	private static final SimpleCommandExceptionType WRONG_DIMENSION = new SimpleCommandExceptionType(Component.translatable(LOCATE_WRONG_DIMENSION));
	private static final SimpleCommandExceptionType NOT_FOUND = new SimpleCommandExceptionType(Component.translatable(LOCATE_NOT_FOUND));
	private static final SimpleCommandExceptionType NO_DATA = new SimpleCommandExceptionType(Component.translatable(NO_PLAYER_DATA));
	private static final DynamicCommandExceptionType NOT_GOD_TIER = new DynamicCommandExceptionType(name -> Component.translatable(RESET_NOT_GOD_TIER, name));
	private static final DynamicCommandExceptionType NO_TITLE = new DynamicCommandExceptionType(name -> Component.translatable(ASCEND_NO_TITLE, name));
	private static final DynamicCommandExceptionType ALREADY_GOD_TIER = new DynamicCommandExceptionType(name -> Component.translatable(ASCEND_ALREADY, name));
	private static final DynamicCommandExceptionType UNKNOWN_BADGE = new DynamicCommandExceptionType(id -> Component.translatable(BADGE_UNKNOWN, id));
	
	@FunctionalInterface
	private interface TargetAction
	{
		int run(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException;
	}
	
	private GodTierCommand()
	{
	}
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		dispatcher.register(Commands.literal("godtier").requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
				.then(Commands.literal("locate").executes(GodTierCommand::locate))
				.then(optionalTarget(Commands.literal("ascend"), GodTierCommand::ascend))
				.then(optionalTarget(Commands.literal("reset"), GodTierCommand::reset))
				.then(Commands.literal("title").then(optionalTarget(Commands.argument("title", TitleArgument.title()),
						(context, target) -> setTitle(context, target, TitleArgument.get(context, "title")))))
				.then(Commands.literal("mastercontrol").then(optionalTarget(Commands.argument("enabled", BoolArgumentType.bool()),
						(context, target) -> setMasterControl(context, target, BoolArgumentType.getBool(context, "enabled")))))
				.then(Commands.literal("maxbadges").then(optionalTarget(Commands.argument("count", IntegerArgumentType.integer(-1)),
						(context, target) -> setMaxBadges(context, target, IntegerArgumentType.getInteger(context, "count")))))
				.then(Commands.literal("lunarsway")
						.then(optionalTarget(Commands.literal("prospit"), (context, target) -> setLunarSway(context, target, GodTierState.LunarSway.PROSPIT)))
						.then(optionalTarget(Commands.literal("derse"), (context, target) -> setLunarSway(context, target, GodTierState.LunarSway.DERSE))))
				.then(Commands.literal("badge")
						.then(Commands.literal("grant").then(badgeArgument(true)))
						.then(Commands.literal("revoke").then(badgeArgument(false)))));
	}
	
	/**
	 * Lets the player that the command works on be left out, in which case it is the one running the command.
	 */
	private static ArgumentBuilder<CommandSourceStack, ?> optionalTarget(ArgumentBuilder<CommandSourceStack, ?> node, TargetAction action)
	{
		node.executes(context -> action.run(context, context.getSource().getPlayerOrException()));
		node.then(Commands.argument("target", EntityArgument.player())
				.executes(context -> action.run(context, EntityArgument.getPlayer(context, "target"))));
		return node;
	}
	
	private static ArgumentBuilder<CommandSourceStack, ?> badgeArgument(boolean grant)
	{
		return optionalTarget(Commands.argument("badge", ResourceLocationArgument.id())
				.suggests((context, builder) -> SharedSuggestionProvider.suggestResource(SkillRegistry.REGISTRY.keySet(), builder)),
				(context, target) -> {
					ResourceLocation id = ResourceLocationArgument.getId(context, "badge");
					return grant ? grantBadge(context, target, id) : revokeBadge(context, target, id);
				});
	}
	
	private static PlayerData playerData(ServerPlayer player) throws CommandSyntaxException
	{
		return PlayerData.get(player).orElseThrow(NO_DATA::create);
	}
	
	private static int locate(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
	{
		ServerLevel level = context.getSource().getLevel();
		if(LandTypePair.getTypes(level).isEmpty())
			throw WRONG_DIMENSION.create();
		BlockPos pos = GodTierTickHandler.getQuestBedOrigin(level);
		if(pos == null)
			throw NOT_FOUND.create();
		
		context.getSource().sendSuccess(() -> Component.translatable(LOCATE_SUCCESS, pos.getX(), pos.getZ()), false);
		return 1;
	}
	
	private static int ascend(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException
	{
		PlayerData data = playerData(target);
		Title title = Title.getTitle(data).orElseThrow(() -> NO_TITLE.create(target.getDisplayName()));
		GodTierState state = data.getData(MSAttachments.GOD_TIER_STATE);
		if(state.isGodTier())
			throw ALREADY_GOD_TIER.create(target.getDisplayName());
		
		state.setCanGodTier(true);
		GodTierAscensionHandler.ascend(target, title, state);
		context.getSource().sendSuccess(() -> Component.translatable(ASCEND_SUCCESS, target.getDisplayName()), true);
		return 1;
	}
	
	private static int reset(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException
	{
		if(!GodTierReset.reset(target))
			throw NOT_GOD_TIER.create(target.getDisplayName());
		
		context.getSource().sendSuccess(() -> Component.translatable(RESET_SUCCESS, target.getDisplayName()), true);
		return 1;
	}
	
	private static int setTitle(CommandContext<CommandSourceStack> context, ServerPlayer target, Title title) throws CommandSyntaxException
	{
		PlayerData data = playerData(target);
		//Unlike when a title is first chosen, this replaces a title that is already there
		data.setData(MSAttachments.TITLE, title);
		target.connection.send(TitleDataPacket.create(title));
		
		//The page badge only goes with the page class
		GodTierSkills skills = data.getData(MSAttachments.GOD_TIER_SKILLS);
		if(title.heroClass() != EnumClass.PAGE)
			skills.revokeSkill(SkillRegistry.BADGE_PAGE.get());
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> Component.translatable(TITLE_SUCCESS, target.getDisplayName(), title.asTextComponent()), true);
		return 1;
	}
	
	private static int setMasterControl(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean enabled) throws CommandSyntaxException
	{
		PlayerData data = playerData(target);
		data.getData(MSAttachments.GOD_TIER_STATE).setMasterControl(enabled);
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> Component.translatable(enabled ? MASTER_CONTROL_ON : MASTER_CONTROL_OFF, target.getDisplayName()), true);
		return 1;
	}
	
	/**
	 * @param count the amount of badge slots, or -1 for the amount set in the config
	 */
	private static int setMaxBadges(CommandContext<CommandSourceStack> context, ServerPlayer target, int count) throws CommandSyntaxException
	{
		PlayerData data = playerData(target);
		data.getData(MSAttachments.GOD_TIER_SKILLS).setMaxBadges(count);
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> count < 0
				? Component.translatable(MAX_BADGES_DEFAULT, target.getDisplayName())
				: Component.translatable(MAX_BADGES_SUCCESS, target.getDisplayName(), count), true);
		return 1;
	}
	
	private static int setLunarSway(CommandContext<CommandSourceStack> context, ServerPlayer target, GodTierState.LunarSway sway) throws CommandSyntaxException
	{
		PlayerData data = playerData(target);
		data.getData(MSAttachments.GOD_TIER_STATE).setLunarSway(sway);
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> Component.translatable(LUNAR_SWAY_SUCCESS, target.getDisplayName(), sway.name().toLowerCase()), true);
		return 1;
	}
	
	private static int grantBadge(CommandContext<CommandSourceStack> context, ServerPlayer target, ResourceLocation id) throws CommandSyntaxException
	{
		Skill skill = SkillRegistry.get(id);
		if(!(skill instanceof Badge badge))
			throw UNKNOWN_BADGE.create(id.toString());
		
		PlayerData data = playerData(target);
		if(!data.getData(MSAttachments.GOD_TIER_SKILLS).addSkill(skill))
		{
			context.getSource().sendFailure(Component.translatable(BADGE_GRANT_FAILED, target.getDisplayName(), skill.getDisplayName()));
			return 0;
		}
		if(target.level() instanceof ServerLevel level)
			badge.onBadgeUnlocked(level, target);
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> Component.translatable(BADGE_GRANT_SUCCESS, skill.getDisplayName(), target.getDisplayName()), true);
		return 1;
	}
	
	private static int revokeBadge(CommandContext<CommandSourceStack> context, ServerPlayer target, ResourceLocation id) throws CommandSyntaxException
	{
		Skill skill = SkillRegistry.get(id);
		if(!(skill instanceof Badge))
			throw UNKNOWN_BADGE.create(id.toString());
		
		PlayerData data = playerData(target);
		if(!data.getData(MSAttachments.GOD_TIER_SKILLS).revokeSkill(skill))
		{
			context.getSource().sendFailure(Component.translatable(BADGE_REVOKE_FAILED, target.getDisplayName(), skill.getDisplayName()));
			return 0;
		}
		GodTierTickHandler.sendDataPacket(target, data);
		
		context.getSource().sendSuccess(() -> Component.translatable(BADGE_REVOKE_SUCCESS, skill.getDisplayName(), target.getDisplayName()), true);
		return 1;
	}
}
