package moscow.xaclient.systems.modules.modules.combat;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.ai.aura.v2.AuraAIV2Manager;
import moscow.xaclient.systems.ai.aura.v2.AuraAIV2Profile;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ButtonSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.systems.target.TargetComparators;
import moscow.xaclient.systems.target.TargetSettings;
import moscow.xaclient.utility.game.CombatUtility;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.game.prediction.ElytraPredictionSystem;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.rotations.MoveCorrection;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationHandler;
import moscow.xaclient.utility.rotations.RotationMath;
import moscow.xaclient.utility.rotations.RotationPriority;
import moscow.xaclient.utility.rotations.RotationState;
import moscow.xaclient.utility.rotations.RotationTask;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "AI Aura V2", category = ModuleCategory.COMBAT, desc = "Adaptive aura with a separate v2 training profile")
public class AIAuraV2 extends BaseModule {
   private final AuraAIV2Manager aiManager = new AuraAIV2Manager();
   private final SliderSetting attackDistance = new SliderSetting(this, "Attack Distance").min(1.0F).max(6.0F).step(0.1F).currentValue(3.0F);
   private final SliderSetting aimDistance = new SliderSetting(this, "Aim Distance").min(1.0F).max(8.0F).step(0.1F).currentValue(4.0F);
   private final SliderSetting influence = new SliderSetting(this, "AI Influence").min(0.0F).max(100.0F).step(1.0F).currentValue(88.0F).suffix("%");
   private final SliderSetting missChance = new SliderSetting(this, "Miss Chance").min(0.0F).max(100.0F).step(1.0F).currentValue(0.0F).suffix("%");
   private final SliderSetting maxYawSpeed = new SliderSetting(this, "Max Yaw Speed").min(8.0F).max(180.0F).step(1.0F).currentValue(120.0F);
   private final SliderSetting maxPitchSpeed = new SliderSetting(this, "Max Pitch Speed").min(4.0F).max(90.0F).step(1.0F).currentValue(55.0F);
   private final SliderSetting targetHold = new SliderSetting(this, "Target Hold").min(0.0F).max(1000.0F).step(25.0F).currentValue(250.0F).suffix("ms");
   private final SelectSetting targets = new SelectSetting(this, "targets").min(1);
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "animals");
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "mobs");
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "invisibles");
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "nakedPlayers").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "friends");
   private final ModeSetting sortingMode = new ModeSetting(this, "Sorting");
   private final ModeSetting.Value distanceSorting = new ModeSetting.Value(this.sortingMode, "Distance").select();
   private final ModeSetting.Value healthSorting = new ModeSetting.Value(this.sortingMode, "Health");
   private final ModeSetting.Value fovSorting = new ModeSetting.Value(this.sortingMode, "FOV");
   private final ModeSetting moveCorrectionMode = new ModeSetting(this, "Move Correction");
   private final ModeSetting.Value silentMoveCorrection = new ModeSetting.Value(this.moveCorrectionMode, "Silent").select();
   private final ModeSetting.Value directMoveCorrection = new ModeSetting.Value(this.moveCorrectionMode, "Direct");
   private final ModeSetting.Value noMoveCorrection = new ModeSetting.Value(this.moveCorrectionMode, "None");
   private final BooleanSetting onlyCriticals = new BooleanSetting(this, "Only Criticals");
   private final BooleanSetting rayTrace = new BooleanSetting(this, "RayTrace").enable();
   private final BooleanSetting throughWalls = new BooleanSetting(this, "Walls").enable();
   private final BooleanSetting onlyWeapon = new BooleanSetting(this, "Only Weapon");
   private final BooleanSetting autoDisableOldAura = new BooleanSetting(this, "Disable Old Aura").enable();
   private final ButtonSetting trainAndLoad = new ButtonSetting(this, "Train & Load V2").action(() -> this.aiManager.startTraining(true));
   private final ButtonSetting cancelTraining = new ButtonSetting(this, "Cancel Training", () -> !this.aiManager.isTraining()).action(this.aiManager::cancelTraining);
   private final ButtonSetting loadProfile = new ButtonSetting(this, "Load V2 Profile").action(this.aiManager::loadProfile);
   private final Timer attackTimer = new Timer();
   private final Timer targetSwitchTimer = new Timer();
   private LivingEntity currentTarget;
   private LivingEntity lastAimTarget;
   private Vec3d aimOffset = new Vec3d(0.5, 0.62, 0.5);
   private long nextAimPointRefreshMs;
   private long nextAttackDelay = -1L;
   private float yawVelocity;
   private float pitchVelocity;
   private boolean shield;

   private final EventListener<ClientPlayerTickEvent> onPlayerTick = event -> {
      if (mc.player == null || mc.world == null || mc.interactionManager == null || mc.getNetworkHandler() == null) {
         this.resetRuntime();
         return;
      }

      if (this.onlyWeapon.isEnabled() && !EntityUtility.isHoldingWeapon()) {
         this.resetRotationState();
         return;
      }

      this.updateTarget();
      if (this.currentTarget == null) {
         this.resetRotationState();
         return;
      }

      Rotation rotation = this.calculateRotation(this.currentTarget);
      this.applyRotation(rotation);
      if (this.shouldAttack(this.currentTarget, rotation)) {
         this.attack(this.currentTarget);
      }
   };

   private void updateTarget() {
      if (this.isTargetInvalid(this.currentTarget) || this.targetSwitchTimer.finished((long)this.targetHold.getCurrentValue())) {
         TargetSettings.Builder builder = new TargetSettings.Builder()
            .targetPlayers(this.players.isSelected())
            .targetAnimals(this.animals.isSelected())
            .targetMobs(this.mobs.isSelected())
            .targetInvisibles(this.invisibles.isSelected())
            .targetNakedPlayers(this.nakedPlayers.isSelected())
            .targetFriends(this.friends.isSelected())
            .requiredRange(this.aimDistance.getCurrentValue());
         if (this.sortingMode.is(this.healthSorting)) {
            builder.sortBy(TargetComparators.HEALTH);
         } else if (this.sortingMode.is(this.fovSorting)) {
            builder.sortBy(TargetComparators.FOV);
         } else {
            builder.sortBy(TargetComparators.DISTANCE);
         }

         XaClient.getInstance().getTargetManager().update(builder.build());
         this.currentTarget = XaClient.getInstance().getTargetManager().getCurrentTarget() instanceof LivingEntity living ? living : null;
         this.targetSwitchTimer.reset();
      }
   }

   private boolean isTargetInvalid(LivingEntity target) {
      return target == null
         || !target.isAlive()
         || target.isRemoved()
         || !mc.world.hasEntity(target)
         || mc.player.squaredDistanceTo(RotationMath.getNearestPoint(target)) > this.aimDistance.getCurrentValue() * this.aimDistance.getCurrentValue();
   }

   private Rotation calculateRotation(LivingEntity target) {
      AuraAIV2Profile profile = this.aiManager.getActiveProfile();
      Vec3d aimPoint = this.getAimPoint(target, profile);
      Rotation targetRotation = RotationMath.getRotationTo(aimPoint);
      RotationHandler handler = XaClient.getInstance().getRotationHandler();
      Rotation currentRotation = handler.isIdling() ? new Rotation(mc.player.getYaw(), mc.player.getPitch()) : handler.getCurrentRotation();
      float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentRotation.getYaw());
      float pitchDelta = targetRotation.getPitch() - currentRotation.getPitch();
      double distance = mc.player.getEyePos().distanceTo(aimPoint);
      int distanceBucket = profile.distanceBucket(distance);
      int angleBucket = profile.angleBucket(Math.abs(yawDelta) + Math.abs(pitchDelta));
      float aiInfluence = MathHelper.clamp(this.influence.getCurrentValue() / 100.0F, 0.0F, 1.0F);
      float yawLimit = this.lerp(this.maxYawSpeed.getCurrentValue(), profile.yawSpeed(distanceBucket, angleBucket), aiInfluence);
      float pitchLimit = this.lerp(this.maxPitchSpeed.getCurrentValue(), profile.pitchSpeed(distanceBucket, angleBucket), aiInfluence);
      float yawAccelerationLimit = this.lerp(this.maxYawSpeed.getCurrentValue(), profile.yawAcceleration(distanceBucket, angleBucket), aiInfluence);
      float pitchAccelerationLimit = this.lerp(this.maxPitchSpeed.getCurrentValue(), profile.pitchAcceleration(distanceBucket, angleBucket), aiInfluence);
      float closeScale = distance < 1.35 ? MathHelper.clamp((float)(distance / 1.35), 0.42F, 0.82F) : 1.0F;
      yawLimit *= closeScale;
      pitchLimit *= MathHelper.clamp(closeScale, 0.32F, 1.0F);
      float wantedYawVelocity = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
      float wantedPitchVelocity = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);
      this.yawVelocity = this.approach(this.yawVelocity, wantedYawVelocity, yawAccelerationLimit);
      this.pitchVelocity = this.approach(this.pitchVelocity, wantedPitchVelocity, pitchAccelerationLimit);
      float smooth = this.lerp(0.88F, profile.smoothing(distanceBucket, angleBucket), aiInfluence);
      if (Math.abs(yawDelta) + Math.abs(pitchDelta) < 4.0F) {
         smooth = Math.min(smooth, 0.72F);
      }

      float yawStep = MathHelper.clamp(this.yawVelocity * smooth, -Math.abs(yawDelta), Math.abs(yawDelta));
      float pitchStep = MathHelper.clamp(this.pitchVelocity * smooth, -Math.abs(pitchDelta), Math.abs(pitchDelta));
      if (Math.signum(yawStep) != Math.signum(yawDelta) && Math.abs(yawDelta) > 0.001F) {
         yawStep = yawDelta;
         this.yawVelocity = yawStep;
      }

      if (Math.signum(pitchStep) != Math.signum(pitchDelta) && Math.abs(pitchDelta) > 0.001F) {
         pitchStep = pitchDelta;
         this.pitchVelocity = pitchStep;
      }

      float jitterScale = MathHelper.clamp((Math.abs(yawDelta) + Math.abs(pitchDelta) - 1.0F) / 18.0F, 0.0F, 1.0F) * aiInfluence;
      float yaw = currentRotation.getYaw() + yawStep + MathUtility.random(-profile.jitterYaw, profile.jitterYaw) * jitterScale;
      float pitch = MathHelper.clamp(currentRotation.getPitch() + pitchStep + MathUtility.random(-profile.jitterPitch, profile.jitterPitch) * jitterScale, -90.0F, 90.0F);
      if (Math.abs(yawDelta) < 0.75F) {
         this.yawVelocity *= 0.35F;
      }

      if (Math.abs(pitchDelta) < 0.75F) {
         this.pitchVelocity *= 0.35F;
      }

      return RotationMath.correctRotation(new Rotation(yaw, pitch));
   }

   private Vec3d getAimPoint(LivingEntity target, AuraAIV2Profile profile) {
      long now = System.currentTimeMillis();
      boolean targetChanged = this.lastAimTarget != target;
      if (targetChanged || now >= this.nextAimPointRefreshMs) {
         if (targetChanged) {
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
         }

         this.aimOffset = new Vec3d(
            MathHelper.clamp(0.5 + MathUtility.random(-profile.aimHorizontalSpread, profile.aimHorizontalSpread), 0.18, 0.82),
            MathHelper.clamp(0.61 + MathUtility.random(-profile.aimVerticalSpread, profile.aimVerticalSpread), 0.34, 0.82),
            MathHelper.clamp(0.5 + MathUtility.random(-profile.aimHorizontalSpread, profile.aimHorizontalSpread), 0.18, 0.82)
         );
         this.lastAimTarget = target;
         this.nextAimPointRefreshMs = now + (long)MathUtility.random(profile.aimPointHoldMs * 0.7F, profile.aimPointHoldMs * 1.25F);
      }

      Box box = target.getBoundingBox();
      if (XaClient.getInstance().getModuleManager().getModule(ElytraTarget.class).isEnabled() && target instanceof PlayerEntity player) {
         Vec3d predicted = ElytraPredictionSystem.predictPlayerPosition(player);
         box = box.offset(predicted.x - target.getX(), predicted.y - target.getY(), predicted.z - target.getZ());
      } else {
         Vec3d velocity = target.getVelocity();
         double distance = mc.player.getEyePos().distanceTo(target.getEyePos());
         double prediction = MathHelper.clamp(distance * 0.11, 0.08, 0.65);
         box = box.offset(velocity.x * prediction, velocity.y * prediction * 0.2, velocity.z * prediction);
      }

      double widthX = box.maxX - box.minX;
      double widthZ = box.maxZ - box.minZ;
      double height = box.maxY - box.minY;
      double x = box.minX + widthX * this.aimOffset.x;
      double y = box.minY + height * this.aimOffset.y;
      double z = box.minZ + widthZ * this.aimOffset.z;
      if (mc.player.getBoundingBox().intersects(target.getBoundingBox().expand(0.18))) {
         Vec3d nearest = RotationMath.getNearestPoint(target);
         x += (nearest.x - x) * 0.68;
         z += (nearest.z - z) * 0.68;
         y = MathHelper.clamp(mc.player.getEyeY() - 0.18, box.minY + height * 0.32, box.minY + height * 0.66);
      }

      return new Vec3d(x, y, z);
   }

   private void applyRotation(Rotation rotation) {
      RotationHandler handler = XaClient.getInstance().getRotationHandler();
      handler.setCurrentTask(new RotationTask(rotation, this.getMoveCorrection(), 180.0F, 180.0F, 180.0F, RotationPriority.TO_TARGET.getPriority()));
      handler.setCurrentRotation(rotation);
      handler.setPrevRotation(rotation);
      handler.setRenderRotation(rotation);
      handler.setState(RotationState.ROTATING);
      handler.getRotationIdle().reset();
      mc.player.setYaw(rotation.getYaw());
      mc.player.setPitch(rotation.getPitch());
   }

   private boolean shouldAttack(LivingEntity target, Rotation rotation) {
      if (target == null || mc.player == null || mc.interactionManager == null) {
         return false;
      }

      if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND) {
         return false;
      }

      if (mc.player.squaredDistanceTo(RotationMath.getNearestPoint(target)) > this.attackDistance.getCurrentValue() * this.attackDistance.getCurrentValue()) {
         return false;
      }

      if (!this.isAttackReady()) {
         return false;
      }

      if (this.rayTrace.isEnabled() && !MathUtility.canTraceWithBlock(this.attackDistance.getCurrentValue(), rotation.getYaw(), rotation.getPitch(), mc.player, target, !this.throughWalls.isEnabled())) {
         return false;
      }

      if (this.onlyCriticals.isEnabled() && !CombatUtility.canPerformCriticalHit(target, true)) {
         return false;
      }

      return true;
   }

   private boolean isAttackReady() {
      AuraAIV2Profile profile = this.aiManager.getActiveProfile();
      if (this.nextAttackDelay < 0L) {
         this.nextAttackDelay = (long)MathHelper.clamp(profile.attackDelayMs * MathUtility.random(0.88, 1.16), 45.0F, 1600.0F);
      }

      return CombatUtility.getMace() != null
         ? this.attackTimer.finished(Math.max(420L, this.nextAttackDelay))
         : mc.player.getAttackCooldownProgress(1.5F) > 0.93F && this.attackTimer.finished(this.nextAttackDelay);
   }

   private void attack(LivingEntity target) {
      this.shield = mc.player.isUsingItem() && mc.player.getActiveItem().getItem().getUseAction(mc.player.getActiveItem()) == UseAction.BLOCK;
      if (this.shield) {
         mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
      }

      if (CombatUtility.shouldBreakShield(target) && CombatUtility.canBreakShield(target)) {
         CombatUtility.tryBreakShield(target);
      }

      var maceSlot = CombatUtility.getMace();
      if (maceSlot != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot.getSlotId()));
      }

      if (MathUtility.random(0.0, 100.0) >= this.missChance.getCurrentValue()) {
         mc.interactionManager.attackEntity(mc.player, target);
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      if (maceSlot != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
      }

      if (this.shield) {
         mc.interactionManager
            .sendSequencedPacket(
               mc.world,
               sequence -> new PlayerInteractItemC2SPacket(
                  mc.player.getActiveHand(),
                  sequence,
                  XaClient.getInstance().getRotationHandler().getCurrentRotation().getYaw(),
                  XaClient.getInstance().getRotationHandler().getCurrentRotation().getPitch()
               )
            );
      }

      this.attackTimer.reset();
      this.nextAttackDelay = -1L;
   }

   private void resetRuntime() {
      this.currentTarget = null;
      XaClient.getInstance().getTargetManager().reset();
      this.resetRotationState();
   }

   private void resetRotationState() {
      this.lastAimTarget = null;
      this.nextAimPointRefreshMs = 0L;
      this.yawVelocity = 0.0F;
      this.pitchVelocity = 0.0F;
      RotationHandler handler = XaClient.getInstance().getRotationHandler();
      handler.setCurrentTask(null);
      if (handler.getCurrentTask() == null) {
         Rotation playerRotation = handler.getPlayerRotation();
         handler.setCurrentRotation(playerRotation);
         handler.setPrevRotation(playerRotation);
         handler.setRenderRotation(playerRotation);
         handler.setState(RotationState.IDLE);
      }
   }

   private MoveCorrection getMoveCorrection() {
      if (this.moveCorrectionMode.is(this.silentMoveCorrection)) {
         return MoveCorrection.SILENT;
      }

      return this.moveCorrectionMode.is(this.directMoveCorrection) ? MoveCorrection.DIRECT : MoveCorrection.NONE;
   }

   private float approach(float current, float target, float maxDelta) {
      return current + MathHelper.clamp(target - current, -maxDelta, maxDelta);
   }

   private float lerp(float from, float to, float delta) {
      return from + (to - from) * delta;
   }

   @Override
   public void onEnable() {
      if (this.autoDisableOldAura.isEnabled()) {
         Aura oldAura = XaClient.getInstance().getModuleManager().getModule(Aura.class);
         if (oldAura.isEnabled()) {
            oldAura.disable();
            MessageUtility.info(Text.of("AI Aura V2: disabled old Aura"));
         }
      }

      this.currentTarget = null;
      this.targetSwitchTimer.reset();
      this.attackTimer.reset();
      this.nextAttackDelay = -1L;
      this.aiManager.resetRuntime();
      super.onEnable();
   }

   @Override
   public void onDisable() {
      this.aiManager.resetRuntime();
      this.resetRuntime();
      super.onDisable();
   }
}
