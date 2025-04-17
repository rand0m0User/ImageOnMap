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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import fr.moribus.imageonmap.commands.bukkit.*;
import fr.moribus.imageonmap.commands.Commands;
import fr.moribus.imageonmap.commands.maptool.DeleteCommand;
import fr.moribus.imageonmap.commands.maptool.ExploreCommand;
import fr.moribus.imageonmap.commands.maptool.GetCommand;
import fr.moribus.imageonmap.commands.maptool.GetRemainingCommand;
import fr.moribus.imageonmap.commands.maptool.GiveCommand;
import fr.moribus.imageonmap.commands.maptool.ListCommand;
import fr.moribus.imageonmap.commands.maptool.NewCommand;
import fr.moribus.imageonmap.commands.maptool.RenameCommand;
import fr.moribus.imageonmap.commands.maptool.UpdateCommand;
import fr.moribus.imageonmap.gui.Gui;
import fr.moribus.imageonmap.i18n.I18n;
import fr.moribus.imageonmap.image.MapInitEvent;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.ui.MapItemManager;

public final class ImageOnMap extends JavaPlugin {

	private static ImageOnMap PLUGIN;

	private FileConfiguration BannedHashesyml;
	private File BannedHashesFile;
	public List<String> BannedHashes;
	private final Path mapsDirectory;
	private final Path imagesDirectory;

	public ImageOnMap() {
		PLUGIN = this;

		var folder = getDataFolder().toPath();
		mapsDirectory = folder.resolve("maps");
		imagesDirectory = folder.resolve("images");
	}

	public static ImageOnMap getPlugin() {
		return PLUGIN;
	}

	public Path getImagesDirectory() {
		return imagesDirectory;
	}

	public Path getMapsDirectory() {
		return mapsDirectory;
	}

	public Path getImageFile(int mapID) {
		return imagesDirectory.resolve("map" + mapID + ".png");
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		// Creating the images and maps directories if necessary
		try {
			checkPluginDirectory(mapsDirectory);
			checkPluginDirectory(imagesDirectory);
		} catch (final IOException ex) {
			getLogger().log(Level.SEVERE, "FATAL: " + ex.getMessage());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		saveDefaultConfig();
		Gui.clearOpenGuis();

		JarFile jarFile = getJarFile();

		try {
			I18n.onEnable(jarFile);
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
					ImageOnMap.getPlugin().getLogger().log(Level.SEVERE,
							"Unable to close JAR file " + getFile().getAbsolutePath(), e);
				}
			}
		}

		// ####################################
		// file loader: hashes

		// Create the data file and load the data configuration
		BannedHashesFile = new File(getDataFolder(), "BannedImageHashes.yml");
		if (!BannedHashesFile.exists()) {
			saveResource("BannedImageHashes.yml", false);
		}
		BannedHashesyml = YamlConfiguration.loadConfiguration(BannedHashesFile);

		// Load the string list from the data configuration
		BannedHashes = BannedHashesyml.getStringList("BannedHashes");

		// ####################################

		PluginConfiguration.BANNED_PDQ_MESSAGE.get();

		// Init all the things !
		I18n.setPrimaryLocale(PluginConfiguration.LANG.get());

		MapManager.init();
		MapInitEvent.init();
		MapItemManager.init();

		Commands.register("maptool", NewCommand.class, ListCommand.class, GetCommand.class, RenameCommand.class,
				DeleteCommand.class, GiveCommand.class, GetRemainingCommand.class, ExploreCommand.class,
				UpdateCommand.class);

		Commands.registerShortcut("maptool", NewCommand.class, "tomap");
		Commands.registerShortcut("maptool", ExploreCommand.class, "maps");
		Commands.registerShortcut("maptool", GiveCommand.class, "givemap");

		initCommand("banhash", new BanHashCommand(), null);
		initCommand("unbanhash", new UnbanHashCommand(), null);

	}

	@Override
	public void onDisable() {
		MapManager.exit();
		MapItemManager.exit();

		// ####################################
		// file loader: hashes

		// Save the string list to the data configuration
		BannedHashesyml.set("BannedHashes", BannedHashes);

		// Save the data configuration to the data file
		try {
			BannedHashesyml.save(BannedHashesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// ####################################

		Gui.clearOpenGuis();
	}

	private void checkPluginDirectory(Path directory) throws IOException {
		if (!Files.isDirectory(directory)) {
			Files.createDirectories(directory);
		}
	}

	/**
	 * Gets the .jar file this plugin is loaded by, or null if it wasn't found.
	 */
	public JarFile getJarFile() {
		try {
			return new JarFile(getFile());
		} catch (IOException e) {
			ImageOnMap.getPlugin().getLogger().log(Level.SEVERE,
					"Unable to load JAR file " + getFile().getAbsolutePath(), e);
			return null;
		}
	}

	public static void DoFancyBan(UUID playerUUID) {
		Player p = Bukkit.getServer().getPlayer(playerUUID);
		String ip = "<offline>"; // default value
		if (p != null) {
			ip = p.getAddress().getAddress().toString().replace("/", "");
		}
		String msg = PluginConfiguration.BANNED_PDQ_MESSAGE.get();
		msg = msg.replace("\\n", "\n");
		msg = msg.replace("%TIME%", formatTime(LocalDateTime.now()));
		msg = msg.replace("%IP%", ip);
		msg = msg.replace("%NAME%", p.getName());

		// re sync to main server thread
		final String message = msg;
		Bukkit.getScheduler().runTask(PLUGIN, () -> {
			p.ban(message, (Duration) null, null);
			p.banPlayerIP(message, false);
		});

	}

	private static String formatTime(LocalDateTime t) {
		return t.format(DateTimeFormatter.ofPattern("MMM dd,, yyyy")).replace(",,", "th").replace(" 0", " ")
				.replace("1th", "1st").replace("2th", "2nd").replace("3th", "3rd");
	}

	private void initCommand(String cmd, CommandExecutor Executor, TabCompleter Completer) {
		PluginCommand c = getCommand(cmd);
		if (c != null) {
			c.setExecutor(Executor);
			c.setTabCompleter(Completer);
		}
	}
}
