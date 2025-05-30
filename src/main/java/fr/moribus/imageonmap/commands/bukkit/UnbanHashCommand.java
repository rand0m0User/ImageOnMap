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
import fr.moribus.imageonmap.ColorChat;
import fr.moribus.imageonmap.ImageOnMap;

public class UnbanHashCommand implements CommandExecutor {

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
			@NotNull String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage("You do not have permission to run this command.");
			return false;
		}
		if (args.length > 1) {
			ColorChat.msg(sender, "Too many parameters! Usage: /unbanhash [hash]");
			return false;
		}
		String hash = args[0];
		// handle the input of a base64 hash from thread.json
		if (Pattern.compile(AutoMod.B64_HASH_PDQ_REGEX).matcher(hash).matches()) {
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
			ColorChat.msg(sender, "&cThis Hash seems to be incomplete, emprty or not a hash at all!");
			return false;
		}
		if (ImageOnMap.getPlugin().BannedHashes.keySet().contains(hash)) {
			ImageOnMap.getPlugin().BannedHashes.remove(hash);
			ColorChat.msg(sender, "&r&2Unbanned the hash: " + hash);
			return true;
		} else {
			ColorChat.msg(sender, "&cThis Hash is not banned.");
			return true;
		}
	}
}
