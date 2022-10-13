package me.quared.region.selection;

import me.quared.region.RegionPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class SelectionManager {

	private static SelectionManager instance;

	private final RegionPlugin plugin;

	private final Map<Player, Location> pos1s, pos2s;

	public SelectionManager(RegionPlugin plugin) {
		instance = this;

		this.plugin = plugin;
		this.pos1s = new HashMap<>();
		this.pos2s = new HashMap<>();
	}

	public static SelectionManager get() {
		if (instance == null)
			new SelectionManager(RegionPlugin.getPlugin(RegionPlugin.class));
		return instance;
	}

	public void setPos1(Player p, Location pos1) {
		pos1s.put(p, pos1);
	}

	public void setPos2(Player p, Location pos2) {
		pos2s.put(p, pos2);
	}

	public Selection getSelection(Player p) {
		if (!hasSelection(p)) return null;
		return new Selection(getPos1(p), getPos2(p));
	}

	public boolean hasSelection(Player p) {
		return pos1s.containsKey(p) && pos2s.containsKey(p) && pos2s.get(p).getWorld().equals(pos1s.get(p).getWorld());
	}

	public Location getPos1(Player p) {
		return pos1s.get(p);
	}

	public Location getPos2(Player p) {
		return pos2s.get(p);
	}

}
