package moscow.xaclient.systems.modules.modules.visuals;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.capes.Cape;
import moscow.xaclient.systems.capes.CapeRegistry;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "Capes", desc = "modules.descriptions.capes", category = ModuleCategory.VISUALS)
public class Capes extends BaseModule {
   private final ModeSetting cape = new ModeSetting(this, "modules.settings.capes.cape");
   private final BooleanSetting self = new BooleanSetting(this, "modules.settings.capes.self").enable();
   private final BooleanSetting friends = new BooleanSetting(this, "modules.settings.capes.friends");
   private final ModeSetting friendsCape = new ModeSetting(this, "modules.settings.capes.friends_cape", () -> !this.friends.isEnabled());

   public Capes() {
      for (Cape value : CapeRegistry.getCapes()) {
         new ModeSetting.Value(this.cape, value.displayKey());
         new ModeSetting.Value(this.friendsCape, value.displayKey());
      }
   }

   public Identifier getCapeFor(String playerName) {
      if (!this.isEnabled() || playerName == null) {
         return null;
      }

      boolean isSelf = playerName.equalsIgnoreCase(mc.getSession().getUsername());
      boolean isFriend = XaClient.getInstance().getFriendManager().isFriend(playerName);
      if (isSelf) {
         return this.self.isEnabled() ? this.textureFor(this.cape) : null;
      }

      if (isFriend && this.friends.isEnabled()) {
         return this.textureFor(this.friendsCape);
      }

      return null;
   }

   private Identifier textureFor(ModeSetting setting) {
      ModeSetting.Value selected = setting.getValue();
      if (selected == null) {
         return null;
      }

      for (Cape value : CapeRegistry.getCapes()) {
         if (value.displayKey().equals(selected.getName())) {
            return value.texture();
         }
      }

      return null;
   }
}
