package moscow.xaclient.utility.game;

import lombok.Generated;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class CrystalUtility implements IMinecraft {
   private static final float CRYSTAL_EXPLOSION_POWER = 6.0F;

   public static float calculateDamage(Vec3d crystalPos, PlayerEntity player, boolean checkBlocks) {
      if (player == null || mc.world == null) {
         return 0.0F;
      }

      Vec3d targetPos = player.getPos();
      if (checkBlocks && isLineBlocked(player.getEyePos(), crystalPos)) {
         return 0.0F;
      }

      float rawDamage = calculateRawExplosionDamage(crystalPos, targetPos);
      float protectionFactor = getProtectionFactor(player);
      return Math.max(0.0F, rawDamage * (1.0F - protectionFactor));
   }

   private static float calculateRawExplosionDamage(Vec3d explosionPos, Vec3d targetPos) {
      double distance = explosionPos.distanceTo(targetPos);
      double impact = 1.0 - distance / (CRYSTAL_EXPLOSION_POWER * 2.0);
      if (impact <= 0.0) {
         return 0.0F;
      }

      double rawDamage = (impact * impact + impact) / 2.0 * 7.0 * CRYSTAL_EXPLOSION_POWER * 2.0 + 1.0;
      return (float)rawDamage;
   }

   private static float getProtectionFactor(PlayerEntity player) {
      float armorValue = (float)player.getAttributeValue(EntityAttributes.ARMOR);
      float armorProtection = Math.min(0.5F, armorValue * 0.02F);
      float resistanceProtection = 0.0F;
      StatusEffectInstance resistance = player.getStatusEffect(StatusEffects.RESISTANCE);
      if (resistance != null) {
         resistanceProtection = (resistance.getAmplifier() + 1) * 0.2F;
      }

      return Math.min(0.8F, armorProtection + resistanceProtection);
   }

   private static boolean isLineBlocked(Vec3d from, Vec3d to) {
      if (mc.world == null) {
         return true;
      }

      return mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType() == Type.BLOCK;
   }

   @Generated
   private CrystalUtility() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
