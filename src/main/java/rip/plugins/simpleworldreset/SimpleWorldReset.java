package rip.plugins.simpleworldreset;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class SimpleWorldReset extends JavaPlugin {

    private static final String LAST_RESET_FILE = "last_reset_time.yml";

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        long startTime = System.currentTimeMillis();
        generateWorlds();
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        this.getLogger().info("Worlds generated in " + duration + "ms");

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        deleteWorlds();
    }

    private long getLastResetTime(String worldKey) {
        File file = new File(this.getDataFolder(), LAST_RESET_FILE);
        if (!file.exists()) return 0;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getLong(worldKey + ".last_reset_time", 0);
    }

    private void saveLastResetTime(String worldKey, long time) {
        File file = new File(this.getDataFolder(), LAST_RESET_FILE);
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        config.set(worldKey + ".last_reset_time", time);

        try {
            config.save(file);
        } catch (IOException e) {
            this.getLogger().warning("Failed to save last reset time for " + worldKey + ": " + e.getMessage());
        }
    }

    private void generateWorlds() {
        this.getConfig().getConfigurationSection("worlds").getKeys(false).forEach(key -> {
            processWorld(key);
        });
    }

    private void processWorld(String configKey) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("worlds");
        String name = section.getString(configKey + ".name");
        String file = section.getString(configKey + ".restore.file");
        long delayInHours = section.getLong(configKey + ".reset_delay_hours", 24); // Default to 24 hours

        if (name == null || file == null) {
            this.getLogger().warning("Invalid config for world " + configKey);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long lastResetTime = getLastResetTime(configKey);

        if ((currentTime - lastResetTime) < delayInHours * 3600 * 1000) {
            this.getLogger().info("Reset delay has not passed for world " + name + ". Skipping reset.");
            return;
        }

        deleteWorld(configKey);
        generateWorld(configKey);
        saveLastResetTime(configKey, currentTime);
    }

    private void generateWorld(String configKey) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("worlds");
        String name = section.getString(configKey + ".name");
        String file = section.getString(configKey + ".restore.file");

        if (name == null || file == null) {
            this.getLogger().warning("Invalid config for world " + configKey);
            return;
        }

//     if the world exists skip it
        if (this.getServer().getWorld(name) != null) {
            this.getLogger().info("World " + name + " already exists");
            return;
        }

//        unzip the world
        this.getLogger().info("Unzipping file: " + file);
        final TarGZipUnArchiver ua = new TarGZipUnArchiver();
        ua.setSourceFile(new File(file));
        ua.setDestDirectory(new File(this.getServer().getWorldContainer().getAbsolutePath()));
        ua.extract();

        String folderName = file.replace(".tar.gz", "");
        File worldFolder = new File(this.getServer().getWorldContainer().getAbsolutePath(), folderName);
        if (!worldFolder.exists()) {
            this.getLogger().warning("World folder " + worldFolder.getAbsolutePath() + " does not exist");
        }
    }

    private void deleteWorlds() {
        this.getConfig().getConfigurationSection("worlds").getKeys(false).forEach(key -> {
            deleteWorld(key);
        });
    }

    private void deleteWorld(String configKey) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("worlds");
        String name = section.getString(configKey + ".name");
        String file = section.getString(configKey + ".restore.file");

        if (name == null || file == null) {
            this.getLogger().warning("Invalid config for world " + configKey);
            return;
        }

        World world = this.getServer().getWorld(name);
        if (world == null) {
            this.getLogger().warning("World " + name + " does not exist");
            return;
        }

        this.getServer().unloadWorld(world, true);
        Bukkit.unloadWorld(world, false);
        File worldFolder = world.getWorldFolder();
        try {
            deleteFolder(worldFolder.toPath());
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().warning("Error occurred while deleting world " + name);
        }
    }

    private void deleteFolder(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
