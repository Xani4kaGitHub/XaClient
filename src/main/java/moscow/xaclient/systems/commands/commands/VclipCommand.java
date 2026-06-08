package moscow.xaclient.systems.commands.commands;

import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class VclipCommand implements IMinecraft {
   private Vec3d target;

   @Compile
   public Command command() {
      return CommandBuilder.begin("vclip", b -> b.aliases("v", "verticalclip").desc("commands.vclip.description").param("distance", p -> p.validator(text -> {
         try {
            return ValidationResult.ok(Double.parseDouble(text));
         } catch (NumberFormatException var2) {
            return ValidationResult.error(Localizator.translate("commands.vclip.invalid"));
         }
      })).handler(this::handle)).build();
   }

   @Compile
   private void handle(CommandContext ctx) {
      double distance = (Double)ctx.arguments().getFirst();
      MinecraftClient mc = MinecraftClient.getInstance();
      Vec3d pos = mc.player.getPos();
      mc.player.setPosition(pos.add(0.0, distance, 0.0));
   }
}
