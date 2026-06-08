package moscow.xaclient.systems.modules.modules.player;

import moscow.xaclient.mixin.accessors.ClientPlayerInteractionManagerAccessor;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;

@ModuleInfo(name = "Fast Break", category = ModuleCategory.PLAYER, desc = "modules.descriptions.fast_break")
public class FastBreak extends BaseModule {
   @Override
   public void tick() {
      if (mc.interactionManager != null) {
         ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).setBlockBreakingCooldown(0);
      }
   }
}
