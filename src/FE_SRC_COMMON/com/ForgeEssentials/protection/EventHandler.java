package com.ForgeEssentials.protection;

import static net.minecraftforge.event.Event.Result.ALLOW;
import static net.minecraftforge.event.Event.Result.DENY;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.CheckSpawn;
import net.minecraftforge.event.entity.living.LivingSpawnEvent.SpecialSpawn;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.ForgeEssentials.api.APIRegistry;
import com.ForgeEssentials.api.permissions.query.PermQuery;
import com.ForgeEssentials.api.permissions.query.PermQueryBlanketSpot;
import com.ForgeEssentials.api.permissions.query.PermQueryPlayer;
import com.ForgeEssentials.api.permissions.query.PermQueryPlayerArea;
import com.ForgeEssentials.core.misc.UnfriendlyItemList;
import com.ForgeEssentials.util.OutputHandler;
import com.ForgeEssentials.util.AreaSelector.WorldPoint;
import com.ForgeEssentials.util.events.PlayerBlockBreak;
import com.ForgeEssentials.util.events.PlayerBlockPlace;

import cpw.mods.fml.common.FMLCommonHandler;

public class EventHandler
{
	@ForgeSubscribe(priority = EventPriority.LOW)
	public void playerAttack(AttackEntityEvent e)
	{
		if (e.target == null)
			return;

		if (e.target instanceof EntityPlayer)
		{
			// Stops players from hitting each other.

			boolean sourceB = !APIRegistry.perms.checkPermAllowed(new PermQueryPlayer(e.entityPlayer, ModuleProtection.PERM_PVP));

			if (sourceB)
			{
				e.setCanceled(true);
				return;
			}

			boolean receiverB = !APIRegistry.perms.checkPermAllowed(new PermQueryPlayer((EntityPlayer) e.target, ModuleProtection.PERM_PVP));

			if (sourceB || receiverB)
			{
				e.setCanceled(true);
			}

		}
		else
		{
			// Stops players from hitting entities.

			PermQuery query = new PermQueryPlayer(e.entityPlayer, ModuleProtection.PERM_OVERRIDE);
			Boolean result = APIRegistry.perms.checkPermAllowed(query);

			if (!result)
			{
				query = new PermQueryPlayer(e.entityPlayer, ModuleProtection.PERM_INTERACT_ENTITY);
				result = APIRegistry.perms.checkPermAllowed(query);
			}

			e.setCanceled(!result);
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void damage(LivingHurtEvent e)
	{
		// do nothing if the source isnt a living thing.
		// actually... if its ANY entity.
		if (e.source.getEntity() == null || !(e.source.getEntity() instanceof EntityLiving))
			return;

		EntityLiving source = (EntityLiving) e.source.getEntity();

		boolean sourcePlayer = e.source.getEntity() instanceof EntityPlayer;
		boolean targetPlayer = e.entityLiving instanceof EntityPlayer;

		if (e.entityLiving == null)
			return;

		if (sourcePlayer && targetPlayer)
		{

			// PVP checks

			boolean sourceB = !APIRegistry.perms.checkPermAllowed(new PermQueryPlayerArea((EntityPlayer) e.entityLiving, ModuleProtection.PERM_PVP, new WorldPoint(e.source.getEntity())));

			if (sourceB)
			{
				e.setCanceled(true);
				return;
			}

			boolean receiverB = !APIRegistry.perms.checkPermAllowed(new PermQueryPlayer((EntityPlayer) e.source.getEntity(), ModuleProtection.PERM_PVP));

			if (sourceB || receiverB)
			{
				e.setCanceled(true);
			}
		}
		else if (sourcePlayer)
		{
			// stop players hitting animals.

			PermQuery query = new PermQueryPlayerArea((EntityPlayer) source, ModuleProtection.PERM_OVERRIDE, new WorldPoint(e.entityLiving));
			Boolean result = APIRegistry.perms.checkPermAllowed(query);

			if (!result)
			{
				query = new PermQueryPlayerArea((EntityPlayer) source, ModuleProtection.PERM_INTERACT_ENTITY, new WorldPoint(e.entityLiving));
				result = APIRegistry.perms.checkPermAllowed(query);
			}

			e.setCanceled(!result);
		}
		else if (targetPlayer)
		{
			// stop people from hitting entites.

			PermQuery query = new PermQueryPlayer((EntityPlayer) e.entityLiving, ModuleProtection.PERM_OVERRIDE);
			Boolean result = APIRegistry.perms.checkPermAllowed(query);

			if (!result)
			{
				query = new PermQueryPlayer((EntityPlayer) e.entityLiving, ModuleProtection.PERM_INTERACT_ENTITY);
				result = APIRegistry.perms.checkPermAllowed(query);
			}

			e.setCanceled(!result);
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void breakEvent(PlayerBlockBreak e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		WorldPoint point = new WorldPoint(e.player.dimension, e.blockX, e.blockY, e.blockZ);
		PermQuery query = new PermQueryPlayerArea(e.player, ModuleProtection.PERM_OVERRIDE, point);
		boolean result = APIRegistry.perms.checkPermAllowed(query);

		if (!result)
		{
			query = new PermQueryPlayerArea(e.player, ModuleProtection.PERM_EDITS, point);
			result = APIRegistry.perms.checkPermAllowed(query);
		}

		e.setCanceled(!result);
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void placeEvent(PlayerBlockPlace e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		WorldPoint point = new WorldPoint(e.player.dimension, e.blockX, e.blockY, e.blockZ);
		PermQuery query = new PermQueryPlayerArea(e.player, ModuleProtection.PERM_OVERRIDE, point);
		boolean result = APIRegistry.perms.checkPermAllowed(query);

		if (!result)
		{
			query = new PermQueryPlayerArea(e.player, ModuleProtection.PERM_EDITS, point);
			result = APIRegistry.perms.checkPermAllowed(query);
		}
		e.setCanceled(!result);
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void playerInteractEventItemUse(PlayerInteractEvent e)
	{
		if (e.action.equals(PlayerInteractEvent.Action.LEFT_CLICK_BLOCK))
			return;

		// item check
		ItemStack stack = e.entityPlayer.getCurrentEquippedItem();
		if (stack == null)
			return;

		WorldPoint point = new WorldPoint(e.entityPlayer);
		if (e.action.equals(PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK))
		{
			if (stack.getItem() instanceof ItemBlock)
			{
				// calculate offsets.
				ForgeDirection dir = ForgeDirection.getOrientation(e.face);
				int x = e.x + dir.offsetX;
				int y = e.y + dir.offsetY;
				int z = e.z + dir.offsetZ;

				point = new WorldPoint(e.entityPlayer.dimension, x, y, z);
			}
			else
				point = new WorldPoint(e.entityPlayer.dimension, e.x, e.y, e.z);
		}

		PermQuery query = new PermQueryPlayerArea(e.entityPlayer, ModuleProtection.PERM_OVERRIDE, point);
		boolean result = APIRegistry.perms.checkPermAllowed(query);

		if (!result)
		{
			String name = UnfriendlyItemList.getName(stack.itemID);
			name = ModuleProtection.PERM_ITEM_USE + "." + name;
			name = name + "." + stack.getItemDamage();

			query = new PermQueryPlayerArea(e.entityPlayer, name, point);
			result = APIRegistry.perms.checkPermAllowed(query);
		}
		
		if (result)
			e.useItem = ALLOW;
		else
			e.useItem = DENY;
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void playerInteractEventBlockUse(PlayerInteractEvent e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		if (e.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK)
		{
			WorldPoint point = new WorldPoint(e.entityPlayer.dimension, e.x, e.y, e.z);
			PermQuery query = new PermQueryPlayerArea(e.entityPlayer, ModuleProtection.PERM_OVERRIDE, point);
			boolean result = APIRegistry.perms.checkPermAllowed(query);

			if (!result)
			{
				// check block usage perm
				query = new PermQueryPlayerArea(e.entityPlayer, ModuleProtection.PERM_INTERACT_BLOCK, point);
				result = APIRegistry.perms.checkPermAllowed(query);
			}
			
			if (result)
				e.useBlock = ALLOW;
			else
				e.useBlock = DENY;
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void entityInteractEvent(EntityInteractEvent e)
	{
		if (FMLCommonHandler.instance().getEffectiveSide().isClient())
			return;

		WorldPoint point = new WorldPoint(e.entityPlayer.dimension, (int) e.target.posX, (int) e.target.posY, (int) e.target.posZ);

		PermQuery query = new PermQueryPlayerArea(e.entityPlayer, ModuleProtection.PERM_OVERRIDE, point);
		Boolean result = APIRegistry.perms.checkPermAllowed(query);

		if (!result)
		{
			query = new PermQueryPlayerArea(e.entityPlayer, ModuleProtection.PERM_INTERACT_ENTITY, point);
			result = APIRegistry.perms.checkPermAllowed(query);
		}

		e.setCanceled(!result);
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void handleSpawn(CheckSpawn e)
	{
		// ignore players
		if (!ModuleProtection.enableMobSpawns || e.entityLiving instanceof EntityPlayer)
			return;

		WorldPoint point = new WorldPoint(e.entityLiving);
		String mobID = EntityList.getEntityString(e.entity);

		PermQueryBlanketSpot query = new PermQueryBlanketSpot(point, ModuleProtection.PERM_MOB_SPAWN_NATURAL + "." + mobID);

		if (!APIRegistry.perms.checkPermAllowed(query))
		{
			e.setResult(Result.DENY);
			OutputHandler.debug(mobID + " : DENIED");
		}
		else
		{
			OutputHandler.debug(mobID + " : ALLOWED");
		}
	}

	@ForgeSubscribe(priority = EventPriority.LOW)
	public void handleSpawn(SpecialSpawn e)
	{
		// ignore players
		if (!ModuleProtection.enableMobSpawns || e.entityLiving instanceof EntityPlayer)
			return;

		WorldPoint point = new WorldPoint(e.entityLiving);
		String mobID = EntityList.getEntityString(e.entity);

		PermQueryBlanketSpot query = new PermQueryBlanketSpot(point, ModuleProtection.PERM_MOB_SPAWN_FORCED + "." + mobID);

		if (!APIRegistry.perms.checkPermAllowed(query))
		{
			e.setResult(Result.DENY);
		}
	}
}
