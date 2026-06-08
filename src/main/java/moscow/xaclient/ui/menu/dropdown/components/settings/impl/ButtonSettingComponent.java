package moscow.xaclient.ui.menu.dropdown.components.settings.impl;

import moscow.xaclient.framework.base.CustomComponent;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.msdf.Font;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.framework.objects.MouseButton;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.setting.settings.ButtonSetting;
import moscow.xaclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.cursor.CursorType;
import moscow.xaclient.utility.game.cursor.CursorUtility;
import moscow.xaclient.utility.gui.GuiUtility;

public class ButtonSettingComponent extends MenuSettingComponent<ButtonSetting> {
   public ButtonSettingComponent(ButtonSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.width = 13.0F;
      this.height = 8.0F;
      super.onInit();
   }

   @Override
   public void update(UIContext context) {
      super.update(context);
   }

   @Override
   protected void renderComponent(UIContext context) {
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      context.drawRoundedRect(
         this.x + 7.0F,
         this.y + 4.0F,
         this.width - 14.0F,
         this.height - 7.0F,
         BorderRadius.all(6.0F),
         Colors.getBackgroundColor().withAlpha(255.0F * (0.3F + 0.2F * this.hoverAnimation.getValue()))
      );
      context.drawCenteredText(
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + this.width / 2.0F,
         this.y + GuiUtility.getMiddleOfBox(nameFont.height(), this.height) - 0.5F,
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.hoverAnimation.getValue()))
      );
   }

   @Override
   public void drawRegular8(UIContext context) {
   }

   @Override
   public void drawSplit(UIContext context) {
      float separatorHeight = 0.5F;
      context.drawRect(this.x, this.y + this.height, this.width, separatorHeight, Colors.getTextColor().withAlpha(5.1F));
   }

   @Override
   public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {
      if (this.isHovered(mouseX, mouseY) && button == MouseButton.LEFT) {
         this.setting.getAction().run();
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      return this.height = 24.0F;
   }
}
