package moscow.xaclient.systems.ai.aura.v2;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.MathHelper;

public class AuraAIV2Profile {
   public static final int DISTANCE_BUCKETS = 3;
   public static final int ANGLE_BUCKETS = 3;
   private static final int VERSION = 2;

   public int version = VERSION;
   public long createdAt = System.currentTimeMillis();
   public int samples;
   public int attacks;
   public long attackDelayMs = 510L;
   public float reactionMs = 65.0F;
   public float aimPointHoldMs = 430.0F;
   public float aimHorizontalSpread = 0.12F;
   public float aimVerticalSpread = 0.08F;
   public float jitterYaw = 0.05F;
   public float jitterPitch = 0.025F;
   public float[][] yawSpeed = defaultMatrix(42.0F, 58.0F, 82.0F);
   public float[][] pitchSpeed = defaultMatrix(18.0F, 28.0F, 44.0F);
   public float[][] yawAcceleration = defaultMatrix(10.0F, 16.0F, 25.0F);
   public float[][] pitchAcceleration = defaultMatrix(5.0F, 8.0F, 14.0F);
   public float[][] smoothing = defaultMatrix(0.68F, 0.76F, 0.84F);

   public static AuraAIV2Profile defaults() {
      return new AuraAIV2Profile();
   }

   public static AuraAIV2Profile fromSamples(List<AuraAIV2Sample> samples, List<Long> attackDelays) {
      AuraAIV2Profile profile = defaults();
      profile.createdAt = System.currentTimeMillis();
      profile.samples = samples.size();
      profile.attacks = attackDelays.size() + (attackDelays.isEmpty() ? 0 : 1);
      if (samples.size() < 25) {
         profile.applyAttackDelays(attackDelays);
         return profile;
      }

      Stats[][] stats = new Stats[DISTANCE_BUCKETS][ANGLE_BUCKETS];
      for (int d = 0; d < DISTANCE_BUCKETS; d++) {
         for (int a = 0; a < ANGLE_BUCKETS; a++) {
            stats[d][a] = new Stats();
         }
      }

      int movingSamples = 0;
      int airborneSamples = 0;
      float yawVarianceSum = 0.0F;
      float pitchVarianceSum = 0.0F;
      for (AuraAIV2Sample sample : samples) {
         int d = clampBucket(sample.distanceBucket, DISTANCE_BUCKETS);
         int a = clampBucket(sample.angleBucket, ANGLE_BUCKETS);
         stats[d][a].add(sample);
         if (sample.moving) {
            movingSamples++;
         }

         if (sample.airborne) {
            airborneSamples++;
         }

         yawVarianceSum += sample.yawStep * sample.yawStep;
         pitchVarianceSum += sample.pitchStep * sample.pitchStep;
      }

      for (int d = 0; d < DISTANCE_BUCKETS; d++) {
         for (int a = 0; a < ANGLE_BUCKETS; a++) {
            Stats stat = stats[d][a];
            if (stat.count < 8) {
               continue;
            }

            float distanceFactor = 0.82F + d * 0.18F;
            float angleFactor = 0.78F + a * 0.28F;
            profile.yawSpeed[d][a] = MathHelper.clamp(stat.avgYawStep() * 2.35F * distanceFactor + stat.avgYawError() * 0.28F, 18.0F, 128.0F);
            profile.pitchSpeed[d][a] = MathHelper.clamp(stat.avgPitchStep() * 2.05F * angleFactor + stat.avgPitchError() * 0.18F, 8.0F, 74.0F);
            profile.yawAcceleration[d][a] = MathHelper.clamp(stat.avgYawAcceleration() * 2.1F + 5.0F + a * 2.0F, 5.0F, 52.0F);
            profile.pitchAcceleration[d][a] = MathHelper.clamp(stat.avgPitchAcceleration() * 1.85F + 2.5F + a, 2.5F, 36.0F);
            profile.smoothing[d][a] = MathHelper.clamp(0.52F + (profile.yawSpeed[d][a] + profile.pitchSpeed[d][a]) / 210.0F, 0.48F, 0.92F);
         }
      }

      float movingRatio = movingSamples / (float)samples.size();
      float airborneRatio = airborneSamples / (float)samples.size();
      profile.reactionMs = MathHelper.clamp(95.0F - movingRatio * 30.0F - airborneRatio * 16.0F, 35.0F, 145.0F);
      profile.aimPointHoldMs = MathHelper.clamp(520.0F - movingRatio * 145.0F, 250.0F, 780.0F);
      profile.jitterYaw = MathHelper.clamp((float)Math.sqrt(yawVarianceSum / samples.size()) * 0.025F, 0.015F, 0.42F);
      profile.jitterPitch = MathHelper.clamp((float)Math.sqrt(pitchVarianceSum / samples.size()) * 0.014F, 0.008F, 0.2F);
      profile.aimHorizontalSpread = MathHelper.clamp(0.09F + movingRatio * 0.12F, 0.06F, 0.24F);
      profile.aimVerticalSpread = MathHelper.clamp(0.05F + airborneRatio * 0.11F, 0.035F, 0.18F);
      profile.applyAttackDelays(attackDelays);
      profile.sanitize();
      return profile;
   }

