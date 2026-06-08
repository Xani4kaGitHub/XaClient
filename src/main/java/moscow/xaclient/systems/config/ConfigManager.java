package moscow.xaclient.systems.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;

public class ConfigManager {
   private final List<ConfigFile> configFiles = new ArrayList<>();
   private ConfigFile current;
   private boolean initialized = false;

   public void handle() {
      if (this.getAutoSaveConfig() == null) {
         this.createConfig("autosave");
      }

      if (!this.initialized) {
         this.scanConfigDirectory();
         this.initialized = true;
      }
   }

   public void directionConfig() {
      try {
         File configDir = new File(FileManager.DIRECTORY, "configs");
         String[] commands = new String[]{"explorer", configDir.getAbsolutePath()};
         Runtime.getRuntime().exec(commands);
      } catch (Exception exception) {
         XaClient.LOGGER.error("Failed to open configs directory: {}", exception.getMessage());
      }
   }

   public void createConfig(String name) {
      if (name != null) {
         this.refresh();
         ConfigFile config = new ConfigFile(name);
         if (config.getFileName().equals("autosave")) {
            config.load();
         }

         config.save();
         if (this.getConfig(config.getFileName()) == null) {
            this.configFiles.add(config);
         }
      }
   }

   public void listConfigs() {
      this.refresh();
      MessageUtility.info(Text.of("Configs:"));

      for (ConfigFile configFile : this.configFiles) {
         int idx = this.configFiles.indexOf(configFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + configFile.getFileName()));
      }
   }

   private void scanConfigDirectory() {
      this.configFiles.clear();
      Path configPath = Paths.get(FileManager.DIRECTORY.getPath(), "configs");
      if (!Files.exists(configPath)) {
         try {
            Files.createDirectories(configPath);
         } catch (IOException exception) {
            XaClient.LOGGER.error("Failed to create configs directory: {}", exception.getMessage());
         }

         return;
      }

      try (Stream<Path> stream = Files.list(configPath)) {
         stream.filter(Files::isRegularFile)
            .filter(path -> FileManager.hasSupportedExtension(path.getFileName().toString()))
            .sorted(Comparator.comparingInt(this::extensionPriority))
            .forEach(path -> {
               String name = FileManager.stripSupportedExtension(path.getFileName().toString());
               if (this.configFiles.stream().noneMatch(configFile -> configFile.getFileName().equalsIgnoreCase(name))) {
                  this.configFiles.add(new ConfigFile(name));
               }
            });
      } catch (IOException exception) {
         XaClient.LOGGER.error("Failed to scan configs directory: {}", exception.getMessage());
      }
   }

   private int extensionPriority(Path path) {
      return path.getFileName().toString().endsWith("." + FileManager.DEFAULT_FILE_TYPE) ? 0 : 1;
   }

   public ConfigFile getConfig(String name, boolean rescan) {
      if (rescan) {
         this.scanConfigDirectory();
      }

      String normalizedName = FileManager.stripSupportedExtension(name);
      return this.configFiles.stream().filter(configFile -> configFile.getFileName().equalsIgnoreCase(normalizedName)).findFirst().orElse(null);
   }

   public ConfigFile getConfig(String name) {
      return this.getConfig(name, false);
   }

   public ConfigFile getAutoSaveConfig() {
      return this.current != null ? this.current : this.getConfig("autosave", false);
   }

   public void refresh() {
      this.scanConfigDirectory();
   }

   @Generated
   public List<ConfigFile> getConfigFiles() {
      return this.configFiles;
   }

   @Generated
   public ConfigFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(ConfigFile current) {
      this.current = current;
   }
}
