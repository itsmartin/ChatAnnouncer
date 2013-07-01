package com.martinbrook.ChatAnnouncer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;


public class ChatAnnouncer extends JavaPlugin {
	private ArrayList<String> chatScript;
	private Boolean chatMuted = false;
	
	public static final ChatColor MAIN_COLOR = ChatColor.GREEN, SIDE_COLOR = ChatColor.GOLD, OK_COLOR = ChatColor.GREEN, WARN_COLOR = ChatColor.LIGHT_PURPLE, ERROR_COLOR = ChatColor.RED,
			DECISION_COLOR = ChatColor.GOLD, ALERT_COLOR = ChatColor.GREEN;
	
	public static final Pattern commandPattern = Pattern.compile("\\[\\&(\\w+)\\]");	
	
	@Override
    public void onEnable() {
		getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }
 
    
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
    	String c = cmd.getName().toLowerCase();
    	String response = null;
    	
    	if (sender.hasPermission("chatannouncer.broadcast") && (c.equals("ca") || c.equals("chatannouncer")))
    		response = cChatannouncer(args);
    	if (sender.hasPermission("chatannouncer.broadcast") && (c.equals("muteall")))
    		response = cMuteall(args);
    	
		if (response != null)
			sender.sendMessage(response);
		
		return true;
    }
    
    private String cChatannouncer(String[] args) {
    	if (this.chatScript != null) return ERROR_COLOR + "Announcement already in progress. Please wait.";
		String scriptFile;
		if (args.length < 1)
			scriptFile = "announcement.txt"; 
		else
			scriptFile = args[0];
		this.playChatScript(scriptFile, true);
		return OK_COLOR + "Broadcasting " + SIDE_COLOR + scriptFile;
    }


	private String cMuteall(String[] args) {
		if (args.length < 1)
			return ERROR_COLOR +"Please specify 'on' or 'off'";
	
		if (args[0].equalsIgnoreCase("on")) {
			this.setChatMuted(true);
			return OK_COLOR + "Chat muted!";
		}
		if (args[0].equalsIgnoreCase("off")) {
			this.setChatMuted(false);
			return OK_COLOR + "Chat unmuted!";
		}
		
		return ERROR_COLOR + "Please specify 'on' or 'off'";
	
	}


	/**
	 * Plays a chat script
	 * 
	 * @param filename The file to read the chat script from
	 * @param muteChat Whether other chat should be muted
	 */
	public void playChatScript(String filename, boolean muteChat) {
		if (muteChat) this.setChatMuted(true);
		chatScript = this.readFile(filename);
		if (chatScript != null)
			continueChatScript(1L);
	}
	
	private void continueChatScript(Long delay) {
		if (chatScript.size() > 0) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				public void run() {
					playNextLines();
				}
			}, delay);
		}
	}
		
		
	/**
	 * Output next line of current chat script, unmuting the chat if it's finished.
	 */
	private void playNextLines() {
		String line;
		while (chatScript.size() > 0) {
			line=chatScript.remove(0);
			if (line.toLowerCase().startsWith("&pause")) {
				int pauseDuration;
				try {
					pauseDuration = Integer.parseInt(line.substring(6).trim());
				} catch (NumberFormatException e) {
					pauseDuration = 2;
				}
				
				continueChatScript(20L * pauseDuration);
				return;
			}
			getServer().broadcastMessage(parseLine(line));
		}
		this.setChatMuted(false);
		chatScript = null;
	}
	
	/**
	 * Converts color codes from input into Bukkit chat colors.
	 * 
	 * @return input string with Bukkit color codes parsed
	 */
	private String parseLine(String input) {
		Matcher m = commandPattern.matcher(input);
		StringBuffer sb = new StringBuffer();
		while(m.find()) {
			String commandResult = processCommand(m.group(1).toLowerCase());
			if (commandResult != null) {
				m.appendReplacement(sb, commandResult);
			}
		}
		m.appendTail(sb);
		return sb.toString();
		
	}
    
	private String processCommand(String command) {
		// Try for a color code
		try {
			ChatColor color = ChatColor.valueOf(command.toUpperCase());
			return color.toString();
		} catch (IllegalArgumentException e) {
		}
		return null;
		
	}
	/**
	 * @return Whether chat is currently muted
	 */
	public boolean isChatMuted() {
		return chatMuted;
	}
	
	/**
	 * Mute or unmute chat
	 * 
	 * @param muted Status to be set
	 */
	public void setChatMuted(Boolean muted) {
		chatMuted = muted;
	}
	
	/**
	 * Read in a file from the plugin data folder, and return its contents as an ArrayList<String> 
	 * 
	 * @param filename
	 * @return
	 */
	public ArrayList<String> readFile(String filename) {
		File fChat = getPluginDataFile(filename, true);
		
		if (fChat == null) return null;
		
		ArrayList<String> lines = new ArrayList<String>();
		try {
			FileReader fr = new FileReader(fChat);
			BufferedReader in = new BufferedReader(fr);
			String s = in.readLine();
	
			while (s != null) {
				lines.add(s);
				s = in.readLine();
			}
	
			in.close();
			fr.close();
			return lines;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Retrieve a File from the plugin data folder.
	 *
	 * @param filename Filename to retrieve
	 * @param mustAlreadyExist true if the file should already exist
	 * @return The specified data file, or null if not found
	 */
	public File getPluginDataFile(String filename, boolean mustAlreadyExist) {
		return getDataFile(getPluginFolder(), filename, mustAlreadyExist);
	}
	
	/**
	 * Initialise the data folder for the plugin.
	 *
	 * @return The data folder for the plugin, or null if it couldn't be created
	 */
	public File getPluginFolder() {
	    File pluginFolder = this.getDataFolder();
	    if (!pluginFolder.isDirectory()){
	        if (!pluginFolder.mkdirs()) {
	            // failed to create the non existent directory, so failed
	            return null;
	        }
	    }
	    return pluginFolder;
	}


	/**
	 * Retrieve a File from a specific folder.
	 * 
	 * @param folder The folder to look for the file in
	 * @param filename Filename to retrieve
	 * @param mustAlreadyExist true if the file should already exist
	 * @return The specified data file, or null if not found
	 */
	public File getDataFile(File folder, String filename, boolean mustAlreadyExist) {
	    if (folder != null) {
	        File file = new File(folder, filename);
	        if (mustAlreadyExist) {
	            if (file.exists()) {
	                return file;
	            }
	        } else {
	            return file;
	        }
	    }
	    return null;
	}
}
