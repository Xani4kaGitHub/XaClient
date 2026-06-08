package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.InputEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.inventory.InventoryUtility;
import moscow.xaclient.utility.inventory.group.SlotGroup;
import moscow.xaclient.utility.inventory.group.SlotGroups;
import moscow.xaclient.utility.inventory.slots.HotbarSlot;
import moscow.xaclient.utility.inventory.slots.InventorySlot;
import moscow.xaclient.utility.rotations.MoveCorrection;
import moscow.xaclient.utility.rotations.Rotation;
import moscow.xaclient.utility.rotations.RotationPriority;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.util.Hand;

@ModuleInfo(name = "Elytra Strafe", category = ModuleCategory.MOVEMENT)
public class ElytraStrafe extends BaseModule {
   private final SliderSetting fireworkSlot = new SliderSetting(this, "modules.settings.elytra_target.fireworkSlot")
      .min(1.0F)
      .max(9.0F)
      .step(1.0F)
      .currentValue(7.0F)
      .suffix(" slot");
   private final SliderSetting fireworkDelay = new SliderSetting(this, "modules.settings.elytra_target.fireworkDelay")
      .min(0.1F)
      .max(2.0F)
      .step(0.1F)
      .currentValue(1.0F)
      .suffix(" sec");
   private final BooleanSetting autoTakeoff = new BooleanSetting(this, "modules.settings.elytra_strafe.autoTakeoff").enable();
   private final moscow.xaclient.utility.time.Timer fireworkTimer = new moscow.xaclient.utility.time.Timer();
   private final EventListener<InputEvent> onInput = event -> {
      if (mc.player != null && mc.player.isGliding()) {
         float yaw = (float)Math.toDegrees(EntityUtility.direction(mc.player.getYaw(), event.getForward(), event.getStrafe()));
         float pitch = !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()
            ? 0.0F
            : (event.getStrafe() + event.getForward() > 0.1F ? -45.0F : -90.0F);
         if (mc.options.sneakKey.isPressed()) {
            pitch *= -1.0F;
         }

         XaClient.getInstance()
            .getRotationHandler()
            .rotate(new Rotation(yaw, pitch), MoveCorrection.DIRECT, 180.0F, 180.0F, 180.0F, RotationPriority.NOT_IMPORTANT);
      }
   };

   @Override
   public void tick() {
      if (mc.player != null) {
         if (this.autoTakeoff.isEnabled()) {
            boolean isElytraEquipped = InventoryUtility.getChestplateSlot().item() == Items.ELYTRA;
            if (!mc.player.isGliding() && isElytraEquipped && !mc.player.isOnGround() && !mc.player.isInFluid()) {
               mc.player.startGliding();
               mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
            } else if (mc.player.isOnGround() && isElytraEquipped && !mc.player.isInFluid() && !mc.player.isGliding()) {
               mc.player.jump();
            }
         }

         if (mc.player.isGliding()) {
            if (this.fireworkTimer.finished((long)(this.fireworkDelay.getCurrentValue() * 1000.0F)) && !mc.player.isUsingItem()) {
               this.useFirework();
            }
         }
      }
   }

   private void useFirework() {
      SlotGroup<HotbarSlot> slotsToSearch = SlotGroups.hotbar();
      HotbarSlot slot = slotsToSearch.findItem(Items.FIREWORK_ROCKET);
      if (slot != null) {
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot.getSlotId()));
         mc.interactionManager
            .sendSequencedPacket(mc.world, sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
         this.fireworkTimer.reset();
      } else {
         SlotGroup<InventorySlot> search = SlotGroups.inventory();
         InventorySlot invSlot = search.findItem(Items.FIREWORK_ROCKET);
         if (invSlot != null) {
            InventoryUtility.hotbarSwap(invSlot.getIdForServer(), (int)(this.fireworkSlot.getCurrentValue() - 1.0F));
            this.fireworkTimer.reset();
         }
      }
   }
}
