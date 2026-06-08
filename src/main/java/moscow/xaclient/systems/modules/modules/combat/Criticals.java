package moscow.xaclient.systems.modules.modules.combat;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.ai.aura.AuraAITrainingBot;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.InternalAttackEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationHandler;
import moscow.xaclient.utility.rotations.RotationMath;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;

@ModuleInfo(name = "Criticals", category = ModuleCategory.COMBAT)
public class Criticals extends BaseModule {
   private int airTicks;
   private final EventListener<InternalAttackEvent> onAttack = event -> {
      Aura aura = XaClient.getInstance().getModuleManager().getModule(Aura.class);
      if (mc.player == null || event.isCancelled() || AuraAITrainingBot.isTrainingBot(event.getEntity()) || aura.isAuraAIRotationActive()) {
         return;
      }

      if (!mc.player.isTouchingWater()) {
         if (!mc.player.isOnGround() && mc.player.fallDistance == 0.0F) {
            RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
            Rotation rot = rotationHandler.isIdling() ? rotationHandler.getPlayerRotation() : rotationHandler.getCurrentRotation();
            rot = new Rotation(rot.getYaw() + MathUtility.random(-1.0, 1.0), rot.getPitch() + MathUtility.random(-1.0, 1.0));
            rot = RotationMath.correctRotation(rot);
            mc.player
               .networkHandler
               .sendPacket(
                  new Full(
                     mc.player.getX(),
                     mc.player.getY() - (mc.player.fallDistance = MathUtility.random(1.0E-5F, 1.0E-4F)),
                     mc.player.getZ(),
                     rot.getYaw(),
                     rot.getPitch(),
                     mc.player.isOnGround(),
                     mc.player.horizontalCollision
                  )
               );
         }
      }
   };

   @Override
   public void tick() {
      if (mc.player.isOnGround()) {
         this.airTicks = 0;
      } else {
         this.airTicks++;
      }
   }

   public boolean canCritical() {
      if (mc.player == null) {
         return false;
      }

      Aura aura = XaClient.getInstance().getModuleManager().getModule(Aura.class);
      return this.isEnabled() && (aura == null || !aura.isAuraAIRotationActive()) && mc.player.fallDistance <= 0.0F && !mc.player.isOnGround();
   }
}
