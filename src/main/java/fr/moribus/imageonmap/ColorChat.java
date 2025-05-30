package fr.moribus.imageonmap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class ColorChat {

	public static String c(String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public static void msg(Player p, String string) {
		p.sendMessage(c(string));
	}

	public static void msg(CommandSender sender, String string) {
		sender.sendMessage(c(string));
	}
}
