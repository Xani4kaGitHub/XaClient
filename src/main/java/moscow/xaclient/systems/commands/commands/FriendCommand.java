package moscow.xaclient.systems.commands.commands;

import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.commands.ValidationResult;
import moscow.xaclient.systems.friends.FriendManager;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class FriendCommand {
   @Compile
   public Command command() {
      return CommandBuilder.begin(
            "friend",
            b -> b.aliases("friends")
               .desc("commands.friends.description")
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
      FriendManager fm = XaClient.getInstance().getFriendManager();
      String var5 = action.toLowerCase();
      switch (var5) {
         case "add":
            fm.add(id);
            break;
         case "remove":
         case "del":
         case "delete":
            fm.remove(id);
            break;
         case "clear":
            fm.clear();
            break;
         case "list":
            this.printList();
      }
   }

   @Compile
   private void printList() {
      List<String> friends = XaClient.getInstance().getFriendManager().listFriends();
      if (friends.isEmpty()) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.list")));

         for (int i = 0; i < friends.size(); i++) {
            MessageUtility.info(Text.of("[" + (i + 1) + "] " + friends.get(i)));
         }
      }
   }
}
