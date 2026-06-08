package moscow.xaclient.systems.modules.modules.player;

import moscow.xaclient.mixin.minecraft.client.IMinecraftClient;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(name = "Fast Place", category = ModuleCategory.PLAYER, desc = "modules.descriptions.fast_place")
public class FastPlace extends BaseModule {
   private final BooleanSetting blocks = new BooleanSetting(
      this,
      "modules.settings.fast_place.blocks",
      "modules.settings.fast_place.blocks.description"
   ).enable();
   private final BooleanSetting crystals = new BooleanSetting(
      this,
      "modules.settings.fast_place.crystals",
      "modules.settings.fast_place.crystals.description"
   );

   @Override
   public void tick() {
      if (mc.player == null) {
         return;
      }

      if (this.shouldRemoveDelay(mc.player.getMainHandStack()) || this.shouldRemoveDelay(mc.player.getOffHandStack())) {
         ((IMinecraftClient)mc).setUseCooldown(0);
      }
   }

   private boolean shouldRemoveDelay(ItemStack stack) {
      if (this.blocks.isEnabled() && stack.getItem() instanceof BlockItem) {
         return true;
      }

      return this.crystals.isEnabled() && stack.isOf(Items.END_CRYSTAL);
   }
}
