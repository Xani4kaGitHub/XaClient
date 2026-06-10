package wtf.wyvern.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import ru.nexusguard.protection.annotations.Native;
import wtf.wyvern.Wyvern;
import wtf.wyvern.base.events.impl.player.EventMoveInput;
import wtf.wyvern.client.modules.api.Category;
import wtf.wyvern.client.modules.api.Module;
import wtf.wyvern.client.modules.api.ModuleAnnotation;
import wtf.wyvern.utility.game.player.MovingUtil;
import wtf.wyvern.utility.game.player.PlayerIntersectionUtil;

@ModuleAnnotation(
   name = "ElytraRecast",
   description = "Позволяет выше прыгать на элитрах",
   category = Category.MOVEMENT
)
public final class ElytraRecast extends Module {
   public static final ElytraRecast INSTANCE = new ElytraRecast();
   private int groundTick = 0;
   private boolean changed = false;

   @EventTarget
   @Native
   public void update(EventMoveInput eventUpdate) {
      if (mc.player.isUsingItem()) {
         if (Wyvern.getInstance().getServerHandler().isServerSprint()) {
            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
            mc.player.setSprinting(false);
         }

         this.groundTick = 5;
      } else if (this.groundTick > 0) {
         --this.groundTick;
         return;
      }

      if (!mc.player.isUsingItem() && !mc.player.isTouchingWater() && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA) && MovingUtil.hasPlayerMovement()) {
         if (mc.player.isOnGround() && mc.player.isWalking()) {
            if (!mc.player.canSprint() || !mc.player.isWalking() || mc.player.isBlind() || mc.player.isUsingItem() || mc.player.shouldSlowDown() && !mc.player.isSubmergedInWater()) {
               if (Wyvern.getInstance().getServerHandler().isServerSprint()) {
                  mc.player.lastSprinting = true;
                  mc.player.setSprinting(false);
               }

               mc.player.setSprinting(false);
            } else {
               if (!mc.player.isSprinting() && Wyvern.getInstance().getServerHandler().isServerSprint()) {
                  mc.player.setSprinting(true);
               }

               if (!Wyvern.getInstance().getServerHandler().isServerSprint()) {
                  mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_SPRINTING));
                  mc.player.setSprinting(true);
                  this.changed = true;
               }
            }

            mc.player.jump();
         } else if (!mc.player.isGliding()) {
            PlayerIntersectionUtil.startFallFlying();
         }
      } else if (this.changed && Wyvern.getInstance().getServerHandler().isServerSprint()) {
         mc.player.lastSprinting = true;
         mc.player.setSprinting(false);
         this.changed = false;
      }

      if (this.groundTick > 0) {
         --this.groundTick;
      }

   }

   public void onDisable() {
      if (Wyvern.getInstance().getServerHandler().isServerSprint() && this.changed) {
         mc.player.lastSprinting = true;
         mc.player.setSprinting(false);
      }

      super.onDisable();
   }
}