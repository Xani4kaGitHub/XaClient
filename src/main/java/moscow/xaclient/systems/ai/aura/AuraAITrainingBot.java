package moscow.xaclient.systems.ai.aura;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import moscow.xaclient.utility.game.FakePlayerEntity;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.math.MathUtility;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class AuraAITrainingBot extends FakePlayerEntity implements IMinecraft {
   private static final UUID UUID_VALUE = UUID.fromString("66123666-6666-6666-6666-66666666a411");
   private long nextRepositionMs;
   private double orbitAngle;
   private float trainingHealth = 20.0F;

   public AuraAITrainingBot(ClientWorld world) {
      super(world, new GameProfile(UUID_VALUE, "AuraAI Bot"));
      this.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
      this.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
      this.getInventory().armor.set(3, new ItemStack(Items.DIAMOND_HELMET));
      this.getInventory().armor.set(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
      this.getInventory().armor.set(1, new ItemStack(Items.DIAMOND_LEGGINGS));
      this.getInventory().armor.set(0, new ItemStack(Items.DIAMOND_BOOTS));
   }

   public void spawnNearPlayer() {
      this.trainingHealth = 20.0F;
      this.setHealth(this.trainingHealth);
      this.reposition(true);
      this.spawn();
   }

   public void tickTraining() {
      if (mc.player == null || this.isRemoved()) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now >= this.nextRepositionMs || this.distanceTo(mc.player) > 7.0F || this.distanceTo(mc.player) < 0.9F) {
         this.reposition(false);
      } else {
         this.strafeAroundPlayer();
      }

      this.lookAtPlayer();
   }

   public void handleLocalHit() {
      if (mc.world == null || mc.player == null) {
         return;
      }

      mc.world.playSoundFromEntity(mc.player, this, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
      if (mc.player.fallDistance > 0.0F && !mc.player.isOnGround()) {
         mc.world.playSoundFromEntity(mc.player, this, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
      } else {
         mc.world.playSoundFromEntity(mc.player, this, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.65F, 1.0F);
      }

      this.trainingHealth -= 4.0F;
      if (this.trainingHealth <= 0.0F) {
         this.trainingHealth = 20.0F;
         this.setHealth(this.trainingHealth);
         this.nextRepositionMs = Math.max(this.nextRepositionMs, System.currentTimeMillis() + (long)MathUtility.random(1800.0, 3200.0));
      } else {
         this.setHealth(this.trainingHealth);
      }
   }

   private void reposition(boolean immediate) {
      if (mc.player == null) {
         return;
      }

      this.orbitAngle = Math.toRadians(mc.player.getYaw() + MathUtility.random(-135.0, 135.0));
      double radius = MathUtility.random(2.0, 3.4);
      Vec3d playerPos = mc.player.getPos();
      double x = playerPos.x + Math.cos(this.orbitAngle) * radius;
      double z = playerPos.z + Math.sin(this.orbitAngle) * radius;
      this.setPosition(x, playerPos.y, z);
      this.setVelocity(Vec3d.ZERO);
      this.nextRepositionMs = System.currentTimeMillis() + (long)MathUtility.random(immediate ? 3500.0 : 4500.0, 7000.0);
   }

   private void strafeAroundPlayer() {
      if (mc.player == null) {
         return;
      }

      this.orbitAngle += MathUtility.random(-0.025, 0.04);
      Vec3d playerPos = mc.player.getPos();
      double radius = MathHelper.clamp(this.distanceTo(mc.player), 2.0F, 3.6F);
      Vec3d target = new Vec3d(playerPos.x + Math.cos(this.orbitAngle) * radius, playerPos.y, playerPos.z + Math.sin(this.orbitAngle) * radius);
      Vec3d delta = target.subtract(this.getPos());
      if (delta.horizontalLength() < 0.02) {
         return;
      }

      Vec3d velocity = new Vec3d(delta.x, 0.0, delta.z).normalize().multiply(0.045);
      this.setVelocity(velocity.x, this.getVelocity().y, velocity.z);
      this.move(MovementType.SELF, velocity);
      this.limbAnimator.setSpeed(0.65F);
      this.setSprinting(true);
   }

   @Override
   public boolean isPushable() {
      return false;
   }

   @Override
   public boolean isCollidable() {
      return false;
   }

   @Override
   public boolean collidesWith(Entity other) {
      return false;
   }

   private void lookAtPlayer() {
      if (mc.player == null) {
         return;
      }

      Vec3d from = this.getPos().add(0.0, this.getStandingEyeHeight(), 0.0);
      Vec3d to = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
      Vec3d delta = to.subtract(from);
      double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
      if (horizontal < 1.0E-4) {
         return;
      }

      float targetYaw = (float)(MathHelper.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
      float targetPitch = (float)(-(MathHelper.atan2(delta.y, horizontal) * 180.0 / Math.PI));
      float yaw = this.getYaw() + MathHelper.wrapDegrees(targetYaw - this.getYaw()) * 0.35F;
      float pitch = MathHelper.clamp(this.getPitch() + (targetPitch - this.getPitch()) * 0.35F, -89.9F, 89.9F);
      this.setYaw(yaw);
      this.setBodyYaw(yaw);
      this.setHeadYaw(yaw);
      this.setPitch(pitch);
   }

   public static boolean isTrainingBot(Entity entity) {
      return entity instanceof AuraAITrainingBot;
   }
}
