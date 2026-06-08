package moscow.xaclient.systems.modules.modules.player;

import java.util.Arrays;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.inventory.InventoryUtility;
import moscow.xaclient.utility.inventory.slots.HotbarSlot;
import moscow.xaclient.utility.inventory.slots.InventorySlot;
import moscow.xaclient.utility.rotations.MoveCorrection;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationMath;
import moscow.xaclient.utility.rotations.RotationPriority;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "Scaffold", desc = "modules.descriptions.scaffold", category = ModuleCategory.PLAYER)
public class Scaffold extends BaseModule {
   private static final List<Block> BLACKLIST = Arrays.asList(
      Blocks.CHEST,
      Blocks.ENDER_CHEST,
      Blocks.TRAPPED_CHEST,
      Blocks.SAND,
      Blocks.CRAFTING_TABLE,
      Blocks.FURNACE,
      Blocks.STONE_PRESSURE_PLATE,
      Blocks.OAK_PRESSURE_PLATE,
      Blocks.BIRCH_PRESSURE_PLATE,
      Blocks.SPRUCE_PRESSURE_PLATE,
      Blocks.JUNGLE_PRESSURE_PLATE,
      Blocks.ACACIA_PRESSURE_PLATE,
      Blocks.DARK_OAK_PRESSURE_PLATE,
      Blocks.CRIMSON_PRESSURE_PLATE,
      Blocks.WARPED_PRESSURE_PLATE
   );
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.scaffold.mode");
   private final ModeSetting.Value legit = new ModeSetting.Value(this.mode, "modules.settings.scaffold.mode.legit").select();
   private final ModeSetting.Value normal = new ModeSetting.Value(this.mode, "modules.settings.scaffold.mode.normal");
   private final ModeSetting.Value silent = new ModeSetting.Value(this.mode, "modules.settings.scaffold.mode.silent");
   private final SliderSetting placeDelay = new SliderSetting(this, "modules.settings.scaffold.place_delay")
      .min(0.0F)
      .max(250.0F)
      .step(5.0F)
      .currentValue(50.0F)
      .suffix("ms");
   private final SliderSetting prediction = new SliderSetting(this, "modules.settings.scaffold.prediction")
      .min(0.0F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(1.0F);
   private final BooleanSetting rotate = new BooleanSetting(this, "modules.settings.scaffold.rotate").enable();
   private final SliderSetting rotateThreshold = new SliderSetting(this, "modules.settings.scaffold.rotate_threshold", () -> !this.rotate.isEnabled())
      .min(0.0F)
      .max(45.0F)
      .step(1.0F)
      .currentValue(10.0F);
   private final SliderSetting yawSpeed = new SliderSetting(this, "modules.settings.scaffold.yaw_speed", () -> !this.rotate.isEnabled())
      .min(10.0F)
      .max(180.0F)
      .step(5.0F)
      .currentValue(100.0F);
   private final SliderSetting pitchSpeed = new SliderSetting(this, "modules.settings.scaffold.pitch_speed", () -> !this.rotate.isEnabled())
      .min(10.0F)
      .max(180.0F)
      .step(5.0F)
      .currentValue(100.0F);
   private final BooleanSetting inventoryBlocks = new BooleanSetting(this, "modules.settings.scaffold.inventory_blocks").enable();
   private final BooleanSetting swing = new BooleanSetting(this, "modules.settings.scaffold.swing").enable();
   private final Timer placeTimer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         BlockPos below = this.getPredictedPos();
         if (mc.world.getBlockState(below).isAir()) {
            int slot = this.findBlockSlot();
            if (slot == -1 && this.inventoryBlocks.isEnabled() && !this.mode.is(this.silent)) {
               slot = this.findInventoryBlock();
               if (slot != -1) {
                  InventorySlot inv = InventoryUtility.getInventorySlot(slot);
                  HotbarSlot target = InventoryUtility.getCurrentHotbarSlot();
                  InventoryUtility.moveItem(inv, target);
                  slot = target.getSlotId();
               }
            }

            if (slot == -1) {
               return;
            }

            int previousSlot = mc.player.getInventory().selectedSlot;
            if (!this.selectSlot(slot)) {
               return;
            }

            if (this.placeTimer.finished((long)this.placeDelay.getCurrentValue())) {
               BlockHitResult hit = this.findHit(below);
               if (hit == null) {
                  this.restoreSlot(previousSlot);
                  return;
               }

               Vec3d hitVec = hit.getPos();
               Rotation rotation = RotationMath.getRotationTo(hitVec);
               float yawDiff = Math.abs(rotation.getYaw() - mc.player.getYaw());
               float pitchDiff = Math.abs(rotation.getPitch() - mc.player.getPitch());
               if (this.rotate.isEnabled() && (yawDiff > this.rotateThreshold.getCurrentValue() || pitchDiff > this.rotateThreshold.getCurrentValue())) {
                  XaClient.getInstance()
                     .getRotationHandler()
                     .rotate(rotation, MoveCorrection.DIRECT, this.yawSpeed.getCurrentValue(), this.pitchSpeed.getCurrentValue(), 100.0F, RotationPriority.USE_ITEM);
               }

               ActionResult result = mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
               if (result.isAccepted() && XaClient.getInstance().getRotationHandler().isIdling()) {
                  if (this.swing.isEnabled()) {
                     mc.player.swingHand(Hand.MAIN_HAND);
                  }

                  this.placeTimer.reset();
               }

               this.restoreSlot(previousSlot);
            }
         }
      }
   };

   private int findInventoryBlock() {
      for (int i = 0; i < 27; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i + 9);
         if (stack.getCount() > 0 && stack.getItem() instanceof BlockItem blockItem && !BLACKLIST.contains(blockItem.getBlock())) {
            return i;
         }
      }

      return -1;
   }

   private BlockPos getPredictedPos() {
      Vec3d vel = mc.player.getVelocity();
      int dx = (int)Math.round(vel.x * this.prediction.getCurrentValue());
      int dz = (int)Math.round(vel.z * this.prediction.getCurrentValue());
      BlockPos pos = mc.player.getBlockPos().add(dx, 0, dz);
      return pos.down();
   }

   private int findBlockSlot() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getCount() > 0 && stack.getItem() instanceof BlockItem blockItem && !BLACKLIST.contains(blockItem.getBlock())) {
            return i;
         }
      }

      return -1;
   }

   private BlockHitResult findHit(BlockPos target) {
      Direction[] faces = new Direction[]{Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

      for (Direction face : faces) {
         BlockPos neighbour = target.offset(face);
         if (!mc.world.getBlockState(neighbour).isAir()) {
            Vec3d hitVec = Vec3d.ofCenter(neighbour).add(Vec3d.of(face.getVector()).multiply(0.5));
            return new BlockHitResult(hitVec, face.getOpposite(), neighbour, false);
         }
      }

      return null;
   }

   private boolean selectSlot(int slot) {
      if (this.mode.is(this.silent)) {
         if (mc.getNetworkHandler() == null) {
            return false;
         }

         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         return true;
      }

      if (mc.player.getInventory().selectedSlot != slot) {
         if (this.mode.is(this.legit)) {
            InventoryUtility.selectHotbarSlot(slot);
         } else {
            mc.player.getInventory().selectedSlot = slot;
         }
      }

      return true;
   }

   private void restoreSlot(int previousSlot) {
      if (this.mode.is(this.silent) && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(previousSlot));
      }
   }
}
