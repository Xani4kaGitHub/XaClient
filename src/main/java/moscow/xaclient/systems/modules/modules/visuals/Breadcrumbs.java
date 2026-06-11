package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import moscow.xaclient.XaClient;
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
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Breadcrumbs", desc = "modules.descriptions.breadcrumbs", category = ModuleCategory.VISUALS)
public class Breadcrumbs extends BaseModule {
   private final SliderSetting size = new SliderSetting(this, "modules.settings.breadcrumbs.size")
      .min(0.2F).max(2.0F).step(0.1F).currentValue(0.7F);
   private final SliderSetting lifetime = new SliderSetting(this, "modules.settings.breadcrumbs.lifetime")
      .min(200.0F).max(5000.0F).step(100.0F).currentValue(1500.0F).suffix("ms");
   private final ColorSetting color = new ColorSetting(this, "color").color(Colors.ACCENT);

   private final List<Circle> circles = new ArrayList<>();
   private double lastX = Double.NaN;
   private double lastZ = Double.NaN;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!EntityUtility.isInGame()) {
         this.circles.clear();
         return;
      }

      double x = mc.player.getX();
      double z = mc.player.getZ();
      double dx = Double.isNaN(this.lastX) ? 0.0 : x - this.lastX;
      double dz = Double.isNaN(this.lastZ) ? 0.0 : z - this.lastZ;
      if (dx * dx + dz * dz > 0.0016) {
         this.circles.add(new Circle(new Vec3d(x, mc.player.getY() + 0.02, z), System.currentTimeMillis()));
      }

      this.lastX = x;
      this.lastZ = z;

      long life = (long)this.lifetime.getCurrentValue();
      Iterator<Circle> it = this.circles.iterator();
      while (it.hasNext()) {
         if (System.currentTimeMillis() - it.next().time > life) {
            it.remove();
         }
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.circles.isEmpty()) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Identifier id = XaClient.id("textures/bloom.png");
      long life = (long)this.lifetime.getCurrentValue();
      float baseSize = this.size.getCurrentValue();
      ColorRGBA base = this.color.getColor();

      RenderSystem.enableBlend();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.depthMask(false);
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (Circle circle : this.circles) {
         float progress = 1.0F - (float)(System.currentTimeMillis() - circle.time) / life;
         if (progress <= 0.0F) {
            continue;
         }

         ms.push();
         RenderUtility.prepareMatrices(ms, circle.pos);
         ms.multiply(camera.getRotation());
         float s = baseSize * progress;
         DrawUtility.drawImage(ms, builder, -s / 2.0, -s / 2.0, 0.0, s, s, base.withAlpha(base.getAlpha() * progress));
         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      RenderSystem.depthMask(true);
      RenderSystem.setShaderTexture(0, 0);
      RenderSystem.disableBlend();
   };

   @Override
   public void onDisable() {
      this.circles.clear();
      this.lastX = Double.NaN;
      this.lastZ = Double.NaN;
   }

   private record Circle(Vec3d pos, long time) {
   }
}
