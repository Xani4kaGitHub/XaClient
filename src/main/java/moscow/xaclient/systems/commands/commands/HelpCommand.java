package moscow.xaclient.systems.commands.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.MessageUtility;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class HelpCommand {
   @Compile
   public Command command() {
      return CommandBuilder.begin("help", b -> b.aliases("помощь", "команды", "commands", "helpme").desc("commands.help.description").handler(this::handle))
         .build();
   }

   @Compile
   private void handle(CommandContext ctx) {
      List<Command> list = new ArrayList<>(XaClient.getInstance().getCommandManager().commands());
      list.sort(Comparator.comparing(c -> c.names().getFirst(), String.CASE_INSENSITIVE_ORDER));
      List<String> infos = new ArrayList<>();
      int counter = 1;

      for (Command command : list) {
         infos.add(
            String.format(
               "%d) %s%s - %s",
               counter++,
               XaClient.getInstance().getCommandManager().getPrefix(),
               command.names().getFirst(),
               Localizator.translate(command.description())
            )
         );
      }

      MessageUtility.info(Text.of("Доступные команды:\n" + String.join("\n", infos)));
   }
}
