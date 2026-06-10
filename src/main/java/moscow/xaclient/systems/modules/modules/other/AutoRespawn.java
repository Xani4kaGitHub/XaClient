package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.gui.screen.DeathScreen;

@ModuleInfo(name = "Auto Respawn", category = ModuleCategory.OTHER, desc = "Automatically respawns after death")
public class AutoRespawn extends BaseModule {
   private final SliderSetting delay = new SliderSetting(this, "Delay").min(0.0F).max(20.0F).step(1.0F).currentValue(5.0F).suffix(" ticks");

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player != null && mc.world != null && mc.currentScreen instanceof DeathScreen && mc.player.deathTime >= this.delay.getCurrentValue()) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
   };
}
