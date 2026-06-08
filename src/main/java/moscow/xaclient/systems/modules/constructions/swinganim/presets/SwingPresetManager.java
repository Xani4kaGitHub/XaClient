package moscow.xaclient.systems.modules.constructions.swinganim.presets;

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
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;

public class SwingPresetManager {
   private final List<SwingPresetFile> swingPresetFiles = new ArrayList<>();
   private SwingPresetFile current;
   private boolean initialized = false;

   @CompileBytecode
   public void handle() {
      if (this.getAutoSavePreset() == null) {
         this.createPreset("autosave");
      }

      if (!this.initialized) {
         this.scanPresetDirectory();
         this.initialized = true;
      }
   }

   public void directionPreset() {
      String[] commands = new String[]{"explorer " + new File(FileManager.DIRECTORY + "/presets", "swing").getAbsolutePath()};

      try {
         Runtime.getRuntime().exec(commands);
      } catch (Exception exception) {
         XaClient.LOGGER.error("Failed to open swing presets directory: {}", exception.getMessage());
      }
   }

   public void createPreset(String name) {
      if (name != null) {
         String normalizedName = FileManager.stripSupportedExtension(name);
         if (this.getPreset(normalizedName, false) != null) {
            XaClient.LOGGER.warn("Preset {} already exists", normalizedName);
         } else {
            SwingPresetFile preset = new SwingPresetFile(normalizedName);
            if (normalizedName.equals("autosave")) {
               preset.load();
            }

            preset.save();
            this.swingPresetFiles.add(preset);
         }
      }
   }

   public void listPresets() {
      MessageUtility.info(Text.of("Swing presets:"));

      for (SwingPresetFile swingPresetFile : this.swingPresetFiles) {
         int idx = this.swingPresetFiles.indexOf(swingPresetFile) + 1;
         MessageUtility.info(Text.of("[" + idx + "] " + swingPresetFile.getFileName()));
      }
   }

   private void scanPresetDirectory() {
      this.swingPresetFiles.clear();
      Path presetPath = Paths.get(FileManager.DIRECTORY + "/presets", "swing");
      if (!Files.exists(presetPath)) {
         try {
            Files.createDirectories(presetPath);
         } catch (IOException exception) {
            XaClient.LOGGER.error("Failed to create swing presets directory: {}", exception.getMessage());
         }

         return;
      }

      try (Stream<Path> stream = Files.list(presetPath)) {
         stream.filter(Files::isRegularFile)
            .filter(path -> FileManager.hasSupportedExtension(path.getFileName().toString()))
            .sorted(Comparator.comparingInt(this::extensionPriority))
            .forEach(path -> {
               String name = FileManager.stripSupportedExtension(path.getFileName().toString());
               if (this.swingPresetFiles.stream().noneMatch(preset -> preset.getFileName().equalsIgnoreCase(name))) {
                  this.swingPresetFiles.add(new SwingPresetFile(name));
               }
            });
      } catch (IOException exception) {
         XaClient.LOGGER.error("Failed to scan swing presets directory: {}", exception.getMessage());
      }
   }

   private int extensionPriority(Path path) {
      return path.getFileName().toString().endsWith("." + FileManager.DEFAULT_FILE_TYPE) ? 0 : 1;
   }

   public SwingPresetFile getPreset(String name, boolean rescan) {
      if (rescan) {
         this.scanPresetDirectory();
      }

      String normalizedName = FileManager.stripSupportedExtension(name);
      return this.swingPresetFiles.stream().filter(swingPresetFile -> swingPresetFile.getFileName().equalsIgnoreCase(normalizedName)).findFirst().orElse(null);
   }

   public SwingPresetFile getPreset(String name) {
      return this.getPreset(name, false);
   }

   public SwingPresetFile getAutoSavePreset() {
      return this.getPreset("autosave", true);
   }

   public void refresh() {
      this.scanPresetDirectory();
   }

   @Generated
   public List<SwingPresetFile> getSwingPresetFiles() {
      return this.swingPresetFiles;
   }

   @Generated
   public SwingPresetFile getCurrent() {
      return this.current;
   }

   @Generated
   public boolean isInitialized() {
      return this.initialized;
   }

   @Generated
   public void setCurrent(SwingPresetFile current) {
      this.current = current;
   }
}
