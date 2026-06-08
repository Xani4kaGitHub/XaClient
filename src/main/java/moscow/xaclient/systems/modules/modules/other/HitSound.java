package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.ai.aura.AuraAITrainingBot;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.PostAttackEvent;
import moscow.xaclient.systems.event.impl.network.ReceivePacketEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.systems.target.TargetSettings;
import moscow.xaclient.utility.game.CombatUtility;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.sounds.CombatSoundLibrary;
import moscow.xaclient.utility.sounds.CombatSoundLibrary.SoundEntry;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

@ModuleInfo(name = "Hit Sound", category = ModuleCategory.VISUALS, desc = "modules.descriptions.hit_sound")
public class HitSound extends BaseModule {
   private static final long CONFIRM_WINDOW_MS = 700L;
   private final ModeSetting sound = new ModeSetting(this, "Sound");
   private final ModeSetting.Value[] soundValues = CombatSoundLibrary.createValues(this.sound, CombatSoundLibrary.HIT_SOUNDS, 3);
   private final ModeSetting trigger = new ModeSetting(this, "Trigger");
   private final ModeSetting.Value anyAttack = new ModeSetting.Value(this.trigger, "Any Attack").select();
   private final ModeSetting.Value confirmedHit = new ModeSetting.Value(this.trigger, "Confirmed Hit");
   private final SliderSetting volume = new SliderSetting(this, "Volume")
      .min(0.0F)
      .max(3.0F)
      .step(0.1F)
      .currentValue(1.5F)
      .suffix(value -> Math.round(value * 100.0F) + "%");
   private final SliderSetting delay = new SliderSetting(this, "Delay").min(0.0F).max(1000.0F).step(25.0F).currentValue(75.0F).suffix("ms");
   private final BooleanSetting onlyCrits = new BooleanSetting(this, "Only Crits");
   private final BooleanSetting onlyWeapon = new BooleanSetting(this, "Only Weapon");
   private final BooleanSetting randomSound = new BooleanSetting(this, "Random Sound");
   private final BooleanSetting ignoreFriends = new BooleanSetting(this, "Ignore Friends").enable();
   private final SelectSetting targets = new SelectSetting(this, "Targets");
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "Players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "Animals").select();
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "Mobs").select();
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "Invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "Naked Players").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "Friends");
   private final Timer timer = new Timer();
   private int pendingEntityId = -1;
   private long pendingAttackMs;

   private final EventListener<PostAttackEvent> onPostAttack = event -> {
      if (mc.player == null || mc.world == null || !(event.getEntity() instanceof LivingEntity target) || !this.isValidTarget(target)) {
         return;
      }

      if (this.onlyWeapon.isEnabled() && !EntityUtility.isHoldingWeapon()) {
         return;
      }

      if (this.onlyCrits.isEnabled() && !CombatUtility.canPerformCriticalHit(target, true)) {
         return;
      }

      if (this.trigger.is(this.anyAttack)) {
         this.play();
      } else {
         this.pendingEntityId = target.getId();
         this.pendingAttackMs = System.currentTimeMillis();
      }
   };
   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (mc.world == null || this.trigger.is(this.anyAttack) || this.pendingEntityId == -1) {
         return;
      }

      if (!(event.getPacket() instanceof EntityStatusS2CPacket packet) || packet.getStatus() != EntityStatuses.PLAY_ATTACK_SOUND) {
         return;
      }

      Entity entity = packet.getEntity(mc.world);
      if (entity == null || entity.getId() != this.pendingEntityId || System.currentTimeMillis() - this.pendingAttackMs > CONFIRM_WINDOW_MS) {
         return;
      }

      this.pendingEntityId = -1;
      this.play();
   };

   private boolean isValidTarget(LivingEntity target) {
      if (AuraAITrainingBot.isTrainingBot(target)) {
         return false;
      }

      if (this.ignoreFriends.isEnabled() && XaClient.getInstance().getFriendManager().isFriend(target.getName().getString())) {
         return false;
      }

      TargetSettings settings = new TargetSettings.Builder()
         .targetPlayers(this.players.isSelected())
         .targetAnimals(this.animals.isSelected())
         .targetMobs(this.mobs.isSelected())
         .targetInvisibles(this.invisibles.isSelected())
         .targetNakedPlayers(this.nakedPlayers.isSelected())
         .targetFriends(this.friends.isSelected())
         .requiredRange(8.0F)
         .build();
      return settings.isEntityValid(target);
   }

   private void play() {
      if (!this.timer.finished((long)this.delay.getCurrentValue())) {
         return;
      }

      SoundEntry entry = this.randomSound.isEnabled()
         ? CombatSoundLibrary.randomPlayable(CombatSoundLibrary.HIT_SOUNDS)
         : CombatSoundLibrary.selected(CombatSoundLibrary.HIT_SOUNDS, this.soundValues);
      CombatSoundLibrary.play(entry, this.volume.getCurrentValue());
      this.timer.reset();
   }
}
