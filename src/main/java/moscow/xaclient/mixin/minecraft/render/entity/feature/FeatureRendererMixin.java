package moscow.xaclient.mixin.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.modules.visuals.AntiInvisible;
import moscow.xaclient.systems.modules.modules.visuals.TotemPop;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FeatureRenderer.class)
public abstract class FeatureRendererMixin {
   @Unique
   private static final AntiInvisible ANTI_INVISIBLE_MODULE = XaClient.getInstance().getModuleManager().getModule(AntiInvisible.class);

   @WrapOperation(
      method = "renderModel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"
      )
   )
   private static void changeModelColor(
      EntityModel<?> instance,
      MatrixStack matrixStack,
      VertexConsumer vertexConsumer,
      int light,
      int overlay,
      int color,
      Operation<Void> original,
      @Local(argsOnly = true) LivingEntityRenderState state
   ) {
      if (ANTI_INVISIBLE_MODULE.isEnabled() && ANTI_INVISIBLE_MODULE.shouldModifyOpacity(state)) {
         Entity entity = ((EntityRenderStateAddition)state).xaclient$getEntity();
         color = entity instanceof ArmorStandEntity
            ? Colors.WHITE.withAlpha(0.0F).getRGB()
            : Colors.WHITE.withAlpha(ANTI_INVISIBLE_MODULE.getOpacity().getCurrentValue() / 100.0F * 255.0F).getRGB();
      }

      if (TotemPop.isRenderingGhost()) {
         color = TotemPop.getGhostColor();
      }

      original.call(new Object[]{instance, matrixStack, vertexConsumer, light, overlay, color});
   }

   @WrapOperation(
      method = "renderModel",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/RenderLayer;getEntityCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"
      )
   )
   private static RenderLayer changeModelRenderLayer(Identifier texture, Operation<RenderLayer> original, @Local(argsOnly = true) LivingEntityRenderState state) {
      return ANTI_INVISIBLE_MODULE.isEnabled() && ANTI_INVISIBLE_MODULE.shouldModifyOpacity(state)
            || TotemPop.isRenderingGhost()
         ? RenderLayer.getItemEntityTranslucentCull(texture)
         : (RenderLayer)original.call(new Object[]{texture});
   }
}
