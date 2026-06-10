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

@ModuleInfo(name = "Air Stuck", category = ModuleCategory.MOVEMENT, desc = "Freezes player movement in the air")
public class AirStuck extends BaseModule {
   private final BooleanSetting onlyFalling = new BooleanSetting(this, "Only Falling");
   private Vec3d frozenPos;

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

   private final EventListener<SendPacketEvent> onSendPacket = event -> {
      if (this.frozenPos != null && event.getPacket() instanceof PlayerMoveC2SPacket) {
         event.cancel();
      }
   };

   @Override
   public void onEnable() {
      this.frozenPos = null;
   }

   @Override
   public void onDisable() {
      this.frozenPos = null;
      if (mc.player != null) {
         mc.player.setVelocity(Vec3d.ZERO);
      }
   }
}
