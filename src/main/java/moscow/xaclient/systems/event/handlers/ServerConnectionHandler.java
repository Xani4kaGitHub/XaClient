package moscow.xaclient.systems.event.handlers;

// import com.viaversion.viafabricplus.ViaFabricPlus;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.ServerConnectionEvent;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

public class ServerConnectionHandler implements IMinecraft {
   private boolean messageSent = false;
   private boolean connected;
   private final EventListener<ServerConnectionEvent> onServerConnection = event -> {
      this.connected = true;
      this.messageSent = false;
   };
   private final EventListener<ClientPlayerTickEvent> onClientPlayerTick = event -> {
      if (this.connected
            && !this.messageSent
            && mc.player != null
            && mc.player.age > 100
            && mc.getCurrentServerEntry() != null
            && FabricLoader.getInstance().isModLoaded("viafabricplus")) {
         // String warning = Localizator.translate("chat.connection_warning",
         // mc.getCurrentServerEntry().address,
         // ViaFabricPlus.getImpl().getTargetVersion());
         // MessageUtility.info(Text.of(warning));
         // this.messageSent = true;
      }
   };

   public ServerConnectionHandler() {
      XaClient.getInstance().getEventManager().subscribe(this);
   }
}
