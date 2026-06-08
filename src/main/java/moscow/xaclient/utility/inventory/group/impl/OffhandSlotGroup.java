package moscow.xaclient.utility.inventory.group.impl;

import java.util.List;
import moscow.xaclient.utility.inventory.group.SlotGroup;
import moscow.xaclient.utility.inventory.slots.OffhandSlot;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
