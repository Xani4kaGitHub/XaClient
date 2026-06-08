package moscow.xaclient.framework.objects.gradient.impl;

import moscow.xaclient.framework.objects.gradient.Gradient;
import moscow.xaclient.utility.colors.ColorRGBA;

public class HorizontalGradient extends Gradient {
   public HorizontalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, startColor, endColor, endColor);
   }

   public HorizontalGradient rotate() {
      return new HorizontalGradient(this.bottomRightColor, this.topLeftColor);
   }
}
