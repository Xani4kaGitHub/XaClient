package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.AttackEvent;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Particles", desc = "modules.descriptions.particles", category = ModuleCategory.VISUALS)
public class Particles extends BaseModule {
   private final SliderSetting count = new SliderSetting(this, "modules.settings.particles.count")
      .min(1.0F).max(30.0F).step(1.0F).currentValue(10.0F);
   private final SliderSetting size = new SliderSetting(this, "modules.settings.particles.size")
      .min(0.05F).max(0.6F).step(0.05F).currentValue(0.2F);
   private final SliderSetting lifetime = new SliderSetting(this, "modules.settings.particles.lifetime")
      .min(200.0F).max(3000.0F).step(100.0F).currentValue(800.0F).suffix("ms");
   private final ColorSetting color = new ColorSetting(this, "color").color(Colors.ACCENT);

   private final List<Particle> particles = new ArrayList<>();
   private final Random random = new Random();

   private final EventListener<AttackEvent> onAttack = event -> {
      Entity entity = event.getEntity();
      if (entity == null) {
         return;
      }

      Vec3d origin = entity.getPos().add(0.0, entity.getHeight() / 2.0, 0.0);
      int amount = (int)this.count.getCurrentValue();
      for (int i = 0; i < amount; i++) {
         Vec3d velocity = new Vec3d(
            (this.random.nextDouble() - 0.5) * 0.2,
            this.random.nextDouble() * 0.2 + 0.05,
            (this.random.nextDouble() - 0.5) * 0.2
         );
         this.particles.add(new Particle(origin, velocity, System.currentTimeMillis()));
      }
   };

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (!EntityUtility.isInGame()) {
         this.particles.clear();
         return;
      }

      long life = (long)this.lifetime.getCurrentValue();
      Iterator<Particle> it = this.particles.iterator();
      while (it.hasNext()) {
         Particle particle = it.next();
         if (System.currentTimeMillis() - particle.spawn > life) {
            it.remove();
            continue;
         }

         particle.pos = particle.pos.add(particle.velocity);
         particle.velocity = particle.velocity.add(0.0, -0.012, 0.0).multiply(0.92);
      }
   };

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (this.particles.isEmpty()) {
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

      for (Particle particle : this.particles) {
         float progress = 1.0F - (float)(System.currentTimeMillis() - particle.spawn) / life;
         if (progress <= 0.0F) {
            continue;
         }

         ms.push();
         RenderUtility.prepareMatrices(ms, particle.pos);
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
      this.particles.clear();
   }

   private static final class Particle {
      private Vec3d pos;
      private Vec3d velocity;
      private final long spawn;

      private Particle(Vec3d pos, Vec3d velocity, long spawn) {
         this.pos = pos;
         this.velocity = velocity;
         this.spawn = spawn;
      }
   }
}
