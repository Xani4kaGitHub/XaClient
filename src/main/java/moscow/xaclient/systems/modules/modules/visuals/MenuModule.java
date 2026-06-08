package moscow.xaclient.systems.modules.modules.visuals;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.modules.modules.other.Sounds;
import moscow.xaclient.ui.menu.MenuScreen;
import moscow.xaclient.ui.menu.api.MenuCloseListener;
import moscow.xaclient.utility.sounds.ClientSounds;

@ModuleInfo(name = "Menu", category = ModuleCategory.VISUALS, key = 344, desc = "modules.descriptions.menu")
public class MenuModule extends BaseModule {
   private static final MenuCloseListener menuCloseListener = new MenuCloseListener();

   @Override
   public void onEnable() {
      if (!(mc.currentScreen instanceof MenuScreen)) {
         MenuScreen menuScreen = XaClient.getInstance().getMenuScreen();
         mc.setScreen(menuScreen);
         Sounds soundsModule = XaClient.getInstance().getModuleManager().getModule(Sounds.class);
         if (soundsModule.isEnabled()) {
            ClientSounds.CLICKGUI_OPEN.play(soundsModule.getVolume().getCurrentValue());
         }

         super.onEnable();
      }
   }

   @Override
   public void onDisable() {
      if (mc.currentScreen instanceof MenuScreen) {
         mc.setScreen(null);
         XaClient.getInstance().getMenuScreen().setClosing(true);
      }

      super.onDisable();
   }
}
