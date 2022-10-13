package me.quared.region;

import me.quared.itemguilib.ItemGuiLib;
import me.quared.region.core.RegionManager;
import me.quared.region.core.command.RegionCommand;
import me.quared.region.selection.SelectionManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class RegionPlugin extends JavaPlugin {

	private FileConfiguration lang;

	@Override
	public void onEnable() {
		ItemGuiLib.setPluginInstance(this);

		loadConfigs();
		loadCommands();

		RegionManager.get();
		SelectionManager.get();
	}

	@Override
	public void onDisable() {
		RegionManager.get().getDBConnection().close();
	}

	private void loadConfigs() {
		saveDefaultConfig();

		saveResource("lang.yml", false);
		this.lang = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "lang.yml"));
	}

	private void loadCommands() {
		getCommand("region").setExecutor(new RegionCommand(this));
	}

	public FileConfiguration getLang() {
		return lang;
	}

}
