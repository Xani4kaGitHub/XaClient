package moscow.xaclient.systems.modules.modules.other;

import moscow.xaclient.XaClient;
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

// Добавляет базовые улучшения чата: антиспам и суффикс сообщений.
@ModuleInfo(name = "Chat Helper", category = ModuleCategory.OTHER, desc = "modules.descriptions.chat_helper")
public class ChatHelper extends BaseModule {
   private final BooleanSetting antiSpam = new BooleanSetting(this, "modules.settings.chat_helper.anti_spam").enable();
   private final BooleanSetting suffixEnabled = new BooleanSetting(this, "modules.settings.chat_helper.suffix");
   private final StringSetting suffix = new StringSetting(this, "modules.settings.chat_helper.suffix_text", () -> !this.suffixEnabled.isEnabled()).text(" | XaClient");
   private String lastReceivedMessage = "";

   // Перед отправкой добавляет суффикс только к обычным сообщениям, не к командам.
   private final EventListener<ChatSendEvent> onChatSend = event -> {
      String message = event.getMessage();
      String commandPrefix = XaClient.getInstance().getCommandManager().getPrefix();
      if (!this.suffixEnabled.isEnabled() || message == null || message.isBlank() || message.startsWith("/") || message.startsWith(commandPrefix)) {
         return;
      }

      String suffixText = this.suffix.getText();
      if (suffixText != null && !suffixText.isBlank() && !message.endsWith(suffixText)) {
         event.setMessage(message + suffixText);
      }
   };

   // Отменяет повторяющиеся входящие сообщения подряд.
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

   // Сбрасывает последнее сообщение, чтобы после повторного включения антиспам не был липким.
   @Override
   public void onDisable() {
      this.lastReceivedMessage = "";
   }

   // Отправляет локальное сообщение от имени клиента.
   public static void sendClientMessage(String message) {
      if (mc.player != null) {
         mc.player.sendMessage(Text.literal("[XaClient] " + message), false);
      }
   }
}
