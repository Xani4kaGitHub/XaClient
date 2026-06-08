package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.InputEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import net.minecraft.block.Blocks;

@ModuleInfo(name = "NoDamageMagma", category = ModuleCategory.MOVEMENT, desc = "Позволяет ходить по магме без урона")
public class NoDamageMagma extends BaseModule {
   private static final float VANILLA_SNEAK_MULTIPLIER = 0.3F;
   private final SliderSetting speed = new SliderSetting(this, "Speed", "Movement multiplier on magma")
      .min(VANILLA_SNEAK_MULTIPLIER)
      .max(1.0F)
      .step(0.05F)
      .currentValue(0.55F);

   private final EventListener<InputEvent> onInput = event -> {
      if (this.shouldProtect()) {
         float multiplier = this.speed.getCurrentValue();
         if (event.isSneak()) {
            multiplier /= VANILLA_SNEAK_MULTIPLIER;
         }

         event.setSneak(true);
         event.setSprint(false);
         event.setForward(event.getForward() * multiplier);
         event.setStrafe(event.getStrafe() * multiplier);
      }
   };

   @Override
   public void tick() {
      if (this.shouldProtect()) {
         mc.player.setSprinting(false);
         mc.options.sprintKey.setPressed(false);
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }

   private boolean shouldProtect() {
      return mc.player != null
         && mc.world != null
         && mc.getNetworkHandler() != null
         && mc.world.getBlockState(mc.player.getSteppingPos()).isOf(Blocks.MAGMA_BLOCK);
   }
}
