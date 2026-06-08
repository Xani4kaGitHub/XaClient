package moscow.xaclient.ui.hud.inline.impl;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.ui.hud.inline.InlineElement;
import moscow.xaclient.ui.hud.inline.InlineValue;
import moscow.xaclient.utility.game.TextUtility;
import moscow.xaclient.utility.game.server.ServerUtility;

public class WorldElement extends InlineElement {
   private final InlineValue cords = new InlineValue(this.elements, "coords");
   private final InlineValue server = new InlineValue(this.elements, "server");
   private final InlineValue tps = new InlineValue(this.elements, "TPS", "TPS");
   private final BooleanSetting shortName = new BooleanSetting(this, "hud.world.compact_server").enable();

   public WorldElement() {
      super("hud.world", "icons/hud/world.png");
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
      this.cords.update(String.format("%s %s %s", Math.round(mc.player.getX()), Math.round(mc.player.getY()), Math.round(mc.player.getZ())));
      this.server.update(ServerUtility.getServerName(this.shortName.isEnabled()), ServerUtility.getIP());
      this.tps.update(TextUtility.formatNumber(XaClient.getInstance().getTpsHandler().getTPS()).replace(",", ".").replace(".0", ""));
   }
}
