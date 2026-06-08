package moscow.xaclient.ui.hud.impl.island;

import moscow.xaclient.systems.setting.settings.SelectSetting;

public class ExtandableStatus extends IslandStatus {
   public ExtandableStatus(SelectSetting setting, String name) {
      super(setting, name);
   }

   @Override
   public boolean canShow() {
      return false;
   }
}
