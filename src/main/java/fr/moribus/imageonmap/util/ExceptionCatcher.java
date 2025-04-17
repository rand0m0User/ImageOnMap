package fr.moribus.imageonmap.util;

import java.util.logging.Level;

import fr.moribus.imageonmap.ImageOnMap;

public class ExceptionCatcher {

	public static void catchException(Thread thread, Throwable throwable) {
		ImageOnMap.getPlugin().getLogger().log(Level.SEVERE, "An exception occurred in the thread " + thread.getName(),
				throwable);
	}
}
