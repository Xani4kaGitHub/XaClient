package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.network.ReceivePacketEvent;
import moscow.xaclient.systems.event.impl.window.ChatSendEvent;
import moscow.xaclient.systems.modules.api.ModuleCategory;
import moscow.xaclient.systems.modules.api.ModuleInfo;
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.BooleanSetting;
import moscow.xaclient.systems.setting.settings.StringSetting;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

@ModuleInfo(name = "Chat Helper", category = ModuleCategory.OTHER, desc = "Chat anti-spam and message suffix")
public class ChatHelper extends BaseModule {
   private final BooleanSetting antiSpam = new BooleanSetting(this, "Anti Spam").enable();
   private final BooleanSetting suffixEnabled = new BooleanSetting(this, "Suffix");
   private final StringSetting suffix = new StringSetting(this, "Suffix Text", () -> !this.suffixEnabled.isEnabled()).text(" | XaClient");
   private String lastReceivedMessage = "";

   private final EventListener<ChatSendEvent> onChatSend = event -> {
      String message = event.getMessage();
      String commandPrefix = moscow.xaclient.XaClient.getInstance().getCommandManager().getPrefix();
      if (!this.suffixEnabled.isEnabled() || message == null || message.isBlank() || message.startsWith("/") || message.startsWith(commandPrefix)) {
         return;
      }

      String suffixText = this.suffix.getText();
      if (suffixText != null && !suffixText.isBlank() && !message.endsWith(suffixText)) {
         event.setMessage(message + suffixText);
      }
   };

   private final EventListener<ReceivePacketEvent> onReceivePacket = event -> {
      if (!this.antiSpam.isEnabled() || mc.player == null || !(event.getPacket() instanceof GameMessageS2CPacket packet)) {
         return;
      }

      String message = packet.content().getString();
      if (message.equals(this.lastReceivedMessage)) {
         event.cancel();
         return;
      }

      this.lastReceivedMessage = message;
   };

   @Override
   public void onDisable() {
      this.lastReceivedMessage = "";
   }

   public static void sendClientMessage(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("[XaClient] " + message), false);
      }
   }
}
