package moscow.xaclient.systems.commands.commands;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.commands.Command;
import moscow.xaclient.systems.commands.CommandBuilder;
import moscow.xaclient.systems.commands.CommandContext;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.game.server.ServerUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;

public class ReHubCommand implements IMinecraft {
   private boolean processing;
   private final Timer timer = new Timer();
   private final EventListener<ClientPlayerTickEvent> onUpdateEvent = event -> {
      if (this.processing && mc.world != null && mc.player != null) {
         if ((ServerUtility.isFT() || ServerUtility.isFT()) && mc.world.getDifficulty() == Difficulty.EASY && this.timer.finished(1000L)) {
            mc.player.networkHandler.sendChatCommand("an" + ServerUtility.ftAn);
            this.timer.reset();
            this.processing = false;
         }
      }
   };

   public ReHubCommand() {
      XaClient.getInstance().getEventManager().subscribe(this);
   }

   public Command command() {
      return CommandBuilder.begin("rct").aliases("reconnect").desc("commands.rehub.description").handler(this::handle).build();
   }

   private void handle(CommandContext ctx) {
      if (mc.player != null && mc.world != null) {
         if (ServerUtility.hasCT) {
            MessageUtility.error(Text.of(Localizator.translate("commands_rehub.ct")));
         } else {
            this.timer.reset();
            mc.player.networkHandler.sendChatCommand("hub");
            this.processing = true;
         }
      }
   }
}
