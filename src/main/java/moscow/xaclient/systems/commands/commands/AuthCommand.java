package moscow.xaclient.systems.commands.commands;

import java.util.Map;
import java.util.Map.Entry;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.systems.modules.modules.other.AutoAuth;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class AuthCommand {
   @Compile
   public Command command() {
      return CommandBuilder.begin("auth", b -> b.aliases("autoAuth", "пароли", "passwords").desc("commands.auth.description").handler(this::handle)).build();
   }

   @Compile
   private void handle(CommandContext ctx) {
      Map<String, String> map = XaClient.getInstance().getModuleManager().getModule(AutoAuth.class).listPassword();
      int counter = 1;
      if (map.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.auth.empty")));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.auth.passwords")));

         for (Entry<String, String> entry : map.entrySet()) {
            String nickname = entry.getKey();
            String password = entry.getValue();
            MessageUtility.info(Text.of(counter++ + ") Ник: " + nickname + " | Пароль: " + password));
         }
      }
   }
}
