package moscow.xaclient.systems.file.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.config.ConfigFile;
import moscow.xaclient.systems.file.ClientFile;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.systems.file.api.FileInfo;
import moscow.xaclient.systems.modules.constructions.swinganim.SwingManager;
import moscow.xaclient.systems.modules.constructions.swinganim.SwingPhase;
import moscow.xaclient.systems.modules.constructions.swinganim.presets.SwingPreset;
import moscow.xaclient.systems.modules.constructions.swinganim.presets.SwingPresetManager;
import moscow.xaclient.systems.modules.modules.other.AutoAuth;
import moscow.xaclient.systems.setting.Setting;
import moscow.xaclient.systems.theme.Theme;
import moscow.xaclient.ui.components.ColorPicker;
import moscow.xaclient.ui.hud.HudElement;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.Session.AccountType;

@FileInfo(name = "client")
public class ClientDataFile extends ClientFile implements IMinecraft {
   @Override
   public void write() {
      JsonObject json = new JsonObject();
      json.addProperty("username", mc.getSession().getUsername());
      json.addProperty("theme", XaClient.getInstance().getThemeManager().getCurrentTheme().name());
      json.addProperty("swing", XaClient.getInstance().getSwingManager().getCurrent());
      json.add("hudElements", this.getHudElementsJsonArray());
      json.add("friends", this.getFriendsJsonArray());
      json.add("colorPickerPresets", this.getColorPickerPresetsJsonArray());
      json.add("password", this.getPassword());
      ConfigFile currentConfig = XaClient.getInstance().getConfigManager().getCurrent();
      if (currentConfig != null) {
         json.addProperty("lastConfig", currentConfig.getFileName());
      }

      try (FileWriter writer = new FileWriter(this.file)) {
         writer.write(FileManager.GSON.toJson(json));
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   @Override
   public void read() {
      try (FileReader reader = new FileReader(this.getReadableFile())) {
         JsonObject object = (JsonObject)FileManager.GSON.fromJson(reader, JsonObject.class);
         if (object.has("username")) {
            String username = object.get("username").getAsString();
            new Session(username, UUID.randomUUID(), "", Optional.empty(), Optional.empty(), AccountType.MOJANG);
         }

         if (object.has("password")) {
            this.loadPass(object.getAsJsonArray("password"));
         }

         if (object.has("swing")) {
            String swing = object.get("swing").getAsString();
            SwingManager swingManager = XaClient.getInstance().getSwingManager();
            SwingPresetManager manager = XaClient.getInstance().getSwingPresetManager();

            for (SwingPreset value : XaClient.getInstance().getSwingManager().getPresets()) {
               if (value.getName().equals(swing)) {
                  swingManager.getBezier().start(value.getBezierStart()).end(value.getBezierEnd());
                  swingManager.getBack().enabled(value.isSwingBack());
                  swingManager.getSpeed().setCurrentValue(value.getSpeed());
                  SwingPhase start = swingManager.getStartPhase();
                  start.getAnchorX().setCurrentValue(value.getFrom().anchorX());
                  start.getAnchorY().setCurrentValue(value.getFrom().anchorY());
                  start.getAnchorZ().setCurrentValue(value.getFrom().anchorZ());
                  start.getMoveX().setCurrentValue(value.getFrom().moveX());
                  start.getMoveY().setCurrentValue(value.getFrom().moveY());
                  start.getMoveZ().setCurrentValue(value.getFrom().moveZ());
                  start.getRotateX().setCurrentValue(value.getFrom().rotateX());
                  start.getRotateY().setCurrentValue(value.getFrom().rotateY());
                  start.getRotateZ().setCurrentValue(value.getFrom().rotateZ());
                  SwingPhase end = swingManager.getEndPhase();
                  end.getAnchorX().setCurrentValue(value.getTo().anchorX());
                  end.getAnchorY().setCurrentValue(value.getTo().anchorY());
                  end.getAnchorZ().setCurrentValue(value.getTo().anchorZ());
                  end.getMoveX().setCurrentValue(value.getTo().moveX());
                  end.getMoveY().setCurrentValue(value.getTo().moveY());
                  end.getMoveZ().setCurrentValue(value.getTo().moveZ());
                  end.getRotateX().setCurrentValue(value.getTo().rotateX());
                  end.getRotateY().setCurrentValue(value.getTo().rotateY());
                  end.getRotateZ().setCurrentValue(value.getTo().rotateZ());
                  swingManager.setCurrent(swing);
               }
            }
         }

         if (object.has("theme")) {
            String themeName = object.get("theme").getAsString();

            try {
               Theme theme = Theme.valueOf(themeName);
               XaClient.getInstance().getThemeManager().setCurrentTheme(theme);
            } catch (IllegalArgumentException var16) {
               XaClient.getInstance().getThemeManager().setCurrentTheme(Theme.DARK);
            }
         }

         if (object.has("friends")) {
            JsonArray friendsArray = object.getAsJsonArray("friends");
            XaClient.getInstance().getFriendManager().clear();

            for (JsonElement friendElement : friendsArray) {
               XaClient.getInstance().getFriendManager().add(friendElement.getAsString());
            }
         }

         if (object.has("colorPickerPresets")) {
            this.loadColorPickerPresets(object.getAsJsonArray("colorPickerPresets"));
         }

         if (object.has("hudElements")) {
            for (JsonElement elemObj : object.getAsJsonArray("hudElements")) {
               JsonObject elementObject = elemObj.getAsJsonObject();
               String name = elementObject.get("name").getAsString();
               float x = elementObject.get("x").getAsFloat();
               float y = elementObject.get("y").getAsFloat();
               boolean showing = elementObject.get("showing").getAsBoolean();
               HudElement element = XaClient.getInstance().getHud().getElementByName(name);
               if (element != null) {
                  element.setX(x);
                  element.setY(y);
                  element.setShowing(showing);
                  if (elementObject.has("settings")) {
                     JsonObject settingsObject = elementObject.getAsJsonObject("settings");

                     for (Setting setting : element.getSettings()) {
                        if (settingsObject.has(setting.getName())) {
                           setting.load(settingsObject.get(setting.getName()));
                        }
                     }
                  }
               }
            }
         }

         if (object.has("lastConfig")) {
            String configName = object.get("lastConfig").getAsString();
            ConfigFile config = XaClient.getInstance().getConfigManager().getConfig(configName);
            if (config != null) {
               config.load();
            }
         }
      } catch (Exception var18) {
         var18.printStackTrace();
      }
   }

   private JsonArray getHudElementsJsonArray() {
      JsonArray hudElementsArray = new JsonArray();

      for (HudElement element : XaClient.getInstance().getHud().getElements()) {
         JsonObject elementObject = new JsonObject();
         elementObject.addProperty("name", element.getName());
         elementObject.addProperty("x", element.getX());
         elementObject.addProperty("y", element.getY());
         elementObject.addProperty("showing", element.isShowing());
         elementObject.add("settings", this.getSettingsJsonObject(element));
         hudElementsArray.add(elementObject);
      }

      return hudElementsArray;
   }

   private JsonObject getSettingsJsonObject(HudElement element) {
      JsonObject settingsObject = new JsonObject();

      for (Setting setting : element.getSettings()) {
         settingsObject.add(setting.getName(), setting.save());
      }

      return settingsObject;
   }

   private JsonArray getFriendsJsonArray() {
      JsonArray friendsJsonArray = new JsonArray();

      for (String friendsName : XaClient.getInstance().getFriendManager().listFriends()) {
         friendsJsonArray.add(friendsName);
      }

      return friendsJsonArray;
   }

   private JsonArray getPassword() {
      JsonArray passwordJsonArray = new JsonArray();

      for (Entry<String, String> pass : XaClient.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword().entrySet()) {
         JsonObject passObject = new JsonObject();
         passObject.addProperty("nick", pass.getValue());
         passObject.addProperty("pass", pass.getKey());
         passwordJsonArray.add(passObject);
      }

      return passwordJsonArray;
   }

   private JsonArray getColorPickerPresetsJsonArray() {
      JsonArray presetsArray = new JsonArray();

      for (ColorPicker.Preset preset : ColorPicker.COLOR_PRESETS) {
         if (preset.isShowing()) {
            JsonObject presetObject = new JsonObject();
            ColorRGBA color = preset.getColor();
            presetObject.addProperty("red", color.getRed());
            presetObject.addProperty("green", color.getGreen());
            presetObject.addProperty("blue", color.getBlue());
            presetObject.addProperty("alpha", color.getAlpha());
            presetsArray.add(presetObject);
         }
      }

      return presetsArray;
   }

   private void loadColorPickerPresets(JsonArray presetsArray) {
      List<ColorPicker.Preset> loadedPresets = new ArrayList<>();

      for (JsonElement presetElement : presetsArray) {
         JsonObject presetObject = presetElement.getAsJsonObject();
         float red = presetObject.get("red").getAsFloat();
         float green = presetObject.get("green").getAsFloat();
         float blue = presetObject.get("blue").getAsFloat();
         float alpha = presetObject.get("alpha").getAsFloat();
         ColorRGBA color = new ColorRGBA(red, green, blue, alpha);
         loadedPresets.add(new ColorPicker.Preset(color));
      }

      ColorPicker.setColorPresets(loadedPresets);
   }

   private void loadPass(JsonArray password) {
      for (JsonElement passElement : password) {
         JsonObject passObject = passElement.getAsJsonObject();
         String nick = passObject.get("nick").getAsString();
         String pass = passObject.get("pass").getAsString();
         XaClient.getInstance().getModuleManager().getModule(AutoAuth.class).put(nick, pass);
      }
   }
}
