package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import net.minecraft.client.gui.screen.DeathScreen;

// Автоматически нажимает кнопку возрождения после смерти.
@ModuleInfo(name = "Auto Respawn", category = ModuleCategory.OTHER, desc = "modules.descriptions.auto_respawn")
public class AutoRespawn extends BaseModule {
   private final SliderSetting delay = new SliderSetting(this, "modules.settings.auto_respawn.delay")
      .min(0.0F)
      .max(20.0F)
      .step(1.0F)
      .currentValue(5.0F)
      .suffix(" ticks");

   // Ждёт указанное число тиков на экране смерти и отправляет respawn.
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player != null && mc.world != null && mc.currentScreen instanceof DeathScreen && mc.player.deathTime >= this.delay.getCurrentValue()) {
         mc.player.requestRespawn();
         mc.setScreen(null);
      }
   };
}
