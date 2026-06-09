package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.game.EntityUtility;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(name = "Strafe", category = ModuleCategory.MOVEMENT, desc = "Fast ground movement")
public class Strafe extends BaseModule {
   private final ModeSetting mode = new ModeSetting(this, "Mode");
   private final ModeSetting.Value metaHvh = new ModeSetting.Value(this.mode, "MetaHvH").select();
   private final BooleanSetting autoSpeed = new BooleanSetting(this, "Auto Speed").enable();
   private final BooleanSetting jumpBoost = new BooleanSetting(this, "Jump Boost").enable();
   private final SliderSetting speed = new SliderSetting(this, "Speed", () -> this.autoSpeed.isEnabled())
      .min(0.05F)
      .max(1.2F)
      .step(0.01F)
      .currentValue(0.19F);
   private final SliderSetting jumpBoostAmount = new SliderSetting(this, "Jump Boost Amount", () -> !this.jumpBoost.isEnabled())
      .min(0.0F)
      .max(0.3F)
      .step(0.01F)
      .currentValue(0.1F);

   @Override
   public void tick() {
      if (mc.player == null || mc.world == null || !this.mode.is(this.metaHvh)) {
         return;
      }

      if (mc.player.isGliding() || mc.player.isInFluid() || !EntityUtility.isPlayerMoving()) {
         return;
      }

      float motion = this.autoSpeed.isEnabled() ? this.getMetaHvhSpeed() : this.speed.getCurrentValue();
      if (this.jumpBoost.isEnabled() && mc.options.jumpKey.isPressed()) {
         motion += this.jumpBoostAmount.getCurrentValue();
      }

      EntityUtility.setSpeed(motion);
   }

   private float getMetaHvhSpeed() {
      StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
      if (speedEffect == null) {
         return 0.19F;
      }

      int amplifier = speedEffect.getAmplifier();
      return switch (amplifier) {
         case 0 -> 0.25F;
         case 1 -> 0.37F;
         case 2 -> 0.46F;
         case 3 -> 0.7F;
         default -> 0.75F + (amplifier - 3) * 0.05F;
      };
   }
}
