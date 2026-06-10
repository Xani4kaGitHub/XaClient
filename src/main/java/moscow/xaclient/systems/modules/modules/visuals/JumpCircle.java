package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Jump Circle", category = ModuleCategory.VISUALS, desc = "modules.descriptions.jump_circle")
public class JumpCircle extends BaseModule {
   private static final int MAX_CIRCLES = 8;
   private static final int SEGMENTS = 96;

   private final SliderSetting radius = new SliderSetting(this, "modules.settings.jump_circle.radius")
      .min(0.5F)
      .max(4.0F)
      .step(0.1F)
      .currentValue(1.85F);
   private final SliderSetting width = new SliderSetting(this, "modules.settings.jump_circle.width")
      .min(0.02F)
      .max(0.35F)
      .step(0.01F)
      .currentValue(0.08F);
   private final SliderSetting duration = new SliderSetting(this, "modules.settings.jump_circle.duration")
      .min(300.0F)
      .max(2500.0F)
      .step(50.0F)
      .currentValue(1450.0F)
      .suffix("ms");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.jump_circle.color").color(Colors.ACCENT);
   private final List<Circle> circles = new ArrayList<>();
   private boolean wasOnGround = true;

   // RU: Отслеживает момент прыжка и создает новый круг под игроком.
   // EN: Tracks the jump moment and creates a new circle under the player.
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!EntityUtility.isInGame()) {
         this.circles.clear();
         return;
      }

      boolean onGround = mc.player.isOnGround();
      if (this.wasOnGround && !onGround) {
         Vec3d pos = new Vec3d(mc.player.getX(), Math.floor(mc.player.getY()) + 0.03, mc.player.getZ());
         this.circles.add(new Circle(pos, System.currentTimeMillis()));
         while (this.circles.size() > MAX_CIRCLES) {
            this.circles.removeFirst();
         }
      }

      this.wasOnGround = onGround;
      this.removeDeadCircles();
   };

   // RU: Рисует плавно расширяющиеся кольца в мире.
   // EN: Renders smoothly expanding rings in the world.
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.circles.isEmpty() || !EntityUtility.isInGame()) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      matrices.push();
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.enableDepthTest();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      long now = System.currentTimeMillis();

      for (Circle circle : this.circles) {
         float progress = MathHelper.clamp((now - circle.startTime) / this.duration.getCurrentValue(), 0.0F, 1.0F);
         float alpha = 1.0F - progress;
         if (alpha <= 0.01F) {
            continue;
         }

         matrices.push();
         RenderUtility.prepareMatrices(matrices, circle.pos);
         this.renderCircle(matrices, builder, progress, alpha);
         matrices.pop();
      }

      RenderUtility.buildBuffer(builder);
      RenderSystem.depthMask(true);
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableBlend();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      matrices.pop();
   };

   @Override
   // RU: Синхронизирует состояние земли при включении модуля.
   // EN: Syncs the ground state when the module is enabled.
   public void onEnable() {
      if (mc.player != null) {
         this.wasOnGround = mc.player.isOnGround();
      }
   }

   @Override
   // RU: Очищает все активные круги при выключении модуля.
   // EN: Clears all active circles when the module is disabled.
   public void onDisable() {
      this.circles.clear();
   }

   // RU: Удаляет круги, у которых закончилась анимация.
   // EN: Removes circles after their animation finishes.
   private void removeDeadCircles() {
      long now = System.currentTimeMillis();
      Iterator<Circle> iterator = this.circles.iterator();

      while (iterator.hasNext()) {
         Circle circle = iterator.next();
         if (now - circle.startTime > this.duration.getCurrentValue()) {
            iterator.remove();
         }
      }
   }

   // RU: Строит кольцо из небольших сегментов без внешних текстур.
   // EN: Builds the ring from small segments without external textures.
   private void renderCircle(MatrixStack matrices, BufferBuilder builder, float progress, float alpha) {
      float eased = this.easeOutCubic(progress);
      float currentRadius = Math.max(0.05F, this.radius.getCurrentValue() * eased);
      float halfWidth = this.width.getCurrentValue() * (0.65F + 0.35F * (1.0F - progress));
      float innerRadius = Math.max(0.01F, currentRadius - halfWidth);
      float outerRadius = currentRadius + halfWidth;
      ColorRGBA base = this.color.getColor();
      ColorRGBA ringColor = base.withAlpha(base.getAlpha() * alpha);
      int color = ringColor.getRGB();

      for (int i = 0; i < SEGMENTS; i++) {
         float angle1 = (float)(Math.PI * 2.0 * i / SEGMENTS);
         float angle2 = (float)(Math.PI * 2.0 * (i + 1) / SEGMENTS);
         float sin1 = MathHelper.sin(angle1);
         float cos1 = MathHelper.cos(angle1);
         float sin2 = MathHelper.sin(angle2);
         float cos2 = MathHelper.cos(angle2);
         builder.vertex(matrices.peek().getPositionMatrix(), cos1 * outerRadius, 0.0F, sin1 * outerRadius).color(color);
         builder.vertex(matrices.peek().getPositionMatrix(), cos2 * outerRadius, 0.0F, sin2 * outerRadius).color(color);
         builder.vertex(matrices.peek().getPositionMatrix(), cos2 * innerRadius, 0.0F, sin2 * innerRadius).color(color);
         builder.vertex(matrices.peek().getPositionMatrix(), cos1 * innerRadius, 0.0F, sin1 * innerRadius).color(color);
      }
   }

   // RU: Делает расширение круга плавнее к концу анимации.
   // EN: Makes the circle expansion smoother near the end of the animation.
   private float easeOutCubic(float value) {
      float inverted = 1.0F - value;
      return 1.0F - inverted * inverted * inverted;
   }

   private record Circle(Vec3d pos, long startTime) {
   }
}
