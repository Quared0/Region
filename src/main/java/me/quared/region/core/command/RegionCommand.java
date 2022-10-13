package me.quared.region.core.command;

import me.quared.region.RegionPlugin;
import me.quared.region.core.Region;
import me.quared.region.core.RegionManager;
import me.quared.region.selection.Selection;
import me.quared.region.selection.SelectionManager;
import me.quared.region.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RegionCommand implements CommandExecutor, TabCompleter {

	private final RegionPlugin plugin;

	public RegionCommand(RegionPlugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		RegionManager rm = RegionManager.get();
		SelectionManager sm = SelectionManager.get();
		if (sender instanceof Player p) {
			if (args.length >= 1) {
				switch (args[0].toLowerCase()) {
					case "create" -> {
						if (!p.hasPermission("region.create")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length >= 2) {
							if (!sm.hasSelection(p)) {
								p.sendMessage(StringUtil.getMessage("no-selection"));
								return false;
							}
							if (rm.getRegion(args[1]) != null) {
								p.sendMessage(StringUtil.getMessage("existing-region"));
								return false;
							}
							for (Region r : rm.getRegions()) {
								if (r.getArea().toBoundingBox().overlaps(sm.getSelection(p).toBoundingBox())) {
									p.sendMessage(StringUtil.getMessage("overlaps-region", r.getName()));
									return false;
								}
							}
							Selection selection = sm.getSelection(p);
							rm.createRegion(args[1], selection.point1().getWorld(), selection);
							p.sendMessage(StringUtil.getMessage("create-success"));
						} else {
							p.sendMessage(StringUtil.getMessage("no-region"));
						}
					}
					case "rename" -> {
						if (!p.hasPermission("region.rename")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length >= 2) {
							Region region = rm.getRegion(args[1]);
							if (region == null) {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
								return false;
							}
							if (args.length == 2) {
								rm.getPendingRename().put(p, region);
								p.sendMessage(StringUtil.getMessage("rename-start"));
							} else {
								p.sendMessage(StringUtil.getMessage("rename-success", region.getName(), args[2]));
								rm.setRegionName(region, args[2]);
							}
						} else {
							p.sendMessage(StringUtil.getMessage("no-region"));
						}
					}
					case "resize" -> {
						if (!p.hasPermission("region.resize")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length >= 2) {
							Region region = rm.getRegion(args[1]);
							if (region == null) {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
								return false;
							}
							if (!sm.hasSelection(p)) {
								p.sendMessage(StringUtil.getMessage("no-selection"));
								return false;
							}

							rm.setRegionArea(region, sm.getSelection(p));
							p.sendMessage(StringUtil.getMessage("resize-success"));
						} else {
							p.sendMessage(StringUtil.getMessage("no-region"));
						}
					}
					case "remove", "delete" -> {
						if (!p.hasPermission("region.remove")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length >= 2) {
							Region region = rm.getRegion(args[1]);
							if (region == null) {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
								return false;
							}
							rm.removeRegion(region);
							p.sendMessage(StringUtil.getMessage("remove-success"));
						}
					}
					case "whereami", "current" -> {
						if (!p.hasPermission("region.current")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (rm.hasRegionAt(p.getLocation())) {
							Region r = rm.getRegionAt(p.getLocation());
							p.sendMessage(StringUtil.getMessage("current-success", r.getName()));
						} else {
							p.sendMessage(StringUtil.getMessage("current-none"));
						}
					}
					case "teleport" -> {
						if (!p.hasPermission("region.teleport")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length >= 2) {
							Region region = rm.getRegion(args[1]);
							if (region == null) {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
								return false;
							}
							World w = region.getArea().point1().getWorld();
							Location centre = region.getArea().toBoundingBox().getCenter().toLocation(w);
							p.teleport(w.getHighestBlockAt(centre).getLocation().clone().add(0.5, 1, 0.5));
						}
					}
					case "wand" -> {
						if (!p.hasPermission("region.wand")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						rm.getWand().giveTo(p);
					}
					case "whitelist" -> {
						if (args.length >= 3) {
							Region region = rm.getRegion(args[2]);
							if (region == null) {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
								return false;
							}
							switch (args[1].toLowerCase()) {
								case "add" -> {
									if (!p.hasPermission("region.whitelist.add")) {
										p.sendMessage(StringUtil.getMessage("no-permission"));
										return false;
									}
									if (args.length >= 4) {
										Player to = Bukkit.getPlayer(args[3]);
										if (to != null) {
											if (region.isWhitelisted(to.getUniqueId())) {
												p.sendMessage(StringUtil.getMessage("already-whitelisted"));
												return false;
											}
											p.sendMessage(StringUtil.getMessage("whitelist-add"));
											rm.addToWhitelist(region, to);
										} else {
											p.sendMessage(StringUtil.getMessage("invalid-player"));
										}
									} else {
										p.sendMessage(StringUtil.getMessage("no-player"));
									}
								}
								case "remove" -> {
									if (!p.hasPermission("region.whitelist.remove")) {
										p.sendMessage(StringUtil.getMessage("no-permission"));
										return false;
									}
									if (args.length == 4) {
										Player to = Bukkit.getPlayer(args[3]);
										if (to != null) {
											if (!region.isWhitelisted(to.getUniqueId())) {
												p.sendMessage(StringUtil.getMessage("not-whitelisted"));
												return false;
											}
											p.sendMessage(StringUtil.getMessage("whitelist-remove"));
											rm.removeFromWhitelist(region, to);
										} else {
											p.sendMessage(StringUtil.getMessage("invalid-player"));
										}
									} else {
										p.sendMessage(StringUtil.getMessage("no-player"));
									}
								}
								case "view", "list" -> {
									if (!p.hasPermission("region.whitelist.list")) {
										p.sendMessage(StringUtil.getMessage("no-permission"));
										return false;
									}
									rm.openWhitelistMenu(p, region, 0);
								}
							}
						} else {
							p.sendMessage(StringUtil.getMessage("no-region"));
						}
					}
					case "menu" -> {
						if (!p.hasPermission("region.menu")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (args.length == 1) {
							rm.openRegionsMenu(p, 0);
						} else {
							if (rm.getRegion(args[1]) != null) {
								rm.openRegionMenu(p, rm.getRegion(args[1]));
							} else {
								p.sendMessage(StringUtil.getMessage("invalid-region"));
							}
						}
					}
					default -> {
						if (!p.hasPermission("region.menu")) {
							p.sendMessage(StringUtil.getMessage("no-permission"));
							return false;
						}
						if (rm.getRegion(args[0]) != null) {
							rm.openRegionMenu(p, rm.getRegion(args[0]));
						} else {
							p.sendMessage(StringUtil.getMessage("invalid-usage", "Please use autocomplete or a valid region name."));
						}
					}
				}
			} else {
				rm.openRegionsMenu(p, 0);
			}
		} else {
			sender.sendMessage(StringUtil.getMessage("player-only"));
		}
		return false;
	}

	@Nullable
	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
		List<String> result = new ArrayList<>();
		List<String> coreOptions = Arrays.asList("wand", "create", "teleport", "remove", "rename", "resize", "whitelist", "whereami", "menu");
		if (args.length == 1) {
			for (String option : coreOptions) {
				if (option.startsWith(args[0].toLowerCase())) {
					result.add(option);
				}
			}
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("whitelist")) {
				if ("add".startsWith(args[1].toLowerCase())) {
					result.add("add");
				}
				if ("remove".startsWith(args[1].toLowerCase())) {
					result.add("remove");
				}
				if ("list".startsWith(args[1].toLowerCase())) {
					result.add("list");
				}
			} else if (coreOptions.contains(args[0].toLowerCase()) && !args[0].equalsIgnoreCase("wand") && !args[0].equalsIgnoreCase("create")) {
				for (Region r : RegionManager.get().getRegions()) {
					if (r.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
						result.add(r.getName());
					}
				}
			}
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("whitelist")) {
				for (Region r : RegionManager.get().getRegions()) {
					if (r.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
						result.add(r.getName());
					}
				}
			}
		} else if (args.length == 4) {
			if (args[0].equalsIgnoreCase("whitelist") && !args[1].equalsIgnoreCase("list")) {
				for (Player p : Bukkit.getOnlinePlayers()) {
					if (p.getName().toLowerCase().startsWith(args[3].toLowerCase())) {
						result.add(p.getName());
					}
				}
			}
		}
		return result;
	}

}
