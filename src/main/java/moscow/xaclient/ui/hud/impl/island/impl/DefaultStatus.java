package moscow.xaclient.ui.hud.impl.island.impl;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.ui.hud.impl.island.DynamicIsland;
import moscow.xaclient.ui.hud.impl.island.IslandStatus;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;

public class DefaultStatus extends IslandStatus {
   public DefaultStatus(SelectSetting setting) {
      super(setting, "default");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicIsland island = XaClient.getInstance().getHud().getIsland();
      float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
      float y = 7.0F;
      String name = XaClient.NAME;
      float width = this.size.width = 20.0F + Fonts.MEDIUM.getFont(7.0F).width(name);
      float height = this.size.height = 15.0F;
      context.drawRoundedRect(x - 6.0F + 10.0F * this.animation.getValue(), y + 4.0F, 7.0F, 7.0F, BorderRadius.all(3.0F), new ColorRGBA(115.0F, 0.0F, 255.0F));
      context.drawText(Fonts.MEDIUM.getFont(7.0F), name, x + 25.0F - 10.0F * this.animation.getValue(), y + 5.0F, Colors.getTextColor());
   }

   @Override
   public boolean canShow() {
      return true;
   }
}
