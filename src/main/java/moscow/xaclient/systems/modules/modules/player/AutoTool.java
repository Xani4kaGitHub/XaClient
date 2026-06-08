package moscow.xaclient.systems.modules.modules.player;

import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.inventory.EnchantmentUtility;
import moscow.xaclient.utility.inventory.InventoryUtility;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(name = "Auto Tool", category = ModuleCategory.PLAYER, desc = "modules.descriptions.auto_tool")
public class AutoTool extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "modules.settings.auto_tool.mode");
   private final ModeSetting.Value legit = new ModeSetting.Value(this.mode, "modules.settings.auto_tool.mode.legit").select();
   private final ModeSetting.Value packet = new ModeSetting.Value(this.mode, "modules.settings.auto_tool.mode.packet");
   private final BooleanSetting onlyWhenMining = new BooleanSetting(this, "modules.settings.auto_tool.only_when_mining").enable();
   private final BooleanSetting switchBack = new BooleanSetting(this, "modules.settings.auto_tool.switch_back").enable();
   private final BooleanSetting preserveTools = new BooleanSetting(this, "modules.settings.auto_tool.preserve_tools").enable();
   private final SliderSetting minDurability = new SliderSetting(
         this, "modules.settings.auto_tool.min_durability", () -> !this.preserveTools.isEnabled()
      )
      .min(1.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(10.0F);
   private final BooleanSetting requireSuitable = new BooleanSetting(this, "modules.settings.auto_tool.require_suitable");
   private final BooleanSetting ignoreCreative = new BooleanSetting(this, "modules.settings.auto_tool.ignore_creative").enable();
   private final SliderSetting switchBackDelay = new SliderSetting(
         this, "modules.settings.auto_tool.switch_back_delay", () -> !this.switchBack.isEnabled()
      )
      .min(0.0F)
      .max(500.0F)
      .step(25.0F)
      .currentValue(200.0F)
      .suffix("ms");
   private int previousSlot = -1;
   private int packetSlot = -1;
   private long lastSwapTime;

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
         this.resetState();
         return;
      }

      if (this.ignoreCreative.isEnabled() && mc.player.isCreative()) {
         this.restoreSlotIfNeeded();
         return;
      }

      if (!(mc.crosshairTarget instanceof BlockHitResult result) || result.getType() != HitResult.Type.BLOCK) {
         this.restoreSlotIfNeeded();
         return;
      }

      BlockPos pos = result.getBlockPos();
      BlockState state = mc.world.getBlockState(pos);
      if (state.isAir()) {
         this.restoreSlotIfNeeded();
         return;
      }

      boolean mining = mc.options.attackKey.isPressed();
      if (this.onlyWhenMining.isEnabled() && !mining) {
         this.restoreSlotIfNeeded();
         return;
      }

      int bestSlot = this.findBestSlot(state);
      if (bestSlot == -1 || bestSlot == mc.player.getInventory().selectedSlot) {
         if (!mining) {
            this.restoreSlotIfNeeded();
         }

         return;
      }

      this.switchTo(bestSlot);
   }

   @Override
   public void onDisable() {
      this.restoreSlot(true);
   }

   private int findBestSlot(BlockState state) {
      int selectedSlot = mc.player.getInventory().selectedSlot;
      float currentSpeed = this.getMiningScore(mc.player.getInventory().getStack(selectedSlot), state);
      int bestSlot = -1;
      float bestSpeed = currentSpeed;

      for (int slot = 0; slot < 9; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!this.isValidTool(stack, state)) {
            continue;
         }

         float speed = this.getMiningScore(stack, state);
         if (speed > bestSpeed) {
            bestSpeed = speed;
            bestSlot = slot;
         }
      }

      return bestSlot;
   }

   private boolean isValidTool(ItemStack stack, BlockState state) {
      if (stack.isEmpty()) {
         return false;
      }

      if (this.requireSuitable.isEnabled() && !stack.isSuitableFor(state)) {
         return false;
      }

      if (this.preserveTools.isEnabled() && stack.isDamageable()) {
         int durability = stack.getMaxDamage() - stack.getDamage();
         if (durability <= (int)this.minDurability.getCurrentValue()) {
            return false;
         }
      }

      return this.getMiningScore(stack, state) > 1.0F;
   }

   private float getMiningScore(ItemStack stack, BlockState state) {
      if (stack.isEmpty()) {
         return 1.0F;
      }

      float speed = stack.getMiningSpeedMultiplier(state);
      int efficiency = EnchantmentUtility.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
      if (efficiency > 0 && speed > 1.0F) {
         speed += efficiency * efficiency + 1;
      }

      return speed;
   }

   private void switchTo(int slot) {
      if (this.previousSlot == -1) {
         this.previousSlot = mc.player.getInventory().selectedSlot;
      }

      this.lastSwapTime = System.currentTimeMillis();
      if (this.mode.is(this.legit)) {
         InventoryUtility.selectHotbarSlot(slot);
      } else {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
         this.packetSlot = slot;
      }
   }

   private void restoreSlotIfNeeded() {
      if (this.previousSlot == -1 || !this.switchBack.isEnabled()) {
         return;
      }

      if (System.currentTimeMillis() - this.lastSwapTime >= (long)this.switchBackDelay.getCurrentValue()) {
         this.restoreSlot(false);
      }
   }

   private void restoreSlot(boolean force) {
      if (this.previousSlot == -1 || mc.player == null || mc.getNetworkHandler() == null) {
         this.resetState();
         return;
      }

      if (force || this.switchBack.isEnabled()) {
         if (this.mode.is(this.legit)) {
            InventoryUtility.selectHotbarSlot(this.previousSlot);
         } else if (this.packetSlot != -1) {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(this.previousSlot));
         }
      }

      this.resetState();
   }

   private void resetState() {
      this.previousSlot = -1;
      this.packetSlot = -1;
      this.lastSwapTime = 0L;
   }
}
