package moscow.xaclient.systems.modules.modules.player;

import lombok.Generated;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SelectSetting;

@ModuleInfo(name = "No Push", category = ModuleCategory.PLAYER, desc = "Удаляет коллизию от внешних факторов")
public class NoPush extends BaseModule {
   private final SelectSetting removePushFrom = new SelectSetting(this, "Отключать для");
   private final SelectSetting.Value entities = new SelectSetting.Value(this.removePushFrom, "Энтити", "Предотвращает отталкивание от сущностей").select();
   private final SelectSetting.Value fluids = new SelectSetting.Value(this.removePushFrom, "Воды и лавы", "Предотвращает выталкивание из воды и лавы");
   private final SelectSetting.Value blocks = new SelectSetting.Value(this.removePushFrom, "Блоков", "Предотвращает отталкивание из блоков").select();

   @Generated
   public SelectSetting getRemovePushFrom() {
      return this.removePushFrom;
   }

   @Generated
   public SelectSetting.Value getEntities() {
      return this.entities;
   }

   @Generated
   public SelectSetting.Value getFluids() {
      return this.fluids;
   }

   @Generated
   public SelectSetting.Value getBlocks() {
      return this.blocks;
   }
}
