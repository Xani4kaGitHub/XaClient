package moscow.xaclient.systems.ai.aura;

import java.util.List;
import net.minecraft.util.math.MathHelper;

public class AuraAIProfile {
   private static final int VERSION = 1;

   public int version = VERSION;
   public long createdAt = System.currentTimeMillis();
   public int samples;
   public int attacks;
   public float yawSpeedPerTick = 55.0F;
   public float pitchSpeedPerTick = 28.0F;
   public float yawAcceleration = 18.0F;
   public float pitchAcceleration = 10.0F;
   public float smoothFactor = 0.78F;
   public float yawJitter = 0.08F;
   public float pitchJitter = 0.035F;
   public long attackDelayMs = 520L;

   public static AuraAIProfile defaults() {
      return new AuraAIProfile();
   }

   public static AuraAIProfile fromSamples(List<AuraAISample> samples, List<Long> attackDelays) {
      AuraAIProfile profile = defaults();
      profile.createdAt = System.currentTimeMillis();
      profile.samples = samples.size();
      profile.attacks = attackDelays.size() + (attackDelays.isEmpty() ? 0 : 1);
      if (samples.isEmpty()) {
         return profile;
      }

      float yawAbs = 0.0F;
      float pitchAbs = 0.0F;
      float yawAcceleration = 0.0F;
      float pitchAcceleration = 0.0F;
      float yawSq = 0.0F;
      float pitchSq = 0.0F;

      for (AuraAISample sample : samples) {
         float absYaw = Math.abs(sample.yawDelta);
         float absPitch = Math.abs(sample.pitchDelta);
         yawAbs += absYaw;
         pitchAbs += absPitch;
         yawAcceleration += Math.abs(sample.yawAcceleration);
         pitchAcceleration += Math.abs(sample.pitchAcceleration);
         yawSq += sample.yawDelta * sample.yawDelta;
         pitchSq += sample.pitchDelta * sample.pitchDelta;
      }

      float count = samples.size();
      float avgYaw = yawAbs / count;
      float avgPitch = pitchAbs / count;
      profile.yawSpeedPerTick = MathHelper.clamp(avgYaw * 3.2F + 10.0F, 12.0F, 90.0F);
      profile.pitchSpeedPerTick = MathHelper.clamp(avgPitch * 2.4F + 5.0F, 5.0F, 65.0F);
      profile.yawAcceleration = MathHelper.clamp(yawAcceleration / count * 2.8F + 4.0F, 4.0F, 60.0F);
      profile.pitchAcceleration = MathHelper.clamp(pitchAcceleration / count * 2.4F + 2.0F, 2.0F, 45.0F);
      float yawVariance = Math.max(0.0F, yawSq / count - avgYaw * avgYaw);
      float pitchVariance = Math.max(0.0F, pitchSq / count - avgPitch * avgPitch);
      profile.yawJitter = MathHelper.clamp((float)Math.sqrt(yawVariance) * 0.08F, 0.02F, 0.75F);
      profile.pitchJitter = MathHelper.clamp((float)Math.sqrt(pitchVariance) * 0.045F, 0.01F, 0.35F);
      float speedTotal = profile.yawSpeedPerTick + profile.pitchSpeedPerTick;
      profile.smoothFactor = MathHelper.clamp(0.42F + speedTotal / 160.0F, 0.42F, 0.9F);

      if (!attackDelays.isEmpty()) {
         long total = 0L;
         int valid = 0;
         for (long delay : attackDelays) {
            if (delay >= 50L && delay <= 2500L) {
               total += delay;
               valid++;
            }
         }

         if (valid > 0) {
            profile.attackDelayMs = clampLong(total / valid, 120L, 1200L);
         }
      }

      return profile;
   }

   public void sanitize() {
      this.version = VERSION;
      this.yawSpeedPerTick = MathHelper.clamp(this.yawSpeedPerTick, 12.0F, 90.0F);
      this.pitchSpeedPerTick = MathHelper.clamp(this.pitchSpeedPerTick, 5.0F, 65.0F);
      this.yawAcceleration = MathHelper.clamp(this.yawAcceleration, 4.0F, 60.0F);
      this.pitchAcceleration = MathHelper.clamp(this.pitchAcceleration, 2.0F, 45.0F);
      this.smoothFactor = MathHelper.clamp(this.smoothFactor, 0.2F, 1.0F);
      this.yawJitter = MathHelper.clamp(this.yawJitter, 0.0F, 2.0F);
      this.pitchJitter = MathHelper.clamp(this.pitchJitter, 0.0F, 0.5F);
      this.attackDelayMs = clampLong(this.attackDelayMs, 50L, 2000L);
   }

   private static long clampLong(long value, long min, long max) {
      return Math.max(min, Math.min(max, value));
   }
}
