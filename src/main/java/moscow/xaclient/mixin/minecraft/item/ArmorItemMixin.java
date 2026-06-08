package moscow.xaclient.mixin.minecraft.item;

import moscow.xaclient.utility.mixins.ArmorItemAddition;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorItem.class)
public abstract class ArmorItemMixin implements ArmorItemAddition {
   @Unique
   private EquipmentType xaclient$type;
   @Unique
   private ArmorMaterial xaclient$material;

   @Inject(method = "<init>", at = @At("TAIL"))
   public void saveArgs(ArmorMaterial material, EquipmentType type, Settings settings, CallbackInfo ci) {
      this.xaclient$type = type;
      this.xaclient$material = material;
   }

   @Override
   public ArmorMaterial xaclient$getMaterial() {
      return this.xaclient$material;
   }

   @Override
   public EquipmentType xaclient$getType() {
      return this.xaclient$type;
   }
}
