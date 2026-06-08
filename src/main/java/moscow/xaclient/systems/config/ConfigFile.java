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
   private List<Module> modules = XaClient.getInstance().getModuleManager().getModules();
   private File file;
   private String fileName;

   public ConfigFile(String fileName) {
      this.fileName = fileName;
      File configsFolder = new File(FileManager.DIRECTORY, "configs");
      if (!configsFolder.exists()) {
         configsFolder.mkdir();
      }

      this.file = new File(configsFolder, fileName + ".%s".formatted("rock"));
   }

   public void load() {
      if (!this.file.exists()) {
         XaClient.LOGGER.warn("Config file not found: {}", this.file.getAbsolutePath());
      } else {
         try {
            try (BufferedReader reader = new BufferedReader(new FileReader(this.file))) {
               JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
               if (jsonObject.has("modules")) {
                  JsonArray modulesArray = jsonObject.getAsJsonArray("modules");
                  int loadedModules = 0;

                  for (JsonElement moduleElement : modulesArray) {
                     JsonObject moduleObject = moduleElement.getAsJsonObject();
                     if (moduleObject.has("name")) {
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
                        } catch (UnknownModuleException var16) {
                           XaClient.LOGGER.warn("Module not found during config load: {}", moduleName);
                        }
                     }
                  }

                  ClientSounds.MODULE.play(XaClient.getInstance().getModuleManager().getModule(Sounds.class).getVolume().getCurrentValue(), 1.0F);
                  XaClient.getInstance().getNotificationManager().addNotification(NotificationType.SUCCESS, Localizator.translate("configs.loaded"));
                  XaClient.LOGGER.info("Loaded {} modules from config {}", loadedModules, this.fileName);
                  if (!this.fileName.equals("autosave")) {
                     XaClient.getInstance().getConfigManager().setCurrent(this);
                  }

                  return;
               }

               XaClient.LOGGER.warn("Invalid config format: missing 'modules' array in {}", this.fileName);
            }
         } catch (Exception var18) {
            XaClient.LOGGER.error("Failed to load config file {}: {}", this.fileName, var18.getMessage());
         }
      }
   }

   public void save() {
      try {
         if (!this.file.exists() && !this.file.createNewFile()) {
            throw new IOException("Failed to create config file: " + this.file.getAbsolutePath());
         }

         JsonObject json = new JsonObject();
         JsonArray modulesJsonArray = this.getModulesJsonArray();
         json.add("modules", modulesJsonArray);
         FileWriter fileWriter = new FileWriter(this.file);

         try {
            fileWriter.write(FileManager.GSON.toJson(json));
         } catch (Throwable var7) {
            try {
               fileWriter.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }

            throw var7;
         }

         fileWriter.close();
         if (!this.fileName.equals("autosave")) {
            XaClient.getInstance().getConfigManager().setCurrent(this);
         }

         XaClient.LOGGER.info("Successfully saved config " + this.fileName);
      } catch (IOException var8) {
         XaClient.LOGGER.error("Failed to save config file", var8);
      }
   }

   public void delete() {
      if (this.file.exists() && this.file.delete()) {
         XaClient.getInstance().getConfigManager().getConfigFiles().remove(this);
         MessageUtility.info(Text.of("Конфиг " + this.fileName + " успешно удален"));
         XaClient.LOGGER.info("Config file deleted: {}", this.file.getAbsolutePath());
      } else {
         MessageUtility.error(Text.of("Произошла ошибка при удалении"));
         XaClient.LOGGER.warn("Failed to delete config file: {}", this.file.getAbsolutePath());
      }
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
   public String getFileName() {
      return this.fileName;
   }
}
