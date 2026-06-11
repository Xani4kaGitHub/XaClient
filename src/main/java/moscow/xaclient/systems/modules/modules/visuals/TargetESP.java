package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.render.CrystalRenderer;
import moscow.xaclient.utility.render.DrawUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@ModuleInfo(name = "Target ESP", category = ModuleCategory.VISUALS, desc = "Помечает активную цель")
public class TargetESP extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.target_esp.mode");
   private final ModeSetting.Value souls = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.souls");
   private final ModeSetting.Value crystals = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.crystals").select();
   private final ModeSetting.Value markers = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.markers");
   private final ModeSetting.Value circle = new ModeSetting.Value(this.mode, "modules.settings.target_esp.mode.circle");
   private final ColorSetting colorTarget = new ColorSetting(this, "color").color(Colors.ACCENT);
   private final Animation animation = new Animation(300L, 0.0F, Easing.BOTH_CUBIC);
   private final Animation moving = new Animation(70L, 0.0F, Easing.LINEAR);
   private LivingEntity prevTarget;
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (EntityUtility.isInGame()) {
         LivingEntity target = XaClient.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity target2 ? target2 : null;
         this.animation.setEasing(Easing.FIGMA_EASE_IN_OUT);
         this.animation.update(target != null);
         this.moving.update(this.moving.getValue() + 10.0F + 50.0F);
         if (target != null) {
            this.prevTarget = target;
         }

         if (this.prevTarget != null && this.animation.getValue() != 0.0F) {
            MatrixStack ms = event.getMatrices();
            ms.push();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            RenderSystem.enableDepthTest();
            if (mc.world
                  .raycast(
                     new RaycastContext(mc.gameRenderer.getCamera().getPos(), this.prevTarget.getEyePos(), ShapeType.COLLIDER, FluidHandling.NONE, mc.player)
                  )
                  .getType()
               != Type.MISS) {
               RenderSystem.disableDepthTest();
            }

            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            if (this.crystals.isSelected()) {
               this.drawCrystals(ms, this.prevTarget);
            } else if (this.markers.isSelected()) {
               this.drawMarkers(ms, this.prevTarget);
            } else if (this.circle.isSelected()) {
               this.drawCircle(ms, this.prevTarget);
            } else {
               this.drawGhosts(ms, this.prevTarget);
            }

            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.disableDepthTest();
            ms.pop();
         }
      }
   };

   private void drawCrystals(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      ColorRGBA color = this.colorTarget.getColor();
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      BufferBuilder builder = CrystalRenderer.createBuffer();

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float size = 0.1F;
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin(i)), cos);
         Vec3d crystalPos = this.getRenderPos(this.prevTarget).add(sin, 1.0, cos);
         Vec3d targetPos = target.getPos().add(0.0, target.getHeight() / 2.0, 0.0);
         Vector3f directionToTarget = new Vector3f(
               (float)(targetPos.x - crystalPos.x), (float)(targetPos.y - crystalPos.y), (float)(targetPos.z - crystalPos.z)
            )
            .normalize();
         Vector3f initialDirection = new Vector3f(0.0F, 1.0F, 0.0F);
         Quaternionf rotation = new Quaternionf().rotationTo(initialDirection, directionToTarget);
         ms.multiply(rotation);
         CrystalRenderer.render(ms, builder, 0.0F, 0.0F, 0.0F, size, color.withAlpha(255.0F * this.animation.getValue()));
         ms.pop();
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
      Identifier id = XaClient.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      float bigSize = 1.0F;

      for (int i = 0; i < 360; i += 20) {
         float val = 1.2F - 0.5F * this.animation.getValue();
         float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 0.3F)) * width * val);
         float size = 0.1F;
         ms.push();
         ms.translate(sin, 0.1F + target.getHeight() * Math.abs(MathUtility.sin(i)), cos);
         ms.multiply(camera.getRotation());
         DrawUtility.drawImage(
            ms,
            buffer,
            (double)(-bigSize / 2.0F),
            (double)(-bigSize / 2.0F),
            0.0,
            (double)bigSize,
            (double)bigSize,
            color.withAlpha(255.0F * this.animation.getValue() * 0.2F)
         );
         ms.pop();
      }

      RenderUtility.buildBuffer(buffer);
   }

   private void drawGhosts(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      ColorRGBA color = this.colorTarget.getColor();
      Identifier id = XaClient.id("textures/bloom.png");
      float width = this.prevTarget.getWidth() * 1.5F;
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      int step = 2;
      int wormTick = 0;
      int wormCD = 0;
      int wormCount = 0;

      for (int i = 0; i < 360; i += step) {
         float size = 0.13F + 0.005F * wormTick;
         float bigSize = 0.7F + 0.005F * wormTick;
         if (wormCD > 0) {
            wormCD -= step;
         } else {
            wormTick += step;
            if (wormTick > 50) {
               wormCD = 100;
               wormTick = 0;
               wormCount++;
            } else {
               float val = Math.max(0.5F, 1.2F - 0.5F * this.animation.getValue());
               float sin = (float)(MathUtility.sin((float)Math.toRadians(i + this.moving.getValue() * 1.0F)) * width * val);
               float cos = (float)(MathUtility.cos((float)Math.toRadians(i + this.moving.getValue() * 1.0F)) * width * val);
               ms.push();
               ms.translate(
                  sin,
                  this.prevTarget.getHeight() / 1.5F
                     + this.prevTarget.getHeight() / 3.0F * MathUtility.sin(Math.toRadians(i / 2.0F + this.moving.getValue() / 5.0F)),
                  cos
               );
               ms.multiply(camera.getRotation());
               DrawUtility.drawImage(
                  ms,
                  builder,
                  (double)(-bigSize / 2.0F),
                  (double)(-bigSize / 2.0F),
                  (double)(-size / 2.0F),
                  (double)bigSize,
                  (double)bigSize,
                  color.withAlpha(color.getAlpha() * this.animation.getValue() * 0.05F)
               );
               DrawUtility.drawImage(
                  ms,
                  builder,
                  (double)(-size / 2.0F),
                  (double)(-size / 2.0F),
                  (double)(-size / 2.0F),
                  (double)size,
                  (double)size,
                  color.withAlpha(color.getAlpha() * this.animation.getValue())
               );
               ms.pop();
            }
         }
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private void drawMarkers(MatrixStack ms, LivingEntity target) {
      Camera camera = mc.gameRenderer.getCamera();
      ColorRGBA color = this.colorTarget.getColor();
      Identifier id = XaClient.id("textures/bloom.png");
      RenderSystem.setShaderTexture(0, id);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      float size = 0.6F + 0.1F * this.animation.getValue();
      float height = target.getHeight() + 0.5F;
      ms.push();
      ms.translate(0.0F, height, 0.0F);
      ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(this.moving.getValue() * 2.0F));
      DrawUtility.drawImage(
         ms,
         builder,
         (double)(-size / 2.0F),
         (double)(-size / 2.0F),
         0.0,
         (double)size,
         (double)size,
         color.withAlpha(255.0F * this.animation.getValue())
      );
      ms.pop();
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private void drawCircle(MatrixStack ms, LivingEntity target) {
      ColorRGBA color = this.colorTarget.getColor();
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
      RenderUtility.prepareMatrices(ms, this.getRenderPos(this.prevTarget));
      float radius = target.getWidth() * 0.9F * (0.8F + 0.2F * this.animation.getValue());
      float ringHeight = 0.12F;
      float alpha = 200.0F * this.animation.getValue();
      var matrix = ms.peek().getPositionMatrix();
      for (int i = 0; i <= 360; i += 6) {
         float rad = (float)Math.toRadians(i + this.moving.getValue() * 0.5F);
         float x = (float)(MathUtility.cos(rad) * radius);
         float z = (float)(MathUtility.sin(rad) * radius);
         builder.vertex(matrix, x, 0.05F, z).color(color.withAlpha(alpha).getRGB());
         builder.vertex(matrix, x, 0.05F + ringHeight, z).color(color.withAlpha(0.0F).getRGB());
      }

      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private Vec3d getRenderPos(LivingEntity target) {
      float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
      return new Vec3d(
         MathHelper.lerp(tickDelta, target.prevX, target.getX()),
         MathHelper.lerp(tickDelta, target.prevY, target.getY()),
         MathHelper.lerp(tickDelta, target.prevZ, target.getZ())
      );
   }

   @Override
   public void tick() {
      super.tick();
   }
}
