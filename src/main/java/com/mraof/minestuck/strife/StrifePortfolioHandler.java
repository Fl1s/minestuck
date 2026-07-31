package com.mraof.minestuck.strife;

import com.mraof.minestuck.MinestuckConfig;
import com.mraof.minestuck.item.MSItems;
import com.mraof.minestuck.item.StrifeCardItem;
import com.mraof.minestuck.item.components.MSItemComponents;
import com.mraof.minestuck.network.StrifePackets;
import com.mraof.minestuck.player.KindAbstratusType;
import com.mraof.minestuck.player.StrifePortfolioData;
import com.mraof.minestuck.player.StrifeSpecibus;
import com.mraof.minestuck.util.MSAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * Server-side helper that encapsulates all mutations to a player's Strife Portfolio.
 * Every method that changes portfolio state MUST (!!!) call {@link #syncToClient} at the end.
 */
public final class StrifePortfolioHandler
{
	
	public static StrifePortfolioData getData(Player player)
	{
		return player.getData(MSAttachments.STRIFE_PORTFOLIO.get());
	}
	
	public static boolean isFull(Player player)
	{
		return getData(player).isPortfolioFull();
	}
	
	public static boolean isEmpty(Player player)
	{
		return getData(player).isPortfolioEmpty();
	}
	
	/** Returns true when the ItemStack has been drawn from a strife deck. */
	public static boolean isHeldWeapon(Player player, ItemStack stack)
	{
		if(stack.isEmpty()) return false;
		StrifePortfolioData data = getData(player);
		if(!data.isArmed() || data.getSelectedSpecibusIndex() < 0 || data.getSelectedWeaponIndex() < 0)
			return false;
		
		StrifeSpecibus selected = data.getSelectedSpecibus();
		if(selected == null || data.getSelectedWeaponIndex() >= selected.getContents().size())
			return false;
		
		return StrifeSpecibus.sameWeapon(selected.getContents().get(data.getSelectedWeaponIndex()), stack);
	}
	
	
	public static void syncToClient(ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(player, new StrifePackets.SyncPortfolioPacket(getData(player)));
	}
	
	public static boolean addSpecibus(ServerPlayer player, StrifeSpecibus specibus)
	{
		StrifePortfolioData data = getData(player);
		
		if(data.isPortfolioFull())
		{
			player.displayClientMessage(Component.translatable("status.strife.portfolioFull"), true);
			return false;
		}
		if(specibus.isAssigned() && data.portfolioHasAbstratus(specibus.getAbstratusName()))
		{
			player.displayClientMessage(
					Component.translatable("status.strife.portfolioDuplicate",
							specibus.getDisplayName()), true);
			return false;
		}
		
		data.addSpecibus(specibus);
		
		if(specibus.isAssigned())
			player.displayClientMessage(
					Component.translatable("status.strife.assign", specibus.getDisplayName()), true);
		
		syncToClient(player);
		return true;
	}
	
	/**
	 * Tries to assign the item in the player's hand to any compatible specibus slot.
	 * If the card is blank, an {@link StrifePackets.OpenStrifeCardGuiPacket} is sent instead.
	 */
	public static void assignStrife(ServerPlayer player, InteractionHand hand)
	{
		ItemStack stack = player.getItemInHand(hand);
		
		if(stack.getItem() instanceof StrifeCardItem)
		{
			StrifeSpecibus specibus = stack.get(MSItemComponents.STRIFE_SPECIBUS_DATA.get());
			if(specibus != null)
			{
				if(addSpecibus(player, specibus))
					stack.shrink(1);
			}
			else
			{
				// Blank card – open the abstrata-selection GUI on client
				PacketDistributor.sendToPlayer(player, new StrifePackets.OpenStrifeCardGuiPacket(hand));
			}
		}
		else
		{
			// Non-card item: try to put it in a weapon deck
			if(addWeapon(player, stack, true))
			{
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			}
		}
	}
	
	/**
	 * Removes the specibus at {@code index} from the portfolio, wraps it in a
	 * {@link StrifeCardItem} and gives it to the player (or drops it).
	 */
	public static void retrieveCard(ServerPlayer player, int index)
	{
		StrifePortfolioData data = getData(player);
		
		// If this slot is currently armed, disarm before removing
		if(data.isArmed() && data.getSelectedSpecibusIndex() == index)
			clearArmedWeapon(player, data);
		
		StrifeSpecibus removed = data.removeSpecibus(index);
		if(removed == null) return;
		
		ItemStack card = createStrifeCard(removed);
		
		net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
		boolean hasRoom = inventory.getSlotWithRemainingSpace(card) >= 0 || inventory.getFreeSlot() >= 0;
		
		if(hasRoom)
			player.addItem(card);
		else
			player.drop(card, false);
		
		syncToClient(player);
	}
	
	/** Convenience overload that always sends status messages. */
	public static boolean addWeapon(ServerPlayer player, ItemStack stack)
	{
		return addWeapon(player, stack, true);
	}
	
	/**
	 * Finds the first compatible specibus slot (selected first, then others) and
	 * adds a copy of {@code stack} to its deck.
	 *
	 * <p>Respects {@code strifeDeckMaxSize} config option.</p>
	 */
	public static boolean addWeapon(ServerPlayer player, ItemStack stack, boolean sendMessage)
	{
		if(stack.isEmpty()) return false;
		StrifePortfolioData data  = getData(player);
		int maxSize = MinestuckConfig.SERVER.strifeDeckMaxSize.get();
		
		StrifeSpecibus fullButCompatible = null;
		
		// 1 - try the selected slot first
		StrifeSpecibus selected = data.getSelectedSpecibus();
		if(selected != null)
		{
			KindAbstratusType type = selected.getKindAbstratus();
			if(type != null && type.partOf(stack))
			{
				if(maxSize >= 0 && selected.getContents().size() >= maxSize)
				{
					fullButCompatible = selected;
				}
				else if(selected.putItemStack(stack))
				{
					if(sendMessage)
						player.displayClientMessage(
								Component.translatable("status.strife.assignWeapon",
										stack.getHoverName(), selected.getDisplayName()), true);
					syncToClient(player);
					return true;
				}
			}
		}
		
		// 2 – try remaining slots
		StrifeSpecibus[] portfolio = data.getPortfolio();
		for(int i = 0; i < StrifePortfolioData.PORTFOLIO_SIZE; i++)
		{
			StrifeSpecibus sp = portfolio[i];
			if(sp == null || sp == selected) continue;
			KindAbstratusType type = sp.getKindAbstratus();
			if(type == null || !type.partOf(stack)) continue;
			
			if(maxSize >= 0 && sp.getContents().size() >= maxSize)
			{
				if(fullButCompatible == null) fullButCompatible = sp;
				continue;
			}
			if(sp.putItemStack(stack))
			{
				if(sendMessage)
					player.displayClientMessage(
							Component.translatable("status.strife.assignWeapon",
									stack.getHoverName(), sp.getDisplayName()), true);
				syncToClient(player);
				return true;
			}
		}
		
		// 3 – failure feedback
		if(sendMessage)
		{
			if(fullButCompatible != null)
				player.displayClientMessage(
						Component.translatable("status.strife.strifeDeckFull",
								fullButCompatible.getDisplayName()), true);
			else
				player.displayClientMessage(
						Component.translatable("status.strife.weaponMismatch",
								stack.getHoverName()), true);
		}
		return false;
	}
	
	/**
	 * Called from the armed tick when the player places a new (non-assigned) item
	 * in their main hand while armed.  Attempts to find the item a compatible slot
	 * and – if found – relocates the arm from the old slot to the new one.
	 *
	 * @return the specibus slot the item was moved into, or {@code null}
	 */
	@Nullable
	public static StrifeSpecibus moveSelectedWeapon(ServerPlayer player, ItemStack newStack)
	{
		StrifePortfolioData data = getData(player);
		int maxSize = MinestuckConfig.SERVER.strifeDeckMaxSize.get();
		StrifeSpecibus selSp = data.getSelectedSpecibus();
		
		StrifeSpecibus[] portfolio = data.getPortfolio();
		
		// Try selected slot first
		if(selSp != null)
		{
			KindAbstratusType type = selSp.getKindAbstratus();
			if(type != null && type.partOf(newStack)
					&& (maxSize < 0 || selSp.getContents().size() < maxSize))
			{
				return armFromDeck(player, data, selSp, data.getSelectedSpecibusIndex(), newStack);
			}
		}
		
		// Try other slots
		for(int i = 0; i < StrifePortfolioData.PORTFOLIO_SIZE; i++)
		{
			StrifeSpecibus sp = portfolio[i];
			if(sp == null || sp == selSp) continue;
			KindAbstratusType type = sp.getKindAbstratus();
			if(type == null || !type.partOf(newStack)) continue;
			if(maxSize >= 0 && sp.getContents().size() >= maxSize) continue;
			
			return armFromDeck(player, data, sp, i, newStack);
		}
		
		return null;
	}
	
	/**
	 * Adds a copy of {@code newStack} to {@code sp}'s deck, then immediately draws a copy of it back
	 * into the player's main hand as the new armed weapon.
	 */
	private static StrifeSpecibus armFromDeck(ServerPlayer player, StrifePortfolioData data, StrifeSpecibus sp, int specibusIndex, ItemStack newStack)
	{
		ItemStack deckCopy = newStack.copy();
		sp.getContents().add(deckCopy);
		int weaponIndex = sp.getContents().size() - 1;
		
		player.setItemInHand(InteractionHand.MAIN_HAND, deckCopy.copy());
		
		data.setSelectedSpecibusIndex(specibusIndex);
		data.setSelectedWeaponIndex(weaponIndex);
		data.setArmed(true);
		syncToClient(player);
		return sp;
	}
	
	/**
	 * Toggles the "armed" state for the weapon at {@code weaponIndex} of the
	 * currently selected specibus slot.
	 *
	 * <ul>
	 *   <li>Hand occupied by a real (unrelated) item → does nothing.</li>
	 *   <li>Hand empty or holding the armed weapon → arm / disarm.</li>
	 * </ul>
	 */
	public static void retrieveWeapon(ServerPlayer player, int weaponIndex, InteractionHand hand)
	{
		StrifePortfolioData data = getData(player);
		StrifeSpecibus selSp = data.getSelectedSpecibus();
		if(selSp == null) return;
		
		ItemStack heldItem = player.getItemInHand(hand);
		boolean handEmpty = heldItem.isEmpty();
		boolean handIsArmedWeapon = isHeldWeapon(player, heldItem);
		
		if(data.isArmed() && data.getSelectedWeaponIndex() == weaponIndex && handIsArmedWeapon)
		{
			player.setItemInHand(hand, ItemStack.EMPTY);
			data.setArmed(false);
			syncToClient(player);
			return;
		}
		
		if(!handEmpty && !handIsArmedWeapon) return;
		
		if(handIsArmedWeapon)
			player.setItemInHand(hand, ItemStack.EMPTY);
		
		if(weaponIndex < 0 || weaponIndex >= selSp.getContents().size())
		{
			data.setArmed(false);
			syncToClient(player);
			return;
		}
		
		ItemStack weapon = selSp.getContents().get(weaponIndex).copy();
		player.setItemInHand(hand, weapon);
		data.setSelectedWeaponIndex(weaponIndex);
		data.setArmed(true);
		syncToClient(player);
	}
	
	/**
	 * Moves a weapon from a specibus deck slot into the player's offhand
	 * (and tries to assign the current offhand item to the portfolio in return).
	 */
	public static void swapOffhandWeapon(ServerPlayer player, int specibusIndex, int weaponIndex)
	{
		StrifePortfolioData data = getData(player);
		StrifeSpecibus sp = data.getPortfolio()[specibusIndex];
		if(sp == null) return;
		
		ItemStack weapon = sp.retrieveStack(weaponIndex);
		if(weapon.isEmpty()) return;
		
		if(data.isArmed()
				&& data.getSelectedSpecibusIndex() == specibusIndex
				&& data.getSelectedWeaponIndex()   == weaponIndex)
		{
			data.setArmed(false);
			ItemStack mainHand = player.getMainHandItem();
			if(StrifeSpecibus.sameWeapon(weapon, mainHand))
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		}
		
		ItemStack currentOffhand = player.getItemInHand(InteractionHand.OFF_HAND);
		if(currentOffhand.isEmpty() || addWeapon(player, currentOffhand, false))
		{
			player.setItemInHand(InteractionHand.OFF_HAND, weapon);
			syncToClient(player);
		}
	}
	
	/**
	 * Removes the currently selected weapon from the active specibus deck and
	 * disarms the player.
	 */
	public static void unassignSelected(ServerPlayer player)
	{
		StrifePortfolioData data = getData(player);
		StrifeSpecibus selSp = data.getSelectedSpecibus();
		if(selSp == null) return;
		
		int wIdx = data.getSelectedWeaponIndex();
		
		if(data.isArmed())
		{
			clearArmedWeapon(player, data);
		}
		
		selSp.unassign(wIdx);
		if(wIdx >= selSp.getContents().size())
			data.setSelectedWeaponIndex(Math.max(0, selSp.getContents().size() - 1));
		syncToClient(player);
	}
	/**
	 * Changes the active specibus slot.  Disarms the player if they were armed.
	 */
	public static void setSelectedSpecibus(ServerPlayer player, int index)
	{
		StrifePortfolioData data = getData(player);
		
		if(data.isArmed())
			clearArmedWeapon(player, data);
		
		data.setSelectedSpecibusIndex(index);
		syncToClient(player);
	}
	
	/** Clears the assigned item from the player's hands and marks data as unarmed. */
	private static void clearArmedWeapon(ServerPlayer player, StrifePortfolioData data)
	{
		for(InteractionHand hand : InteractionHand.values())
		{
			if(isHeldWeapon(player, player.getItemInHand(hand)))
			{
				player.setItemInHand(hand, ItemStack.EMPTY);
				break;
			}
		}
		data.setArmed(false);
	}
	
	/** Wraps a {@link StrifeSpecibus} into a {@link StrifeCardItem} ItemStack. */
	public static ItemStack createStrifeCard(@Nullable StrifeSpecibus specibus)
	{
		ItemStack card = new ItemStack(MSItems.STRIFE_CARD.get());
		if(specibus != null)
			card.set(MSItemComponents.STRIFE_SPECIBUS_DATA.get(), specibus);
		return card;
	}
}