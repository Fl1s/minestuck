package com.mraof.minestuck.client.gui.godtier;

import com.mraof.minestuck.player.godtier.skill.HoardOfTheAlchemizerBadge;
import com.mraof.minestuck.player.godtier.skill.PatchOfTheHoarderBadge;
import com.mraof.minestuck.player.godtier.skill.StrifeBadge;
import com.mraof.minestuck.client.gui.GristSelectorScreen;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import com.mraof.minestuck.player.godtier.skill.BadgePage;
import com.mraof.minestuck.player.godtier.skill.BadgeOverlord;
import com.mraof.minestuck.player.godtier.skill.BadgeLevel;
import com.mraof.minestuck.network.GodTierConfigPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.network.GodTierMeditationPackets;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.EnumClass;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.player.godtier.skill.Badge;
import com.mraof.minestuck.player.godtier.skill.MasterBadge;
import com.mraof.minestuck.player.godtier.skill.Skill;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class GodTierMeditationScreen extends Screen
{
	public static final String TITLE_KEY = "minestuck.god_tier.meditation";
	public static final String LEVEL_KEY = "minestuck.god_tier.level";
	public static final String XP_KEY = "minestuck.god_tier.xp";
	public static final String BADGE_SLOTS_KEY = "minestuck.god_tier.badge_slots";
	public static final String SKILL_TOOLTIP_KEY = "minestuck.god_tier.skill.%s.tooltip";
	public static final String SKILL_DESC_KEY = "minestuck.god_tier.skill.%s.desc";
	public static final String DEFENSE_DESC2_KEY = "minestuck.god_tier.skill.defense.desc2";
	public static final String SKILL_NEXT_LEVEL_KEY = "minestuck.god_tier.skill.next_level";
	public static final String MASTER_BADGE_WARNING_KEY = "minestuck.god_tier.master_badge_warning";
	public static final String SHOW_BADGE_INFO_KEY = "minestuck.god_tier.show_badge_info";
	public static final String NO_BADGES_LEFT_KEY = "minestuck.god_tier.no_badges_left";
	public static final String BADGE_BLOCKED_KEY = "minestuck.god_tier.badge_blocked";
	public static final String BADGE_DISABLED_KEY = "minestuck.god_tier.badge_disabled";
	public static final String NEED_XP_KEY = "minestuck.god_tier.need_xp";
	public static final String GENERAL_MAX_KEY = "minestuck.god_tier.general_max";
	
	private static final ResourceLocation guiMeditation = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/god_tier_meditation.png");
	private static final ResourceLocation BADGE_LOCKED = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/badge_locked.png");
	private static final ResourceLocation BADGE_DISABLED = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/badge_disabled.png");
	
	private static final int X_SIZE = 256;
	private static final int Y_SIZE = 210;
	
	private static final int DEFAULT_COLOR = 0x80FF20;
	private static final GodTierStat[] STATS = {GodTierStat.DEFENSE, GodTierStat.ATTACK, GodTierStat.LUCK, GodTierStat.SPEED};
	
	private static final Map<EnumAspect, Integer> MAIN_COLORS = Map.ofEntries(Map.entry(EnumAspect.BREATH, 0x47E2FA), Map.entry(EnumAspect.LIGHT, 0xF6FA4E), Map.entry(EnumAspect.SPACE, 0xFAFAFA), Map.entry(EnumAspect.TIME, 0xFF2106), Map.entry(EnumAspect.LIFE, 0x72EB34), Map.entry(EnumAspect.VOID, 0x001856), Map.entry(EnumAspect.HEART, 0xBD1864), Map.entry(EnumAspect.HOPE, 0xFFDE55), Map.entry(EnumAspect.BLOOD, 0xB71015), Map.entry(EnumAspect.RAGE, 0x9C4DAC), Map.entry(EnumAspect.MIND, 0x06FFC9), Map.entry(EnumAspect.DOOM, 0x306800));
	
	private static final java.text.DecimalFormat STAT_FORMAT = new java.text.DecimalFormat("#.##");
	
	private int xOffset, yOffset;
	private boolean mouseClicked;
	private int clickTime;
	private boolean showExtra;
	
	private final List<Skill> badges = new ArrayList<>();
	private final List<Skill> masterBadges = new ArrayList<>();
	
	public GodTierMeditationScreen()
	{
		super(Component.translatable(TITLE_KEY));
	}
	
	@Override
	protected void init()
	{
		super.init();
		mouseClicked = false;
		clickTime = 0;
		xOffset = (width - X_SIZE) / 2;
		yOffset = (height - Y_SIZE) / 2;
		setupBadges();
	}
	
	private void setupBadges()
	{
		badges.clear();
		masterBadges.clear();
		
		for(Skill skill : SkillRegistry.REGISTRY)
		{
			if(!(skill instanceof Badge)) continue;
			if(!(hasMasterControl() || canAppearOnList(skill) || ClientPlayerData.hasSkill(skill.id()))) continue;
			
			if(skill instanceof MasterBadge) masterBadges.add(skill);
			else badges.add(skill);
		}
		badges.sort(Comparator.comparingInt(Skill::sortIndex));
		masterBadges.sort(Comparator.comparingInt(Skill::sortIndex));
	}
	
	private static boolean hasMasterControl()
	{
		return GodTierConfigPacket.ClientGodTierConfig.godTierMasterControl();
	}
	
	private static boolean canAppearOnList(Skill skill)
	{
		if(skill instanceof BadgeOverlord)
			return false;
		if(skill instanceof StrifeBadge)
			return StrifeBadge.isRelevant();
		if(skill instanceof PatchOfTheHoarderBadge)
			return PatchOfTheHoarderBadge.isSupported();
		if(skill instanceof BadgePage)
			return ClientPlayerData.getTitle() != null && ClientPlayerData.getTitle().heroClass() == EnumClass.PAGE;
		return true;
	}
	
	private static boolean isReadable(Skill skill)
	{
		if(skill instanceof BadgeOverlord)
			return ClientPlayerData.hasSkill(skill.id()) || (hasMasterControl() && ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL) >= BadgeOverlord.REQUIRED_LEVEL);
		if(skill instanceof BadgeLevel badgeLevel)
			return ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL) >= badgeLevel.requiredLevel();
		return true;
	}
	
	private static boolean isOverlordActive()
	{
		return ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_OVERLORD.get().id());
	}
	
	private int statX(int i)
	{
		return xOffset + (i % 2 == 0 ? 129 : 20);
	}
	
	private int statY(int i)
	{
		return yOffset + (i / 2 == 0 ? 63 : 99);
	}
	
	private int masterBadgeX(int i)
	{
		return xOffset + (X_SIZE - masterBadges.size() * 22) / 2 + i * 22;
	}
	
	private int masterBadgeY()
	{
		return yOffset + 124;
	}
	
	private int badgeRows()
	{
		return Math.max(2, (int) Math.ceil(badges.size() / 20F));
	}
	
	private int badgeX(int i)
	{
		return xOffset + (X_SIZE - ((badges.size() + 1) / badgeRows()) * 22) / 2 + (i / badgeRows()) * 22;
	}
	
	private int badgeY(int i)
	{
		return yOffset + 157 + (i % badgeRows()) * 22;
	}
	
	private boolean canUpgrade()
	{
		int maxGodTier = MinestuckConfig.SERVER.maxGodTier.get();
		if(maxGodTier >= 0 && ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL) >= maxGodTier) return false;
		
		return minecraft.player.isCreative() || minecraft.player.experienceLevel >= MinestuckConfig.SERVER.godTierXpThreshold.get();
	}
	
	@Override
	public void tick()
	{
		super.tick();
		//The server may change what the player has or can see (such as unlocking a master badge)
		if(minecraft != null && minecraft.player != null && minecraft.player.tickCount % 10 == 0)
			setupBadges();
	}
	
	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		renderTransparentBackground(guiGraphics);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		
		showExtra = hasShiftDown();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		EnumAspect aspect = ClientPlayerData.getTitle() == null ? null : ClientPlayerData.getTitle().heroAspect();
		int mainColor = aspect == null ? DEFAULT_COLOR : MAIN_COLORS.getOrDefault(aspect, DEFAULT_COLOR);
		int generalLevel = ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL);
		float generalFilled = Mth.clamp(ClientPlayerData.getGodTierXp(GodTierStat.GENERAL) / getXpToNext(GodTierStat.GENERAL), 0F, 1F);
		
		renderGeneralXp(guiGraphics, mainColor, generalLevel, generalFilled);
		renderTitle(guiGraphics);
		renderBadges(guiGraphics, mainColor);
		renderSkillXp(guiGraphics, mouseX, mouseY);
		renderStatTooltips(guiGraphics, mouseX, mouseY);
		renderBadgeTooltips(guiGraphics, mouseX, mouseY);
		
		if(mouseClicked) clickTime++;
		else clickTime = 0;
		if(clickTime > 20 && clickTime % 5 == 0) upgradeSkills(mouseX, mouseY);
	}
	
	private void renderGeneralXp(GuiGraphics guiGraphics, int mainColor, int level, float filled)
	{
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(getRed(mainColor), getGreen(mainColor), getBlue(mainColor), 1F);
		guiGraphics.blit(guiMeditation, xOffset + 37, yOffset + 10, 0, 0, 182, 5, 256, 256);
		guiGraphics.blit(guiMeditation, xOffset + 37, yOffset + 10, 0, 5, (int) (182 * filled), 5, 256, 256);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
		
		String text = String.valueOf(level);
		renderBorderedText(guiGraphics, xOffset + 36 - font.width(text), yOffset + 10, text, mainColor, 0);
	}
	
	private void renderTitle(GuiGraphics guiGraphics)
	{
		guiGraphics.blit(guiMeditation, xOffset + 40, yOffset + 15, 0, 223, 176, 33, 256, 256);
		String title = ClientPlayerData.getTitle() == null ? "" : ClientPlayerData.getTitle().asTextComponent().getString();
		guiGraphics.drawString(font, title, xOffset + X_SIZE / 2 - font.width(title) / 2, yOffset + 30, 0xFFFFFF, false);
	}
	
	private void renderBadges(GuiGraphics guiGraphics, int mainColor)
	{
		int badgesLeft = Math.max(0, ClientPlayerData.getBadgeLimit() - ClientPlayerData.getUnlockedSkillCount());
		Component slots = Component.translatable(BADGE_SLOTS_KEY, badgesLeft);
		guiGraphics.drawString(font, slots, xOffset + X_SIZE / 2 - font.width(slots) / 2, yOffset + 147, mainColor, false);
		
		for(int i = 0; i < masterBadges.size(); i++)
			renderBadge(guiGraphics, masterBadges.get(i), masterBadgeX(i), masterBadgeY());
		for(int i = 0; i < badges.size(); i++)
			renderBadge(guiGraphics, badges.get(i), badgeX(i), badgeY(i));
	}
	
	private void renderBadge(GuiGraphics guiGraphics, Skill skill, int x, int y)
	{
		boolean owned = ClientPlayerData.hasSkill(skill.id());
		boolean master = skill instanceof MasterBadge;
		ResourceLocation texture;
		
		if(!isReadable(skill))
			texture = BADGE_LOCKED;
		else if(master && !(ClientPlayerData.getMasterBadge() == null || owned || isOverlordActive()))
			texture = BADGE_LOCKED;
		else
		{
			texture = skill.getTextureLocation();
			if(!owned) RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 1F);
		}
		
		guiGraphics.blit(texture, x, y, 0, 0, 20, 20, 20, 20);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		
		if(owned && !ClientPlayerData.isSkillEnabled(skill.id()))
			guiGraphics.blit(BADGE_DISABLED, x, y, 0, 0, 20, 20, 20, 20);
	}
	
	private void renderSkillXp(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		for(int i = 0; i < STATS.length; i++)
		{
			GodTierStat stat = STATS[i];
			int skillX = statX(i);
			int skillY = statY(i);
			int level = ClientPlayerData.getGodTierLevel(stat);
			float filled = Mth.clamp(ClientPlayerData.getGodTierXp(stat) / getXpToNext(stat), 0F, 1F);
			
			guiGraphics.blit(guiMeditation, skillX + 10, skillY + 6, 0, 10, 77, 5, 256, 256);
			guiGraphics.blit(guiMeditation, skillX + 10, skillY + 6, 0, 15, (int) (77 * filled), 5, 256, 256);
			guiGraphics.blit(guiMeditation, skillX, skillY, 164 + (i + 1) * 18, 0, 18, 18, 256, 256);
			
			String levelText = String.valueOf(level);
			renderBorderedText(guiGraphics, skillX + 9 - font.width(levelText) / 2, skillY + 9 - font.lineHeight / 2, levelText, getLevelColor(), 0);
			
			int uOffset;
			if(!canUpgrade()) uOffset = 36;
			else if(isMouseOver(skillX + 89, skillY, 18, 18, mouseX, mouseY)) uOffset = mouseClicked ? 36 : 18;
			else uOffset = 0;
			guiGraphics.blit(guiMeditation, skillX + 89, skillY, uOffset, 20, 18, 18, 256, 256);
		}
	}
	
	private int getLevelColor()
	{
		if(ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_PAGE.get().id())) return 0xFFD84C;
		if(ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_OVERLORD.get().id())) return 0xFF0000;
		return DEFAULT_COLOR;
	}
	
	private void renderStatTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		for(int i = 0; i < STATS.length; i++)
		{
			GodTierStat stat = STATS[i];
			int skillX = statX(i);
			int skillY = statY(i);
			if(isMouseOver(skillX, skillY, 18, 18, mouseX, mouseY))
			{
				int level = ClientPlayerData.getGodTierLevel(stat);
				double multiplier = statMultiplier();
				double bonus = Math.pow(level, 0.765) * stat.attributeMod() * multiplier;
				if(stat.operation() != AttributeModifier.Operation.ADD_VALUE) bonus *= 100;
				
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable(SKILL_TOOLTIP_KEY.formatted(stat.getSerializedName()), level));
				tooltip.add(Component.translatable(SKILL_DESC_KEY.formatted(stat.getSerializedName()), STAT_FORMAT.format(bonus)));
				if(stat == GodTierStat.DEFENSE)
					tooltip.add(Component.translatable(DEFENSE_DESC2_KEY, STAT_FORMAT.format(level * 0.2 * multiplier)));
				tooltip.add(Component.translatable(SKILL_NEXT_LEVEL_KEY, STAT_FORMAT.format(Math.max(0, getXpToNext(stat) - ClientPlayerData.getGodTierXp(stat)))));
				guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
			} else if(isMouseOver(skillX + 89, skillY, 18, 18, mouseX, mouseY))
			{
				int maxGodTier = MinestuckConfig.SERVER.maxGodTier.get();
				if(!minecraft.player.isCreative() && minecraft.player.experienceLevel < MinestuckConfig.SERVER.godTierXpThreshold.get())
					guiGraphics.renderTooltip(font, Component.translatable(NEED_XP_KEY, MinestuckConfig.SERVER.godTierXpThreshold.get()), mouseX, mouseY);
				else if(maxGodTier >= 0 && ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL) >= maxGodTier)
					guiGraphics.renderTooltip(font, Component.translatable(GENERAL_MAX_KEY, maxGodTier), mouseX, mouseY);
			}
		}
	}
	
	private double statMultiplier()
	{
		double multiplier = 1;
		if(ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_PAGE.get().id())) multiplier *= 2;
		if(ClientPlayerData.isSkillEnabled(SkillRegistry.BADGE_OVERLORD.get().id())) multiplier *= 3;
		return multiplier;
	}
	
	private void renderBadgeTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		List<Component> tooltip = getBadgeTooltip(mouseX, mouseY);
		if(tooltip != null && !tooltip.isEmpty()) guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
	}
	
	@Nullable
	private List<Component> getBadgeTooltip(int mouseX, int mouseY)
	{
		for(int i = 0; i < masterBadges.size(); i++)
			if(isMouseOver(masterBadgeX(i), masterBadgeY(), 20, 20, mouseX, mouseY))
				return createBadgeTooltip(masterBadges.get(i), true);
		
		for(int i = 0; i < badges.size(); i++)
			if(isMouseOver(badgeX(i), badgeY(i), 20, 20, mouseX, mouseY))
				return createBadgeTooltip(badges.get(i), false);
		return null;
	}
	
	private List<Component> createBadgeTooltip(Skill skill, boolean master)
	{
		List<Component> tooltip = new ArrayList<>();
		boolean owned = ClientPlayerData.hasSkill(skill.id());
		Player player = minecraft.player;
		
		if(!isReadable(skill))
		{
			tooltip.add(skill.getDisplayName().copy().withStyle(ChatFormatting.OBFUSCATED));
			tooltip.add(skill.getReadRequirements());
		} else if(master)
		{
			if(ClientPlayerData.getMasterBadge() == null && !isOverlordActive())
			{
				tooltip.add(skill.getDisplayName());
				if(showExtra)
					tooltip.add(skill.getDisplayTooltip(player));
				else
				{
					tooltip.add(skill.getUnlockRequirements());
					tooltip.add(Component.translatable(MASTER_BADGE_WARNING_KEY));
					tooltip.add(Component.translatable(SHOW_BADGE_INFO_KEY));
				}
			} else if(owned || isOverlordActive())
			{
				tooltip.add(skill.getDisplayName());
				tooltip.add(skill.getDisplayTooltip(player));
				if(owned && !ClientPlayerData.isSkillEnabled(skill.id())) tooltip.add(Component.translatable(BADGE_DISABLED_KEY));
			}
		} else if(owned)
		{
			tooltip.add(skill.getDisplayName());
			tooltip.add(skill.getDisplayTooltip(player));
			if(!ClientPlayerData.isSkillEnabled(skill.id())) tooltip.add(Component.translatable(BADGE_DISABLED_KEY));
		} else
		{
			tooltip.add(skill.getDisplayName());
			if(showExtra)
				tooltip.add(skill.getDisplayTooltip(player));
			else
			{
				if(ClientPlayerData.getBadgeLimit() - ClientPlayerData.getUnlockedSkillCount() > 0)
					tooltip.add(skill.getUnlockRequirements());
				else tooltip.add(Component.translatable(NO_BADGES_LEFT_KEY));
				tooltip.add(Component.translatable(SHOW_BADGE_INFO_KEY));
			}
		}
		return tooltip;
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		mouseClicked = true;
		upgradeSkills((int) mouseX, (int) mouseY);
		clickBadges((int) mouseX, (int) mouseY, button);
		return result;
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button)
	{
		mouseClicked = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	private void upgradeSkills(int mouseX, int mouseY)
	{
		if(!canUpgrade()) return;
		for(int i = 0; i < STATS.length; i++)
		{
			if(isMouseOver(statX(i) + 89, statY(i), 18, 18, mouseX, mouseY))
			{
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
				int amount = showExtra ? 5 : 1;
				PacketDistributor.sendToServer(new GodTierMeditationPackets.UpgradeSkill(STATS[i], amount));
			}
		}
	}
	
	private void clickBadges(int mouseX, int mouseY, int button)
	{
		for(int i = 0; i < masterBadges.size(); i++)
			if(isMouseOver(masterBadgeX(i), masterBadgeY(), 20, 20, mouseX, mouseY))
			{
				Skill badge = masterBadges.get(i);
				if(isReadable(badge))
				{
					boolean unlock = !isOverlordActive() && !ClientPlayerData.hasSkill(badge.id()) && ClientPlayerData.getMasterBadge() == null && button == 0;
					sendBadgeClick(badge, unlock);
				}
				return;
			}
		
		for(int i = 0; i < badges.size(); i++)
			if(isMouseOver(badgeX(i), badgeY(i), 20, 20, mouseX, mouseY))
			{
				Skill badge = badges.get(i);
				if(isReadable(badge))
				{
					if(badge instanceof HoardOfTheAlchemizerBadge && ClientPlayerData.hasSkill(badge.id()) && button == 0)
					{
						mouseClicked = false;
						clickTime = 0;
						minecraft.setScreen(GristSelectorScreen.forGristHoard(this));
					} else sendBadgeClick(badge, !ClientPlayerData.hasSkill(badge.id()) && button == 0);
				}
				return;
			}
	}
	
	private void sendBadgeClick(Skill badge, boolean unlock)
	{
		if(unlock)
			PacketDistributor.sendToServer(new GodTierMeditationPackets.AttemptBadgeUnlock(badge.id()));
		else if(ClientPlayerData.hasSkill(badge.id()))
			PacketDistributor.sendToServer(new GodTierMeditationPackets.ToggleBadge(badge.id()));
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	private int getXpToNext(GodTierStat stat)
	{
		EnumClass heroClass = ClientPlayerData.getTitle() == null ? null : ClientPlayerData.getTitle().heroClass();
		if(heroClass == null) return 50;
		
		int x = ClientPlayerData.getGodTierLevel(stat) + 1;
		return Math.max(1, switch(heroClass)
		{
			case KNIGHT, PRINCE -> (int) (Math.exp((x - 1) * 0.04) * 50);
			case HEIR, MAID -> (int) Math.min(2500, Math.pow(x - 1, 2) + 50);
			case MUSE, SEER, MAGE -> (int) (4 * Math.sin(x - 1) + 2 * x) * 5 + 50;
			case BARD, WITCH, SYLPH -> (int) (x * 12 + Math.sin(2 * (x - 1)) * 10 + 25);
			case THIEF, ROGUE -> 10 * Math.min(x + (x % 10), x + ((10 - x) % 10)) + 50;
			case PAGE -> 14 * x + 50;
			case LORD -> 50 * x;
		});
	}
	
	private float getRed(int color)
	{
		return ((color >> 16) & 0xFF) / 255F;
	}
	
	private float getGreen(int color)
	{
		return ((color >> 8) & 0xFF) / 255F;
	}
	
	private float getBlue(int color)
	{
		return (color & 0xFF) / 255F;
	}
	
	private void renderBorderedText(GuiGraphics guiGraphics, int x, int y, String text, int color, int borderColor)
	{
		guiGraphics.drawString(font, text, x + 1, y, borderColor, false);
		guiGraphics.drawString(font, text, x - 1, y, borderColor, false);
		guiGraphics.drawString(font, text, x, y + 1, borderColor, false);
		guiGraphics.drawString(font, text, x, y - 1, borderColor, false);
		guiGraphics.drawString(font, text, x, y, color, false);
	}
	
	private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY)
	{
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}