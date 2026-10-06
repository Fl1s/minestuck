package com.mraof.minestuck.client.gui.godtier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.network.GodTierMeditationPackets;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.EnumAspect;
import com.mraof.minestuck.player.godtier.GodTierStat;
import com.mraof.minestuck.player.godtier.skill.Badge;
import com.mraof.minestuck.player.godtier.skill.MasterBadge;
import com.mraof.minestuck.player.godtier.skill.Skill;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

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
	public static final String SKILL_NEXT_LEVEL_KEY = "minestuck.god_tier.skill.next_level";
	public static final String MASTER_BADGE_WARNING_KEY = "minestuck.god_tier.master_badge_warning";
	public static final String SHOW_BADGE_INFO_KEY = "minestuck.god_tier.show_badge_info";
	public static final String NO_BADGES_LEFT_KEY = "minestuck.god_tier.no_badges_left";
	public static final String BADGE_BLOCKED_KEY = "minestuck.god_tier.badge_blocked";
	public static final String BADGE_DISABLED_KEY = "minestuck.god_tier.badge_disabled";
	public static final String NEED_XP_KEY = "minestuck.god_tier.need_xp";
	public static final String GENERAL_MAX_KEY = "minestuck.god_tier.general_max";
	private static final ResourceLocation guiMeditation = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/god_tier_meditation.png");
	private static final ResourceLocation guiMeditationMockup = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/god_tier_meditation_mockup.png");
	
	private static final int X_SIZE = 256;
	private static final int Y_SIZE = 210;
	
	private static final Map<EnumAspect, Integer> MAIN_COLORS = Map.ofEntries(Map.entry(EnumAspect.BREATH, 0x47E2FA), Map.entry(EnumAspect.LIGHT, 0xF6FA4E), Map.entry(EnumAspect.SPACE, 0xFAFAFA), Map.entry(EnumAspect.TIME, 0xFF2106), Map.entry(EnumAspect.LIFE, 0x72EB34), Map.entry(EnumAspect.VOID, 0x001856), Map.entry(EnumAspect.HEART, 0xBD1864), Map.entry(EnumAspect.HOPE, 0xFFDE55), Map.entry(EnumAspect.BLOOD, 0xB71015), Map.entry(EnumAspect.RAGE, 0x9C4DAC), Map.entry(EnumAspect.MIND, 0x06FFC9), Map.entry(EnumAspect.DOOM, 0x306800));
	
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
			if(!ClientPlayerData.hasSkill(skill.id())) continue;
			if(skill instanceof MasterBadge) masterBadges.add(skill);
			else if(skill instanceof Badge) badges.add(skill);
		}
		badges.sort(Comparator.comparingInt(Skill::sortIndex));
		masterBadges.sort(Comparator.comparingInt(Skill::sortIndex));
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
	{
		renderTransparentBackground(guiGraphics);
		guiGraphics.blit(guiMeditationMockup, xOffset, yOffset, 0, 0, X_SIZE, Y_SIZE, 256, 256);
		
		EnumAspect aspect = ClientPlayerData.getTitle() == null ? null : ClientPlayerData.getTitle().heroAspect();
		int mainColor = aspect == null ? 0x80FF20 : MAIN_COLORS.getOrDefault(aspect, 0x80FF20);
		int generalLevel = ClientPlayerData.getGodTierLevel(GodTierStat.GENERAL);
		float generalXp = ClientPlayerData.getGodTierXp(GodTierStat.GENERAL);
		float generalFilled = Mth.clamp(generalXp / 50F, 0F, 1F);
		
		renderGeneralXp(guiGraphics, mainColor, generalLevel, generalFilled);
		renderTitle(guiGraphics);
		renderBadges(guiGraphics, mouseX, mouseY);
		renderSkillXp(guiGraphics, mouseX, mouseY);
		renderTooltips(guiGraphics, mouseX, mouseY);
		
		super.render(guiGraphics, mouseX, mouseY, partialTick);
		
		if(mouseClicked) clickTime++;
		else clickTime = 0;
		if(clickTime > 20 && clickTime % 5 == 0) upgradeSkills(mouseX, mouseY);
	}
	
	private void renderGeneralXp(GuiGraphics guiGraphics, int mainColor, int level, float filled)
	{
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(getRed(mainColor), getGreen(mainColor), getBlue(mainColor), 1F);
		guiGraphics.blit(guiMeditation, xOffset + 37, yOffset + 10, 0, 0, 182, 5, 256, 256);
		guiGraphics.blit(guiMeditation, xOffset + 37, yOffset + 10, 0, 5, (int) (182 * filled), 5);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
		
		String text = String.valueOf(level);
		renderBorderedText(guiGraphics, xOffset + 36 - font.width(text), yOffset + 10, text, mainColor);
	}
	
	private void renderTitle(GuiGraphics guiGraphics)
	{
		guiGraphics.blit(guiMeditation, xOffset + 40, yOffset + 15, 0, 223, 176, 33, 256, 256);
		String title = ClientPlayerData.getTitle() == null ? "" : ClientPlayerData.getTitle().asTextComponent().getString();
		guiGraphics.drawCenteredString(font, title, xOffset + X_SIZE / 2, yOffset + 30, 0xFFFFFF);
	}
	
	private void renderBadges(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		int badgesLeft = ClientPlayerData.getBadgeLimit() - (int) badges.stream().filter(b -> ClientPlayerData.isSkillEnabled(b.id())).count();
		guiGraphics.drawCenteredString(font, Component.translatable(BADGE_SLOTS_KEY, badgesLeft), xOffset + X_SIZE / 2, yOffset + 147, 0xFFFFFF);
		
		renderBadgeList(guiGraphics, masterBadges, yOffset + 124);
		int rows = Math.max(2, (int) Math.ceil(badges.size() / 20F));
		for(int i = 0; i < badges.size(); i++)
		{
			int x = xOffset + (X_SIZE - ((badges.size() + 1) / rows) * 22) / 2 + (i / rows) * 22;
			int y = yOffset + 157 + (i % rows) * 22;
			renderBadge(guiGraphics, badges.get(i), x, y);
		}
	}
	
	private void renderBadgeList(GuiGraphics guiGraphics, List<Skill> list, int y)
	{
		for(int i = 0; i < list.size(); i++)
		{
			int x = xOffset + (X_SIZE - list.size() * 22) / 2 + i * 22;
			renderBadge(guiGraphics, list.get(i), x, y);
		}
	}
	
	private void renderBadge(GuiGraphics guiGraphics, Skill skill, int x, int y)
	{
		ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/skills/badges/" + skill.id().getPath() + ".png");
		guiGraphics.blit(texture, x, y, 0, 0, 20, 20, 256, 256);
		if(!ClientPlayerData.isSkillEnabled(skill.id()))
			guiGraphics.blit(ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/badge_disabled.png"), x, y, 0, 0, 20, 20, 256, 256);
	}
	
	private void renderSkillXp(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		GodTierStat[] stats = {GodTierStat.DEFENSE, GodTierStat.ATTACK, GodTierStat.LUCK, GodTierStat.SPEED};
		for(int i = 0; i < stats.length; i++)
		{
			int skillX = xOffset + (i % 2 == 0 ? 129 : 20);
			int skillY = yOffset + (i / 2 == 0 ? 63 : 99);
			int level = ClientPlayerData.getGodTierLevel(stats[i]);
			float xp = ClientPlayerData.getGodTierXp(stats[i]);
			float filled = Mth.clamp(xp / getXpToNext(stats[i]), 0F, 1F);
			
			guiGraphics.blit(guiMeditation, skillX + 10, skillY + 6, 0, 10, 77, 5, 256, 256);
			guiGraphics.blit(guiMeditation, skillX + 10, skillY + 6, 0, 15, (int) (77 * filled), 5);
			guiGraphics.blit(guiMeditation, skillX, skillY, 164 + (i + 1) * 18, 0, 18, 18, 256, 256);
			guiGraphics.drawCenteredString(font, String.valueOf(level), skillX + 9, skillY + 6, 0xFFFFFF);
			
			boolean canUpgrade = minecraft.player.isCreative() || minecraft.player.experienceLevel >= 30;
			int uOffset;
			if(!canUpgrade) uOffset = 36;
			else if(isMouseOver(skillX + 89, skillY, 18, 18, mouseX, mouseY)) uOffset = mouseClicked ? 36 : 18;
			else uOffset = 0;
			guiGraphics.blit(guiMeditation, skillX + 89, skillY, uOffset, 20, 18, 18, 256, 256);
		}
	}
	
	private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		GodTierStat[] stats = {GodTierStat.DEFENSE, GodTierStat.ATTACK, GodTierStat.LUCK, GodTierStat.SPEED};
		for(int i = 0; i < stats.length; i++)
		{
			int skillX = xOffset + (i % 2 == 0 ? 129 : 20);
			int skillY = yOffset + (i / 2 == 0 ? 63 : 99);
			if(isMouseOver(skillX, skillY, 18, 18, mouseX, mouseY))
			{
				List<Component> tooltip = new ArrayList<>();
				tooltip.add(Component.translatable(SKILL_TOOLTIP_KEY.formatted(stats[i].getSerializedName()), ClientPlayerData.getGodTierLevel(stats[i])));
				tooltip.add(Component.translatable(SKILL_NEXT_LEVEL_KEY, ClientPlayerData.getGodTierXp(stats[i])));
				guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
			}
		}
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
		GodTierStat[] stats = {GodTierStat.DEFENSE, GodTierStat.ATTACK, GodTierStat.LUCK, GodTierStat.SPEED};
		for(int i = 0; i < stats.length; i++)
		{
			GodTierStat stat = stats[i];
			int skillX = xOffset + (i % 2 == 0 ? 129 : 20);
			int skillY = yOffset + (i / 2 == 0 ? 63 : 99);
			if((minecraft.player.isCreative() || minecraft.player.experienceLevel >= 30) && isMouseOver(skillX + 89, skillY, 18, 18, mouseX, mouseY))
				PacketDistributor.sendToServer(new GodTierMeditationPackets.UpgradeSkill(stat));
		}
	}
	
	private int getXpToNext(GodTierStat stat)
	{
		return Math.max(1, (int) (50 * Math.pow(1.1, ClientPlayerData.getGodTierLevel(stat))));
	}
	
	private void clickBadges(int mouseX, int mouseY, int button)
	{
		for(int i = 0; i < masterBadges.size(); i++)
		{
			int x = xOffset + (X_SIZE - masterBadges.size() * 22) / 2 + i * 22;
			int y = yOffset + 124;
			if(isMouseOver(x, y, 20, 20, mouseX, mouseY))
			{
				Skill badge = masterBadges.get(i);
				if(ClientPlayerData.hasSkill(badge.id()))
					PacketDistributor.sendToServer(new GodTierMeditationPackets.ToggleBadge(badge.id()));
				else if(button == 0)
					PacketDistributor.sendToServer(new GodTierMeditationPackets.AttemptBadgeUnlock(badge.id()));
				return;
			}
		}
		
		int rows = Math.max(2, (int) Math.ceil(badges.size() / 20F));
		for(int i = 0; i < badges.size(); i++)
		{
			int x = xOffset + (X_SIZE - ((badges.size() + 1) / rows) * 22) / 2 + (i / rows) * 22;
			int y = yOffset + 157 + (i % rows) * 22;
			if(isMouseOver(x, y, 20, 20, mouseX, mouseY))
			{
				Skill badge = badges.get(i);
				if(ClientPlayerData.hasSkill(badge.id()))
					PacketDistributor.sendToServer(new GodTierMeditationPackets.ToggleBadge(badge.id()));
				else if(button == 0)
					PacketDistributor.sendToServer(new GodTierMeditationPackets.AttemptBadgeUnlock(badge.id()));
				return;
			}
		}
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
	
	private void renderBorderedText(GuiGraphics guiGraphics, int x, int y, String text, int color)
	{
		guiGraphics.drawString(font, text, x + 1, y, 0);
		guiGraphics.drawString(font, text, x - 1, y, 0);
		guiGraphics.drawString(font, text, x, y + 1, 0);
		guiGraphics.drawString(font, text, x, y - 1, 0);
		guiGraphics.drawString(font, text, x, y, color);
	}
	
	private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY)
	{
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) showExtra = true;
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers)
	{
		if(keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) showExtra = false;
		return super.keyReleased(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
