package moscow.xaclient.systems.modules.modules.visuals;

import lombok.Generated;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SliderSetting;
import moscow.xaclient.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "Anti Invisible", category = ModuleCategory.VISUALS)
public class AntiInvisible extends BaseModule {
   private final SliderSetting opacity = new SliderSetting(this, "modules.settings.anti_invisible.opacity")
      .min(10.0F)
      .max(100.0F)
      .step(1.0F)
      .currentValue(50.0F)
      .suffix(number -> "%");

   public boolean shouldModifyOpacity(EntityRenderState renderState) {
      Entity entity = ((EntityRenderStateAddition)renderState).xaclient$getEntity();
      return entity.isInvisible();
   }

   @Generated
   public SliderSetting getOpacity() {
      return this.opacity;
   }
}
