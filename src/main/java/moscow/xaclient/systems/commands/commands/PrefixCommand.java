package moscow.xaclient.systems.commands.commands;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.commands.CommandRegistry;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class PrefixCommand {
   @Compile
   public Command command() {
      return CommandBuilder.begin(
            "prefix",
            b -> b.desc("commands.prefix.description")
               .param("action", p -> p.optional().literal("list", "clear", "default", "set", "create"))
               .param(
                  "new",
                  p -> p.optional()
                     .validator(
                        text -> (ValidationResult)(text.length() > 1
                           ? ValidationResult.error(Localizator.translate("commands.prefix.invalid_length"))
                           : ValidationResult.ok(text))
                     )
               )
               .handler(this::handle)
         )
         .build();
   }

   @Compile
   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String newPrefix = (String)ctx.arguments().get(1);
      CommandRegistry registry = XaClient.getInstance().getCommandManager();
      String current = registry.getPrefix();
      if (action == null) {
         MessageUtility.info(Text.of(Localizator.translate("commands.prefix.current", current)));
      } else {
         String var6 = action.toLowerCase();
         switch (var6) {
            case "list":
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.current", current)));
               break;
            case "clear":
            case "default":
            case "reset":
               registry.setPrefix(".");
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.reset")));
               break;
            case "set":
            case "create":
               if (newPrefix == null || newPrefix.isEmpty()) {
                  MessageUtility.error(Text.of(Localizator.translate("commands.prefix.empty")));
                  return;
               }

               registry.setPrefix(newPrefix);
               MessageUtility.info(Text.of(Localizator.translate("commands.prefix.set", newPrefix)));
         }
      }
   }
}
