package moscow.xaclient.systems.modules.modules.visuals;

import lombok.Generated;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Extra Tab", category = ModuleCategory.VISUALS, desc = "modules.descriptions.extra_tab")
public class ExtraTab extends BaseModule {
   private final SliderSetting limit = new SliderSetting(this, "modules.settings.extra_tab.limit")
      .min(80.0F)
      .max(300.0F)
      .step(10.0F)
      .currentValue(200.0F);

   public int getEntryLimit() {
      return (int)this.limit.getCurrentValue();
   }

   @Generated
   public SliderSetting getLimit() {
      return this.limit;
   }
}
