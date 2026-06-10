package moscow.xaclient.systems.modules.modules.visuals;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.AspectRatioEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;

// Меняет соотношение сторон камеры через матрицу проекции мира.
@ModuleInfo(name = "Aspect Ratio", category = ModuleCategory.VISUALS, desc = "modules.descriptions.aspect_ratio")
public class AspectRatio extends BaseModule {
   private final SliderSetting ratio = new SliderSetting(this, "modules.settings.aspect_ratio.ratio").min(0.5F).max(4.0F).step(0.01F).currentValue(1.78F);

   // Передаёт выбранное соотношение сторон в mixin рендера.
   private final EventListener<AspectRatioEvent> onAspectRatio = event -> {
      event.setRatio(this.ratio.getCurrentValue());
      event.cancel();
   };
}
