package com.mraof.minestuck.client.gui.godtier;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.network.GodTierMeditationPackets;
import com.mraof.minestuck.player.ClientPlayerData;
import com.mraof.minestuck.player.godtier.skill.Badge;
import com.mraof.minestuck.player.godtier.skill.MasterBadge;
import com.mraof.minestuck.player.godtier.skill.Skill;
import com.mraof.minestuck.player.godtier.skill.SkillRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GodTierBadgeScreen extends Screen
{
	public static final String TITLE_KEY = "minestuck.god_tier.manage_badges";
	
	private static final ResourceLocation BADGE_DISABLED = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/badge_disabled.png");
	private static final ResourceLocation SASH_DERSE = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/sash_derse.png");
	private static final ResourceLocation SASH_PROSPIT = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/gui/sash_prospit.png");
	private static final ResourceLocation CURSOR = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "textures/item/sash_kit.png");
	
	private static final int X_SIZE = 240;
	private static final float BADGES_PER_ROW = 16;
	
	private final List<Skill> badges = new ArrayList<>();
	private final List<Skill> masterBadges = new ArrayList<>();
	@Nullable
	private Skill hoveredBadge;
	private boolean showExtra;
	
	public GodTierBadgeScreen()
	{
		super(Component.translatable(TITLE_KEY));
	}
	
	@Override
	protected void init()
	{
		super.init();
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
		
		renderSash(guiGraphics, mouseX, mouseY);
		renderBadges(guiGraphics, mouseX, mouseY);
		renderCursor(guiGraphics, mouseX, mouseY);
		
		hoveredBadge = getHoveredBadge(mouseX, mouseY);
		if(hoveredBadge != null)
			guiGraphics.renderComponentTooltip(font, createTooltip(hoveredBadge), mouseX, mouseY);
	}
	
	private void renderSash(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		// TODO: replace with synced lunar sway once GodTierState packet includes it
		ResourceLocation sash = ClientPlayerData.getGodTierKarma() >= 0 ? SASH_PROSPIT : SASH_DERSE;
		int xOffset = (width - X_SIZE) / 2;
		int yOffset = height / 2 - 22 - 38;
		guiGraphics.blit(sash, xOffset, yOffset, 0, 0, X_SIZE, 142, 256, 256);
	}
	
	private void renderBadges(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		int yOffset = height / 2 - 22;
		int xOffset = (width - X_SIZE) / 2;
		
		for(int i = 0; i < masterBadges.size(); i++)
		{
			int x = xOffset + (X_SIZE - masterBadges.size() * 22) / 2 + i * 22;
			renderBadge(guiGraphics, masterBadges.get(i), x, yOffset - 23);
		}
		
		int rows = (int) Math.max(2, Math.ceil(badges.size() / BADGES_PER_ROW));
		for(int i = 0; i < badges.size(); i++)
		{
			int x = xOffset + (X_SIZE - ((badges.size() + 1) / rows) * 22) / 2 + (i / rows) * 22;
			int y = yOffset + (i % rows) * 22;
			renderBadge(guiGraphics, badges.get(i), x, y);
		}
	}
	
	private void renderBadge(GuiGraphics guiGraphics, Skill skill, int x, int y)
	{
		guiGraphics.blit(skill.getTextureLocation(), x, y, 0, 0, 20, 20, 20, 20);
		if(!ClientPlayerData.isSkillEnabled(skill.id()))
			guiGraphics.blit(BADGE_DISABLED, x, y, 0, 0, 20, 20, 20, 20);
	}
	
	private void renderCursor(GuiGraphics guiGraphics, int mouseX, int mouseY)
	{
		guiGraphics.blit(CURSOR, mouseX, mouseY, 0, 0, 16, 16, 16, 16);
	}
	
	@Nullable
	private Skill getHoveredBadge(int mouseX, int mouseY)
	{
		int yOffset = height / 2 - 22;
		int xOffset = (width - X_SIZE) / 2;
		
		for(int i = 0; i < masterBadges.size(); i++)
		{
			int x = xOffset + (X_SIZE - masterBadges.size() * 22) / 2 + i * 22;
			if(isMouseOver(x, yOffset - 23, 20, 20, mouseX, mouseY))
				return masterBadges.get(i);
		}
		
		int rows = (int) Math.max(2, Math.ceil(badges.size() / BADGES_PER_ROW));
		for(int i = 0; i < badges.size(); i++)
		{
			int x = xOffset + (X_SIZE - ((badges.size() + 1) / rows) * 22) / 2 + (i / rows) * 22;
			int y = yOffset + (i % rows) * 22;
			if(isMouseOver(x, y, 20, 20, mouseX, mouseY))
				return badges.get(i);
		}
		return null;
	}
	
	private List<Component> createTooltip(Skill skill)
	{
		List<Component> tooltip = new ArrayList<>();
		tooltip.add(skill.getDisplayName());
		tooltip.add(ClientPlayerData.hasSkill(skill.id()) ? skill.getDisplayTooltip() : skill.getUnlockRequirements());
		if(!ClientPlayerData.isSkillEnabled(skill.id()))
			tooltip.add(Component.translatable(GodTierMeditationScreen.BADGE_DISABLED_KEY));
		if(showExtra)
			tooltip.add(skill.getReadRequirements());
		return tooltip;
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button)
	{
		boolean result = super.mouseClicked(mouseX, mouseY, button);
		if(hoveredBadge != null && ClientPlayerData.hasSkill(hoveredBadge.id()))
			PacketDistributor.sendToServer(new GodTierMeditationPackets.ToggleBadge(hoveredBadge.id()));
		return result;
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
	
	private boolean isMouseOver(int x, int y, int width, int height, int mouseX, int mouseY)
	{
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
