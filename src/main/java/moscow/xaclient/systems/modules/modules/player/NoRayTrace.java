package moscow.xaclient.systems.modules.modules.player;

import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.systems.target.TargetSettings;
import net.minecraft.entity.Entity;

@ModuleInfo(name = "No Ray Trace", category = ModuleCategory.PLAYER, desc = "modules.descriptions.no_ray_trace")
public class NoRayTrace extends BaseModule {
   private final SelectSetting targets = new SelectSetting(this, "targets");
   private final SelectSetting.Value players = new SelectSetting.Value(this.targets, "players").select();
   private final SelectSetting.Value animals = new SelectSetting.Value(this.targets, "animals").select();
   private final SelectSetting.Value mobs = new SelectSetting.Value(this.targets, "mobs").select();
   private final SelectSetting.Value invisibles = new SelectSetting.Value(this.targets, "invisibles").select();
   private final SelectSetting.Value nakedPlayers = new SelectSetting.Value(this.targets, "nakedPlayers").select();
   private final SelectSetting.Value friends = new SelectSetting.Value(this.targets, "friends");
   private final SelectSetting.Value armorStands = new SelectSetting.Value(this.targets, "armorStands");

   public boolean shouldSkip(Entity entity) {
      TargetSettings settings = new TargetSettings.Builder()
         .targetPlayers(this.players.isSelected())
         .targetAnimals(this.animals.isSelected())
         .targetMobs(this.mobs.isSelected())
         .targetInvisibles(this.invisibles.isSelected())
         .targetNakedPlayers(this.nakedPlayers.isSelected())
         .targetFriends(this.friends.isSelected())
         .targetArmorStands(this.armorStands.isSelected())
         .build();
      return settings.isEntityValid(entity);
   }
}
