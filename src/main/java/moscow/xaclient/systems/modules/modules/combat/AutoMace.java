package moscow.xaclient.systems.modules.modules.combat;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.SendPacketEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.utility.game.CombatUtility;
import moscow.xaclient.utility.inventory.slots.HotbarSlot;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

@ModuleInfo(name = "Auto Mace", category = ModuleCategory.COMBAT, desc = "modules.descriptions.auto_mace")
public class AutoMace extends BaseModule {
   private boolean spoofingAttack;
   private final EventListener<SendPacketEvent> onSendPacket = event -> {
      if (this.spoofingAttack || mc.player == null || mc.world == null || mc.player.networkHandler == null) {
         return;
      }

      if (!(event.getPacket() instanceof PlayerInteractEntityC2SPacket packet)) {
         return;
      }

      HotbarSlot maceSlot = CombatUtility.getMace();
      if (maceSlot == null) {
         return;
      }

      int currentSlot = mc.player.getInventory().selectedSlot;
      if (maceSlot.getSlotId() == currentSlot) {
         return;
      }

      event.cancel();
      this.spoofingAttack = true;

      try {
         mc.player.getInventory().selectedSlot = maceSlot.getSlotId();
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceSlot.getSlotId()));
         mc.player.networkHandler.sendPacket(packet);
      } finally {
         mc.player.getInventory().selectedSlot = currentSlot;
         mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
         this.spoofingAttack = false;
      }
   };

   @Override
   public void onDisable() {
      this.spoofingAttack = false;
      super.onDisable();
   }
}
