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
   private final SliderSetting aimSpeed = new SliderSetting(this, "Aim Speed").min(0.5F).max(2.5F).step(0.05F).currentValue(1.35F).suffix("x");
   private final SliderSetting maxYawSpeed = new SliderSetting(this, "Max Yaw Speed").min(8.0F).max(260.0F).step(1.0F).currentValue(165.0F);
   private final SliderSetting maxPitchSpeed = new SliderSetting(this, "Max Pitch Speed").min(4.0F).max(180.0F).step(1.0F).currentValue(95.0F);
   private final SliderSetting targetHold = new SliderSetting(this, "Target Hold").min(0.0F).max(1000.0F).step(25.0F).currentValue(250.0F).suffix("ms");
   private final SliderSetting criticalChance = new SliderSetting(this, "Critical Chance")
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(75.0F)
      .suffix("%");
   private final BooleanSetting microMovement = new BooleanSetting(this, "Micro Movement");
   private final SliderSetting microMovementAmount = new SliderSetting(this, "Micro Amount", () -> !this.microMovement.isEnabled())
      .min(0.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(35.0F)
      .suffix("%");
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
   private boolean criticalDecisionReady;
   private boolean requireCriticalForNextAttack;
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
            .requiredRange(this.aimDistance.getCurrentValue())
            .filter(entity -> entity instanceof LivingEntity living && this.canAimAt(living));
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
         || !this.canAimAt(target)
         || mc.player.squaredDistanceTo(RotationMath.getNearestPoint(target)) > this.aimDistance.getCurrentValue() * this.aimDistance.getCurrentValue();
   }

   private boolean canAimAt(LivingEntity target) {
      if (this.throughWalls.isEnabled()) {
         return true;
      }

      if (mc.player == null || mc.world == null || target == null) {
         return false;
      }

      Vec3d eyePos = mc.player.getEyePos();
      Box box = target.getBoundingBox();
      return this.hasClearPath(eyePos, RotationMath.getNearestPoint(target))
         || this.hasClearPath(eyePos, target.getEyePos())
         || this.hasClearPath(eyePos, new Vec3d(box.getCenter().x, box.minY + (box.maxY - box.minY) * 0.55, box.getCenter().z));
   }

   private boolean hasClearPath(Vec3d start, Vec3d end) {
      if (start.squaredDistanceTo(end) <= 0.0001) {
         return true;
      }

      return mc.world.raycast(new RaycastContext(start, end, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType() != Type.BLOCK;
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
      float speedScale = this.aimSpeed.getCurrentValue();
      float yawLimit = this.lerp(this.maxYawSpeed.getCurrentValue(), profile.yawSpeed(distanceBucket, angleBucket), aiInfluence) * speedScale;
      float pitchLimit = this.lerp(this.maxPitchSpeed.getCurrentValue(), profile.pitchSpeed(distanceBucket, angleBucket), aiInfluence) * speedScale;
      float accelerationScale = 0.85F + speedScale * 0.45F;
      float yawAccelerationLimit = this.lerp(this.maxYawSpeed.getCurrentValue(), profile.yawAcceleration(distanceBucket, angleBucket), aiInfluence) * accelerationScale;
      float pitchAccelerationLimit = this.lerp(this.maxPitchSpeed.getCurrentValue(), profile.pitchAcceleration(distanceBucket, angleBucket), aiInfluence) * accelerationScale;
      float closeBoost = distance < 1.45 ? MathHelper.clamp((float)((1.45 - distance) / 1.45), 0.0F, 1.0F) : 0.0F;
      yawLimit *= 1.0F + closeBoost * 0.35F;
      pitchLimit *= 1.0F + closeBoost * 0.12F;
      yawAccelerationLimit *= 1.0F + closeBoost * 0.35F;
      pitchAccelerationLimit *= 1.0F + closeBoost * 0.18F;
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

      float microScale = this.getMicroMovementScale();
      float jitterScale = MathHelper.clamp((Math.abs(yawDelta) + Math.abs(pitchDelta) - 1.0F) / 18.0F, 0.0F, 1.0F) * aiInfluence * microScale;
      float yaw = currentRotation.getYaw() + yawStep + MathUtility.random(-profile.jitterYaw, profile.jitterYaw) * jitterScale;
      float pitch = MathHelper.clamp(currentRotation.getPitch() + pitchStep + MathUtility.random(-profile.jitterPitch, profile.jitterPitch) * jitterScale, -90.0F, 90.0F);
      if (distance < 1.2) {
         pitch = MathHelper.clamp(pitch, currentRotation.getPitch() - 18.0F, currentRotation.getPitch() + 22.0F);
      }
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

         float microScale = this.getMicroMovementScale();
         this.aimOffset = new Vec3d(
            MathHelper.clamp(0.5 + MathUtility.random(-profile.aimHorizontalSpread, profile.aimHorizontalSpread) * microScale, 0.18, 0.82),
            MathHelper.clamp(0.58 + MathUtility.random(-profile.aimVerticalSpread, profile.aimVerticalSpread) * microScale, 0.34, 0.72),
            MathHelper.clamp(0.5 + MathUtility.random(-profile.aimHorizontalSpread, profile.aimHorizontalSpread) * microScale, 0.18, 0.82)
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
      double nearestDistance = mc.player.getEyePos().distanceTo(RotationMath.getNearestPoint(target));
      if (nearestDistance < 1.2 || mc.player.getBoundingBox().intersects(target.getBoundingBox().expand(0.18))) {
         Vec3d nearest = RotationMath.getNearestPoint(target);
         float closeBlend = nearestDistance < 0.7 ? 0.88F : 0.72F;
         x += (nearest.x - x) * closeBlend;
         z += (nearest.z - z) * closeBlend;
         double closeMinY = Math.max(box.minY + height * 0.32, mc.player.getEyeY() - 0.65);
         double closeMaxY = Math.min(box.minY + height * 0.64, mc.player.getEyeY() - 0.06);
         if (closeMaxY > closeMinY) {
            y = MathHelper.clamp(y, closeMinY, closeMaxY);
         } else {
            y = MathHelper.clamp(mc.player.getEyeY() - 0.2, box.minY + height * 0.32, box.minY + height * 0.64);
         }
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

      if (this.shouldRequireCriticalForAttack() && !CombatUtility.canPerformCriticalHit(target, true)) {
         return false;
      }

      return true;
   }

   private boolean isAttackReady() {
      AuraAIV2Profile profile = this.aiManager.getActiveProfile();
      if (this.nextAttackDelay < 0L) {
         this.nextAttackDelay = (long)MathHelper.clamp(profile.attackDelayMs * MathUtility.random(0.88, 1.16), 45.0F, 1600.0F);
         this.criticalDecisionReady = true;
         this.requireCriticalForNextAttack = MathUtility.random(0.0, 100.0) < this.criticalChance.getCurrentValue();
      }

      return CombatUtility.getMace() != null
         ? this.attackTimer.finished(Math.max(420L, this.nextAttackDelay))
         : mc.player.getAttackCooldownProgress(1.5F) > 0.93F && this.attackTimer.finished(this.nextAttackDelay);
   }

   private boolean shouldRequireCriticalForAttack() {
      if (!this.onlyCriticals.isEnabled()) {
         return false;
      }

      if (!this.criticalDecisionReady) {
         this.criticalDecisionReady = true;
         this.requireCriticalForNextAttack = MathUtility.random(0.0, 100.0) < this.criticalChance.getCurrentValue();
      }

      return this.requireCriticalForNextAttack;
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
      this.criticalDecisionReady = false;
      this.requireCriticalForNextAttack = false;
   }

   private void resetRuntime() {
      this.currentTarget = null;
      XaClient.getInstance().getTargetManager().reset();
      this.resetAttackState();
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

   private float getMicroMovementScale() {
      return this.microMovement.isEnabled() ? MathHelper.clamp(this.microMovementAmount.getCurrentValue() / 100.0F, 0.0F, 1.0F) : 0.0F;
   }

   private void resetAttackState() {
      this.nextAttackDelay = -1L;
      this.criticalDecisionReady = false;
      this.requireCriticalForNextAttack = false;
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
      this.resetAttackState();
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
