package moscow.xaclient.framework.objects.gradient.impl;

import moscow.xaclient.framework.objects.gradient.Gradient;
import moscow.xaclient.utility.colors.ColorRGBA;

class DiagonalGradient extends Gradient {
   public DiagonalGradient(ColorRGBA startColor, ColorRGBA endColor) {
      super(startColor, endColor, endColor, startColor);
   }

   public DiagonalGradient rotate() {
      return new DiagonalGradient(this.topRightColor, this.bottomLeftColor);
   }
}
