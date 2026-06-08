package moscow.xaclient.systems.modules.constructions.swinganim.presets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.systems.setting.Setting;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

public class SwingPresetFile implements IMinecraft {
   private final File file;
   private final File legacyFile;
   private final String fileName;
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation activeAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

   public SwingPresetFile(String fileName) {
      this.fileName = FileManager.stripSupportedExtension(fileName);
      File configsFolder = new File(FileManager.DIRECTORY + "/presets", "swing");
      if (!configsFolder.exists()) {
         configsFolder.mkdirs();
      }

      this.file = new File(configsFolder, this.fileName + "." + FileManager.DEFAULT_FILE_TYPE);
      this.legacyFile = new File(configsFolder, this.fileName + "." + FileManager.LEGACY_FILE_TYPE);
   }

   public void load() {
      File readableFile = this.getReadableFile();
      if (!readableFile.exists()) {
         XaClient.LOGGER.warn("Swing preset file not found: {}", readableFile.getAbsolutePath());
         return;
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(readableFile))) {
         JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
         JsonObject animation = jsonObject.getAsJsonObject("animation");

         for (Setting setting : XaClient.getInstance().getSwingManager().getSharedSettings().getSettings()) {
            if (animation.has(setting.getName())) {
               setting.load(animation.get(setting.getName()));
            }
         }

         JsonObject startPhase = jsonObject.getAsJsonObject("startPhase");

         for (Setting setting : XaClient.getInstance().getSwingManager().getStartPhase().getSettings()) {
            if (startPhase.has(setting.getName())) {
               setting.load(startPhase.get(setting.getName()));
            }
         }

         JsonObject endPhase = jsonObject.getAsJsonObject("endPhase");

         for (Setting setting : XaClient.getInstance().getSwingManager().getEndPhase().getSettings()) {
            if (endPhase.has(setting.getName())) {
               setting.load(endPhase.get(setting.getName()));
            }
         }

         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getSwingPresetManager().setCurrent(this);
         }
      } catch (Exception exception) {
         XaClient.LOGGER.error("Failed to load swing preset file {}: {}", readableFile.getName(), exception.getMessage());
      }
   }

   public void save() {
      try {
         File parent = this.file.getParentFile();
         if (parent != null && !parent.exists()) {
            parent.mkdirs();
         }

         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create swing preset file: " + this.file.getAbsolutePath());
         }

         JsonObject json = new JsonObject();
         JsonObject animation = new JsonObject();

         for (Setting setting : XaClient.getInstance().getSwingManager().getSharedSettings().getSettings()) {
            animation.add(setting.getName(), setting.save());
         }

         json.add("animation", animation);
         JsonObject startPhase = new JsonObject();

         for (Setting setting : XaClient.getInstance().getSwingManager().getStartPhase().getSettings()) {
            startPhase.add(setting.getName(), setting.save());
         }

         json.add("startPhase", startPhase);
         JsonObject endPhase = new JsonObject();

         for (Setting setting : XaClient.getInstance().getSwingManager().getEndPhase().getSettings()) {
            endPhase.add(setting.getName(), setting.save());
         }

         json.add("endPhase", endPhase);

         try (FileWriter fileWriter = new FileWriter(this.file)) {
            fileWriter.write(FileManager.GSON.toJson(json));
         }

         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getSwingPresetManager().setCurrent(this);
         }
      } catch (IOException exception) {
         XaClient.LOGGER.error("Failed to save swing preset file", exception);
      }
   }

   public void delete() {
      Path filePath = this.getReadableFile().toPath();

      try {
         Files.delete(filePath);
         XaClient.getInstance().getSwingPresetManager().getSwingPresetFiles().remove(this);
         XaClient.LOGGER.info("Swing preset file deleted: {}", filePath);
      } catch (NoSuchFileException exception) {
         XaClient.LOGGER.warn("Tried to delete a file that does not exist: {}", filePath);
      } catch (IOException exception) {
         MessageUtility.error(Text.of("Failed to delete preset"));
         XaClient.LOGGER.warn("Failed to delete swing preset file: {}. Reason: {}", filePath, exception.getMessage());
      }
   }

   public File getReadableFile() {
      return this.file.exists() ? this.file : this.legacyFile;
   }

   @Generated
   public File getFile() {
      return this.file;
   }

   @Generated
   public String getFileName() {
      return this.fileName;
   }

   @Generated
   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }

   @Generated
   public Animation getActiveAnimation() {
      return this.activeAnimation;
   }
}
