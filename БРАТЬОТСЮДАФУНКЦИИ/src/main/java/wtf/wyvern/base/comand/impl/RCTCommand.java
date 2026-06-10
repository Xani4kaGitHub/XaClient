package wtf.wyvern.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import wtf.wyvern.Wyvern;
import wtf.wyvern.base.comand.api.CommandAbstract;
import wtf.wyvern.base.notify.NotifyManager;
import wtf.wyvern.base.repository.RCTRepository;
import wtf.wyvern.utility.game.server.ServerHandler;
import wtf.wyvern.utility.interfaces.IClient;

public class RCTCommand extends CommandAbstract implements IClient {
   private final RCTRepository repository = Wyvern.getInstance().getRCTRepository();

   public RCTCommand() {
      super("rct");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.executes((context) -> {
         ServerHandler serverHandler = Wyvern.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("️[RCT]", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            this.repository.reconnect(serverHandler.getAnarchy());
            return 1;
         }
      });
      builder.then(CommandAbstract.arg("anarchy", IntegerArgumentType.integer(1, 63)).executes((context) -> {
         ServerHandler serverHandler = Wyvern.getInstance().getServerHandler();
         if (!serverHandler.isHolyWorld()) {
            NotifyManager.getInstance().addNotification("[RCT]", Text.literal(" Не работает на этом " + String.valueOf(Formatting.RED) + "сервере"));
            return 1;
         } else if (serverHandler.isPvp()) {
            NotifyManager.getInstance().addNotification("[RCT]️", Text.literal(" Вы находитесь в режиме " + String.valueOf(Formatting.RED) + "пвп"));
            return 1;
         } else {
            int anarchy = (Integer)context.getArgument("anarchy", Integer.class);
            this.repository.reconnect(anarchy);
            return 1;
         }
      }));
   }
}