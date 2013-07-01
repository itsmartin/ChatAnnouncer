package com.martinbrook.ChatAnnouncer;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ChatListener implements Listener {
	
	private ChatAnnouncer plugin;
	
	public ChatListener(ChatAnnouncer plugin) {
		this.plugin = plugin;
	}


	@EventHandler
	public void onPlayerChatEvent(AsyncPlayerChatEvent e) {
		if (plugin.isChatMuted() && !e.getPlayer().hasPermission("chatannouncer.speakwhenmuted"))
			e.setCancelled(true);
	}
	
	
	@EventHandler
	public void onPlayerComandPreprocessEvent(PlayerCommandPreprocessEvent e) {
		if (plugin.isChatMuted() && !e.getPlayer().hasPermission("chatannouncer.speakwhenmuted") && e.getMessage().toLowerCase().startsWith("/me "))
			e.setCancelled(true);
	}

}
