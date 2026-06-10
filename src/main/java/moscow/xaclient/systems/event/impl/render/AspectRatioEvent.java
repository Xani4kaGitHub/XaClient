package moscow.xaclient.systems.event.impl.render;

import lombok.Generated;
import moscow.xaclient.systems.event.EventCancellable;

public class AspectRatioEvent extends EventCancellable {
   private float ratio;

   @Generated
   public float getRatio() {
      return this.ratio;
   }

   @Generated
   public void setRatio(float ratio) {
      this.ratio = ratio;
   }
}
