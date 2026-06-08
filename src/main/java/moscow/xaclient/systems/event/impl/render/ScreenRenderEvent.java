package moscow.xaclient.systems.event.impl.render;

import lombok.Generated;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.systems.event.Event;

public class ScreenRenderEvent extends Event {
   private final CustomDrawContext context;
   private final int mouseX;
   private final int mouseY;
   private final float tickDelta;

   @Generated
   public CustomDrawContext getContext() {
      return this.context;
   }

   @Generated
   public int getMouseX() {
      return this.mouseX;
   }

   @Generated
   public int getMouseY() {
      return this.mouseY;
   }

   @Generated
   public float getTickDelta() {
      return this.tickDelta;
   }

   @Generated
   public ScreenRenderEvent(CustomDrawContext context, int mouseX, int mouseY, float tickDelta) {
      this.context = context;
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.tickDelta = tickDelta;
   }
}
