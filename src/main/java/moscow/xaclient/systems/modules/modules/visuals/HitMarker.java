package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.PostAttackEvent;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Hit Marker", category = ModuleCategory.VISUALS, desc = "modules.descriptions.hit_marker")
public class HitMarker extends BaseModule {
   private final SliderSetting size = new SliderSetting(this, "modules.settings.hit_marker.size")
      .min(0.15F)
      .max(2.0F)
      .step(0.05F)
      .currentValue(0.5F);
   private final SliderSetting duration = new SliderSetting(this, "modules.settings.hit_marker.duration")
      .min(100.0F)
      .max(1500.0F)
      .step(50.0F)
      .currentValue(550.0F)
      .suffix("ms");
   private final BooleanSetting glow = new BooleanSetting(this, "modules.settings.hit_marker.glow").enable();
   private final ColorSetting color = new ColorSetting(this, "modules.settings.hit_marker.color").color(Colors.ACCENT);
   private final Identifier texture = XaClient.id("textures/hit.png");
   private final List<Marker> markers = new ArrayList<>();

   // RU: Создает маркер в точке, куда пришел удар по сущности.
   // EN: Creates a marker at the resolved entity hit point.
   private final EventListener<PostAttackEvent> onPostAttack = event -> {
      if (!EntityUtility.isInGame() || event.getEntity() == null) {
         return;
      }

      this.markers.add(new Marker(this.resolveHitPosition(event.getEntity()), System.currentTimeMillis()));
   };

   // RU: Рисует маркеры поверх мира и плавно убирает старые.
   // EN: Renders markers in the world and fades old ones out.
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.markers.isEmpty() || !EntityUtility.isInGame()) {
         return;
      }

      this.removeDeadMarkers();
      if (this.markers.isEmpty()) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      Camera camera = event.getCamera();
      matrices.push();
      RenderSystem.enableBlend();
      if (this.glow.isEnabled()) {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      } else {
         RenderSystem.defaultBlendFunc();
      }

      RenderSystem.disableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      RenderSystem.setShaderTexture(0, this.texture);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Marker marker : this.markers) {
         float progress = this.getProgress(marker);
         float alpha = this.getAlpha(progress);
         if (alpha <= 0.01F) {
            continue;
         }

         float markerSize = this.size.getCurrentValue() * this.getScale(progress);
         ColorRGBA markerColor = this.color.getColor().withAlpha(this.color.getColor().getAlpha() * alpha);
         matrices.push();
         RenderUtility.prepareMatrices(matrices, marker.pos);
         matrices.multiply(camera.getRotation());
         DrawUtility.drawImage(
            matrices,
            builder,
            -markerSize / 2.0,
            -markerSize / 2.0,
            0.0,
            markerSize,
            markerSize,
            markerColor
         );
         matrices.pop();
      }

      RenderUtility.buildBuffer(builder);
      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      matrices.pop();
   };

   @Override
   // RU: Удаляет все маркеры при выключении модуля.
   // EN: Removes all markers when the module is disabled.
   public void onDisable() {
      this.markers.clear();
   }

   // RU: Ищет точку пересечения взгляда игрока с хитбоксом цели.
   // EN: Resolves where the player's view intersects the target hitbox.
   private Vec3d resolveHitPosition(Entity target) {
      Vec3d fallback = target.getBoundingBox().getCenter();
      if (mc.player == null) {
         return fallback;
      }

      Vec3d eyePos = mc.player.getCameraPosVec(1.0F);
      Vec3d lookVec = mc.player.getRotationVec(1.0F);
      double distance = Math.max(eyePos.distanceTo(fallback) + 1.0, 6.0);
      Optional<Vec3d> hitPos = target.getBoundingBox().raycast(eyePos, eyePos.add(lookVec.multiply(distance)));
      return hitPos.orElse(fallback);
   }

   // RU: Удаляет маркеры после завершения их времени жизни.
   // EN: Removes markers after their lifetime ends.
   private void removeDeadMarkers() {
      Iterator<Marker> iterator = this.markers.iterator();

      while (iterator.hasNext()) {
         if (this.getProgress(iterator.next()) >= 1.0F) {
            iterator.remove();
         }
      }
   }

   // RU: Возвращает прогресс жизни маркера от 0 до 1.
   // EN: Returns marker lifetime progress from 0 to 1.
   private float getProgress(Marker marker) {
      return Math.min(1.0F, (System.currentTimeMillis() - marker.startTime) / this.duration.getCurrentValue());
   }

   // RU: Считает прозрачность появления и исчезновения маркера.
   // EN: Calculates marker fade-in and fade-out opacity.
   private float getAlpha(float progress) {
      if (progress < 0.25F) {
         return this.easeOutCubic(progress / 0.25F);
      }

      return 1.0F - this.easeInCubic((progress - 0.25F) / 0.75F);
   }

   // RU: Считает небольшой pop-up масштаб маркера.
   // EN: Calculates the small pop-up scale for the marker.
   private float getScale(float progress) {
      if (progress < 0.2F) {
         return 0.55F + 0.45F * this.easeOutBack(progress / 0.2F);
      }

      return 1.0F - 0.15F * this.easeInCubic(progress);
   }

   // RU: Плавная функция для появления.
   // EN: Smooth easing function for appearing.
   private float easeOutCubic(float value) {
      float inverted = 1.0F - value;
      return 1.0F - inverted * inverted * inverted;
   }

   // RU: Плавная функция для исчезновения.
   // EN: Smooth easing function for disappearing.
   private float easeInCubic(float value) {
      return value * value * value;
   }

   // RU: Добавляет легкое увеличение в начале анимации.
   // EN: Adds a slight overshoot at the start of the animation.
   private float easeOutBack(float value) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow(value - 1.0F, 3.0) + c1 * (float)Math.pow(value - 1.0F, 2.0);
   }

   private record Marker(Vec3d pos, long startTime) {
   }
}
