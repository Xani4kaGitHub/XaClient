package moscow.xaclient.systems.modules.modules.combat;

import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.RangeSetting;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.systems.target.TargetSettings;
import moscow.xaclient.utility.game.CombatUtility;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

@ModuleInfo(name = "Trigger Bot", category = ModuleCategory.COMBAT, desc = "modules.descriptions.trigger_bot")
public class TriggerBot extends BaseModule {
   private final Timer timer = new Timer();
   private long nextAttackDelay = 120L;
   private final SliderSetting attackRange = new SliderSetting(this, "Attack Range")
      .min(1.0F)
      .max(6.0F)
      .step(0.1F)
      .currentValue(3.0F);
   private final SliderSetting cooldown = new SliderSetting(this, "Cooldown")
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(93.0F)
      .suffix("%");
   private final RangeSetting cps = new RangeSetting(this, "CPS")
      .min(1.0F)
      .max(20.0F)
      .step(0.5F)
      .firstValue(7.0F)
      .secondValue(10.0F);
   private final BooleanSetting onlyWeapon = new BooleanSetting(this, "Only Weapon").enable();
   private final BooleanSetting pauseOnUse = new BooleanSetting(this, "Pause On Use").enable();
   private final SelectSetting targets = new SelectSetting(this, "targets");
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "animals").select();
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "mobs").select();
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "nakedPlayers").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "friends");
   private final SelectSetting.Value criticalPlayers = new SelectSetting.Value(this.targets, "criticalPlayers").select();

   @Override
   public void tick() {
      if (mc.player != null && mc.interactionManager != null) {
         TargetSettings settings = new TargetSettings.Builder()
            .targetPlayers(this.players.isSelected())
            .targetAnimals(this.animals.isSelected())
            .targetMobs(this.mobs.isSelected())
            .targetInvisibles(this.invisibles.isSelected())
            .targetNakedPlayers(this.nakedPlayers.isSelected())
            .targetFriends(this.friends.isSelected())
            .requiredRange(this.attackRange.getCurrentValue())
            .build();
         if (mc.targetedEntity instanceof LivingEntity livingEntity && settings.isEntityValid(livingEntity) && this.shouldAttack(livingEntity)) {
            mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.nextAttackDelay = this.randomAttackDelay();
            this.timer.reset();
         }

         super.tick();
      }
   }

   private boolean shouldAttack(LivingEntity entity) {
      if (mc.player == null) {
         return false;
      } else if (!this.timer.finished(this.nextAttackDelay)) {
         return false;
      } else if (mc.player.getAttackCooldownProgress(0.5F) * 100.0F < this.cooldown.getCurrentValue()) {
         return false;
      } else if (entity.distanceTo(mc.player) > this.attackRange.getCurrentValue()) {
         return false;
      } else if (this.onlyWeapon.isEnabled() && !EntityUtility.isHoldingWeapon()) {
         return false;
      } else if (this.pauseOnUse.isEnabled() && mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) {
         return false;
      }

      return !this.shouldRequireCritical(entity) || this.canCritical(entity);
   }

   private boolean shouldRequireCritical(LivingEntity entity) {
      return this.criticalPlayers.isSelected() && entity instanceof PlayerEntity;
   }

   private boolean canCritical(LivingEntity entity) {
      return CombatUtility.canPerformCriticalHit(entity, false);
   }

   private long randomAttackDelay() {
      float minCps = Math.min(this.cps.getFirstValue(), this.cps.getSecondValue());
      float maxCps = Math.max(this.cps.getFirstValue(), this.cps.getSecondValue());
      float cps = Math.max(1.0F, MathUtility.random(minCps, maxCps));
      return (long)(1000.0F / cps);
   }

   @Override
   public void onEnable() {
      this.nextAttackDelay = this.randomAttackDelay();
      this.timer.reset();
      super.onEnable();
   }
}
