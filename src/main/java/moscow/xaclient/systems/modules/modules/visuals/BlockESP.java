package moscow.xaclient.systems.modules.modules.visuals;

import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.impl.AbstractSetting;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.render.Draw3DUtility;
import moscow.xaclient.utility.render.RenderUtility;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Block ESP", category = ModuleCategory.VISUALS, desc = "modules.descriptions.block_esp")
public class BlockESP extends BaseModule {
   private final SliderSetting radius = new SliderSetting(this, "modules.settings.block_esp.radius")
      .min(1.0F)
      .max(30.0F)
      .step(1.0F)
      .currentValue(20.0F);
   private final SelectSetting blocks = new SelectSetting(this, "modules.settings.block_esp.blocks").min(1);
   private final SelectSetting.Value chests = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.chests").select();
   private final SelectSetting.Value furnaces = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.furnaces").select();
   private final SelectSetting.Value spawners = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.spawners").select();
   private final SelectSetting.Value brewingStands = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.brewing_stands").select();
   private final SelectSetting.Value enderChests = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.ender_chests").select();
   private final SelectSetting.Value detectorRails = new SelectSetting.Value(this.blocks, "modules.settings.block_esp.blocks.detector_rails").select();
   private final SelectSetting render = new SelectSetting(this, "modules.settings.block_esp.render").min(1);
   private final SelectSetting.Value fill = new SelectSetting.Value(this.render, "modules.settings.block_esp.render.fill");
   private final SelectSetting.Value outline = new SelectSetting.Value(this.render, "modules.settings.block_esp.render.outline").select();
   private final SelectSetting.Value diagonals = new SelectSetting.Value(this.render, "modules.settings.block_esp.render.diagonals");
   private final SliderSetting fillAlpha = new SliderSetting(this, "modules.settings.block_esp.fill_alpha", () -> !this.fill.isSelected())
      .min(0.0F)
      .max(255.0F)
      .step(5.0F)
      .currentValue(45.0F);
   private final SliderSetting outlineAlpha = new SliderSetting(
         this, "modules.settings.block_esp.outline_alpha", () -> !this.outline.isSelected() && !this.diagonals.isSelected()
      )
      .min(0.0F)
      .max(255.0F)
      .step(5.0F)
      .currentValue(150.0F);
   private final SliderSetting lineWidth = new SliderSetting(
         this, "modules.settings.block_esp.line_width", () -> !this.outline.isSelected() && !this.diagonals.isSelected()
      )
      .min(1.0F)
      .max(6.0F)
      .step(0.5F)
      .currentValue(2.0F);
   private final SliderSetting expand = new SliderSetting(this, "modules.settings.block_esp.expand")
      .min(0.0F)
      .max(0.08F)
      .step(0.01F)
      .currentValue(0.002F);
   private final BooleanSetting throughWalls = new BooleanSetting(this, "modules.settings.block_esp.through_walls").enable();
   private final Map<Block, ColorRGBA> customBlocks = new HashMap<>();
   private final AbstractSetting customBlocksSetting = new AbstractSetting(this, "modules.settings.block_esp.custom_blocks", () -> true) {
      @Override
      public JsonElement save() {
         JsonObject object = new JsonObject();
         for (Entry<Block, ColorRGBA> entry : BlockESP.this.customBlocks.entrySet()) {
            object.addProperty(Registries.BLOCK.getId(entry.getKey()).toString(), entry.getValue().toHex());
         }

         return object;
      }

      @Override
      public void load(JsonElement element) {
         BlockESP.this.customBlocks.clear();
         if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Entry<String, JsonElement> entry : object.entrySet()) {
               Identifier id = Identifier.tryParse(entry.getKey());
               if (id != null && Registries.BLOCK.containsId(id)) {
                  Block block = Registries.BLOCK.get(id);
                  if (block != Blocks.AIR && entry.getValue().isJsonPrimitive()) {
                     try {
                        BlockESP.this.customBlocks.put(block, ColorRGBA.fromHex(entry.getValue().getAsString()));
                     } catch (IllegalArgumentException ignored) {
                     }
                  }
               }
            }
         }
      }
   };
   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (mc.world == null || mc.player == null) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      Camera camera = mc.gameRenderer.getCamera();
      Vec3d cameraPos = camera.getPos();
      BlockPos playerPos = mc.player.getBlockPos();
      int r = (int)this.radius.getCurrentValue();
      RenderSystem.enableBlend();
      if (this.throughWalls.isEnabled()) {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.disableCull();
      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      if (this.fill.isSelected()) {
         BufferBuilder quadsBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         this.renderBlocks(matrices, quadsBuffer, playerPos, cameraPos, r, true);
         RenderUtility.buildBuffer(quadsBuffer);
      }

      if (this.outline.isSelected() || this.diagonals.isSelected()) {
         RenderSystem.lineWidth(this.lineWidth.getCurrentValue());
         BufferBuilder linesBuffer = RenderSystem.renderThreadTesselator().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
         this.renderBlocks(matrices, linesBuffer, playerPos, cameraPos, r, false);
         RenderUtility.buildBuffer(linesBuffer);
         RenderSystem.lineWidth(1.0F);
      }

      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
   };

   private void renderBlocks(MatrixStack matrices, BufferBuilder buffer, BlockPos playerPos, Vec3d cameraPos, int radius, boolean filled) {
      BlockPos.Mutable pos = new BlockPos.Mutable();
      for (int x = playerPos.getX() - radius; x <= playerPos.getX() + radius; x++) {
         for (int y = playerPos.getY() - radius; y <= playerPos.getY() + radius; y++) {
            for (int z = playerPos.getZ() - radius; z <= playerPos.getZ() + radius; z++) {
               pos.set(x, y, z);
               BlockState state = mc.world.getBlockState(pos);
               ColorRGBA color = this.getBlockColor(state.getBlock());
               if (color != null) {
                  Box box = new Box(pos).expand(this.expand.getCurrentValue()).offset(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());
                  if (filled) {
                     Draw3DUtility.renderFilledBox(matrices, buffer, box, color.withAlpha(this.fillAlpha.getCurrentValue()));
                  } else {
                     ColorRGBA lineColor = color.withAlpha(this.outlineAlpha.getCurrentValue());
                     if (this.outline.isSelected()) {
                        Draw3DUtility.renderOutlinedBox(matrices, buffer, box, lineColor);
                     }

                     if (this.diagonals.isSelected()) {
                        Draw3DUtility.renderBoxInternalDiagonals(matrices, buffer, box, lineColor);
                     }
                  }
               }
            }
         }
      }
   }

   public boolean addCustomBlock(String blockId, ColorRGBA color) {
      Identifier id = Identifier.tryParse(blockId.contains(":") ? blockId : "minecraft:" + blockId);
      if (id == null) {
         return false;
      }

      Block block = Registries.BLOCK.get(id);
      if (block == Blocks.AIR) {
         return false;
      }

      this.customBlocks.put(block, color);
      return true;
   }

   public boolean removeCustomBlock(String blockId) {
      Identifier id = Identifier.tryParse(blockId.contains(":") ? blockId : "minecraft:" + blockId);
      if (id == null) {
         return false;
      }

      return this.customBlocks.remove(Registries.BLOCK.get(id)) != null;
   }

   public Map<String, ColorRGBA> getCustomBlocks() {
      Map<String, ColorRGBA> result = new HashMap<>();
      for (Entry<Block, ColorRGBA> entry : this.customBlocks.entrySet()) {
         result.put(Registries.BLOCK.getId(entry.getKey()).toString(), entry.getValue());
      }

      return result;
   }

   private ColorRGBA getBlockColor(Block block) {
      ColorRGBA customColor = this.customBlocks.get(block);
      if (customColor != null) {
         return customColor;
      } else if (this.chests.isSelected() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST)) {
         return new ColorRGBA(139.0F, 69.0F, 19.0F);
      } else if (this.furnaces.isSelected() && (block == Blocks.FURNACE || block == Blocks.BLAST_FURNACE || block == Blocks.SMOKER)) {
         return new ColorRGBA(128.0F, 128.0F, 128.0F);
      } else if (this.spawners.isSelected() && block == Blocks.SPAWNER) {
         return new ColorRGBA(255.0F, 0.0F, 255.0F);
      } else if (this.brewingStands.isSelected() && block == Blocks.BREWING_STAND) {
         return new ColorRGBA(0.0F, 191.0F, 255.0F);
      } else if (this.enderChests.isSelected() && block == Blocks.ENDER_CHEST) {
         return new ColorRGBA(75.0F, 0.0F, 130.0F);
      } else {
         return this.detectorRails.isSelected() && block == Blocks.DETECTOR_RAIL ? new ColorRGBA(255.0F, 165.0F, 0.0F) : null;
      }
   }
}
