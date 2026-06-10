package wtf.wyvern.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import wtf.wyvern.base.events.impl.player.EventUpdate;
import wtf.wyvern.client.modules.api.Category;
import wtf.wyvern.client.modules.api.Module;
import wtf.wyvern.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "NoJumpDelay",
   category = Category.PLAYER,
   description = "Убирает задержку на прыжок"
)
public final class NoDelay extends Module {
   public static final NoDelay INSTANCE = new NoDelay();

   @EventTarget
   public void onUpdate(EventUpdate event) {
      if (mc.player != null && mc.world != null) {
         mc.player.jumpingCooldown = 0;
      }
   }
}