package com.civpvp.attrhider;

import java.util.List;

import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

/**
 * Uses ProtocolLib to strip away stuff that should never have been sent in the first place
 * such as enchantment, durability and potion duration information.
 * @author Squeenix
 *
 */
public class AttrHider extends JavaPlugin {
	
	@Override
	public void onEnable() {
		registerPacketAdapters();
	}
	
	public void registerPacketAdapters() {
		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT) {
				@Override
				public void onPacketSending(PacketEvent event) {
					ItemStack old = event.getPacket().getItemModifier().read(0);
					if(old == null) return;
					ItemStack clean = new ItemStack(old.getType());
					if(old.hasItemMeta() && clean.hasItemMeta()) {
						ItemMeta oldMeta = old.getItemMeta();
						ItemMeta newMeta = clean.getItemMeta();
						if(oldMeta instanceof LeatherArmorMeta) {
							Color color = ((LeatherArmorMeta)oldMeta).getColor();
							((LeatherArmorMeta)newMeta).setColor(color);
						}
						if(oldMeta.hasEnchants()) {
							clean.addEnchantment(Enchantment.DURABILITY, 1);
						}
						if(oldMeta instanceof BannerMeta) {
							DyeColor base = ((BannerMeta)oldMeta).getBaseColor();
							List<Pattern> pattern = ((BannerMeta)oldMeta).getPatterns();
							((BannerMeta)newMeta).setBaseColor(base);
							((BannerMeta)newMeta).setPatterns(pattern);
						}
						clean.setItemMeta(newMeta);
					}
					event.getPacket().getItemModifier().write(0, clean);
				}
			});
		
		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EFFECT) {
				@Override
				public void onPacketSending(PacketEvent event) {
					if(event.getPacket().getIntegers().read(0) != event.getPlayer().getEntityId()) {
						event.getPacket().getBytes().write(0, (byte) 26);
						event.getPacket().getBytes().write(1, (byte) 420);
						event.getPacket().getBytes().write(2, (byte) 0);
						event.getPacket().getIntegers().write(1, 420);
					}
				}
			});
		
		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
				@Override
				public void onPacketSending(PacketEvent event) {
					Player player = event.getPlayer();
					Entity entity = getEntityById(event.getPlayer().getWorld(), event.getPacket().getIntegers().read(0));
					
					if(entity != null && entity instanceof LivingEntity && entity.getPassenger() != player 
							&& entity != player && !(entity instanceof Wither) && !(entity instanceof EnderDragon)) {
						List<WrappedWatchableObject> list = event.getPacket().getWatchableCollectionModifier().read(0);
						for(WrappedWatchableObject wwo : list) {
							if(wwo.getIndex() == 7 && ((Float)wwo.getValue()) > 0) {
								wwo.setValue(20f);
							}
						}
						event.getPacket().getWatchableCollectionModifier().write(0, list);
					}
				}
			});
	}
	
	private Entity getEntityById(World world, int id) {
		for(Entity e : world.getEntities()) {
			if(e.getEntityId() == id) {
				return e;
			}
		}
		return null;
	}
}

