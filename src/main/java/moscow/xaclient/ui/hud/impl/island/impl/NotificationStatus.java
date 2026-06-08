package moscow.xaclient.ui.hud.impl.island.impl;

import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.systems.notifications.Notification;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.ui.hud.impl.island.DynamicIsland;
import moscow.xaclient.ui.hud.impl.island.IslandStatus;
import moscow.xaclient.utility.colors.Colors;

public class NotificationStatus extends IslandStatus {
   public NotificationStatus(SelectSetting setting) {
      super(setting, "alerts");
   }

   @Override
   public void draw(CustomDrawContext context) {
      DynamicIsland island = XaClient.getInstance().getHud().getIsland();
      List<Notification> notifications = XaClient.getInstance().getNotificationManager().getNotifications();
      if (!notifications.isEmpty()) {
         Notification active = notifications.getLast();
         float x = sr.getScaledWidth() / 2.0F - island.getSize().width / 2.0F;
         float y = 7.0F;
         float width = this.size.width = 18.0F + Fonts.MEDIUM.getFont(7.0F).width(active.getText());
         float height = this.size.height = 15.0F;

         for (Notification notification : notifications) {
            notification.getShowing().setDuration(500L);
            notification.getShowing().update(active == notification);
            context.drawRoundedRect(
               x - 6.0F + 10.0F * this.animation.getValue() * notification.getShowing().getValue(),
               y + 4.0F,
               7.0F,
               7.0F,
               BorderRadius.all(3.0F),
               notification.getType().getColor().withAlpha(255.0F * notification.getShowing().getValue())
            );
            context.drawText(
               Fonts.MEDIUM.getFont(7.0F),
               notification.getText(),
               x + 25.0F - 10.0F * this.animation.getValue() * notification.getShowing().getValue(),
               y + 5.0F,
               Colors.getTextColor().withAlpha(255.0F * notification.getShowing().getValue())
            );
         }
      }
   }

   @Override
   public boolean canShow() {
      List<Notification> notifications = XaClient.getInstance().getNotificationManager().getNotifications();
      return !notifications.isEmpty() && !notifications.getLast().getTimer().finished(notifications.getLast().getDuration());
   }
}
