package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

@ModuleInfo(name = "Line Glyphes", desc = "modules.descriptions.line_glyphes", category = ModuleCategory.VISUALS)
public class LineGlyphes extends BaseModule {
   private final SliderSetting radius = new SliderSetting(this, "modules.settings.line_glyphes.radius")
      .min(0.5F).max(3.0F).step(0.1F).currentValue(1.2F);
   private final SliderSetting speed = new SliderSetting(this, "modules.settings.line_glyphes.speed")
      .min(0.5F).max(5.0F).step(0.1F).currentValue(2.0F);
   private final SliderSetting lines = new SliderSetting(this, "modules.settings.line_glyphes.lines")
      .min(1.0F).max(6.0F).step(1.0F).currentValue(3.0F);
   private final SliderSetting glowLayers = new SliderSetting(this, "modules.settings.line_glyphes.glow")
      .min(1.0F).max(6.0F).step(1.0F).currentValue(4.0F);
   private final ColorSetting color = new ColorSetting(this, "color").color(Colors.ACCENT);

   private float phase;

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!EntityUtility.isInGame()) {
         return;
      }

      this.phase += this.speed.getCurrentValue() * event.getTickDelta() * 0.05F;
      MatrixStack ms = event.getMatrices();
      ms.push();
      RenderUtility.prepareMatrices(ms, mc.player.getLerpedPos(event.getTickDelta()));

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

      ColorRGBA base = this.color.getColor();
      int lineCount = (int)this.lines.getCurrentValue();
      int layers = (int)this.glowLayers.getCurrentValue();
      float r = this.radius.getCurrentValue();
      float height = mc.player.getHeight();

      for (int layer = layers; layer >= 1; layer--) {
         float width = layer * 1.5F;
         float alpha = base.getAlpha() * (0.5F / layer);
         RenderSystem.lineWidth(width);
         BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
         Matrix4f matrix = ms.peek().getPositionMatrix();
         int colorRGB = base.withAlpha(alpha).getRGB();

         for (int line = 0; line < lineCount; line++) {
            float offset = line * (360.0F / lineCount);
            for (int i = 0; i <= 360; i += 8) {
               float angle = (float)Math.toRadians(i + offset);
               float t = i / 360.0F;
               float y = height * t;
               float dynamicR = r * (float)Math.sin(t * Math.PI);
               float x = (float)(MathUtility.cos(angle + this.phase) * dynamicR);
               float z = (float)(MathUtility.sin(angle + this.phase) * dynamicR);
               builder.vertex(matrix, x, y, z).color(colorRGB);
            }
         }

         BufferRenderer.drawWithGlobalProgram(builder.end());
      }

      RenderSystem.lineWidth(1.0F);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      ms.pop();
   };

   @Override
   public void tick() {
      super.tick();
   }
}
