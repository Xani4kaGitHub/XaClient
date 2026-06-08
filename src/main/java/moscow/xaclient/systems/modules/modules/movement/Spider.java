package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.EntityJumpEvent;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;

@ModuleInfo(name = "Spider", category = ModuleCategory.MOVEMENT, desc = "modules.descriptions.spider")
public class Spider extends BaseModule {
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (mc.player.horizontalCollision) {
         mc.player.setOnGround(mc.player.age % 3 == 0);
         mc.player.prevY -= 2.0E-232;
         if (mc.player.isOnGround()) {
            mc.player.setVelocity(mc.player.getVelocity().getX(), 0.42, mc.player.getVelocity().getZ());
         }
      }
   };
   private final EventListener<EntityJumpEvent> onJump = event -> {};
}
