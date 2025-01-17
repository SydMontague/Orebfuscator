/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.DeprecatedMethods;
import com.lishid.orebfuscator.NmsInstance;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

public class ConfigManager {
	private static final int CONFIG_VERSION = 13;

	private JavaPlugin plugin;
	private Logger logger;
	private OrebfuscatorConfig orebfuscatorConfig;
	private MaterialReader materialReader;

	public ConfigManager(JavaPlugin plugin, Logger logger, OrebfuscatorConfig orebfuscatorConfig) {
		this.plugin = plugin;
		this.logger = logger;
		this.orebfuscatorConfig = orebfuscatorConfig;
		this.materialReader = new MaterialReader(this.plugin, this.logger);
	}

	public WorldConfig getWorld(World world) {
		if (world == null) {
			return null;
		}

		WorldConfig baseCfg;

		switch (world.getEnvironment()) {
		case THE_END:
			baseCfg = this.orebfuscatorConfig.getEndWorld();
			break;
		case NETHER:
			baseCfg = this.orebfuscatorConfig.getNetherWorld();
			break;
		default:
			baseCfg = this.orebfuscatorConfig.getNormalWorld();
			break;
		}

		WorldConfig cfg = this.orebfuscatorConfig.getWorld(world.getName());

		if (cfg == null) {
			return baseCfg;
		}

		if (!cfg.isInitialized()) {
			cfg.init(baseCfg);
			this.logger.log(Level.INFO, Globals.LogPrefix + "Config for world '" + world.getName() + "' is initialized.");
		}

		return cfg;
	}

	public void load() {
		// Version check
		int version = getInt("ConfigVersion", CONFIG_VERSION);
		if (version < CONFIG_VERSION) {
			if (version <= 12) {
				new Convert12To13(this.plugin).convert();
				logger.info(Globals.LogPrefix + "Configuration file have been converted to new version.");
			} else {
				getConfig().set("ConfigVersion", CONFIG_VERSION);
			}
		}

		boolean useCache = getBoolean("Booleans.UseCache", true);
		int maxLoadedCacheFiles = getInt("Integers.MaxLoadedCacheFiles", 64, 16, 128);
		String cacheLocation = getString("Strings.CacheLocation", "orebfuscator_cache");
		int deleteCacheFilesAfterDays = getInt("Integers.DeleteCacheFilesAfterDays", 0);
		boolean enabled = getBoolean("Booleans.Enabled", true);
		boolean updateOnDamage = getBoolean("Booleans.UpdateOnDamage", true);

		int engineMode = getInt("Integers.EngineMode", 2);
		if (engineMode != 1 && engineMode != 2) {
			engineMode = 2;
			logger.info(Globals.LogPrefix + "EngineMode must be 1 or 2.");
		}

		int initialRadius = getInt("Integers.InitialRadius", 1, 0, 2);
		if (initialRadius == 0) {
			logger.info(Globals.LogPrefix
					+ "Warning, InitialRadius is 0. This will cause all exposed blocks to be obfuscated.");
		}

		int updateRadius = getInt("Integers.UpdateRadius", 2, 1, 5);
		boolean noObfuscationForMetadata = getBoolean("Booleans.NoObfuscationForMetadata", true);
		String noObfuscationForMetadataTagName = getString("Strings.NoObfuscationForMetadataTagName", "NPC");
		boolean noObfuscationForOps = getBoolean("Booleans.NoObfuscationForOps", false);
		boolean noObfuscationForPermission = getBoolean("Booleans.NoObfuscationForPermission", false);
		boolean loginNotification = getBoolean("Booleans.LoginNotification", true);
		byte[] transparentBlocks = generateTransparentBlocks(engineMode);

		this.orebfuscatorConfig.setUseCache(useCache);
		this.orebfuscatorConfig.setMaxLoadedCacheFiles(maxLoadedCacheFiles);
		this.orebfuscatorConfig.setCacheLocation(cacheLocation);
		this.orebfuscatorConfig.setDeleteCacheFilesAfterDays(deleteCacheFilesAfterDays);
		this.orebfuscatorConfig.setEnabled(enabled);
		this.orebfuscatorConfig.setUpdateOnDamage(updateOnDamage);
		this.orebfuscatorConfig.setEngineMode(engineMode);
		this.orebfuscatorConfig.setInitialRadius(initialRadius);
		this.orebfuscatorConfig.setUpdateRadius(updateRadius);
		this.orebfuscatorConfig.setNoObfuscationForMetadata(noObfuscationForMetadata);
		this.orebfuscatorConfig.setNoObfuscationForMetadataTagName(noObfuscationForMetadataTagName);
		this.orebfuscatorConfig.setNoObfuscationForOps(noObfuscationForOps);
		this.orebfuscatorConfig.setNoObfuscationForPermission(noObfuscationForPermission);
		this.orebfuscatorConfig.setLoginNotification(loginNotification);
		this.orebfuscatorConfig.setTransparentBlocks(transparentBlocks);

		new WorldReader(this.plugin, this.logger, this.orebfuscatorConfig, this.materialReader).load();

		this.orebfuscatorConfig.setProximityHiderEnabled();

		this.logger.info(Globals.LogPrefix + "Proximity Hider is "
				+ (this.orebfuscatorConfig.isProximityHiderEnabled() ? "Enabled" : "Disabled"));

		save();
	}

