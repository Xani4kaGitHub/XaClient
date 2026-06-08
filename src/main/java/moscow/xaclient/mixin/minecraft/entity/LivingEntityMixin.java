package moscow.xaclient.mixin.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.impl.game.EntityDeathEvent;
import moscow.xaclient.systems.event.impl.game.EntityJumpEvent;
import moscow.xaclient.systems.modules.modules.player.NoDelay;
import moscow.xaclient.systems.modules.modules.player.NoPush;
import moscow.xaclient.systems.modules.modules.visuals.SwingAnimation;
import moscow.xaclient.utility.rotations.MoveCorrection;
import moscow.xaclient.utility.rotations.RotationHandler;
import moscow.xaclient.utility.rotations.RotationTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
   @Shadow
   private int jumpingCooldown;

   @Shadow
   public abstract void remove(RemovalReason var1);

   @Shadow
   public abstract ItemStack getMainHandStack();

   @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
   public int replaceSwingSpeed(int original) {
      SwingAnimation swingAnimationModule = XaClient.getInstance().getModuleManager().getModule(SwingAnimation.class);
      return swingAnimationModule.isEnabled() && swingAnimationModule.shouldApplyAnimation(this.getMainHandStack())
            ? (int) (original * XaClient.getInstance().getSwingManager().getSpeed().getCurrentValue())
            : original;
   }

   @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
   public void triggerJumpEvent(CallbackInfo ci) {
      LivingEntity livingEntity = (LivingEntity) (Object) this;
      EntityJumpEvent event = new EntityJumpEvent(livingEntity);
      XaClient.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @ModifyExpressionValue(method = "jump", at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/util/math/Vec3d;"))
   public Vec3d movementCorrection(Vec3d original) {
      RotationHandler rotationHandler = XaClient.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if ((Object) this != MinecraftClient.getInstance().player) {
         return original;
      } else if (currentTask != null && currentTask.getMoveCorrection() != MoveCorrection.NONE) {
         float yaw = rotationHandler.getCurrentRotation().getYaw() * (float) (Math.PI / 180.0);
         return new Vec3d(-MathHelper.sin(yaw) * 0.2F, 0.0, MathHelper.cos(yaw) * 0.2F);
      } else {
         return original;
      }
   }

   @Inject(method = "tickMovement", at = @At("HEAD"))
   public void removeJumpDelay(CallbackInfo ci) {
      NoDelay noDelay = XaClient.getInstance().getModuleManager().getModule(NoDelay.class);
      if (noDelay.isEnabled() && noDelay.getJump().isEnabled()) {
         this.jumpingCooldown = 0;
      }
   }

   @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
   private void removePushFromEntity(CallbackInfoReturnable<Boolean> cir) {
      NoPush noPush = XaClient.getInstance().getModuleManager().getModule(NoPush.class);
      LivingEntity entity = (LivingEntity) (Object) this;
      if (entity instanceof ClientPlayerEntity && noPush.isEnabled() && noPush.getEntities().isSelected()) {
         cir.setReturnValue(false);
      }
   }

   @Inject(method = "onDeath", at = @At("TAIL"))
   public void triggerEntityDeathEvent(DamageSource damageSource, CallbackInfo ci) {
      LivingEntity entity = (LivingEntity) (Object) this;
      XaClient.getInstance().getEventManager().triggerEvent(new EntityDeathEvent(entity, damageSource));
   }

   @Redirect(method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"))
   private float redirectGetPitch(LivingEntity instance) {
      RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getPitch() : rotationHandler.getCurrentRotation().getPitch();
   }

   @Redirect(method = "calcGlidingVelocity(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d redirectGetRotationVector(LivingEntity instance) {
      RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
      return rotationHandler.isIdling() ? instance.getRotationVector()
            : rotationHandler.getCurrentRotation().getRotationVector();
   }
}
