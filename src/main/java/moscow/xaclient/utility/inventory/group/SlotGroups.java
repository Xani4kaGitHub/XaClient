package moscow.xaclient.utility.inventory.group;

import moscow.xaclient.utility.inventory.group.impl.ArmorSlotsGroup;
import moscow.xaclient.utility.inventory.group.impl.HotbarSlotsGroup;
import moscow.xaclient.utility.inventory.group.impl.InventorySlotsGroup;
import moscow.xaclient.utility.inventory.group.impl.OffhandSlotGroup;
import moscow.xaclient.utility.inventory.slots.ArmorSlot;
import moscow.xaclient.utility.inventory.slots.HotbarSlot;
import moscow.xaclient.utility.inventory.slots.InventorySlot;
import moscow.xaclient.utility.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
