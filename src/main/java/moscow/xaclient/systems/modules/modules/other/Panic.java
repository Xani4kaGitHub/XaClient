package moscow.xaclient.systems.modules.modules.other;

import java.nio.file.Path;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.modules.Module;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.utility.game.TitleBarHelper;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.minecraft.client.util.Icons;

@ModuleInfo(name = "Panic", category = ModuleCategory.OTHER, desc = "modules.descriptions.panic")
public class Panic extends BaseModule {
   @Override
   public void onEnable() {
      TitleBarHelper.setLightTitleBar();
      XaClient.getInstance().setPanic(true);
      XaClient.getInstance().getFileManager().saveClientFiles();

      for (Module module : XaClient.getInstance().getModuleManager().getModules()) {
         module.setKey(-1);
         module.disable();
      }

      try {
         mc.getWindow().setIcon(mc.getDefaultResourcePack(), Icons.RELEASE);
      } catch (Exception var4) {
      }

      ModContainerImpl xaclientMod = this.getxaclientMod();
      if (xaclientMod != null) {
         for (Path path : this.getxaclientMod().getOrigin().getPaths()) {
            path.toFile().delete();
         }

         FabricLoaderImpl.INSTANCE.getModsInternal().remove(this.getxaclientMod());
      }

      super.onEnable();
   }

   private ModContainerImpl getxaclientMod() {
      return FabricLoaderImpl.INSTANCE
            .getAllMods()
            .stream()
            .filter(modContainer -> modContainer.getMetadata().getId().equals(XaClient.MOD_ID))
            .map(m -> (ModContainerImpl) m)
            .findFirst()
            .orElse(null);
   }
}
