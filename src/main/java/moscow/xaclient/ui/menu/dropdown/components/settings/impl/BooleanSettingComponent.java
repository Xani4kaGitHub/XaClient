package moscow.xaclient.ui.menu.dropdown.components.settings.impl;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomComponent;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.msdf.Font;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.framework.objects.MouseButton;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.modules.visuals.Interface;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.ui.menu.dropdown.components.settings.MenuSettingComponent;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.animation.types.ColorAnimation;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.game.cursor.CursorType;
import moscow.xaclient.utility.game.cursor.CursorUtility;
import moscow.xaclient.utility.gui.GuiUtility;

public class BooleanSettingComponent extends MenuSettingComponent<BooleanSetting> {
   private Animation circleOpacityAnimation;
   private Animation enableAnimation;
   private ColorAnimation backgroundColorAnimation;

   public BooleanSettingComponent(BooleanSetting setting, CustomComponent parent) {
      super(setting, parent);
   }

   @Override
   public void onInit() {
      this.circleOpacityAnimation = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);
      this.enableAnimation = new Animation(300L, Easing.BAKEK);
      this.backgroundColorAnimation = new ColorAnimation(300L, new ColorRGBA(24.0F, 24.0F, 27.0F), Easing.FIGMA_EASE_IN_OUT);
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
      this.circleOpacityAnimation.update(this.setting.isEnabled() ? 1.0F : 0.75F);
      this.enableAnimation.update(this.setting.isEnabled() ? 1.0F : 0.0F);
      this.backgroundColorAnimation
         .update(
            this.setting.isEnabled() ? new ColorRGBA(151.0F, 71.0F, 255.0F) : XaClient.getInstance().getThemeManager().getCurrentTheme().getAdditionalColor()
         );
      this.hoverAnimation.update(this.isHovered(context.getMouseX(), context.getMouseY()));
      if (this.isHovered(context.getMouseX(), context.getMouseY())) {
         CursorUtility.set(CursorType.HAND);
      }

      float checkWidth = 13.0F;
      float checkHeight = 8.0F;
      Font nameFont = Fonts.REGULAR.getFont(8.0F);
      float leftPadding = 10.0F;
      float nameHeight = nameFont.height();
      float headerHeight = 19.0F;
      context.drawFadeoutText(
         nameFont,
         Localizator.translate(this.setting.getName()),
         this.x + leftPadding,
         this.y + GuiUtility.getMiddleOfBox(nameFont.height(), headerHeight) - 0.5F,
         Colors.getTextColor().withAlpha(255.0F * (0.75F + 0.25F * this.enableAnimation.getValue() + 0.25F * this.hoverAnimation.getValue())),
         0.7F,
         0.99F,
         this.width - checkWidth - 20.0F
      );
      context.drawRoundedRect(
         this.x + this.width - checkWidth - 9.0F,
         this.y + 5.0F,
         checkWidth,
         checkHeight,
         BorderRadius.all(3.0F),
         this.backgroundColorAnimation.getColor().withAlpha(!this.setting.isEnabled() ? 255.0F - 100.0F * Interface.glass() : 255.0F)
      );
      context.drawRoundedRect(
         this.x + this.width - checkWidth - 8.0F + 5.0F * this.enableAnimation.getValue(),
         this.y + 6.0F,
         6.0F,
         6.0F,
         BorderRadius.all(4.0F),
         new ColorRGBA(255.0F, 255.0F, 255.0F).withAlpha(this.circleOpacityAnimation.getValue() * 255.0F)
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
         this.setting.toggle();
      }

      super.onMouseReleased(mouseX, mouseY, button);
   }

   @Override
   public float getHeight() {
      return this.height = 18.0F;
   }
}
