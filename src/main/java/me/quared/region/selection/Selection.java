package me.quared.region.selection;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

public record Selection(Location point1, Location point2) {

	public BoundingBox toBoundingBox() {
		return new BoundingBox(point1().getX(), point1().getY(), point1().getZ(), point2().getX(), point2().getY(), point2().getZ());
	}

}
