package moscow.xaclient.ui.menu.dropdown.components.settings;

import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomComponent;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.setting.Setting;
import moscow.xaclient.ui.components.popup.Popup;
import moscow.xaclient.ui.menu.dropdown.DropDownScreen;
import moscow.xaclient.ui.menu.dropdown.components.module.ModuleComponent;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;

public abstract class MenuSettingComponent<T extends Setting> extends CustomComponent {
   private final CustomComponent parent;
   protected final T setting;
   private final Animation visibilityAnimation = new Animation(300L, Easing.BAKEK_PAGES);
   protected final Animation hoverAnimation = new Animation(300L, Easing.FIGMA_EASE_IN_OUT);

   public MenuSettingComponent(T setting, CustomComponent parent) {
      this.parent = parent;
      this.setting = setting;
   }

   @Override
   public void update(UIContext context) {
      String translatedDescription = Localizator.translateOrEmpty(this.setting.getDescription());
      if (this.parent instanceof ModuleComponent component
         && (component.getParent().isHovered(context) && this.isHovered(context) || XaClient.getInstance().getMenuScreen() instanceof DropDownScreen)) {
         ((DropDownScreen)XaClient.getInstance().getMenuScreen()).setDesc(Localizator.translate(translatedDescription));
      }

      if (this.parent instanceof Popup && this.isHovered(context)) {
         XaClient.getInstance().getHud().setDesc(Localizator.translate(translatedDescription));
      }

      super.update(context);
   }

   @Override
   public void onInit() {
      super.onInit();
   }

   public float getOpacity() {
      return this.visibilityAnimation.getValue();
   }

   public void drawRegular8(UIContext context) {
   }

   public void drawSplit(UIContext context) {
   }

   @Generated
   public CustomComponent getParent() {
      return this.parent;
   }

   @Generated
   public T getSetting() {
      return this.setting;
   }

   @Generated
   public Animation getVisibilityAnimation() {
      return this.visibilityAnimation;
   }

   @Generated
   public Animation getHoverAnimation() {
      return this.hoverAnimation;
   }
}
