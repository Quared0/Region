package me.quared.region.core.listener;

import me.quared.region.core.Region;
import me.quared.region.core.RegionManager;
import me.quared.region.util.StringUtil;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class RegionInteractListeners implements Listener {

	@EventHandler
	public void event(BlockBreakEvent event) {
		interact(event.getPlayer(), event.getBlock(), event);
	}

	@EventHandler
	public void event(BlockPlaceEvent event) {
		interact(event.getPlayer(), event.getBlock(), event);
	}

	@EventHandler
	public void event(PlayerBucketFillEvent event) {
		interact(event.getPlayer(), event.getBlock(), event);
	}

	@EventHandler
	public void event(PlayerBucketEmptyEvent event) {
		interact(event.getPlayer(), event.getBlock(), event);
	}

	@EventHandler
	public void event(PlayerInteractEvent event) {
		if (event.getClickedBlock() == null) return;
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
			interact(event.getPlayer(), event.getClickedBlock(), event);
		}
	}

	public void interact(Player p, Block b, Cancellable event) {
		RegionManager rm = RegionManager.get();
		if (p.hasPermission("region.bypass")) return;
		if (rm.hasRegionAt(b.getLocation())) {
			Region r = rm.getRegionAt(b.getLocation());
			if (!r.isWhitelisted(p.getUniqueId())) {
				p.sendMessage(StringUtil.getMessage("player-not-whitelisted"));
				p.spawnParticle(Particle.SMOKE_NORMAL, b.getLocation().clone().add(0, 1, 0), 10);
				event.setCancelled(true);
			}
		}
	}

}
