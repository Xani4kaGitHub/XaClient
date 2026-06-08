package moscow.xaclient.systems.ai.aura;

public class AuraAISample {
   public final long timeDeltaMs;
   public final float yawDelta;
   public final float pitchDelta;
   public final float yawAcceleration;
   public final float pitchAcceleration;
   public final boolean moving;
   public final boolean sprinting;
   public final boolean airborne;

   public AuraAISample(
      long timeDeltaMs,
      float yawDelta,
      float pitchDelta,
      float yawAcceleration,
      float pitchAcceleration,
      boolean moving,
      boolean sprinting,
      boolean airborne
   ) {
      this.timeDeltaMs = timeDeltaMs;
      this.yawDelta = yawDelta;
      this.pitchDelta = pitchDelta;
      this.yawAcceleration = yawAcceleration;
      this.pitchAcceleration = pitchAcceleration;
      this.moving = moving;
      this.sprinting = sprinting;
      this.airborne = airborne;
   }
}
