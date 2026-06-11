package moscow.xaclient.systems.modules.modules.visuals;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.custommodel.CustomModelType;
import moscow.xaclient.systems.custommodel.model.AbstractCustomModel;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "Custom Models", desc = "modules.descriptions.custom_models", category = ModuleCategory.VISUALS)
public class CustomModels extends BaseModule {
   private final ModeSetting model = new ModeSetting(this, "modules.settings.custom_models.model");
   private final BooleanSetting friends = new BooleanSetting(this, "modules.settings.custom_models.friends");

   public CustomModels() {
      for (CustomModelType type : CustomModelType.values()) {
         new ModeSetting.Value(this.model, type.getKey());
      }
   }

   private CustomModelType selected() {
      ModeSetting.Value value = this.model.getValue();
      if (value == null) {
         return null;
      }

      for (CustomModelType type : CustomModelType.values()) {
         if (type.getKey().equals(value.getName())) {
            return type;
         }
      }

      return null;
   }

   public boolean appliesTo(String playerName) {
      if (!this.isEnabled() || playerName == null) {
         return false;
      }

      if (playerName.equalsIgnoreCase(mc.getSession().getUsername())) {
         return true;
      }

      return this.friends.isEnabled() && XaClient.getInstance().getFriendManager().isFriend(playerName);
   }

   public AbstractCustomModel getModel() {
      CustomModelType type = this.selected();
      return type == null ? null : type.getModel();
   }

   public Identifier getTexture() {
      CustomModelType type = this.selected();
      return type == null ? null : type.getTexture();
   }
}
