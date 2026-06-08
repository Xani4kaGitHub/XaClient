package moscow.xaclient.ui.hud.impl.island;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Generated;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.systems.setting.settings.SelectSetting;
import moscow.xaclient.utility.animation.base.Animation;
import moscow.xaclient.utility.animation.base.Easing;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.colors.Colors;
import moscow.xaclient.utility.interfaces.IScaledResolution;

public abstract class IslandStatus extends SelectSetting.Value implements IScaledResolution {
   protected IslandSize size = new IslandSize(48.0F, 15.0F);
   protected final Animation animation = new Animation(500L, Easing.BAKEK_SIZE);

   public IslandStatus(SelectSetting parent, String name) {
      super(parent, "hud.dynamic_island.statuses." + name);
      this.select();
   }

   public void draw(CustomDrawContext context) {
   }

   public void drawWithAlpha(CustomDrawContext context) {
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.animation.getValue());
      this.draw(context);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public void click(float mouseX, float mouseY, int button) {
   }

   public abstract boolean canShow();

   public ColorRGBA getColor() {
      return Colors.getBackgroundColor();
   }

   @Generated
   public IslandSize getSize() {
      return this.size;
   }

   @Generated
   public Animation getAnimation() {
      return this.animation;
   }
}
