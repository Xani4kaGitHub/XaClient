package moscow.xaclient.systems.commands.commands;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.target.TargetManager;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class TargetCommand {
   @Compile
   public Command command() {
      return CommandBuilder.begin(
            "target",
            b -> b.aliases("targets")
               .desc("Управление списком таргетов")
               .param("action", p -> p.literal("add", "remove", "del", "delete", "clear", "list"))
               .param("id", p -> p.optional().validator(ValidationResult::ok))
               .handler(this::handle)
         )
         .build();
   }

   @Compile
   private void handle(CommandContext ctx) {
      String action = (String)ctx.arguments().get(0);
      String id = (String)ctx.arguments().get(1);
      TargetManager tm = XaClient.getInstance().getTargetManager();
      String var5 = action.toLowerCase();
      switch (var5) {
         case "add":
            tm.addTarget(id);
            break;
         case "remove":
         case "del":
         case "delete":
            tm.removeTarget(id);
            break;
         case "clear":
            tm.clearTarget();
            break;
         case "list":
            tm.listTarget();
      }
   }
}