   public void sanitize() {
      this.version = VERSION;
      this.attackDelayMs = clampLong(this.attackDelayMs, 50L, 1800L);
      this.reactionMs = MathHelper.clamp(this.reactionMs, 0.0F, 250.0F);
      this.aimPointHoldMs = MathHelper.clamp(this.aimPointHoldMs, 120.0F, 1200.0F);
      this.aimHorizontalSpread = MathHelper.clamp(this.aimHorizontalSpread, 0.0F, 0.38F);
      this.aimVerticalSpread = MathHelper.clamp(this.aimVerticalSpread, 0.0F, 0.28F);
      this.jitterYaw = MathHelper.clamp(this.jitterYaw, 0.0F, 0.8F);
      this.jitterPitch = MathHelper.clamp(this.jitterPitch, 0.0F, 0.35F);
      this.yawSpeed = sanitizeMatrix(this.yawSpeed, defaultMatrix(42.0F, 58.0F, 82.0F), 12.0F, 150.0F);
      this.pitchSpeed = sanitizeMatrix(this.pitchSpeed, defaultMatrix(18.0F, 28.0F, 44.0F), 4.0F, 90.0F);
      this.yawAcceleration = sanitizeMatrix(this.yawAcceleration, defaultMatrix(10.0F, 16.0F, 25.0F), 3.0F, 70.0F);
      this.pitchAcceleration = sanitizeMatrix(this.pitchAcceleration, defaultMatrix(5.0F, 8.0F, 14.0F), 1.5F, 48.0F);
      this.smoothing = sanitizeMatrix(this.smoothing, defaultMatrix(0.68F, 0.76F, 0.84F), 0.25F, 1.0F);
   }

   public int distanceBucket(double distance) {
      if (distance < 1.7) {
         return 0;
      }

      return distance < 3.2 ? 1 : 2;
   }

   public int angleBucket(float totalAngle) {
      if (totalAngle < 8.0F) {
         return 0;
      }

      return totalAngle < 32.0F ? 1 : 2;
   }

   public float yawSpeed(int distanceBucket, int angleBucket) {
      return this.yawSpeed[clampBucket(distanceBucket, DISTANCE_BUCKETS)][clampBucket(angleBucket, ANGLE_BUCKETS)];
   }

   public float pitchSpeed(int distanceBucket, int angleBucket) {
      return this.pitchSpeed[clampBucket(distanceBucket, DISTANCE_BUCKETS)][clampBucket(angleBucket, ANGLE_BUCKETS)];
   }

   public float yawAcceleration(int distanceBucket, int angleBucket) {
      return this.yawAcceleration[clampBucket(distanceBucket, DISTANCE_BUCKETS)][clampBucket(angleBucket, ANGLE_BUCKETS)];
   }

   public float pitchAcceleration(int distanceBucket, int angleBucket) {
      return this.pitchAcceleration[clampBucket(distanceBucket, DISTANCE_BUCKETS)][clampBucket(angleBucket, ANGLE_BUCKETS)];
   }

   public float smoothing(int distanceBucket, int angleBucket) {
      return this.smoothing[clampBucket(distanceBucket, DISTANCE_BUCKETS)][clampBucket(angleBucket, ANGLE_BUCKETS)];
   }

   private void applyAttackDelays(List<Long> attackDelays) {
      if (attackDelays.isEmpty()) {
         return;
      }

      List<Long> valid = new ArrayList<>();
      for (long delay : attackDelays) {
         if (delay >= 50L && delay <= 2200L) {
            valid.add(delay);
         }
      }

      if (valid.isEmpty()) {
         return;
      }

      valid.sort(Long::compareTo);
      long median = valid.get(valid.size() / 2);
      this.attackDelayMs = clampLong(median, 90L, 1300L);
   }

   private static float[][] defaultMatrix(float close, float medium, float far) {
      return new float[][]{
         {close * 0.72F, close, close * 1.24F},
         {medium * 0.72F, medium, medium * 1.24F},
         {far * 0.72F, far, far * 1.24F}
      };
   }

   private static float[][] sanitizeMatrix(float[][] value, float[][] fallback, float min, float max) {
      if (value == null || value.length != DISTANCE_BUCKETS) {
         value = fallback;
      }

      float[][] sanitized = new float[DISTANCE_BUCKETS][ANGLE_BUCKETS];
      for (int d = 0; d < DISTANCE_BUCKETS; d++) {
         float[] row = d < value.length ? value[d] : null;
         if (row == null || row.length != ANGLE_BUCKETS) {
            row = fallback[d];
         }

         for (int a = 0; a < ANGLE_BUCKETS; a++) {
            sanitized[d][a] = MathHelper.clamp(row[a], min, max);
         }
      }

      return sanitized;
   }

   private static int clampBucket(int bucket, int max) {
      return Math.max(0, Math.min(max - 1, bucket));
   }

   private static long clampLong(long value, long min, long max) {
      return Math.max(min, Math.min(max, value));
   }

   private static class Stats {
      private int count;
      private float yawStep;
      private float pitchStep;
      private float yawAcceleration;
      private float pitchAcceleration;
      private float yawError;
      private float pitchError;

      private void add(AuraAIV2Sample sample) {
         this.count++;
         this.yawStep += Math.abs(sample.yawStep);
         this.pitchStep += Math.abs(sample.pitchStep);
         this.yawAcceleration += Math.abs(sample.yawAcceleration);
         this.pitchAcceleration += Math.abs(sample.pitchAcceleration);
         this.yawError += Math.abs(sample.yawError);
         this.pitchError += Math.abs(sample.pitchError);
      }

      private float avgYawStep() {
         return this.count == 0 ? 0.0F : this.yawStep / this.count;
      }

      private float avgPitchStep() {
         return this.count == 0 ? 0.0F : this.pitchStep / this.count;
      }

      private float avgYawAcceleration() {
         return this.count == 0 ? 0.0F : this.yawAcceleration / this.count;
      }

      private float avgPitchAcceleration() {
         return this.count == 0 ? 0.0F : this.pitchAcceleration / this.count;
      }

      private float avgYawError() {
         return this.count == 0 ? 0.0F : this.yawError / this.count;
      }

      private float avgPitchError() {
         return this.count == 0 ? 0.0F : this.pitchError / this.count;
      }
   }
}
