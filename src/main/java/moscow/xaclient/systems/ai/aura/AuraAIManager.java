package moscow.xaclient.systems.ai.aura;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.AttackEvent;
import moscow.xaclient.systems.event.impl.game.InternalAttackEvent;
import moscow.xaclient.systems.event.impl.game.WorldChangeEvent;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.file.FileManager;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.math.MathUtility;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationMath;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AuraAIManager implements IMinecraft {
   private static final long WARMUP_MS = 5000L;
   private static final long RECORD_MS = 30000L;

   private final File profileFile = new File(FileManager.DIRECTORY, "aura_ai" + File.separator + "profile.json");
   private final List<AuraAISample> samples = new ArrayList<>();
   private final List<Long> attackDelays = new ArrayList<>();
   private AuraAIProfile profile = AuraAIProfile.defaults();
   private boolean profileLoaded;
   private boolean training;
   private boolean loadAfterTraining;
   private long trainingStartMs;
   private long lastTickMs;
   private long lastAttackMs;
   private long nextAuraAttackDelay = -1L;
   private AuraAITrainingBot trainingBot;
   private LivingEntity lastAimTarget;
   private Vec3d aimPointOffset = new Vec3d(0.5, 0.62, 0.5);
   private long nextAimPointRefreshMs;
   private float lastYaw;
   private float lastPitch;
   private float lastYawDelta;
   private float lastPitchDelta;
   private float yawVelocity;
   private float pitchVelocity;

   private final EventListener<ClientPlayerTickEvent> onPlayerTick = event -> this.tickTraining();
   private final EventListener<AttackEvent> onAttack = event -> this.recordAttack();
   private final EventListener<WorldChangeEvent> onWorldChange = event -> {
      this.training = false;
      this.removeTrainingBot();
   };
   private final EventListener<InternalAttackEvent> onInternalAttack = event -> {
      if (this.trainingBot != null && event.getEntity() == this.trainingBot) {
         event.cancel();
         this.recordAttack();
         this.trainingBot.handleLocalHit();
      }
   };

   public AuraAIManager() {
      XaClient.getInstance().getEventManager().subscribe(this);
      this.loadProfile();
   }

   public void startTraining(boolean loadAfterTraining) {
      if (mc.player == null) {
         MessageUtility.warn(Text.of("AuraAI: enter a world before training"));
         return;
      }

      this.training = true;
      this.loadAfterTraining = loadAfterTraining;
      this.trainingStartMs = System.currentTimeMillis();
      this.lastTickMs = 0L;
      this.lastAttackMs = 0L;
      this.lastYaw = mc.player.getYaw();
      this.lastPitch = mc.player.getPitch();
      this.lastYawDelta = 0.0F;
      this.lastPitchDelta = 0.0F;
      this.samples.clear();
      this.attackDelays.clear();
      this.spawnTrainingBot();
      MessageUtility.info(Text.of("AuraAI: 5s warmup, then 30s fight the local bot"));
   }

   public void cancelTraining() {
      if (!this.training) {
         MessageUtility.warn(Text.of("AuraAI: no active training"));
         return;
      }

      this.training = false;
      this.loadAfterTraining = false;
      this.lastTickMs = 0L;
      this.lastAttackMs = 0L;
      this.samples.clear();
      this.attackDelays.clear();
      this.removeTrainingBot();
      XaClient.getInstance().getTargetManager().reset();
      MessageUtility.info(Text.of("AuraAI: training cancelled"));
   }

   public boolean loadProfile() {
      if (!this.profileFile.exists()) {
         this.profile = AuraAIProfile.defaults();
         this.profileLoaded = false;
         return false;
      }

      try (BufferedReader reader = Files.newBufferedReader(this.profileFile.toPath(), StandardCharsets.UTF_8)) {
         AuraAIProfile loaded = FileManager.GSON.fromJson(reader, AuraAIProfile.class);
         if (loaded == null) {
            throw new IOException("empty profile");
         }

         loaded.sanitize();
         this.profile = loaded;
         this.profileLoaded = true;
         MessageUtility.info(Text.of("AuraAI: profile loaded"));
         return true;
      } catch (Exception exception) {
         this.profile = AuraAIProfile.defaults();
         this.profileLoaded = false;
         XaClient.LOGGER.error("Failed to load AuraAI profile", exception);
         MessageUtility.error(Text.of("AuraAI: failed to load profile"));
         return false;
      }
   }

   public Rotation calculateRotation(LivingEntity target, float influencePercent) {
      AuraAIProfile activeProfile = this.profileLoaded ? this.profile : AuraAIProfile.defaults();
      Vec3d aimPoint = this.getStableAimPoint(target);
      Rotation targetRotation = RotationMath.getRotationTo(aimPoint);
      Rotation currentRotation = XaClient.getInstance().getRotationHandler().isIdling()
         ? new Rotation(mc.player.getYaw(), mc.player.getPitch())
         : XaClient.getInstance().getRotationHandler().getCurrentRotation();
      float currentYaw = currentRotation.getYaw();
      float currentPitch = currentRotation.getPitch();
      float yawDelta = MathHelper.wrapDegrees(targetRotation.getYaw() - currentYaw);
      float pitchDelta = targetRotation.getPitch() - currentPitch;
      float influence = MathHelper.clamp(influencePercent / 100.0F, 0.0F, 1.0F);
      float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);
      double distance = mc.player.getEyePos().distanceTo(target.getEyePos());
      float closeFactor = distance < 1.35 ? MathHelper.clamp((float)(distance / 1.35), 0.35F, 1.0F) : 1.0F;
      float distanceFactor = MathHelper.clamp(totalDelta / 18.0F, 0.75F, 2.35F);
      float movementFactor = target.getVelocity().horizontalLength() > 0.045 || mc.player.getVelocity().horizontalLength() > 0.045 ? 1.18F : 1.0F;
      float yawLimit = this.lerp(180.0F, Math.max(42.0F, activeProfile.yawSpeedPerTick * distanceFactor * movementFactor), influence);
      float pitchLimit = this.lerp(90.0F, Math.max(16.0F, activeProfile.pitchSpeedPerTick * distanceFactor * 0.86F * movementFactor), influence);
      float yawAccelerationLimit = this.lerp(180.0F, Math.max(12.0F, activeProfile.yawAcceleration * 1.85F), influence);
      float pitchAccelerationLimit = this.lerp(90.0F, Math.max(6.0F, activeProfile.pitchAcceleration * 1.35F), influence);
      if (distance < 1.35) {
         pitchLimit *= MathHelper.clamp(closeFactor, 0.45F, 0.85F);
         pitchAccelerationLimit *= MathHelper.clamp(closeFactor, 0.45F, 0.8F);
         yawAccelerationLimit *= MathHelper.clamp(closeFactor, 0.65F, 0.95F);
      }

      float wantedYawVelocity = MathHelper.clamp(yawDelta, -yawLimit, yawLimit);
      float wantedPitchVelocity = MathHelper.clamp(pitchDelta, -pitchLimit, pitchLimit);
      this.yawVelocity = this.approach(this.yawVelocity, wantedYawVelocity, yawAccelerationLimit);
      this.pitchVelocity = this.approach(this.pitchVelocity, wantedPitchVelocity, pitchAccelerationLimit);
      float smooth = this.lerp(1.0F, MathHelper.clamp(activeProfile.smoothFactor + 0.08F, 0.55F, 0.96F), influence);
      if (totalDelta < 10.0F) {
         smooth = Math.min(smooth, 0.86F);
      }

      float closeYawScale = distance < 1.35 ? MathHelper.clamp(closeFactor, 0.62F, 0.92F) : 1.0F;
      float closePitchScale = distance < 1.35 ? MathHelper.clamp(closeFactor, 0.38F, 0.78F) : 1.0F;
      float yawStep = this.yawVelocity * smooth * closeYawScale;
      float pitchStep = this.pitchVelocity * smooth * closePitchScale;
      if (distance < 1.35) {
         pitchStep = MathHelper.clamp(pitchStep, -5.5F, 5.5F);
      }

      if (Math.signum(yawStep) != Math.signum(yawDelta) && Math.abs(yawDelta) > 0.001F) {
         yawStep = yawDelta;
         this.yawVelocity = yawStep;
      }

      if (Math.signum(pitchStep) != Math.signum(pitchDelta) && Math.abs(pitchDelta) > 0.001F) {
         pitchStep = pitchDelta;
         this.pitchVelocity = pitchStep;
      }

      yawStep = MathHelper.clamp(yawStep, -Math.abs(yawDelta), Math.abs(yawDelta));
      pitchStep = MathHelper.clamp(pitchStep, -Math.abs(pitchDelta), Math.abs(pitchDelta));
      float closeJitterScale = distance < 1.35 ? MathHelper.clamp(closeFactor, 0.15F, 0.7F) : 1.0F;
      float jitterScale = MathHelper.clamp((totalDelta - 1.5F) / 14.0F, 0.0F, 1.0F) * influence * closeJitterScale;
      float jitterYaw = MathUtility.random(-activeProfile.yawJitter, activeProfile.yawJitter) * jitterScale;
      float jitterPitch = MathUtility.random(-activeProfile.pitchJitter, activeProfile.pitchJitter) * jitterScale * 0.35F;
      float nextYaw = currentYaw + yawStep + jitterYaw;
      float nextPitch = MathHelper.clamp(currentPitch + pitchStep + jitterPitch, -90.0F, 90.0F);
      if (Math.abs(yawDelta) < 0.85F) {
         this.yawVelocity *= 0.35F;
      }

      if (Math.abs(pitchDelta) < 0.85F) {
         this.pitchVelocity *= 0.35F;
      }

      return RotationMath.correctRotation(new Rotation(nextYaw, nextPitch));
   }

   private Vec3d getStableAimPoint(LivingEntity target) {
      long now = System.currentTimeMillis();
      boolean targetChanged = this.lastAimTarget != target;
      if (targetChanged || now >= this.nextAimPointRefreshMs) {
         if (targetChanged) {
            this.yawVelocity = 0.0F;
            this.pitchVelocity = 0.0F;
         }

         this.aimPointOffset = new Vec3d(
            MathHelper.clamp(0.5 + MathUtility.random(-0.12, 0.12), 0.24, 0.76),
            MathHelper.clamp(0.58 + MathUtility.random(-0.07, 0.07), 0.42, 0.72),
            MathHelper.clamp(0.5 + MathUtility.random(-0.12, 0.12), 0.24, 0.76)
         );
         this.lastAimTarget = target;
         this.nextAimPointRefreshMs = now + (long)MathUtility.random(450.0, 850.0);
      }

      Vec3d playerEye = mc.player.getEyePos();
      Vec3d targetVelocity = target.getVelocity();
      double distance = playerEye.distanceTo(target.getEyePos());
      double prediction = distance < 1.35 ? 0.15 : 0.85;
      Box box = target.getBoundingBox().offset(targetVelocity.x * prediction, targetVelocity.y * prediction * 0.2, targetVelocity.z * prediction);
      double height = box.maxY - box.minY;
      double targetX = box.minX + (box.maxX - box.minX) * this.aimPointOffset.x;
      double targetY = box.minY + height * MathHelper.clamp(this.aimPointOffset.y, 0.38, distance < 1.35 ? 0.64 : 0.78);
      double targetZ = box.minZ + (box.maxZ - box.minZ) * this.aimPointOffset.z;
      if (mc.player.getBoundingBox().intersects(target.getBoundingBox().expand(0.15))) {
         Vec3d nearest = RotationMath.getNearestPoint(target);
         targetX += (nearest.x - targetX) * 0.65;
         targetZ += (nearest.z - targetZ) * 0.65;
      }

      if (playerEye.y > box.minY && playerEye.y < box.maxY + 0.35 && distance < 1.35) {
         targetY = MathHelper.clamp(playerEye.y - MathUtility.random(0.1, 0.28), box.minY + height * 0.32, box.minY + height * 0.64);
      }

      double horizontalDistanceSq = MathHelper.square(targetX - playerEye.x) + MathHelper.square(targetZ - playerEye.z);
      if (horizontalDistanceSq < 0.0324 && mc.player.getBoundingBox().intersects(target.getBoundingBox().expand(0.25))) {
         float yaw = XaClient.getInstance().getRotationHandler().getCurrentRotation().getYaw();
         double yawRadians = Math.toRadians(yaw);
         double forwardX = -Math.sin(yawRadians);
         double forwardZ = Math.cos(yawRadians);
         targetX = MathHelper.clamp(playerEye.x + forwardX * 0.22, box.minX + 0.02, box.maxX - 0.02);
         targetZ = MathHelper.clamp(playerEye.z + forwardZ * 0.22, box.minZ + 0.02, box.maxZ - 0.02);
      }

      return new Vec3d(targetX, targetY, targetZ);
   }

   public boolean isAuraAttackReady(Timer attackTimer) {
      if (!this.profileLoaded) {
         return true;
      }

      if (this.nextAuraAttackDelay < 0L) {
         this.nextAuraAttackDelay = (long)MathHelper.clamp(
            this.profile.attackDelayMs * MathUtility.random(0.85, 1.15),
            50.0F,
            1600.0F
         );
      }

      return attackTimer.finished(this.nextAuraAttackDelay);
   }

   public void markAuraAttack() {
      this.nextAuraAttackDelay = -1L;
   }

   public void resetRuntime() {
      this.yawVelocity = 0.0F;
      this.pitchVelocity = 0.0F;
      this.nextAuraAttackDelay = -1L;
      this.lastAimTarget = null;
      this.nextAimPointRefreshMs = 0L;
      if (!this.training) {
         this.removeTrainingBot();
      }
   }

   private void tickTraining() {
      if (!this.training || mc.player == null) {
         this.removeTrainingBot();
         return;
      }

      long now = System.currentTimeMillis();
      if (this.trainingBot == null || this.trainingBot.isRemoved()) {
         this.spawnTrainingBot();
      }

      if (this.trainingBot != null) {
         this.trainingBot.tickTraining();
      }

      long elapsed = now - this.trainingStartMs;
      if (elapsed < WARMUP_MS) {
         long left = Math.max(0L, (WARMUP_MS - elapsed + 999L) / 1000L);
         MessageUtility.overlay(MessageUtility.LogLevel.INFO, Text.of("AuraAI recording starts in " + left + "s"));
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
         this.lastTickMs = now;
         return;
      }

      long recordElapsed = elapsed - WARMUP_MS;
      if (recordElapsed >= RECORD_MS) {
         this.finishTraining();
         return;
      }

      long deltaMs = this.lastTickMs == 0L ? 50L : now - this.lastTickMs;
      if (deltaMs > 0L && deltaMs <= 250L) {
         float yaw = mc.player.getYaw();
         float pitch = mc.player.getPitch();
         float yawDelta = MathHelper.wrapDegrees(yaw - this.lastYaw);
         float pitchDelta = pitch - this.lastPitch;
         this.samples.add(
            new AuraAISample(
               deltaMs,
               yawDelta,
               pitchDelta,
               yawDelta - this.lastYawDelta,
               pitchDelta - this.lastPitchDelta,
               mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F,
               mc.player.isSprinting(),
               !mc.player.isOnGround()
            )
         );
         this.lastYaw = yaw;
         this.lastPitch = pitch;
         this.lastYawDelta = yawDelta;
         this.lastPitchDelta = pitchDelta;
      }

      this.lastTickMs = now;
      long secondsLeft = Math.max(0L, (RECORD_MS - recordElapsed + 999L) / 1000L);
      MessageUtility.overlay(MessageUtility.LogLevel.INFO, Text.of("AuraAI recording: " + secondsLeft + "s left"));
   }

   private void recordAttack() {
      if (!this.training) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - this.trainingStartMs < WARMUP_MS) {
         return;
      }

      if (this.lastAttackMs != 0L) {
         this.attackDelays.add(now - this.lastAttackMs);
      }

      this.lastAttackMs = now;
   }

   private void finishTraining() {
      this.training = false;
      this.removeTrainingBot();
      this.profile = AuraAIProfile.fromSamples(this.samples, this.attackDelays);
      this.profileLoaded = true;
      this.saveProfile();
      if (this.loadAfterTraining) {
         this.loadProfile();
      }

      MessageUtility.info(Text.of("AuraAI: profile saved (" + this.samples.size() + " samples)"));
   }

   private void spawnTrainingBot() {
      if (mc.world == null || mc.player == null) {
         return;
      }

      this.removeTrainingBot();
      this.trainingBot = new AuraAITrainingBot(mc.world);
      this.trainingBot.spawnNearPlayer();
      XaClient.getInstance().getTargetManager().reset();
   }

   private void removeTrainingBot() {
      if (this.trainingBot != null) {
         if (!this.trainingBot.isRemoved()) {
            this.trainingBot.remove();
         }

         this.trainingBot = null;
      }
   }

   private void saveProfile() {
      try {
         File parent = this.profileFile.getParentFile();
         if (parent != null) {
            Files.createDirectories(parent.toPath());
         }

         try (BufferedWriter writer = Files.newBufferedWriter(this.profileFile.toPath(), StandardCharsets.UTF_8)) {
            writer.write(FileManager.GSON.toJson(this.profile));
         }
      } catch (IOException exception) {
         XaClient.LOGGER.error("Failed to save AuraAI profile", exception);
         MessageUtility.error(Text.of("AuraAI: failed to save profile"));
      }
   }

   private float approach(float current, float target, float maxDelta) {
      return current + MathHelper.clamp(target - current, -maxDelta, maxDelta);
   }

   private float lerp(float from, float to, float delta) {
      return from + (to - from) * delta;
   }

   public boolean isTraining() {
      return this.training;
   }

   public boolean isProfileLoaded() {
      return this.profileLoaded;
   }
}
