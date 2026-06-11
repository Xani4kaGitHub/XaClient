package moscow.xaclient.utility.rotations;

import lombok.Generated;

public class RotationTask {
   private final Rotation rotation;
   private final MoveCorrection moveCorrection;
   private final float speedX;
   private final float speedY;
   private final float returnSpeed;
   private final int priority;
   private final boolean smooth;
   private final float smoothFactor;

   public RotationTask(Rotation rotation, MoveCorrection moveCorrection, float speedX, float speedY, float returnSpeed, int priority) {
      this(rotation, moveCorrection, speedX, speedY, returnSpeed, priority, false, 1.0F);
   }

   public RotationTask(Rotation rotation, MoveCorrection moveCorrection, float speedX, float speedY, float returnSpeed, int priority, boolean smooth, float smoothFactor) {
      this.rotation = rotation;
      this.moveCorrection = moveCorrection;
      this.speedX = speedX;
      this.speedY = speedY;
      this.priority = priority;
      this.returnSpeed = returnSpeed;
      this.smooth = smooth;
      this.smoothFactor = smoothFactor;
   }

   public RotationTask(Rotation rotation, float speedX, float speedY, long returnSpeed, int priority) {
      this(rotation, MoveCorrection.NONE, speedX, speedY, (float)returnSpeed, priority);
   }

   @Generated
   public boolean isSmooth() {
      return this.smooth;
   }

   @Generated
   public float getSmoothFactor() {
      return this.smoothFactor;
   }

   @Generated
   public Rotation getRotation() {
      return this.rotation;
   }

   @Generated
   public MoveCorrection getMoveCorrection() {
      return this.moveCorrection;
   }

   @Generated
   public float getSpeedX() {
      return this.speedX;
   }

   @Generated
   public float getSpeedY() {
      return this.speedY;
   }

   @Generated
   public float getReturnSpeed() {
      return this.returnSpeed;
   }

   @Generated
   public int getPriority() {
      return this.priority;
   }
}
