/*
 * This file is part of SpoutPlugin (http://www.spout.org/).
 *
 * SpoutPlugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SpoutPlugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.getspout.spout.player;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import org.bukkit.plugin.Plugin;

import org.getspout.commons.io.CRCStore;
import org.getspout.commons.io.CRCStoreRunnable;
import org.getspout.commons.io.FileUtil;
import org.getspout.commons.io.CRCStore.URLCheck;
import org.getspout.spout.Spout;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.packet.PacketCacheDeleteFile;
import org.getspout.spoutapi.packet.PacketPreCacheCompleted;
import org.getspout.spoutapi.packet.PacketPreCacheFile;
import org.getspout.spoutapi.player.FileManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class SimpleFileManager implements FileManager {
	private Map<Plugin,  List<File>> preLoginCache = new HashMap<Plugin,  List<File>>();
	private Map<Plugin,  List<String>> preLoginUrlCache = new HashMap<Plugin,  List<String>>();
	private Map<Plugin, List<String>> cachedFiles = new HashMap<Plugin,  List<String>>();
	private static final String[] validExtensions = {"txt", "yml", "xml", "png", "jpg", "ogg", "midi", "wav", "zip"};

	public void onPlayerJoin(final SpoutPlayer player) {
		if (player.isSpoutCraftEnabled()) {
			Iterator<Entry<Plugin, List<File>>> i = preLoginCache.entrySet().iterator();
			while (i.hasNext()) {
				Entry<Plugin, List<File>> next = i.next();
				for (File file : next.getValue()) {
					long crc = -1;
					try {
						byte[] data = new byte[FileUtils.readFileToByteArray(file).length];
						crc = FileUtil.getCRC(file, data);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (crc !=-1) {
						player.sendPacket(new PacketPreCacheFile(next.getKey().getDescription().getName(), file.getPath(), crc, false));
					}
				}
			}
			LinkedList<Thread> urlThreads = new LinkedList<Thread>();
			Iterator<Entry<Plugin, List<String>>> j = preLoginUrlCache.entrySet().iterator();
			while (j.hasNext()) {
				final Entry<Plugin, List<String>> next = j.next();
				for (final String url : next.getValue()) {
					URLCheck urlCheck = new URLCheck(url, new byte[4096], new CRCStoreRunnable() {
						Long CRC;

						public void setCRC(Long CRC) {
							this.CRC = CRC;
						}

						public void run() {
							player.sendPacket(new PacketPreCacheFile(next.getKey().getDescription().getName(), url, CRC, true));
						}
					});
					urlCheck.setName(url);
					urlCheck.start();
					urlThreads.add(urlCheck);
				}
			}
			new URLCheckJoin(urlThreads, player).start();
		}
	}

	private class URLCheckJoin extends Thread {
		private final List<Thread> threads;
		private final SpoutPlayer player;

		public URLCheckJoin(List<Thread> threads, SpoutPlayer player) {
			this.threads = threads;
			this.player = player;
		}

		public void run() {
			while (!threads.isEmpty()) {
				Iterator<Thread> i = threads.iterator();
				while (i.hasNext()) {
					Thread t = i.next();
					try {
						t.join();
						i.remove();
					} catch (InterruptedException ie) {
					}
				}
			}
			player.sendPacket(new PacketPreCacheCompleted());
		}
	}

	@Override
	public List<String> getCache(Plugin plugin) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		List<String> cache = cachedFiles.get(plugin);
		if (cache == null) {
			cache = new ArrayList<String>(1);
		}
		if (preLoginCache.containsKey(plugin)) {
			for (File f : preLoginCache.get(plugin)) {
				cache.add(f.toString());
			}
		}
		if (preLoginUrlCache.containsKey(plugin)) {
			for (String s : preLoginUrlCache.get(plugin)) {
				cache.add(s);
			}
		}
		return cache;
	}

	@Override
	public boolean addToPreLoginCache(Plugin plugin, File file) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (file == null) {
			throw new NullPointerException("File may not be null");
		}
		if (canCache(file)) {
			List<File> cache = preLoginCache.get(plugin);
			if (cache == null) {
				cache = new ArrayList<File>();
			}
			cache.add(file);
			preLoginCache.put(plugin, cache);
			return true;
		}
		return false;
	}

	@Override
	public boolean addToPreLoginCache(Plugin plugin, String fileUrl) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (fileUrl == null) {
			throw new NullPointerException("The file url may not be null");
		}
		if (canCache(fileUrl)) {
			List<String> cache = preLoginUrlCache.get(plugin);
			if (cache == null) {
				cache = new ArrayList<String>();
			}
			cache.add(fileUrl);
			preLoginUrlCache.put(plugin, cache);
			return true;
		}
		return false;
	}

	@Override
	public boolean addToPreLoginCache(Plugin plugin, Collection<File> files) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (files == null) {
			throw new NullPointerException("The file collection may not be null");
		}
		for (File file: files) {
			if (file == null || !file.exists() || file.isDirectory()) {
				throw new IllegalArgumentException("Invalid Files! Files must not be null and must exist!");
			}
			if (!canCache(file)) {
				return false;
			}
		}
		List<File> cache = preLoginCache.get(plugin);
		if (cache == null) {
			cache = new ArrayList<File>();
		}
		cache.addAll(files);
		preLoginCache.put(plugin, cache);
		return true;
	}

	@Override
	public boolean addToPreLoginCache(Plugin plugin, List<String> fileUrls) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (fileUrls == null) {
			throw new NullPointerException("The url list may not be null");
		}
		for (String file: fileUrls) {
			if (!canCache(file)) {
				return false;
			}
		}
		List<String> cache = preLoginUrlCache.get(plugin);
		if (cache == null) {
			cache = new ArrayList<String>();
		}
		cache.addAll(fileUrls);
		preLoginUrlCache.put(plugin, cache);
		return true;
	}

	@Override
	public boolean addToPreLoginCache(Plugin plugin, InputStream input, String fileName) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (input == null) {
			throw new NullPointerException("Inputstream may not be null");
		}
		if (fileName == null) {
			throw new NullPointerException("Filename may not be null");
		}
		if (canCache(fileName)) {
			File result = addToTempDirectory(input, fileName);
			if (result != null) {
				addToPreLoginCache(plugin, result);
			}
		}
		return false;
	}

	@Override
	public boolean addToCache(Plugin plugin, File file) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (file == null || !file.exists() || file.isDirectory()) {
			throw new IllegalArgumentException("Invalid File! Files must not be null and must exist!");
		}
		if (addToPreLoginCache(plugin, file)) {
			String fileName = FileUtil.getFileName(file.getPath());
			long crc = -1;
			try {
				crc = CRCStore.getCRC(fileName, FileUtils.readFileToByteArray(file));
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (crc !=-1) {
				for (SpoutPlayer player : SpoutManager.getOnlinePlayers()) {
					if (player.isSpoutCraftEnabled()) {
						player.sendPacket(new PacketPreCacheFile(plugin.getDescription().getName(), file.getPath(), crc, false));
					}
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean addToCache(final Plugin plugin, final String fileUrl) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (addToPreLoginCache(plugin, fileUrl)) {
			URLCheck urlCheck = new URLCheck(fileUrl, new byte[4096], new CRCStoreRunnable() {
				Long CRC;

				public void setCRC(Long CRC) {
					this.CRC = CRC;
				}

				public void run() {
					for (SpoutPlayer player : SpoutManager.getOnlinePlayers()) {
						if (player.isSpoutCraftEnabled()) {
							player.sendPacket(new PacketPreCacheFile(plugin.getDescription().getName(), fileUrl, CRC, true));
						}
					}
				}
			});
			urlCheck.start();
		}
		return false;
	}

	@Override
	public boolean addToCache(Plugin plugin, Collection<File> files) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		boolean success = true;
		for (File file : files) {
			if (!addToCache(plugin, file)) {
				success = false;
			}
		}
		return success;
	}

	@Override
	public boolean addToCache(Plugin plugin, List<String> fileUrls) {
		boolean success = true;
		for (String file : fileUrls) {
			if (!addToCache(plugin, file)) {
				success = false;
			}
		}
		return success;
	}


	@Override
	public boolean addToCache(Plugin plugin, InputStream input, String fileName) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		if (canCache(fileName)) {
			File result = addToTempDirectory(input, fileName);
			if (result != null) {
				addToCache(plugin, result);
			}
		}
		return false;
	}

	@Override
	public void removeFromCache(Plugin plugin, String file) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		List<File> cache = preLoginCache.get(plugin);
		if (cache != null) {
			Iterator<File> i = cache.iterator();
			while (i.hasNext()) {
				File next = i.next();
				String fileName = FileUtil.getFileName(next.getPath());
				if (fileName.equals(file)) {
					i.remove();
				}
			}
		}
		List<String> urlCache = preLoginUrlCache.get(plugin);
		if (urlCache != null) {
			Iterator<String> i = urlCache.iterator();
			while (i.hasNext()) {
				String next = i.next();
				String fileName = FileUtil.getFileName(next);
				if (fileName.equals(file)) {
					i.remove();
				}
			}
		}
		for (SpoutPlayer player : SpoutManager.getOnlinePlayers()) {
			if (player.isSpoutCraftEnabled()) {
				player.sendPacket(new PacketCacheDeleteFile(plugin.getDescription().getName(), file));
			}
		}
	}

	@Override
	public void removeFromCache(Plugin plugin, List<String> files) {
		if (plugin == null) {
			throw new NullPointerException("Plugin may not be null");
		}
		for (String file : files) {
			removeFromCache(plugin, file);
		}
	}

	@Override
	public boolean canCache(File file) {
		String filename = FileUtil.getFileName(file.getPath());
		return FilenameUtils.isExtension(filename, validExtensions);
	}

	@Override
	public boolean canCache(String fileUrl) {
		String filename = FileUtil.getFileName(fileUrl);
		return FilenameUtils.isExtension(filename, validExtensions);
	}

	private static File getTempDirectory() {
		File dir = new File(Spout.getInstance().getDataFolder(), "temp");
		if (!dir.exists()) {
			dir.mkdir();
		}
		return dir;
	}

	private static File addToTempDirectory(InputStream input, String fileName) {
		BufferedOutputStream output = null;
		try {
			File temp = new File(getTempDirectory(), fileName);
			output = new BufferedOutputStream(new FileOutputStream(temp));
			IOUtils.copy(input, output);
			return temp;
		} catch (IOException e) {
			return null;
		} finally {
			try {
				if (output != null) {
					output.close();
				}
			} catch (IOException ignore) { }
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ignore) { }
		}
	}

	public static void clearTempDirectory() {
		try {
			FileUtils.deleteDirectory(getTempDirectory());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
