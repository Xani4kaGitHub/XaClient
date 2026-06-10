package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.SendPacketEvent;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;

// Замораживает игрока на текущей позиции и блокирует отправку movement-пакетов.
@ModuleInfo(name = "Air Stuck", category = ModuleCategory.MOVEMENT, desc = "modules.descriptions.air_stuck")
public class AirStuck extends BaseModule {
   private final BooleanSetting onlyFalling = new BooleanSetting(this, "modules.settings.air_stuck.only_falling");
   private Vec3d frozenPos;

   // Фиксирует первую подходящую позицию и удерживает игрока на ней каждый тик.
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (this.frozenPos == null) {
         if (this.onlyFalling.isEnabled() && (mc.player.isOnGround() || mc.player.getVelocity().y >= 0.0)) {
            return;
         }

         this.frozenPos = mc.player.getPos();
      }

      mc.player.setPosition(this.frozenPos.x, this.frozenPos.y, this.frozenPos.z);
      mc.player.setVelocity(Vec3d.ZERO);
      mc.player.fallDistance = 0.0F;
   };

   // Не даёт клиенту отправлять серверу новые координаты, пока позиция заморожена.
   private final EventListener<SendPacketEvent> onSendPacket = event -> {
      if (this.frozenPos != null && event.getPacket() instanceof PlayerMoveC2SPacket) {
         event.cancel();
      }
   };

   // Сбрасывает сохранённую позицию при включении, чтобы заморозка начиналась заново.
   @Override
   public void onEnable() {
      this.frozenPos = null;
   }

   // Очищает состояние и гасит скорость после отключения.
   @Override
   public void onDisable() {
      this.frozenPos = null;
      if (mc.player != null) {
         mc.player.setVelocity(Vec3d.ZERO);
      }
   }
}
