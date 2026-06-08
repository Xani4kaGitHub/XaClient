package moscow.xaclient.ui.mainmenu;

import lombok.Generated;
import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.UIContext;
import moscow.xaclient.framework.objects.BorderRadius;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.game.cursor.CursorType;
import moscow.xaclient.utility.game.cursor.CursorUtility;
import moscow.xaclient.utility.render.obj.Rect;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class CustomButton extends Rect {
   private final String icon;
   private final float iconSize;
   private final Runnable onClick;
   private final ColorRGBA backgroundColor = new ColorRGBA(58.0F, 58.0F, 58.0F);
   private final Animation activeAnim = new Animation(400L, 0.0F, Easing.BAKEK);
   private final Animation hoverAnim = new Animation(300L, 0.0F, Easing.FIGMA_EASE_IN_OUT);

   @Compile
   public void draw(UIContext context) {
      if (this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F) {
         CursorUtility.set(CursorType.HAND);
      }

      this.hoverAnim.update(this.hovered(context.getMouseX(), context.getMouseY()) && this.activeAnim.getValue() == 1.0F);
      context.drawRoundedRect(
         this.x,
         this.y,
         this.width,
         this.height,
         BorderRadius.all(Math.min(this.width, this.height) / 2.0F),
         this.backgroundColor.withAlpha(255.0F * (0.33F * this.activeAnim.getValue() + 0.2F * this.hoverAnim.getValue()))
      );
      context.drawTexture(
         XaClient.id(this.icon),
         this.x + (this.width - this.iconSize) / 2.0F,
         this.y + (this.height - this.iconSize) / 2.0F,
         this.iconSize,
         this.iconSize,
         ColorRGBA.WHITE.withAlpha(255.0F * this.activeAnim.getValue())
      );
   }

   @Compile
   public void click(double mouseX, double mouseY, int button) {
      if (this.hovered(mouseX, mouseY) && button == 0 && this.activeAnim.getValue() == 1.0F) {
         this.onClick.run();
      }
   }

   @Generated
   public CustomButton(String icon, float iconSize, Runnable onClick) {
      this.icon = icon;
      this.iconSize = iconSize;
      this.onClick = onClick;
   }

   @Generated
   public Animation getActiveAnim() {
      return this.activeAnim;
   }
}
