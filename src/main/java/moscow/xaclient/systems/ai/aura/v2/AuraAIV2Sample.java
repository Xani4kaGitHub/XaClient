package moscow.xaclient.systems.ai.aura.v2;

public class AuraAIV2Sample {
   public final int distanceBucket;
   public final int angleBucket;
   public final long timeDeltaMs;
   public final float yawError;
   public final float pitchError;
   public final float yawStep;
   public final float pitchStep;
   public final float yawAcceleration;
   public final float pitchAcceleration;
   public final boolean moving;
   public final boolean airborne;

   public AuraAIV2Sample(
      int distanceBucket,
      int angleBucket,
      long timeDeltaMs,
      float yawError,
      float pitchError,
      float yawStep,
      float pitchStep,
      float yawAcceleration,
      float pitchAcceleration,
      boolean moving,
      boolean airborne
   ) {
      this.distanceBucket = distanceBucket;
      this.angleBucket = angleBucket;
      this.timeDeltaMs = timeDeltaMs;
      this.yawError = yawError;
      this.pitchError = pitchError;
      this.yawStep = yawStep;
      this.pitchStep = pitchStep;
      this.yawAcceleration = yawAcceleration;
      this.pitchAcceleration = pitchAcceleration;
      this.moving = moving;
      this.airborne = airborne;
   }
}
