package moscow.xaclient.systems.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.Module;
import moscow.xaclient.systems.modules.exception.UnknownModuleException;
import moscow.xaclient.systems.modules.modules.other.Sounds;
import moscow.xaclient.systems.modules.modules.visuals.MenuModule;
import moscow.xaclient.systems.notifications.NotificationType;
import moscow.xaclient.systems.setting.Setting;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.sounds.ClientSounds;
import net.minecraft.text.Text;

public class ConfigFile implements IMinecraft {
   private final List<Module> modules = XaClient.getInstance().getModuleManager().getModules();
   private final File file;
   private final File legacyFile;
   private final String fileName;

   public ConfigFile(String fileName) {
      this.fileName = FileManager.stripSupportedExtension(fileName);
      File configsFolder = new File(FileManager.DIRECTORY, "configs");
      if (!configsFolder.exists()) {
         configsFolder.mkdirs();
      }

      this.file = new File(configsFolder, this.fileName + "." + FileManager.DEFAULT_FILE_TYPE);
      this.legacyFile = new File(configsFolder, this.fileName + "." + FileManager.LEGACY_FILE_TYPE);
   }

   public void load() {
      File readableFile = this.getReadableFile();
      if (!readableFile.exists()) {
         XaClient.LOGGER.warn("Config file not found: {}", readableFile.getAbsolutePath());
         return;
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(readableFile))) {
         JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
         if (!jsonObject.has("modules")) {
            XaClient.LOGGER.warn("Invalid config format: missing 'modules' array in {}", readableFile.getName());
            return;
         }

         JsonArray modulesArray = jsonObject.getAsJsonArray("modules");
         int loadedModules = 0;

         for (JsonElement moduleElement : modulesArray) {
            JsonObject moduleObject = moduleElement.getAsJsonObject();
            if (!moduleObject.has("name")) {
               continue;
            }

            String moduleName = moduleObject.get("name").getAsString();
            boolean enabled = moduleObject.has("enabled") && moduleObject.get("enabled").getAsBoolean();
            int key = moduleObject.has("key") ? moduleObject.get("key").getAsInt() : 0;

            try {
               Module module = XaClient.getInstance().getModuleManager().getModule(moduleName);
               if (!(module instanceof MenuModule)) {
                  module.setEnabled(enabled, true);
                  module.setKey(key);
               }

               if (moduleObject.has("settings")) {
                  JsonObject settingsObject = moduleObject.getAsJsonObject("settings");

                  for (Setting setting : module.getSettings()) {
                     if (settingsObject.has(setting.getName())) {
                        setting.load(settingsObject.get(setting.getName()));
                     }
                  }
               }

               loadedModules++;
            } catch (UnknownModuleException ignored) {
               XaClient.LOGGER.warn("Module not found during config load: {}", moduleName);
            }
         }

         ClientSounds.MODULE.play(XaClient.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), 1.0F);
         XaClient.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Localizator.translate("configs.loaded"));
         XaClient.LOGGER.info("Loaded {} modules from config {}", loadedModules, readableFile.getName());
         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getConfigManager().setCurrent(this);
         }
      } catch (Exception exception) {
         XaClient.LOGGER.error("Failed to load config file {}: {}", readableFile.getName(), exception.getMessage());
      }
   }

   public void save() {
      try {
         File parent = this.file.getParentFile();
         if (parent != null && !parent.exists()) {
            parent.mkdirs();
         }

         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create config file: " + this.file.getAbsolutePath());
         }

         JsonObject json = new JsonObject();
         json.add("modules", this.getModulesJsonArray());

         try (FileWriter fileWriter = new FileWriter(this.file)) {
            fileWriter.write(FileManager.GSON.toJson(json));
         }

         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getConfigManager().setCurrent(this);
         }

         XaClient.LOGGER.info("Successfully saved config {}", this.fileName);
      } catch (IOException exception) {
         XaClient.LOGGER.error("Failed to save config file", exception);
      }
   }

   public void delete() {
      File targetFile = this.getReadableFile();
      if (targetFile.exists() && targetFile.delete()) {
         XaClient.getInstance().getConfigManager().getConfigFiles().remove(this);
         MessageUtility.info(Text.of("Config " + this.fileName + " deleted"));
         XaClient.LOGGER.info("Config file deleted: {}", targetFile.getAbsolutePath());
      } else {
         MessageUtility.error(Text.of("Failed to delete config"));
         XaClient.LOGGER.warn("Failed to delete config file: {}", targetFile.getAbsolutePath());
      }
   }

   public File getReadableFile() {
      return this.file.exists() ? this.file : this.legacyFile;
   }

   private JsonArray getModulesJsonArray() {
      JsonArray modulesJsonArray = new JsonArray();

      for (Module module : this.modules) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("name", module.getName());
         moduleObject.addProperty("enabled", module.isEnabled());
         moduleObject.addProperty("key", module.getKey());
         moduleObject.add("settings", this.getSettingsJsonObject(module));
         modulesJsonArray.add(moduleObject);
      }

      return modulesJsonArray;
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
}
