package moscow.xaclient.systems.ai.aura.v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.ai.aura.AuraAITrainingBot;
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
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AuraAIV2Manager implements IMinecraft {
   private static final long WARMUP_MS = 4000L;
   private static final long RECORD_MS = 40000L;

   private final File profileFile = new File(FileManager.DIRECTORY, "aura_ai" + File.separator + "v2_profile.json");
   private final List<AuraAIV2Sample> samples = new ArrayList<>();
   private final List<Long> attackDelays = new ArrayList<>();
   private AuraAIV2Profile profile = AuraAIV2Profile.defaults();
   private boolean profileLoaded;
   private boolean training;
   private boolean loadAfterTraining;
   private long trainingStartMs;
   private long lastTickMs;
   private long lastAttackMs;
   private float lastYaw;
   private float lastPitch;
   private float lastYawStep;
   private float lastPitchStep;
   private AuraAITrainingBot trainingBot;

   private final EventListener<ClientPlayerTickEvent> onPlayerTick = event -> this.tickTraining();
   private final EventListener<AttackEvent> onAttack = event -> this.recordAttack();
   private final EventListener<InternalAttackEvent> onInternalAttack = event -> {
      if (this.trainingBot != null && event.getEntity() == this.trainingBot) {
         event.cancel();
         this.recordAttack();
         this.trainingBot.handleLocalHit();
      }
   };
   private final EventListener<WorldChangeEvent> onWorldChange = event -> {
      this.training = false;
      this.removeTrainingBot();
   };

   public AuraAIV2Manager() {
      XaClient.getInstance().getEventManager().subscribe(this);
      this.loadProfile();
   }

   public void startTraining(boolean loadAfterTraining) {
      if (mc.player == null || mc.world == null) {
         MessageUtility.warn(Text.of("AI Aura V2: enter a world before training"));
         return;
      }

      this.training = true;
      this.loadAfterTraining = loadAfterTraining;
      this.trainingStartMs = System.currentTimeMillis();
      this.lastTickMs = 0L;
      this.lastAttackMs = 0L;
      this.lastYaw = mc.player.getYaw();
      this.lastPitch = mc.player.getPitch();
      this.lastYawStep = 0.0F;
      this.lastPitchStep = 0.0F;
      this.samples.clear();
      this.attackDelays.clear();
      this.spawnTrainingBot();
      MessageUtility.info(Text.of("AI Aura V2: 4s warmup, then 40s fight the local bot"));
   }

   public void cancelTraining() {
      if (!this.training) {
         MessageUtility.warn(Text.of("AI Aura V2: no active training"));
         return;
      }

      this.training = false;
      this.loadAfterTraining = false;
      this.samples.clear();
      this.attackDelays.clear();
      this.removeTrainingBot();
      XaClient.getInstance().getTargetManager().reset();
      MessageUtility.info(Text.of("AI Aura V2: training cancelled"));
   }

   public boolean loadProfile() {
      if (!this.profileFile.exists()) {
         this.profile = AuraAIV2Profile.defaults();
         this.profileLoaded = false;
         return false;
      }

      try (BufferedReader reader = Files.newBufferedReader(this.profileFile.toPath(), StandardCharsets.UTF_8)) {
         AuraAIV2Profile loaded = FileManager.GSON.fromJson(reader, AuraAIV2Profile.class);
         if (loaded == null) {
            throw new IOException("empty profile");
         }

         loaded.sanitize();
         this.profile = loaded;
         this.profileLoaded = true;
         MessageUtility.info(Text.of("AI Aura V2: profile loaded"));
         return true;
      } catch (Exception exception) {
         this.profile = AuraAIV2Profile.defaults();
         this.profileLoaded = false;
         XaClient.LOGGER.error("Failed to load AI Aura V2 profile", exception);
         MessageUtility.error(Text.of("AI Aura V2: failed to load profile"));
         return false;
      }
   }

   public AuraAIV2Profile getActiveProfile() {
      return this.profileLoaded ? this.profile : AuraAIV2Profile.defaults();
   }

   public void resetRuntime() {
      if (!this.training) {
         this.removeTrainingBot();
      }
   }

   private void tickTraining() {
      if (!this.training || mc.player == null || mc.world == null) {
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
         MessageUtility.overlay(MessageUtility.LogLevel.INFO, Text.of("AI Aura V2 recording starts in " + left + "s"));
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
         this.lastYawStep = 0.0F;
         this.lastPitchStep = 0.0F;
         this.lastTickMs = now;
         return;
      }

      long recordElapsed = elapsed - WARMUP_MS;
      if (recordElapsed >= RECORD_MS) {
         this.finishTraining();
         return;
      }

      this.recordAimSample(now);
      long secondsLeft = Math.max(0L, (RECORD_MS - recordElapsed + 999L) / 1000L);
      MessageUtility.overlay(MessageUtility.LogLevel.INFO, Text.of("AI Aura V2 recording: " + secondsLeft + "s left"));
   }

   private void recordAimSample(long now) {
      if (this.trainingBot == null || this.trainingBot.isRemoved()) {
         return;
      }

      long deltaMs = this.lastTickMs == 0L ? 50L : now - this.lastTickMs;
      if (deltaMs <= 0L || deltaMs > 250L) {
         this.lastTickMs = now;
         this.lastYaw = mc.player.getYaw();
         this.lastPitch = mc.player.getPitch();
         return;
      }

      Rotation targetRotation = RotationMath.getRotationTo(this.trainingBot.getEyePos());
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();
      float yawError = MathHelper.wrapDegrees(targetRotation.getYaw() - yaw);
      float pitchError = targetRotation.getPitch() - pitch;
      float yawStep = MathHelper.wrapDegrees(yaw - this.lastYaw);
      float pitchStep = pitch - this.lastPitch;
      double distance = mc.player.getEyePos().distanceTo(this.trainingBot.getEyePos());
      int distanceBucket = this.profile.distanceBucket(distance);
      int angleBucket = this.profile.angleBucket(Math.abs(yawError) + Math.abs(pitchError));
      boolean moving = mc.player.forwardSpeed != 0.0F || mc.player.sidewaysSpeed != 0.0F;
      this.samples.add(
         new AuraAIV2Sample(
            distanceBucket,
            angleBucket,
            deltaMs,
            yawError,
            pitchError,
            yawStep,
            pitchStep,
            yawStep - this.lastYawStep,
            pitchStep - this.lastPitchStep,
            moving,
            !mc.player.isOnGround()
         )
      );
      this.lastTickMs = now;
      this.lastYaw = yaw;
      this.lastPitch = pitch;
      this.lastYawStep = yawStep;
      this.lastPitchStep = pitchStep;
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
      this.profile = AuraAIV2Profile.fromSamples(this.samples, this.attackDelays);
      this.profileLoaded = true;
      this.saveProfile();
      if (this.loadAfterTraining) {
         this.loadProfile();
      }

      MessageUtility.info(Text.of("AI Aura V2: profile saved (" + this.samples.size() + " samples)"));
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
         XaClient.LOGGER.error("Failed to save AI Aura V2 profile", exception);
         MessageUtility.error(Text.of("AI Aura V2: failed to save profile"));
      }
   }

   public boolean isTraining() {
      return this.training;
   }

   public boolean isProfileLoaded() {
      return this.profileLoaded;
   }

   public Vec3d getTrainingBotEyePos() {
      return this.trainingBot == null ? null : this.trainingBot.getEyePos();
   }
}
