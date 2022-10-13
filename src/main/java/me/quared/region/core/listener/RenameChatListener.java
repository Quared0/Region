package me.quared.region.core.listener;

import me.quared.region.core.RegionManager;
import me.quared.region.util.StringUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class RenameChatListener implements Listener {

	@EventHandler
	public void onChat(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		RegionManager rm = RegionManager.get();
		if (rm.getPendingRename().containsKey(p)) {
			e.setCancelled(true);
			String name = e.getMessage().split(" ")[0];
			if (name.equalsIgnoreCase("exit")) {
				rm.getPendingRename().remove(p);
				p.sendMessage(StringUtil.getMessage("rename-cancel"));
				return;
			}
			if (name.length() > 20) {
				p.sendMessage(StringUtil.getMessage("rename-too-long"));
				return;
			}

			p.sendMessage(StringUtil.getMessage("rename-success", rm.getPendingRename().get(p).getName(), name));
			RegionManager.get().setRegionName(rm.getPendingRename().get(p), name);

			rm.getPendingRename().remove(p);
		}
	}

}
