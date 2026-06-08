package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.ai.aura.AuraAITrainingBot;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.EntityDeathEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.sounds.CombatSoundLibrary;
import moscow.xaclient.utility.sounds.CombatSoundLibrary.SoundEntry;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(name = "Kill Sound", category = ModuleCategory.VISUALS, desc = "modules.descriptions.kill_sound")
public class KillSound extends BaseModule {
   private final ModeSetting sound = new ModeSetting(this, "Sound");
   private final ModeSetting.Value[] soundValues = CombatSoundLibrary.createValues(this.sound, CombatSoundLibrary.KILL_SOUNDS, 0);
   private final SliderSetting volume = new SliderSetting(this, "Volume")
      .min(0.0F)
      .max(3.0F)
      .step(0.1F)
      .currentValue(1.75F)
      .suffix(value -> Math.round(value * 100.0F) + "%");
   private final SliderSetting delay = new SliderSetting(this, "Delay").min(0.0F).max(2000.0F).step(50.0F).currentValue(100.0F).suffix("ms");
   private final SliderSetting maxDistance = new SliderSetting(this, "Max Distance").min(0.0F).max(128.0F).step(1.0F).currentValue(32.0F);
   private final BooleanSetting randomSound = new BooleanSetting(this, "Random Sound");
   private final BooleanSetting playersOnly = new BooleanSetting(this, "Players Only").enable();
   private final BooleanSetting ignoreFriends = new BooleanSetting(this, "Ignore Friends").enable();
   private final Timer timer = new Timer();

   private final EventListener<EntityDeathEvent> onEntityDeath = event -> {
      if (mc.player == null || mc.world == null) {
         return;
      }

      LivingEntity entity = event.getEntity();
      if (entity == mc.player || AuraAITrainingBot.isTrainingBot(entity)) {
         return;
      }

      if (this.playersOnly.isEnabled() && !(entity instanceof PlayerEntity)) {
         return;
      }

      if (this.ignoreFriends.isEnabled() && XaClient.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
         return;
      }

      if (this.maxDistance.getCurrentValue() > 0.0F && entity.distanceTo(mc.player) > this.maxDistance.getCurrentValue()) {
         return;
      }

      LivingEntity killer = event.getKillerEntity();
      Entity damageSource = event.getSource().getSource();
      Entity damageAttacker = event.getSource().getAttacker();
      if (killer != mc.player && entity.getAttacker() != mc.player && damageSource != mc.player && damageAttacker != mc.player) {
         return;
      }

      this.play();
   };

   private void play() {
      if (!this.timer.finished((long)this.delay.getCurrentValue())) {
         return;
      }

      SoundEntry entry = this.randomSound.isEnabled()
         ? CombatSoundLibrary.randomPlayable(CombatSoundLibrary.KILL_SOUNDS)
         : CombatSoundLibrary.selected(CombatSoundLibrary.KILL_SOUNDS, this.soundValues);
      CombatSoundLibrary.play(entry, this.volume.getCurrentValue());
      this.timer.reset();
   }
}
