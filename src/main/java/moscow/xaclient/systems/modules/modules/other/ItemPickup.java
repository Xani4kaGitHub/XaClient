package moscow.xaclient.systems.modules.modules.other;

import java.util.Map;
import java.util.Map.Entry;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.game.PickupEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.notifications.NotificationType;
import moscow.xaclient.utility.game.ItemUtility;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

@ModuleInfo(name = "Item Pickup", category = ModuleCategory.OTHER, enabledByDefault = true, desc = "Уведомляет вас при поднятии донатного предмета")
public class ItemPickup extends BaseModule {
   private final Map<String, String> don = Map.of(
      "krush-helmet",
      "Вы подобрали Шлем крушителя!",
      "krush-chestplate",
      "Вы подобрали Нагрудник крушителя!",
      "krush-leggings",
      "Вы подобрали Поножи крушителя!",
      "krush-boots",
      "Вы подобрали Ботинки крушителя!",
      "krush-sword",
      "Вы подобрали донатный предмет: Меч крушителя"
   );
   private final EventListener<PickupEvent> onPickupEvent = event -> {
      ItemStack stack = event.getItemStack();

      for (Entry<String, String> entry : this.don.entrySet()) {
         if (ItemUtility.checkDonItem(stack, entry.getKey())) {
            MessageUtility.info(Text.of(entry.getValue()));
            return;
         }
      }

      if (ItemUtility.isDonItem(stack)) {
         String name = stack.getName().getString();
         XaClient.getInstance()
            .getNotificationManager()
            .addNotificationOther(NotificationType.INFO, "Донатный предмет", "Вы подобрали донатный предмет: " + name);
      }
   };
}
