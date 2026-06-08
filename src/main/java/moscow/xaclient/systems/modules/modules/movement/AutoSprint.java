package moscow.xaclient.systems.modules.modules.movement;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;

@ModuleInfo(name = "Auto Sprint", category = ModuleCategory.MOVEMENT, enabledByDefault = true)
public class AutoSprint extends BaseModule {
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> mc.options.sprintKey.setPressed(true);
}
