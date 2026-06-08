package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.render.Draw3DUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

@ModuleInfo(name = "Block HighLight", category = ModuleCategory.VISUALS, desc = "modules.descriptions.block_highlight")
public class BlockHighLight extends BaseModule {
   private final SelectSetting render = new SelectSetting(this, "modules.settings.block_highlight.render").min(1);
   private final SelectSetting.Value fill = new SelectSetting.Value(this.render, "modules.settings.block_highlight.render.fill").select();
   private final SelectSetting.Value outline = new SelectSetting.Value(this.render, "modules.settings.block_highlight.render.outline").select();
   private final SelectSetting.Value diagonals = new SelectSetting.Value(this.render, "modules.settings.block_highlight.render.diagonals");
   private final ModeSetting shape = new ModeSetting(this, "modules.settings.block_highlight.shape");
   private final ModeSetting.Value outlineShape = new ModeSetting.Value(this.shape, "modules.settings.block_highlight.shape.outline_shape").select();
   private final ModeSetting.Value fullBlock = new ModeSetting.Value(this.shape, "modules.settings.block_highlight.shape.full_block");
   private final ColorSetting color = new ColorSetting(this, "modules.settings.block_highlight.color")
      .color(new ColorRGBA(90.0F, 170.0F, 255.0F, 255.0F));
   private final SliderSetting fillAlpha = new SliderSetting(
         this, "modules.settings.block_highlight.fill_alpha", () -> !this.fill.isSelected()
      )
      .min(0.0F)
      .max(255.0F)
      .step(5.0F)
      .currentValue(45.0F);
   private final SliderSetting outlineAlpha = new SliderSetting(
         this, "modules.settings.block_highlight.outline_alpha", () -> !this.outline.isSelected() && !this.diagonals.isSelected()
      )
      .min(0.0F)
      .max(255.0F)
      .step(5.0F)
      .currentValue(160.0F);
   private final SliderSetting lineWidth = new SliderSetting(
         this, "modules.settings.block_highlight.line_width", () -> !this.outline.isSelected() && !this.diagonals.isSelected()
      )
      .min(1.0F)
      .max(6.0F)
      .step(0.5F)
      .currentValue(2.0F);
   private final SliderSetting expand = new SliderSetting(this, "modules.settings.block_highlight.expand")
      .min(0.0F)
      .max(0.08F)
      .step(0.01F)
      .currentValue(0.002F);
   private final BooleanSetting throughWalls = new BooleanSetting(this, "modules.settings.block_highlight.through_walls").enable();
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world == null || mc.player == null) {
         return;
      }

      if (!(mc.crosshairTarget instanceof BlockHitResult result) || result.getType() != HitResult.Type.BLOCK) {
         return;
      }

      BlockPos pos = result.getBlockPos();
      BlockState state = mc.world.getBlockState(pos);
      if (state.isAir()) {
         return;
      }

      List<Box> boxes = this.getBoxes(state, pos);
      if (boxes.isEmpty()) {
         return;
      }

      this.renderBoxes(event, boxes);
   };

   private List<Box> getBoxes(BlockState state, BlockPos pos) {
      if (this.shape.is(this.fullBlock)) {
         return List.of(new Box(pos));
      }

      VoxelShape voxelShape = state.getOutlineShape(mc.world, pos);
      if (voxelShape.isEmpty()) {
         return List.of(new Box(pos));
      }

      return voxelShape.getBoundingBoxes().stream().map(box -> box.offset(pos)).toList();
   }

   private void renderBoxes(Render3DEvent event, List<Box> boxes) {
      MatrixStack matrices = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      RenderSystem.enableBlend();
      if (this.throughWalls.isEnabled()) {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      if (this.fill.isSelected()) {
         BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

         for (Box box : boxes) {
            Draw3DUtility.renderFilledBox(
               matrices,
               quadsBuffer,
               box.expand(this.expand.getCurrentValue()).offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ()),
               this.color.getColor().withAlpha(this.fillAlpha.getCurrentValue())
            );
         }

         RenderUtility.buildBuffer(quadsBuffer);
      }

      if (this.outline.isSelected() || this.diagonals.isSelected()) {
         RenderSystem.lineWidth(this.lineWidth.getCurrentValue());
         BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (Box box : boxes) {
            Box renderBox = box.expand(this.expand.getCurrentValue()).offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
            ColorRGBA renderColor = this.color.getColor().withAlpha(this.outlineAlpha.getCurrentValue());
            if (this.outline.isSelected()) {
               Draw3DUtility.renderOutlinedBox(matrices, linesBuffer, renderBox, renderColor);
            }

            if (this.diagonals.isSelected()) {
               Draw3DUtility.renderBoxInternalDiagonals(matrices, linesBuffer, renderBox, renderColor);
            }
         }

         RenderUtility.buildBuffer(linesBuffer);
         RenderSystem.lineWidth(1.0F);
      }

      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   }
}
