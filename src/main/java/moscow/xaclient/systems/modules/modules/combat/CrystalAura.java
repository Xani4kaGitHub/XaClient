package moscow.xaclient.systems.modules.modules.combat;

import com.mojang.blaze3d.systems.RenderSystem;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ColorSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.systems.target.TargetSettings;
import moscow.xaclient.systems.target.TargetComparators;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.CrystalUtility;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.inventory.group.SlotGroup;
import moscow.xaclient.utility.inventory.group.SlotGroups;
import moscow.xaclient.utility.inventory.slots.HotbarSlot;
import moscow.xaclient.utility.render.Draw3DUtility;
import moscow.xaclient.utility.render.RenderUtility;
import moscow.xaclient.utility.rotations.MoveCorrection;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationPriority;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Crystal Aura", desc = "modules.descriptions.crystal_aura", category = ModuleCategory.COMBAT)
public class CrystalAura extends BaseModule {
   private final SliderSetting targetRange = new SliderSetting(this, "modules.settings.crystal_aura.target_range")
      .min(1.0F).max(12.0F).step(0.5F).currentValue(8.0F).suffix("modules.settings.suffix.blocks");
   private final SliderSetting placeRange = new SliderSetting(this, "modules.settings.crystal_aura.place_range")
      .min(1.0F).max(6.0F).step(0.5F).currentValue(4.5F).suffix("modules.settings.suffix.blocks");
   private final SliderSetting breakRange = new SliderSetting(this, "modules.settings.crystal_aura.break_range")
      .min(1.0F).max(6.0F).step(0.5F).currentValue(4.5F).suffix("modules.settings.suffix.blocks");
   private final SliderSetting minDamage = new SliderSetting(this, "modules.settings.crystal_aura.min_damage")
      .min(1.0F).max(20.0F).step(0.5F).currentValue(6.0F);
   private final SliderSetting maxSelfDamage = new SliderSetting(this, "modules.settings.crystal_aura.max_self_damage")
      .min(0.0F).max(20.0F).step(0.5F).currentValue(8.0F);
   private final SliderSetting placeDelay = new SliderSetting(this, "modules.settings.crystal_aura.place_delay")
      .min(0.0F).max(1000.0F).step(50.0F).currentValue(100.0F).suffix("ms");
   private final SliderSetting breakDelay = new SliderSetting(this, "modules.settings.crystal_aura.break_delay")
      .min(0.0F).max(1000.0F).step(50.0F).currentValue(50.0F).suffix("ms");
   private final BooleanSetting autoPlace = new BooleanSetting(this, "modules.settings.crystal_aura.auto_place").enable();
   private final BooleanSetting autoBreak = new BooleanSetting(this, "modules.settings.crystal_aura.auto_break").enable();
   private final BooleanSetting selfSave = new BooleanSetting(this, "modules.settings.crystal_aura.self_save").enable();
   private final BooleanSetting throughWalls = new BooleanSetting(this, "modules.settings.crystal_aura.through_walls");
   private final BooleanSetting rotate = new BooleanSetting(this, "modules.settings.crystal_aura.rotate").enable();
   private final BooleanSetting smooth = new BooleanSetting(this, "modules.settings.crystal_aura.smooth", () -> !this.rotate.isEnabled());
   private final SliderSetting smoothFactor = new SliderSetting(this, "modules.settings.crystal_aura.smooth_factor", () -> !this.rotate.isEnabled() || !this.smooth.isEnabled())
      .min(0.05F).max(1.0F).step(0.05F).currentValue(0.35F);
   private final BooleanSetting render = new BooleanSetting(this, "modules.settings.crystal_aura.render").enable();
   private final ColorSetting color = new ColorSetting(this, "color").color(Colors.ACCENT);

   private final Timer placeTimer = new Timer();
   private final Timer breakTimer = new Timer();
   private BlockPos renderPos;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      this.renderPos = null;
      if (!EntityUtility.isInGame()) {
         return;
      }

      PlayerEntity target = this.findTarget();
      if (target == null) {
         return;
      }

      if (this.autoBreak.isEnabled()) {
         this.doBreak(target);
      }

