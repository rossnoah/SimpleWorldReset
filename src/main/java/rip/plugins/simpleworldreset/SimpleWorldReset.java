package rip.plugins.simpleworldreset;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class SimpleWorldReset extends JavaPlugin {

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



    private void generateWorlds(){
        this.getConfig().getConfigurationSection("worlds").getKeys(false).forEach(key -> {
            generateWorld(key);
        });
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
            return;
        }

    }

    private void deleteWorlds(){
        this.getConfig().getConfigurationSection("worlds").getKeys(false).forEach(key -> {
            deleteWorld(key);
        });

    }

    private void deleteWorld(String configKey){
        ConfigurationSection section = this.getConfig().getConfigurationSection("worlds");
        String name = section.getString(configKey + ".name");
        String file = section.getString(configKey + ".restore.file");

        if (name == null || file == null) {
            this.getLogger().warning("Invalid config for world " + configKey);
            return;
        }


        World world = this.getServer().getWorld(name);
        if(world == null) {
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
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}



