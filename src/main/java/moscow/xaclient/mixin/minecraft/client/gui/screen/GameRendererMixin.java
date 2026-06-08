package moscow.xaclient.mixin.minecraft.client.gui.screen;

import com.llamalad7.mixinextras.sugar.Local;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.modules.player.NoRayTrace;
import moscow.xaclient.systems.modules.modules.visuals.Removals;
import moscow.xaclient.utility.render.Utils;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
   @Inject(
      method = "renderWorld",
      at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V", args = "ldc=hand")
   )
   private void onRenderWorld(
      RenderTickCounter tickCounter,
      CallbackInfo ci,
      @Local(ordinal = 0) Matrix4f projection,
      @Local(ordinal = 2) Matrix4f view,
      @Local(ordinal = 1) float tickDelta,
      @Local MatrixStack matrices
   ) {
      Utils.onRender(view, projection);
   }

   @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
   private void tiltViewWhenHurtHook(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
      Removals removals = XaClient.getInstance().getModuleManager().getModule(Removals.class);
      if (removals.isEnabled() && removals.getHurtCam().isSelected()) {
         ci.cancel();
      }
   }

   @Inject(method = "findCrosshairTarget", at = @At("RETURN"), cancellable = true)
   private void noRayTraceHook(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta, CallbackInfoReturnable<HitResult> cir) {
      NoRayTrace noRayTrace = XaClient.getInstance().getModuleManager().getModule(NoRayTrace.class);
      if (noRayTrace.isEnabled() && cir.getReturnValue() instanceof EntityHitResult hitResult && noRayTrace.shouldSkip(hitResult.getEntity())) {
         cir.setReturnValue(camera.raycast(blockInteractionRange, tickDelta, false));
      }
   }

   @Redirect(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
   private float renderWorldHook(float delta, float first, float second) {
      Removals removals = XaClient.getInstance().getModuleManager().getModule(Removals.class);
      return removals.isEnabled() && removals.getNausea().isSelected() ? 0.0F : MathHelper.lerp(delta, first, second);
   }
}
