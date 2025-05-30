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

package fr.moribus.imageonmap;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import pdqhashing.types.Hash256;

public class AutoMod {
	public static String HASH_PDQ_REGEX = "^[0-9A-Fa-f]{64}+$";
	public static String B64_HASH_PDQ_REGEX = "^[A-Za-z0-9+/]{42}=$";

	// auto moderation subsystem to prevent "unwanted" or potentially illegal images
	// from being posted, automatically bans the uploader (name + IP) with a reason
	// configurable
	// in the configuration

	public static void DoFancyBan(String offendinghash, UUID playerUUID) {
		Player p = Bukkit.getServer().getPlayer(playerUUID);
		String ip = "<offline>"; // default value
		if (p != null) {
			ip = p.getAddress().getAddress().toString().replace("/", "");
		}
		String timestr = ImageOnMap.getPlugin().BannedHashes.get(offendinghash).TIMESTR;
		String reason = ImageOnMap.getPlugin().BannedHashes.get(offendinghash).REASON;
		boolean perm = timestr.equals("PERM");
		String msg;
		if (perm) {
			 msg = PluginConfiguration.PERMBANNED_PDQ_MESSAGE.get();
		} else {
			 msg = PluginConfiguration.BANNED_PDQ_MESSAGE.get();
		}
		Duration d = null;
		if (!perm) {
			d = parseTime(timestr);
			String expireson = d.toDays() + " days and " + ((d.toDays() == 0) ? d.toHours() : d.toHours() % d.toDays())
					+ " hours";
			msg = msg.replace("%EXPIRES%", expireson);
		}

		msg = msg.replace("\\n", "\n");
		msg = msg.replace("%REASON%", reason);
		msg = msg.replace("%TIME%", formatTime(LocalDateTime.now()));
		msg = msg.replace("%IP%", ip);
		msg = msg.replace("%NAME%", p.getName());

		// re sync to main server thread
		final String message = msg;
		final Duration duration = d;
		if (perm) {
			Bukkit.getScheduler().runTask(ImageOnMap.getPlugin(), () -> {
				p.ban(message, (Duration) null, null);

				p.banPlayerIP(message, false);
			});
		} else {
			Bukkit.getScheduler().runTask(ImageOnMap.getPlugin(), () -> {
				p.ban(message, duration, null);
				p.banIp(message, duration, "", false);
			});
		}

	}

	public static String formatTime(LocalDateTime t) {
		return t.format(DateTimeFormatter.ofPattern("MMM dd,, yyyy")).replace(",,", "th").replace(" 0", " ")
				.replace("1th", "1st").replace("2th", "2nd").replace("3th", "3rd");
	}

	public static Duration parseTime(String arg) {
		switch (arg.charAt(arg.length() - 1)) {
		case 's':
			return Duration.ofSeconds(Integer.parseInt(arg.replace('s', ' ').trim()));
		case 'm':
			return Duration.ofMinutes(Integer.parseInt(arg.replace('m', ' ').trim()));
		case 'h':
			return Duration.ofHours(Integer.parseInt(arg.replace('h', ' ').trim()));
		case 'd':
			return Duration.ofDays(Integer.parseInt(arg.replace('d', ' ').trim()));
		default:
			return Duration.ZERO;
		}
	}

	// check the hash of an image to all hashes in the database, if there is a
	// match, ban the player and prevent the map from being rendered the rest of the
	// way
	public static boolean IsBanned(String hash, UUID playerUUID) {
		Hash256 img = GetH256(hash);
		for (String dbh : ImageOnMap.getPlugin().BannedHashes.keySet()) {
			int dist = img.hammingDistance(GetH256(dbh));
			Bukkit.getServer().getConsoleSender()
					.sendMessage("compare: " + hash.toString() + ", in db: " + dbh + ", hammingDistance: " + dist);
			if (dist <= 10) { // tolerance
				Bukkit.getServer().getConsoleSender()
						.sendMessage("PDQ hash of image likly matches! returning ban. user posted image:"
								+ hash.toString() + ", in db: " + dbh + ", hammingDistance: " + dist);
				DoFancyBan(hash, playerUUID);
				return true;
			}
		}
		return false;
	}

	// duplicate code removal
	private static Hash256 GetH256(String hash) {
		try {
			return Hash256.fromHexString(hash);
		} catch (Exception e) {
			if (!Pattern.compile(HASH_PDQ_REGEX).matcher(hash).matches()) {
				Bukkit.getServer().getConsoleSender().sendMessage("invalid PDQ hsh! :" + hash);
			}
			return null; // not a valid hash, should never happen!
		}
	}
}