      if (this.autoPlace.isEnabled()) {
         this.doPlace(target);
      }
   };

   private PlayerEntity findTarget() {
      TargetSettings settings = new TargetSettings.Builder()
         .targetPlayers(true)
         .targetNakedPlayers(true)
         .requiredRange(this.targetRange.getCurrentValue())
         .sortBy(TargetComparators.DISTANCE)
         .build();
      LivingEntity current = XaClient.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity living ? living : null;
      if (current == null
         || !current.isAlive()
         || !mc.world.hasEntity(current)
         || mc.player.distanceTo(current) > this.targetRange.getCurrentValue()) {
         XaClient.getInstance().getTargetManager().update(settings);
         current = XaClient.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity living ? living : null;
      }

      return current instanceof PlayerEntity player ? player : null;
   }

   private void doBreak(PlayerEntity target) {
      if (!this.breakTimer.finished((long)this.breakDelay.getCurrentValue())) {
         return;
      }

      double range = this.breakRange.getCurrentValue();
      EndCrystalEntity best = null;
      double bestDist = Double.MAX_VALUE;

      for (Entity entity : mc.world.getEntities()) {
         if (!(entity instanceof EndCrystalEntity crystal)) {
            continue;
         }

         double dist = mc.player.getEyePos().distanceTo(crystal.getPos());
         if (dist > range) {
            continue;
         }

         float self = CrystalUtility.calculateDamage(crystal.getPos(), mc.player, false);
         if (this.selfSave.isEnabled() && self > this.maxSelfDamage.getCurrentValue() && self >= mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0F) {
            continue;
         }

         if (dist < bestDist) {
            bestDist = dist;
            best = crystal;
         }
      }

      if (best != null) {
         if (this.rotate.isEnabled()) {
            this.rotateTo(best.getPos());
         }

         mc.interactionManager.attackEntity(mc.player, best);
         mc.player.swingHand(Hand.MAIN_HAND);
         this.breakTimer.reset();
      }
   }

   private void doPlace(PlayerEntity target) {
      if (!this.placeTimer.finished((long)this.placeDelay.getCurrentValue())) {
         return;
      }

      SlotGroup<HotbarSlot> hotbar = SlotGroups.hotbar();
      HotbarSlot slot = hotbar.findItem(Items.END_CRYSTAL);
      if (slot == null) {
         return;
      }

      BlockPos bestBase = null;
      float bestDamage = 0.0F;
      double range = this.placeRange.getCurrentValue();
      BlockPos playerPos = BlockPos.ofFloored(mc.player.getPos());
      int radius = MathHelper.ceil(range);

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos base = playerPos.add(x, y, z);
               if (!this.canPlaceOn(base)) {
                  continue;
               }

               Vec3d crystalPos = new Vec3d(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
               if (mc.player.getEyePos().distanceTo(crystalPos) > range) {
                  continue;
               }

               if (!this.throughWalls.isEnabled() && CrystalUtility.calculateDamage(crystalPos, mc.player, true) == 0.0F
                  && CrystalUtility.calculateDamage(crystalPos, target, true) == 0.0F) {
                  continue;
               }

               float targetDamage = CrystalUtility.calculateDamage(crystalPos, target, false);
               float selfDamage = CrystalUtility.calculateDamage(crystalPos, mc.player, false);
               if (targetDamage < this.minDamage.getCurrentValue()) {
                  continue;
               }

               if (this.selfSave.isEnabled() && selfDamage > this.maxSelfDamage.getCurrentValue()) {
                  continue;
               }

               float score = targetDamage - selfDamage * 0.5F;
               if (score > bestDamage) {
                  bestDamage = score;
                  bestBase = base;
               }
            }
         }
      }

      if (bestBase != null) {
         if (this.rotate.isEnabled()) {
            this.rotateTo(new Vec3d(bestBase.getX() + 0.5, bestBase.getY() + 1.0, bestBase.getZ() + 0.5));
         }

         int prev = mc.player.getInventory().selectedSlot;
         mc.player.getInventory().selectedSlot = slot.getIdForServer() - 36;
         BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(bestBase), Direction.UP, bestBase, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
         mc.player.swingHand(Hand.MAIN_HAND);
         mc.player.getInventory().selectedSlot = prev;
         this.renderPos = bestBase;
         this.placeTimer.reset();
      }
   }

   private boolean canPlaceOn(BlockPos base) {
      var block = mc.world.getBlockState(base).getBlock();
      if (block != Blocks.OBSIDIAN && block != Blocks.BEDROCK) {
         return false;
      }

      BlockPos above = base.up();
      if (!mc.world.getBlockState(above).isAir()) {
         return false;
      }

      Box box = new Box(above);
      for (Entity entity : mc.world.getEntities()) {
         if ((entity instanceof EndCrystalEntity || entity instanceof LivingEntity) && entity.getBoundingBox().intersects(box)) {
            return false;
         }
      }

      return true;
   }

   private void rotateTo(Vec3d pos) {
      Vec3d eyes = mc.player.getEyePos();
      double diffX = pos.x - eyes.x;
      double diffY = pos.y - eyes.y;
      double diffZ = pos.z - eyes.z;
      double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
      float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
      float pitch = (float)(-Math.toDegrees(Math.atan2(diffY, diffXZ)));
      if (this.smooth.isEnabled()) {
         XaClient.getInstance().getRotationHandler()
            .rotateSmooth(new Rotation(yaw, pitch), MoveCorrection.SILENT, 180.0F, this.smoothFactor.getCurrentValue(), 180.0F, RotationPriority.TO_TARGET);
      } else {
         XaClient.getInstance().getRotationHandler()
            .rotate(new Rotation(yaw, pitch), MoveCorrection.SILENT, 180.0F, 180.0F, 180.0F, RotationPriority.TO_TARGET);
      }
   }

   private final EventListener<Render3DEvent> onRender3D = event -> {
      if (!this.render.isEnabled() || this.renderPos == null) {
         return;
      }

      MatrixStack ms = event.getMatrices();
      ms.push();
      RenderUtility.prepareMatrices(ms);
      BlockPos above = this.renderPos.up();
      Box box = new Box(above).contract(0.1, 0.1, 0.1);
      RenderUtility.setupRender3D(true);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buffer = RenderSystem.renderThreadTesselator().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      Draw3DUtility.renderFilledBox(ms, buffer, box, this.color.getColor().withAlpha(80.0F));
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      RenderUtility.endRender3D();
      ms.pop();
   };

   @Override
   public void tick() {
      super.tick();
   }
}
