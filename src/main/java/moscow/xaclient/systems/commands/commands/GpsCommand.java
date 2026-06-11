package moscow.xaclient.systems.commands.commands;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.msdf.Fonts;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.render.HudRenderEvent;
import moscow.xaclient.utility.colors.ColorRGBA;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class GpsCommand implements IMinecraft {
   private static Vec3d target;

   private final EventListener<HudRenderEvent> onHudRender = event -> {
      if (target == null || !EntityUtility.isInGame()) {
         return;
      }

      double dx = target.x - mc.player.getX();
      double dz = target.z - mc.player.getZ();
      double distance = Math.sqrt(dx * dx + dz * dz);
      float angleToTarget = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float relative = MathHelper.wrapDegrees(angleToTarget - mc.player.getYaw());

      float screenWidth = mc.getWindow().getScaledWidth();
      float screenHeight = mc.getWindow().getScaledHeight();
      float cx = screenWidth / 2.0F;
      float cy = screenHeight / 2.0F - 30.0F;
      float size = 22.0F;
      ColorRGBA color = ColorRGBA.WHITE;

      MatrixStack ms = event.getContext().getMatrices();
      ms.push();
      ms.translate(cx, cy, 0.0F);
      ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(relative));
      ms.translate(-cx, -cy, 0.0F);
      event.getContext().drawTexture(XaClient.id("textures/arrow.png"), cx - size / 2.0F, cy - size / 2.0F, size, size, color);
      ms.pop();

      String label = String.format("%.0fm", distance);
      float textWidth = Fonts.MEDIUM.getFont(9.0F).width(label);
      event.getContext().drawText(Fonts.MEDIUM.getFont(9.0F), label, cx - textWidth / 2.0F, cy + size / 2.0F + 2.0F, ColorRGBA.WHITE);
   };

   public GpsCommand() {
      XaClient.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("gps")
         .desc("commands.gps.description")
         .param("x", p -> p.validator(this::verify))
         .param("y", p -> p.optional().validator(this::verify))
         .param("z", p -> p.optional().validator(this::verify))
         .handler(this::handle)
         .build();
   }

   private ValidationResult verify(String input) {
      if (input.equalsIgnoreCase("off") || input.equalsIgnoreCase("clear") || input.equalsIgnoreCase("reset")) {
         return ValidationResult.ok(input);
      }

      try {
         Double.parseDouble(input);
         return ValidationResult.ok(input);
      } catch (NumberFormatException var3) {
         return ValidationResult.error("commands.gps.invalid");
      }
   }

   private void handle(CommandContext ctx) {
      String x = (String)ctx.arguments().get(0);
      String y = (String)ctx.arguments().get(1);
      String z = (String)ctx.arguments().get(2);

      if (x.equalsIgnoreCase("off") || x.equalsIgnoreCase("clear") || x.equalsIgnoreCase("reset")) {
         target = null;
         MessageUtility.info(Text.of(moscow.xaclient.systems.localization.Localizator.translate("commands.gps.cleared")));
         return;
      }

      try {
         double px = Double.parseDouble(x);
         double py;
         double pz;
         if (z != null) {
            py = Double.parseDouble(y);
            pz = Double.parseDouble(z);
         } else if (y != null) {
            py = mc.player != null ? mc.player.getY() : 0.0;
            pz = Double.parseDouble(y);
         } else {
            MessageUtility.error(Text.of(moscow.xaclient.systems.localization.Localizator.translate("commands.gps.usage")));
            return;
         }

         target = new Vec3d(px, py, pz);
         MessageUtility.info(Text.of(moscow.xaclient.systems.localization.Localizator.translate("commands.gps.set", (int)px, (int)py, (int)pz)));
      } catch (NumberFormatException var10) {
         MessageUtility.error(Text.of(moscow.xaclient.systems.localization.Localizator.translate("commands.gps.invalid")));
      }
   }
}
