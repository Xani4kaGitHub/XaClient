package moscow.xaclient.mixin.minecraft.client.render.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import moscow.xaclient.XaClient;
import moscow.xaclient.mixin.accessors.BipedEntityModelAccessor;
import moscow.xaclient.systems.modules.modules.visuals.AntiInvisible;
import moscow.xaclient.systems.modules.modules.visuals.FriendMarkers;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.countermine.AntiAim;
import moscow.xaclient.utility.mixins.EntityRenderStateAddition;
import moscow.xaclient.utility.rotations.RotationHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   @Unique
   private static final AntiInvisible ANTI_INVISIBLE_MODULE = XaClient.getInstance().getModuleManager().getModule(AntiInvisible.class);

   @Shadow
   public abstract Identifier getTexture(S var1);

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F")
   )
   public float changeYaw(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity && !AntiAim.FORCE) {
         RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
         float yaw = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getYaw();
         rotationHandler.getServerRotation().setYaw(yaw);
         return yaw;
      } else {
         return oldValue;
      }
   }

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F")
   )
   public float changeHeadYaw(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity && !AntiAim.FORCE) {
         RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
         float yaw = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getYaw();
         rotationHandler.getServerRotation().setYaw(yaw);
         return yaw;
      } else {
         return oldValue;
      }
   }

   @ModifyExpressionValue(
      method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")
   )
   public float changePitch(float oldValue, LivingEntity entity) {
      if (entity instanceof ClientPlayerEntity && !AntiAim.FORCE) {
         RotationHandler rotationHandler = XaClient.getInstance().getRotationHandler();
         float pitch = rotationHandler.isIdling() ? oldValue : rotationHandler.getRenderRotation().getPitch();
         rotationHandler.getServerRotation().setPitch(pitch);
         return pitch;
      } else {
         return oldValue;
      }
   }

   @WrapOperation(
      method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private void changeModelColor(
      EntityModel<?> instance,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      int color,
      Operation<Void> original,
      @Local(argsOnly = true) S livingEntityRenderState
   ) {
      if (ANTI_INVISIBLE_MODULE.isEnabled() && ANTI_INVISIBLE_MODULE.shouldModifyOpacity(livingEntityRenderState)) {
         Entity entity = ((EntityRenderStateAddition)livingEntityRenderState).xaclient$getEntity();
         color = entity instanceof ArmorStandEntity
            ? Colors.WHITE.withAlpha(0.0F).getRGB()
            : Colors.WHITE.withAlpha(ANTI_INVISIBLE_MODULE.getOpacity().getCurrentValue() / 100.0F * 255.0F).getRGB();
      }

      Entity entity = ((EntityRenderStateAddition)livingEntityRenderState).xaclient$getEntity();
      FriendMarkers markers = XaClient.getInstance().getModuleManager().getModule(FriendMarkers.class);
      if (entity instanceof PlayerEntity player
         && instance instanceof BipedEntityModel<?> model
         && markers.isEnabled()
         && markers.getHeads().isSelected()
         && XaClient.getInstance().getFriendManager().isFriend(player.getName().getString())) {
         BipedEntityModelAccessor accessor = (BipedEntityModelAccessor)model;
         float scale = 1.09F;
         accessor.xaclient$getHead().scale(new Vector3f(scale, scale, scale));
         original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
      }

      original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
   }

   @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
   private RenderLayer changeRenderLayer(RenderLayer original, S state, boolean showBody, boolean translucent, boolean showOutline) {
      if (ANTI_INVISIBLE_MODULE.isEnabled() && !showBody && !translucent && !showOutline) {
         state.invisible = false;
         return RenderLayer.getItemEntityTranslucentCull(this.getTexture(state));
      } else {
         return original;
      }
   }
}
