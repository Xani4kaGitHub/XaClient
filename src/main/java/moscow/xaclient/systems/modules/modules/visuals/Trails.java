package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
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
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@ModuleInfo(name = "Trails", desc = "modules.descriptions.trails", category = ModuleCategory.VISUALS)
public class Trails extends BaseModule {
   private final SliderSetting height = new SliderSetting(this, "modules.settings.trails.height")
      .min(0.2F).max(2.0F).step(0.1F).currentValue(1.0F);
   private final SliderSetting lifetime = new SliderSetting(this, "modules.settings.trails.lifetime")
      .min(200.0F).max(5000.0F).step(100.0F).currentValue(1200.0F).suffix("ms");
   private final ColorSetting color = new ColorSetting(this, "color").color(Colors.ACCENT);

   private final Deque<Point> points = new ArrayDeque<>();
   private double lastX = Double.NaN;
   private double lastZ = Double.NaN;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!EntityUtility.isInGame()) {
         this.points.clear();
         return;
      }

      double x = mc.player.getX();
      double z = mc.player.getZ();
      double dx = Double.isNaN(this.lastX) ? 0.0 : x - this.lastX;
      double dz = Double.isNaN(this.lastZ) ? 0.0 : z - this.lastZ;
      if (dx * dx + dz * dz > 0.0009) {
         this.points.addLast(new Point(new Vec3d(x, mc.player.getY(), z), System.currentTimeMillis()));
      }

      this.lastX = x;
      this.lastZ = z;

      long life = (long)this.lifetime.getCurrentValue();
      Iterator<Point> it = this.points.iterator();
      while (it.hasNext()) {
         if (System.currentTimeMillis() - it.next().time > life) {
            it.remove();
         }
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.points.size() < 2) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      ms.push();
      RenderUtility.prepareMatrices(ms);
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = ms.peek().getPositionMatrix();
      long life = (long)this.lifetime.getCurrentValue();
      float h = this.height.getCurrentValue();
      ColorRGBA base = this.color.getColor();

      for (Point point : this.points) {
         float progress = 1.0F - (float)(System.currentTimeMillis() - point.time) / life;
         if (progress <= 0.0F) {
            continue;
         }

         int bottom = base.withAlpha(base.getAlpha() * progress).getRGB();
         int top = base.withAlpha(0.0F).getRGB();
         builder.vertex(matrix, (float)point.pos.x, (float)point.pos.y, (float)point.pos.z).color(bottom);
         builder.vertex(matrix, (float)point.pos.x, (float)(point.pos.y + h), (float)point.pos.z).color(top);
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      ms.pop();
   };

   @Override
   public void onDisable() {
      this.points.clear();
      this.lastX = Double.NaN;
      this.lastZ = Double.NaN;
   }

   private record Point(Vec3d pos, long time) {
   }
}