	public void setEngineMode(int value) {
		getConfig().set("Integers.EngineMode", value);
		save();
		this.orebfuscatorConfig.setEngineMode(value);
	}

	public void setUpdateRadius(int value) {
		getConfig().set("Integers.UpdateRadius", value);
		save();
		this.orebfuscatorConfig.setUpdateRadius(value);
	}

	public void setInitialRadius(int value) {
		getConfig().set("Integers.InitialRadius", value);
		save();
		this.orebfuscatorConfig.setInitialRadius(value);
	}

	public void setProximityHiderDistance(int value) {
		getConfig().set("Integers.ProximityHiderDistance", value);
		save();
		this.orebfuscatorConfig.getDefaultWorld().getProximityHiderConfig().setDistance(value);
	}

	public void setNoObfuscationForOps(boolean value) {
		getConfig().set("Booleans.NoObfuscationForOps", value);
		save();
		this.orebfuscatorConfig.setNoObfuscationForOps(value);
	}

	public void setNoObfuscationForPermission(boolean value) {
		getConfig().set("Booleans.NoObfuscationForPermission", value);
		save();
		this.orebfuscatorConfig.setNoObfuscationForPermission(value);
	}

	public void setLoginNotification(boolean value) {
		getConfig().set("Booleans.LoginNotification", value);
		save();
		this.orebfuscatorConfig.setLoginNotification(value);
	}

	public void setUseCache(boolean value) {
		getConfig().set("Booleans.UseCache", value);
		save();
		this.orebfuscatorConfig.setUseCache(value);
	}

	public void setEnabled(boolean value) {
		getConfig().set("Booleans.Enabled", value);
		save();
		this.orebfuscatorConfig.setEnabled(value);
	}

	private FileConfiguration getConfig() {
		return this.plugin.getConfig();
	}

	private void save() {
		this.plugin.saveConfig();
	}

	private String getString(String path, String defaultData, boolean withSave) {
		if (getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			getConfig().set(path, defaultData);
		}

		return getConfig().getString(path, defaultData);
	}

	private String getString(String path, String defaultData) {
		return getString(path, defaultData, true);
	}

	private int getInt(String path, int defaultData) {
		return getInt(path, defaultData, true);
	}

	private int getInt(String path, int defaultData, boolean withSave) {
		if (getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			getConfig().set(path, defaultData);
		}

		return getConfig().getInt(path, defaultData);
	}

	private int getInt(String path, int defaultData, int min, int max, boolean withSave) {
		if (getConfig().get(path) == null && withSave) {
			getConfig().set(path, defaultData);
		}

		int value = getConfig().get(path) != null ? getConfig().getInt(path, defaultData) : defaultData;

		if (value < min) {
			value = min;
		} else if (value > max) {
			value = max;
		}

		return value;
	}

	private int getInt(String path, int defaultData, int min, int max) {
		return getInt(path, defaultData, min, max, true);
	}

	private boolean getBoolean(String path, boolean defaultData, boolean withSave) {
		if (getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			getConfig().set(path, defaultData);
		}

		return getConfig().getBoolean(path, defaultData);
	}

	private boolean getBoolean(String path, boolean defaultData) {
		return getBoolean(path, defaultData, true);
	}

	private byte[] generateTransparentBlocks(int engineMode) {
		byte[] transparentBlocks = new byte[MaterialHelper.getMaxId() + 1];

		Arrays.stream(Material.values())
			.filter(material -> material.isBlock() && DeprecatedMethods.isTransparent(material))
			.forEach(material -> NmsInstance.get().getMaterialIds(material).forEach(id -> transparentBlocks[id] = 1));

		Arrays.stream(NmsInstance.get().getExtraTransparentBlocks())
			.forEach(material -> NmsInstance.get().getMaterialIds(material).forEach(id -> transparentBlocks[id] = 1));

		byte status = (byte) (engineMode == 1 ? 0 : 1);
		NmsInstance.get().getMaterialIds(Material.LAVA).stream().forEach(id -> transparentBlocks[id] = status);

		return transparentBlocks;
	}
}