package moscow.xaclient.systems.event.impl.window;

import lombok.Generated;
import moscow.xaclient.systems.event.EventCancellable;

// Событие перед отправкой сообщения в чат, позволяет модулям изменить текст.
public class ChatSendEvent extends EventCancellable {
   private String message;

   // Сохраняет исходное сообщение из ChatScreen.
   public ChatSendEvent(String message) {
      this.message = message;
   }

   // Возвращает текущий текст сообщения после возможных изменений.
   @Generated
   public String getMessage() {
      return this.message;
   }

   // Заменяет текст, который будет отправлен в чат.
   @Generated
   public void setMessage(String message) {
      this.message = message;
   }
}
