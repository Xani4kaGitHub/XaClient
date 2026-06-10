package moscow.xaclient.systems.event.impl.render;

import lombok.Generated;
import moscow.xaclient.systems.event.EventCancellable;

// Событие для подмены соотношения сторон в матрице проекции мира.
public class AspectRatioEvent extends EventCancellable {
   private float ratio;

   // Возвращает соотношение сторон, заданное модулем.
   @Generated
   public float getRatio() {
      return this.ratio;
   }

   // Задаёт новое соотношение сторон для projection matrix.
   @Generated
   public void setRatio(float ratio) {
      this.ratio = ratio;
   }
}
