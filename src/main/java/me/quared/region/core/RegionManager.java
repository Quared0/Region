package me.quared.region.core;

import me.quared.itemguilib.gui.Gui;
import me.quared.itemguilib.gui.GuiObject;
import me.quared.itemguilib.items.prefab.CustomItemPrefab;
import me.quared.region.RegionPlugin;
import me.quared.region.core.listener.RegionInteractListeners;
import me.quared.region.core.listener.RenameChatListener;
import me.quared.region.selection.Selection;
import me.quared.region.selection.SelectionManager;
import me.quared.region.sql.DatabaseConnection;
import me.quared.region.util.StringUtil;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class RegionManager {

	private static RegionManager instance;

	private final RegionPlugin plugin;
	private final List<Region> regions;
	private final CustomItemPrefab wand;
	private final DatabaseConnection dbConnection;

	private final Map<Player, Region> pendingRename;

	public RegionManager(RegionPlugin plugin) {
		instance = this;

		FileConfiguration config = plugin.getConfig();

		this.regions = new ArrayList<>();
		this.pendingRename = new HashMap<>();
		this.plugin = plugin;
		this.wand = new CustomItemPrefab(new ItemStack(Material.valueOf(plugin.getConfig().getString("item.wand.material"))), false, ev -> {
			if (ev.getClickedBlock() == null) return;

			Player p = ev.getPlayer();
			Location location = ev.getClickedBlock().getLocation();
			String locationFormatted = location.getX() + ", " + location.getY() + ", " + location.getZ();

			SelectionManager sm = SelectionManager.get();

			if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
				if (location.equals(sm.getPos2(p))) {
					return;
				}
				sm.setPos2(p, location);
				p.sendMessage(StringUtil.getMessage("position2-set", locationFormatted));
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
				ev.setCancelled(true);
			} else if (ev.getAction() == Action.LEFT_CLICK_BLOCK) {
				if (location.equals(sm.getPos1(p))) {
					return;
				}
				sm.setPos1(p, location);
				p.sendMessage(StringUtil.getMessage("position1-set", locationFormatted));
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.5F);
				ev.setCancelled(true);
			}
		}).setName(StringUtil.colourize(plugin.getConfig().getString("item.wand.name")));
		this.dbConnection = new DatabaseConnection(
				config.getString("sql.host"),
				config.getInt("sql.port"),
				config.getString("sql.database"),
				config.getString("sql.username"),
				config.getString("sql.password"));

		Bukkit.getPluginManager().registerEvents(new RenameChatListener(), plugin);
		Bukkit.getPluginManager().registerEvents(new RegionInteractListeners(), plugin);

		loadFromStorage();
	}

	void loadFromStorage() {
		DatabaseConnection db = getDBConnection();

		// TABLES
		try (Connection connection = db.getDataSource().getConnection()) {
			PreparedStatement stmt = connection.prepareStatement("""
					CREATE TABLE IF NOT EXISTS `region_regions` (
					\t`id` INT(11) NOT NULL,
					\t`name` VARCHAR(20) NOT NULL,
					\t`world` VARCHAR(32) NOT NULL,
					\t`x1` INT(11) NOT NULL DEFAULT 0,
					\t`y1` INT(11) NOT NULL DEFAULT 0,
					\t`z1` INT(11) NOT NULL DEFAULT 0,
					\t`x2` INT(11) NOT NULL DEFAULT 0,
					\t`y2` INT(11) NOT NULL DEFAULT 0,
					\t`z2` INT(11) NOT NULL DEFAULT 0,
					\tPRIMARY KEY (`id`)
					);""");
			stmt.execute();

			stmt = connection.prepareStatement("""
					CREATE TABLE IF NOT EXISTS `region_whitelists` (
					\t`region_id` INT(11) NOT NULL,
					\t`player_uuid` VARCHAR(36) NOT NULL
					);""");
			stmt.execute();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}

		// SELECTING
		try (Connection connection = db.getDataSource().getConnection()) {
			PreparedStatement stmt = connection.prepareStatement("SELECT * FROM `region_regions`");
			ResultSet set = stmt.executeQuery();
			while (set.next()) {
				regions.add(new Region(set.getInt("id"),
						set.getString("name"),
						new Selection(
								new Location(Bukkit.getWorld(set.getString("world")), set.getInt("x1"), set.getInt("y1"), set.getInt("z1")),
								new Location(Bukkit.getWorld(set.getString("world")), set.getInt("x2"), set.getInt("y2"), set.getInt("z2"))
						)));
			}

			stmt = connection.prepareStatement("SELECT * FROM `region_whitelists`");
			set = stmt.executeQuery();
			while (set.next()) {
				Region region = getRegion(set.getInt("region_id"));

				if (region == null) continue;
				region.addWhitelist(UUID.fromString(set.getString("player_uuid")));
			}
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	public DatabaseConnection getDBConnection() {
		return dbConnection;
	}

	public @Nullable Region getRegion(int id) {
		return regions.stream().filter(r -> r.getId() == id).findFirst().orElse(null);
	}

	public static RegionManager get() {
		if (instance == null)
			new RegionManager(RegionPlugin.getPlugin(RegionPlugin.class));
		return instance;
	}

	public @Nullable Region getRegion(String name) {
		return regions.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
	}

	public void createRegion(String name, World w, Selection area) {
		int id = getNextId();
		DatabaseConnection db = getDBConnection();
		try (Connection conn = db.getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO `region_regions` (id,name,world,x1,y1,z1,x2,y2,z2) VALUES (?,?,?,?,?,?,?,?,?)");
			stmt.setInt(1, id);
			stmt.setString(2, name);
			stmt.setString(3, w.getName());
			stmt.setInt(4, area.point1().getBlockX());
			stmt.setInt(5, area.point1().getBlockY());
			stmt.setInt(6, area.point1().getBlockZ());
			stmt.setInt(7, area.point2().getBlockX());
			stmt.setInt(8, area.point2().getBlockY());
			stmt.setInt(9, area.point2().getBlockZ());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		regions.add(new Region(id, name, area));
	}

	public void removeRegion(Region region) {
		DatabaseConnection db = getDBConnection();
		try (Connection conn = db.getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM `region_regions` WHERE `id`=?");
			stmt.setInt(1, region.getId());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		regions.remove(region);
	}

	public void setRegionArea(Region region, Selection area) {
		DatabaseConnection db = getDBConnection();
		try (Connection conn = db.getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("UPDATE `region_regions` SET `x1`=?,`y1`=?,`z1`=?,`x2`=?,`y2`=?,`z2`=? WHERE `id`=?");
			stmt.setInt(1, area.point1().getBlockX());
			stmt.setInt(2, area.point1().getBlockY());
			stmt.setInt(3, area.point1().getBlockZ());
			stmt.setInt(4, area.point2().getBlockX());
			stmt.setInt(5, area.point2().getBlockY());
			stmt.setInt(6, area.point2().getBlockZ());
			stmt.setInt(7, region.getId());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		region.setArea(area);
	}

	public void setRegionName(Region region, String name) {
		DatabaseConnection db = getDBConnection();
		try (Connection conn = db.getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("UPDATE `region_regions` SET `name`=? WHERE `id`=?");
			stmt.setString(1, name);
			stmt.setInt(2, region.getId());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		region.setName(name);
	}

	private int getNextId() {
		Region r = getRegions().stream().max(Comparator.comparingInt(Region::getId)).orElse(null);
		if (r == null) return 1;
		return r.getId() + 1;
	}

	public @Unmodifiable List<Region> getRegions() {
		return Collections.unmodifiableList(regions);
	}

	public CustomItemPrefab getWand() {
		return wand;
	}

	public void openRegionsMenu(Player player, int page) {
		ConfigurationSection guiSection = RegionPlugin.getPlugin(RegionPlugin.class).getConfig().getConfigurationSection("gui.regions");
		Map<Integer, GuiObject> objects = new HashMap<>();

		final int PAGE_CAPACITY = 21;
		final int PAGE_SIZE = 45;

		objects.put(40, new GuiObject(Material.valueOf(guiSection.getString("items.close.material")), ev -> {
			ev.getWhoClicked().closeInventory();
		}).setName(StringUtil.colourize(guiSection.getString("items.close.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.close.lore"))));

		if (page < Math.floor(((double) RegionManager.get().getRegions().size() - 1) / PAGE_CAPACITY)) {
			objects.put(41, new GuiObject(Material.valueOf(guiSection.getString("items.next-page.material")), ev -> {
				openRegionsMenu(((Player) ev.getWhoClicked()), page + 1);
			}).setName(StringUtil.colourize(guiSection.getString("items.next-page.name")))
					.addLore(StringUtil.colourize(guiSection.getStringList("items.next-page.lore"))));
		}

		if (page > 0) {
			objects.put(39, new GuiObject(Material.valueOf(guiSection.getString("items.previous-page.material")), ev -> {
				openRegionsMenu(((Player) ev.getWhoClicked()), page - 1);
			}).setName(StringUtil.colourize(guiSection.getString("items.previous-page.name")))
					.addLore(StringUtil.colourize(guiSection.getStringList("items.previous-page.lore"))));
		}

		if (guiSection.contains("border")) {
			final int WIDTH = 9;
			for (int i = 0; i < guiSection.getInt("size"); i++) {
				if (i % WIDTH == 0 || i % WIDTH == 8 || i < WIDTH || i > guiSection.getInt("size") - WIDTH) {
					if (!objects.containsKey(i)) {
						objects.put(i, new GuiObject(Material.valueOf(guiSection.getString("border"))).setName(" "));
					}
				}
			}
		}

		// ITEMS
		int k = 0;
		for (int i = page * PAGE_CAPACITY; i < (page + 1) * PAGE_CAPACITY; i++) {
			if (RegionManager.get().getRegions().size() <= i) break;

			Region region = RegionManager.get().getRegions().get(i);
			GuiObject regionBlock = new GuiObject(Material.valueOf(guiSection.getString("items.entry.material")), ev -> {
				openRegionMenu(((Player) ev.getWhoClicked()), region);
			}).setName(StringUtil.formatString(guiSection.getString("items.entry.name"), region.getName()))
					.addLore(StringUtil.formatStrings(guiSection.getStringList("items.entry.lore"), region.getName()));

			while (objects.containsKey(k)) k++;
			if (k < PAGE_SIZE) {
				objects.put(k, regionBlock);
			}
		}

		Gui gui = new Gui(objects, PAGE_SIZE, guiSection.getString("title"));
		player.openInventory(gui.getInventory());
	}

	public void openRegionMenu(Player p, Region region) {
		ConfigurationSection guiSection = RegionPlugin.getPlugin(RegionPlugin.class).getConfig().getConfigurationSection("gui.region");
		Map<Integer, GuiObject> objects = new HashMap<>();

		final int PAGE_SIZE = 45;

		objects.put(4, new GuiObject(Material.valueOf(guiSection.getString("items.info.material")))
				.setName(StringUtil.colourize(String.format(guiSection.getString("items.info.name"), region.getName())))
				.addLore(StringUtil.formatStrings(guiSection.getStringList("items.info.lore"), region.getName())));

		objects.put(40, new GuiObject(Material.valueOf(guiSection.getString("items.close.material")), ev -> {
			openRegionsMenu(p, 0);
		}).setName(StringUtil.colourize(guiSection.getString("items.close.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.close.lore"))));

		objects.put(20, new GuiObject(Material.valueOf(guiSection.getString("items.whitelist.material")), ev -> {
			Player clicker = ((Player) ev.getWhoClicked());
			Bukkit.dispatchCommand(clicker, "region whitelist list " + region.getName());
		}).setName(StringUtil.colourize(guiSection.getString("items.whitelist.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.whitelist.lore"))));

		objects.put(21, new GuiObject(Material.valueOf(guiSection.getString("items.rename.material")), ev -> {
			Player clicker = ((Player) ev.getWhoClicked());
			clicker.closeInventory();
			Bukkit.dispatchCommand(clicker, "region rename " + region.getName());
		}).setName(StringUtil.colourize(guiSection.getString("items.rename.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.rename.lore"))));

		objects.put(23, new GuiObject(Material.valueOf(guiSection.getString("items.teleport.material")), ev -> {
			Player clicker = ((Player) ev.getWhoClicked());
			clicker.closeInventory();
			Bukkit.dispatchCommand(clicker, "region teleport " + region.getName());
		}).setName(StringUtil.colourize(guiSection.getString("items.teleport.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.teleport.lore"))));

		objects.put(24, new GuiObject(Material.valueOf(guiSection.getString("items.resize.material")), ev -> {
			Player clicker = ((Player) ev.getWhoClicked());
			clicker.closeInventory();
			Bukkit.dispatchCommand(clicker, "region resize " + region.getName());
		}).setName(StringUtil.colourize(guiSection.getString("items.resize.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.resize.lore"))));

		if (guiSection.contains("border")) {
			final int WIDTH = 9;
			for (int i = 0; i < guiSection.getInt("size"); i++) {
				if (i % WIDTH == 0 || i % WIDTH == 8 || i < WIDTH || i > guiSection.getInt("size") - WIDTH) {
					if (!objects.containsKey(i)) {
						objects.put(i, new GuiObject(Material.valueOf(guiSection.getString("border"))).setName(" "));
					}
				}
			}
		}

		Gui gui = new Gui(objects, PAGE_SIZE, String.format(guiSection.getString("title"), region.getName()));
		p.openInventory(gui.getInventory());
	}

	public void openWhitelistMenu(Player player, Region region, int page) {
		ConfigurationSection guiSection = RegionPlugin.getPlugin(RegionPlugin.class).getConfig().getConfigurationSection("gui.whitelist");
		Map<Integer, GuiObject> objects = new HashMap<>();

		final int PAGE_CAPACITY = 21;
		final int PAGE_SIZE = 45;

		objects.put(40, new GuiObject(Material.valueOf(guiSection.getString("items.close.material")), ev -> {
			openRegionMenu(player, region);
		}).setName(StringUtil.colourize(guiSection.getString("items.close.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.close.lore"))));

		objects.put(4, new GuiObject(Material.valueOf(guiSection.getString("items.info.material")))
				.setName(StringUtil.colourize(String.format(guiSection.getString("items.info.name"), region.getName())))
				.addLore(StringUtil.formatStrings(guiSection.getStringList("items.info.lore"), region.getName())));

		objects.put(36, new GuiObject(Material.valueOf(guiSection.getString("items.howto.material")))
				.setName(StringUtil.colourize(guiSection.getString("items.howto.name")))
				.addLore(StringUtil.colourize(guiSection.getStringList("items.howto.lore"))));

		if (page < Math.floor(((double) region.getWhitelisted().size() - 1) / PAGE_CAPACITY)) {
			objects.put(41, new GuiObject(Material.valueOf(guiSection.getString("items.next-page.material")), ev -> {
				openWhitelistMenu(((Player) ev.getWhoClicked()), region, page + 1);
			}).setName(StringUtil.colourize(guiSection.getString("items.next-page.name")))
					.addLore(StringUtil.colourize(guiSection.getStringList("items.next-page.lore"))));
		}

		if (page > 0) {
			objects.put(39, new GuiObject(Material.valueOf(guiSection.getString("items.previous-page.material")), ev -> {
				openWhitelistMenu(((Player) ev.getWhoClicked()), region, page - 1);
			}).setName(StringUtil.colourize(guiSection.getString("items.previous-page.name")))
					.addLore(StringUtil.colourize(guiSection.getStringList("items.previous-page.lore"))));
		}

		if (guiSection.contains("border")) {
			final int WIDTH = 9;
			for (int i = 0; i < guiSection.getInt("size"); i++) {
				if (i % WIDTH == 0 || i % WIDTH == 8 || i < WIDTH || i > guiSection.getInt("size") - WIDTH) {
					if (!objects.containsKey(i)) {
						objects.put(i, new GuiObject(Material.valueOf(guiSection.getString("border"))).setName(" "));
					}
				}
			}
		}

		// ITEMS
		int k = 0;
		for (int i = page * PAGE_CAPACITY; i < (page + 1) * PAGE_CAPACITY; i++) {
			if (region.getWhitelisted().size() <= i) break;

			UUID uuid = region.getWhitelisted().get(i);
			GuiObject headObject = new GuiObject(Material.valueOf(guiSection.getString("items.entry.material")), ev -> {
				removeFromWhitelist(region, uuid);
				ev.getWhoClicked().sendMessage(StringUtil.getMessage("whitelist-remove"));
				ev.getWhoClicked().closeInventory();
			}).setName(StringUtil.formatString(guiSection.getString("items.entry.name"), Bukkit.getOfflinePlayer(uuid).getName()))
					.addLore(StringUtil.formatStrings(guiSection.getStringList("items.entry.lore"), Bukkit.getOfflinePlayer(uuid).getName()));

			SkullMeta meta = (SkullMeta) headObject.getItem().getItemMeta();
			meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
			headObject.getItem().setItemMeta(meta);

			while (objects.containsKey(k)) k++;
			if (k < PAGE_SIZE) {
				objects.put(k, headObject);
			}
		}

		Gui gui = new Gui(objects, PAGE_SIZE, String.format(guiSection.getString("title"), region.getName()));
		player.openInventory(gui.getInventory());
	}

	public Map<Player, Region> getPendingRename() {
		return pendingRename;
	}

	public void addToWhitelist(Region region, Player to) {
		addToWhitelist(region, to.getUniqueId());
	}

	public void addToWhitelist(Region region, UUID to) {
		if (region.isWhitelisted(to)) return;

		try (Connection conn = getDBConnection().getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO `region_whitelists` (region_id,player_uuid) VALUES (?,?)");
			stmt.setInt(1, region.getId());
			stmt.setString(2, to.toString());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		region.addWhitelist(to);
	}

	public void removeFromWhitelist(Region region, Player to) {
		removeFromWhitelist(region, to.getUniqueId());
	}

	public void removeFromWhitelist(Region region, UUID to) {
		if (!region.isWhitelisted(to)) return;

		try (Connection conn = getDBConnection().getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM `region_whitelists` WHERE `region_id`=? AND `player_uuid`=?");
			stmt.setInt(1, region.getId());
			stmt.setString(2, to.toString());
			stmt.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		region.removeWhitelist(to);
	}

	public boolean hasRegionAt(Location location) {
		return getRegions().stream().anyMatch(r -> r.getArea().toBoundingBox().contains(location.toVector()));
	}

	public Region getRegionAt(Location location) {
		return getRegions().stream().filter(r -> r.getArea().toBoundingBox().contains(location.toVector())).findFirst().orElse(null);
	}

}
