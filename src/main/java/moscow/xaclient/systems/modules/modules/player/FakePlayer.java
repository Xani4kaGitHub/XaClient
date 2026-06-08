package moscow.xaclient.systems.modules.modules.player;

import com.mojang.authlib.GameProfile;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.AttackEvent;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.game.FakePlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleInfo(name = "Fake Player", category = ModuleCategory.PLAYER, desc = "Создает фейкового игрока для тестирования")
public class FakePlayer extends BaseModule {
   private ModeSetting modeSetting;
   private ModeSetting.Value defaultKit;
   private ModeSetting.Value netheriteKit;
   private ModeSetting.Value diamondKit;
   private SliderSetting countSetting;

   private static final UUID FP_UUID_BASE = UUID.fromString("66123666-6666-6666-6666-666666666600");
   private static final String FP_NAME_BASE = "Fake Player";
   private static final float HIT_DAMAGE_HP = 2.0F;
   private final List<FakePlayerInstance> fakes = new ArrayList<>();
   private boolean lastAttackPressed = false;

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.world == null || mc.player == null) return;

      int total = fakes.size();
      for (int i = 0; i < total; i++) {
         FakePlayerInstance fp = fakes.get(i);
         fp.lookAtPlayer();
      }

      boolean pressed = mc.options.attackKey != null && mc.options.attackKey.isPressed();
      if (pressed && !lastAttackPressed) {
         tryAttackFakeUnderCrosshair();
      }
      lastAttackPressed = pressed;
   };

   private final EventListener<AttackEvent> onAttack = event -> {
      Entity entity = event.getEntity();
      if (!(entity instanceof FakePlayerInstance fp)) return;
      if (!fakes.contains(fp)) return;

      damageFakePlayer(fp);
   };

   public FakePlayer() {
      this.initialize();
   }

   private void initialize() {
      this.modeSetting = new ModeSetting(this, "modules.settings.fakeplayer.mode");
      this.defaultKit = new ModeSetting.Value(this.modeSetting, "modules.settings.fakeplayer.defaultKit").select();
      this.netheriteKit = new ModeSetting.Value(this.modeSetting, "modules.settings.fakeplayer.netheriteKit");
      this.diamondKit = new ModeSetting.Value(this.modeSetting, "modules.settings.fakeplayer.diamondKit");

      this.countSetting = new SliderSetting(this, "modules.settings.fakeplayer.count")
         .min(1.0F)
         .max(12.0F)
         .step(1.0F)
         .currentValue(1.0F);
   }

   @Override
   public void onEnable() {
      if (mc.world == null || mc.player == null) {
         this.setEnabled(false);
         return;
      }

      int count = (int) countSetting.getCurrentValue();
      String mode = defaultKit.isSelected() ? "default" : (netheriteKit.isSelected() ? "nether" : "diamond");

      for (int i = 1; i <= count; i++) {
         FakePlayerInstance fp = spawnOne(i, count, mode);
         if (fp != null) {
            fakes.add(fp);
         }
      }
      super.onEnable();
   }

   @Override
   public void onDisable() {
      for (FakePlayerInstance fp : fakes) {
         fp.remove();
      }
      fakes.clear();
      super.onDisable();
   }

   private FakePlayerInstance spawnOne(int slot, int total, String mode) {
      UUID uuid = new UUID(FP_UUID_BASE.getMostSignificantBits(), FP_UUID_BASE.getLeastSignificantBits() + (slot - 1));
      String name = slot == 1 ? FP_NAME_BASE : (FP_NAME_BASE + " " + slot);

      FakePlayerInstance fp = new FakePlayerInstance(mc.world, new GameProfile(uuid, name));
      fp.copyPositionAndRotation(mc.player);
      fp.updatePosition(slot, total);

      if (mode.equals("default")) {
         fp.setStackInHand(Hand.MAIN_HAND, mc.player.getMainHandStack().copy());
         ItemStack off = mc.player.getOffHandStack();
         if (off.isEmpty() || off.getItem() != Items.TOTEM_OF_UNDYING) {
            fp.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         } else {
            fp.setStackInHand(Hand.OFF_HAND, off.copy());
         }
         try {
            var inv = mc.player.getInventory();
            fp.getInventory().armor.set(3, inv.armor.get(3).copy());
            fp.getInventory().armor.set(2, inv.armor.get(2).copy());
            fp.getInventory().armor.set(1, inv.armor.get(1).copy());
            fp.getInventory().armor.set(0, inv.armor.get(0).copy());
         } catch (Throwable ignored) {}
      } else if (mode.equals("nether")) {
         fp.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
         fp.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         fp.getInventory().armor.set(3, new ItemStack(Items.NETHERITE_HELMET));
         fp.getInventory().armor.set(2, new ItemStack(Items.NETHERITE_CHESTPLATE));
         fp.getInventory().armor.set(1, new ItemStack(Items.NETHERITE_LEGGINGS));
         fp.getInventory().armor.set(0, new ItemStack(Items.NETHERITE_BOOTS));
      } else {
         fp.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
         fp.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         fp.getInventory().armor.set(3, new ItemStack(Items.DIAMOND_HELMET));
         fp.getInventory().armor.set(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
         fp.getInventory().armor.set(1, new ItemStack(Items.DIAMOND_LEGGINGS));
         fp.getInventory().armor.set(0, new ItemStack(Items.DIAMOND_BOOTS));
      }

      fp.setHealth(mc.player.getHealth());
      fp.spawn();
      return fp;
   }

   private void tryAttackFakeUnderCrosshair() {
      HitResult hr = mc.crosshairTarget;
      if (!(hr instanceof EntityHitResult ehr)) return;
      Entity e = ehr.getEntity();
      if (!(e instanceof FakePlayerInstance fp)) return;

      if (!fakes.contains(fp)) return;

      damageFakePlayer(fp);
   }

   private void damageFakePlayer(FakePlayerInstance fp) {
      mc.world.playSoundFromEntity(mc.player, fp, SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0F, 1.0F);
      if (mc.player.fallDistance > 0.0f && !mc.player.isOnGround()) {
         mc.world.playSoundFromEntity(mc.player, fp, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
      }

      float next = fp.getHealth() - HIT_DAMAGE_HP;
      if (next > 0.0F) {
         fp.setHealth(next);
      } else {
         ItemStack off = fp.getOffHandStack();
         ItemStack main = fp.getMainHandStack();
         boolean offTotem = off.getItem() == Items.TOTEM_OF_UNDYING;
         boolean mainTotem = main.getItem() == Items.TOTEM_OF_UNDYING;

         if (offTotem || mainTotem) {
            if (offTotem) fp.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            else fp.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);

            mc.world.playSoundFromEntity(mc.player, fp, SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0F, 1.0F);

            if (mc.player.networkHandler != null) {
               new EntityStatusS2CPacket(fp, (byte) 35).apply(mc.player.networkHandler);
            }

            fp.setHealth(fp.getMaxHealth());
            fp.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
         } else {
            fp.setHealth(0.0F);
         }
      }
   }

   private static final class FakePlayerInstance extends FakePlayerEntity {
      FakePlayerInstance(ClientWorld world, GameProfile profile) {
         super(world, profile);
      }

      void updatePosition(int slot, int total) {
         if (mc.player == null) return;
         double r = 2.2;
         double a = total <= 1 ? 0.0 : (slot - 1) * (Math.PI * 2.0) / (double) total;
         Vec3d p = mc.player.getPos();
         double x = p.x + Math.cos(a) * r;
         double y = p.y;
         double z = p.z + Math.sin(a) * r;
         this.setPosition(x, y, z);
      }

      void lookAtPlayer() {
         if (mc.player == null) return;
         Vec3d from = this.getPos().add(0.0, this.getStandingEyeHeight(), 0.0);
         Vec3d to = mc.player.getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
         Vec3d d = to.subtract(from);

         double distXZ = Math.sqrt(d.x * d.x + d.z * d.z);
         if (distXZ < 1.0E-4) return;

         float targetYaw = (float) (MathHelper.atan2(d.z, d.x) * (180.0 / Math.PI)) - 90.0F;
         float targetPitch = (float) (-(MathHelper.atan2(d.y, distXZ) * (180.0 / Math.PI)));

         float yaw = this.getYaw() + MathHelper.wrapDegrees(targetYaw - this.getYaw()) * 0.35F;
         float pitch = MathHelper.clamp(this.getPitch() + (targetPitch - this.getPitch()) * 0.35F, -89.9f, 89.9f);

         this.setYaw(yaw);
         this.setBodyYaw(yaw);
         this.setHeadYaw(yaw);
         this.setPitch(pitch);
      }
   }
}
