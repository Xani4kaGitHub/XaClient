package moscow.xaclient.systems.modules.listeners;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.Module;

public class ModuleTickListener implements EventListener<ClientPlayerTickEvent> {
   public void onEvent(ClientPlayerTickEvent event) {
      for (Module module : XaClient.getInstance().getModuleManager().getModules()) {
         if (module.isEnabled()) {
            module.tick();
         }
      }
   }
}
