/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap.commands.bukkit;

import java.util.Base64;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import fr.moribus.imageonmap.AutoMod;
import fr.moribus.imageonmap.BanReason;
import fr.moribus.imageonmap.ColorChat;
import fr.moribus.imageonmap.ImageOnMap;

public class BanHashCommand implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (!sender.isOp()) { // boilerplate
			ColorChat.msg(sender, "&r&cYou do not have permission to run this command.");
			return false;
		}
		String hash;
		String timestr;
		try {
			hash = args[0];
		} catch (Exception e) {
			ColorChat.msg(sender, "&cyou must provide a hash!");
			return false;
		}
		try {
			timestr = args[1];
		} catch (Exception e) {
			ColorChat.msg(sender, "&cyou must provide a ban duration (eg, 3d, 30d, 10m)!");
			return true;
		}
		String reason = String.join(" ", args).replace(hash, "").replace(timestr, "").trim();
		if (reason.strip() == "") {
			ColorChat.msg(sender, "&cyou must provide a reason!");
			return false;
		}
		// if (args.length < 3) {
		// ColorChat.msg(sender,
		// "&r&cToo many parameters! &r&2Usage: &b/maptool ban &r&8[&r&bhash&r&8]
		// &r&8[&r&bduration&r&8] &r&8[&r&breason&r&8]");
		// return false;
		// }

		// handle the input of a base64 hash from thread.json
		if (hash.endsWith("=")) {
			hash = hash.replace("\\/", "/");
			try {
				StringBuilder hexString = new StringBuilder();
				for (byte b : Base64.getDecoder().decode(hash)) {
					String hex = Integer.toHexString(0xff & b); // Ensure positive value for hex representation
					if (hex.length() == 1) {
						hexString.append('0'); // Pad single-digit hex values with a leading zero
					}
					hexString.append(hex);
				}
				hash = hexString.toString();
				if (!Pattern.compile(AutoMod.HASH_PDQ_REGEX).matcher(hash).matches()) {
					ColorChat.msg(sender, "&r&cThis Hash seems to be incomplete, emprty or not a hash at all!");
					return false;
				}
			} catch (IllegalArgumentException e) {
				// Handle invalid Base64 input (e.g., characters not in the Base64 alphabet)
				ColorChat.msg(sender, "&r&cThis &r&&6base-64 encoded&r&c hash seems to be malformed!");
				return false;
			}
		}

		// standard HEX hash
		if (!Pattern.compile(AutoMod.HASH_PDQ_REGEX).matcher(hash).matches()) {
			ColorChat.msg(sender, "&r&cThis Hash seems to be incomplete, emprty or not a hash at all!");
			ColorChat.msg(sender, "&r&c" + hash);
			return false;
		}
		hash.toLowerCase(); // make formatting consistent
		// String reason = String.join(" ", args).replace(args[0], "").replace(args[1],
		// "").trim();

		if (!ImageOnMap.getPlugin().BannedHashes.keySet().contains(hash)) {
			ImageOnMap.getPlugin().BannedHashes.put(hash, new BanReason(reason, args[1]));
			ColorChat.msg(sender, "&r&2Banned hash: " + hash);
			return true;
		} else {
			ColorChat.msg(sender, "&r&cThis Hash is already banned.");
			ColorChat.msg(sender, "&r&6orignal reason: &r&8\"" + ImageOnMap.getPlugin().BannedHashes.get(hash).REASON
					+ "\"&r&6 duration: &r&8\"" + ImageOnMap.getPlugin().BannedHashes.get(hash).TIMESTR + "\"&r&6.");
			return true;
		}
	}
}
