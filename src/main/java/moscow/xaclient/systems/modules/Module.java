package moscow.xaclient.systems.modules;

import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.setting.SettingsContainer;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.interfaces.IScaledResolution;
import moscow.xaclient.utility.interfaces.Toggleable;

public interface Module extends Toggleable, IMinecraft, IScaledResolution, SettingsContainer {
   void disable();

   void enable();

   void tick();

   ModuleInfo getInfo();

   String getName();

   default String getDescription() {
      String translationKey = "modules.descriptions.%s".formatted(this.getName().toLowerCase().replace(" ", "_"));
      return Localizator.translate(translationKey);
   }

   int getKey();

   ModuleCategory getCategory();

   boolean isEnabled();

   boolean isHidden();

   Animation getKeybindsAnimation();

   void setKey(int var1);

   void setEnabled(boolean var1, boolean var2);
}
