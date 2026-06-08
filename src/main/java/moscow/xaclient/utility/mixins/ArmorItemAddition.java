package moscow.xaclient.utility.mixins;

import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;

public interface ArmorItemAddition {
   EquipmentType xaclient$getType();

   ArmorMaterial xaclient$getMaterial();
}
