package me.quared.region.core;

import me.quared.region.selection.Selection;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Region {

	private final List<UUID> whitelisted;
	private final int id;
	private Selection area;
	private String name;

	public Region(int id, String name, Selection area) {
		this.id = id;
		this.area = area;
		this.name = name;
		this.whitelisted = new ArrayList<>();
	}

	public Selection getArea() {
		return area;
	}

	void setArea(Selection area) {
		this.area = area;
	}

	public String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}

	void addWhitelist(UUID uuid) {
		whitelisted.add(uuid);
	}

	void removeWhitelist(UUID uuid) {
		whitelisted.remove(uuid);
	}

	public boolean isWhitelisted(UUID uuid) {
		return whitelisted.contains(uuid);
	}

	public @Unmodifiable List<UUID> getWhitelisted() {
		return Collections.unmodifiableList(whitelisted);
	}

	public int getId() {
		return id;
	}

}
