package moscow.xaclient.systems.modules.modules.other;

import lombok.Generated;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.ReceivePacketEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.sounds.ClientSounds;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "Sounds", category = ModuleCategory.OTHER, enabledByDefault = true, desc = "Добавляет звуки клиента")
public class Sounds extends BaseModule {
   private final SliderSetting volume = new SliderSetting(this, "Громкость звука").step(0.1F).min(0.1F).max(1.0F).currentValue(1.0F);
   private final EventListener<ReceivePacketEvent> receivePacket = event -> {
      if (event.getPacket() instanceof GameMessageS2CPacket packet) {
         String msg = packet.content().getString();
         if (msg.contains("Вы успешно купили") || msg.contains("отправлено игроку")) {
            ClientSounds.APPLEPAY.play(this.volume.getCurrentValue(), 1.0F);
         }
      }
   };

   @Generated
   public SliderSetting getVolume() {
      return this.volume;
   }

   @Generated
   public EventListener<ReceivePacketEvent> getReceivePacket() {
      return this.receivePacket;
   }
}
