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
import moscow.xaclient.systems.modules.Module;
import moscow.xaclient.systems.setting.Setting;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;

public class SwingPresetFile implements IMinecraft {
   private final File file;
   private final String fileName;
   private final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);
   private final Animation activeAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

   public SwingPresetFile(String fileName) {
      this.fileName = fileName;
      File configsFolder = new File(FileManager.DIRECTORY + "/presets", "swing");
      if (!configsFolder.exists()) {
         configsFolder.mkdir();
      }

      this.file = new File(configsFolder, fileName + ".%s".formatted("rock"));
   }

   public void load() {
      if (!this.file.exists()) {
         XaClient.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
      } else {
         try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
            JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject animation = jsonObject.getAsJsonObject("animation");

            for (Setting setting : XaClient.getInstance().getSwingManager().getSharedSettings().getSettings()) {
               if (animation.has(setting.getName())) {
                  setting.load(animation.get(setting.getName()));
               }
            }

            JsonObject startPhase = jsonObject.getAsJsonObject("startPhase");

            for (Setting settingx : XaClient.getInstance().getSwingManager().getStartPhase().getSettings()) {
               if (startPhase.has(settingx.getName())) {
                  settingx.load(startPhase.get(settingx.getName()));
               }
            }

            JsonObject endPhase = jsonObject.getAsJsonObject("endPhase");

            for (Setting settingxx : XaClient.getInstance().getSwingManager().getEndPhase().getSettings()) {
               if (endPhase.has(settingxx.getName())) {
                  settingxx.load(endPhase.get(settingxx.getName()));
               }
            }

            if (!this.fileName.equals("autosave")) {
               XaClient.getInstance().getSwingPresetManager().setCurrent(this);
            }
         } catch (Exception var10) {
            XaClient.LOGGER.error("Failed to load config file {}: {}", this.fileName, var10.getMessage());
         }
      }
   }

   public void save() {
      try {
         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create config file: " + this.file.getAbsolutePath());
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
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(FileManager.GSON.toJson(json));
         } catch (Throwable var9) {
            try {
               fileWriter.close();
            } catch (Throwable var8) {
               var9.addSuppressed(var8);
            }

            throw var9;
         }

         fileWriter.close();
         System.out.println("saved");
         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getSwingPresetManager().setCurrent(this);
         }
      } catch (IOException var10) {
         XaClient.LOGGER.error("Failed to save config file", var10);
      }
   }

   public void delete() {
      Path filePath = this.file.toPath();

      try {
         Files.delete(filePath);
         XaClient.getInstance().getSwingPresetManager().getSwingPresetFiles().remove(this);
         XaClient.LOGGER.info("Config file deleted: {}", filePath);
      } catch (NoSuchFileException var3) {
         XaClient.LOGGER.warn("Tried to delete a file that does not exist: {}", filePath);
      } catch (IOException var4) {
         MessageUtility.error(Text.of("Произошла ошибка при удалении"));
         XaClient.LOGGER.warn("Failed to delete config file: {}. Reason: {}", filePath, var4.getMessage());
      }
   }

   private JsonObject getSettingsJsonObject(Module module) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : module.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
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
