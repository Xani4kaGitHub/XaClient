package moscow.xaclient.systems.modules.modules.visuals;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.AspectRatioEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;

@ModuleInfo(name = "Aspect Ratio", category = ModuleCategory.VISUALS, desc = "Changes the world projection aspect ratio")
public class AspectRatio extends BaseModule {
   private final SliderSetting ratio = new SliderSetting(this, "Ratio").min(0.5F).max(4.0F).step(0.01F).currentValue(1.78F);

   private final EventListener<AspectRatioEvent> onAspectRatio = event -> {
      event.setRatio(this.ratio.getCurrentValue());
      event.cancel();
   };
}
