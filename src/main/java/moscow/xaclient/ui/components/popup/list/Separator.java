package moscow.xaclient.ui.components.popup.list;

import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.ui.components.popup.PopupComponent;
import moscow.xaclient.utility.colors.Colors;

public class Separator extends PopupComponent {
   @Override
   protected void renderComponent(UIContext context) {
      context.drawRect(this.x, this.y, this.width, this.height, Colors.getSeparatorColor());
   }

   @Override
   public float getHeight() {
      return this.height = 4.0F;
   }
}
