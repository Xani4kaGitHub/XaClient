package moscow.xaclient.mixin.minecraft.render.entity.feature;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.modules.visuals.Capes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerCapeModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CapeFeatureRenderer.class)
public abstract class CapeFeatureRendererMixin {
   @Unique
   private PlayerCapeModel<PlayerEntityRenderState> xaclient$capeModel;

   @Inject(
      method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/state/PlayerEntityRenderState;FF)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void xaclient$customCape(
      MatrixStack matrices, VertexConsumerProvider provider, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci
   ) {
      Capes module = XaClient.getInstance().getModuleManager().getModule(Capes.class);
      if (module == null || state.invisible) {
         return;
      }

      Identifier texture = module.getCapeFor(state.name);
      if (texture == null) {
         return;
      }

      if (this.xaclient$capeModel == null) {
         this.xaclient$capeModel = new PlayerCapeModel<>(MinecraftClient.getInstance().getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER_CAPE));
      }

      matrices.push();
      VertexConsumer consumer = provider.getBuffer(RenderLayer.getEntitySolid(texture));
      this.xaclient$capeModel.setAngles(state);
      this.xaclient$capeModel.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
      matrices.pop();
      ci.cancel();
   }
}
