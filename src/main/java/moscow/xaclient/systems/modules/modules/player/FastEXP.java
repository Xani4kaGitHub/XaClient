package moscow.xaclient.systems.modules.modules.player;

import moscow.xaclient.mixin.minecraft.client.IMinecraftClient;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(name = "Fast EXP", category = ModuleCategory.PLAYER, desc = "modules.descriptions.fast_exp")
public class FastEXP extends BaseModule {
   @Override
   public void tick() {
      if (mc.player == null) {
         return;
      }

      if (this.isExperienceBottle(mc.player.getMainHandStack()) || this.isExperienceBottle(mc.player.getOffHandStack())) {
         ((IMinecraftClient)mc).setUseCooldown(0);
      }
   }

   private boolean isExperienceBottle(ItemStack stack) {
      return stack.isOf(Items.EXPERIENCE_BOTTLE);
   }
}
