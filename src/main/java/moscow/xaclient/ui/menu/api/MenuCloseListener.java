package moscow.xaclient.ui.menu.api;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.HudRenderEvent;
import moscow.xaclient.systems.modules.modules.visuals.MenuModule;
import moscow.xaclient.ui.menu.MenuScreen;
import moscow.xaclient.ui.menu.dropdown.DropDownScreen;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;

public class MenuCloseListener implements IMinecraft {
   private final EventListener<HudRenderEvent> onHudRender = event -> {
      MenuScreen menuScreen = XaClient.getInstance().getMenuScreen();
      if (mc.currentScreen == null) {
         if (!(menuScreen instanceof DropDownScreen)) {
            XaClient.getInstance().setMenuScreen(new DropDownScreen());
         }
      }

      if (menuScreen != null) {
         menuScreen.getMenuAnimation().update(menuScreen.isClosing() ? 0.0F : 1.0F);
         if (!(mc.currentScreen instanceof MenuScreen) && XaClient.getInstance().getModuleManager().getModule(MenuModule.class).isEnabled()) {
            XaClient.getInstance().getModuleManager().getModule(MenuModule.class).setEnabled(false);
         }

         if (menuScreen.getMenuAnimation().getValue() > 0.1F && !(mc.currentScreen instanceof MenuScreen) && menuScreen.isClosing()) {
            UIContext context = UIContext.of(event.getContext(), -1, -1, MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false));
            menuScreen.render(context);
         }
      }
   };

   public MenuCloseListener() {
      XaClient.getInstance().getEventManager().subscribe(this);
   }
}
