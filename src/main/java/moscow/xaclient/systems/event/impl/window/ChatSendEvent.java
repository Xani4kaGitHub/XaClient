package moscow.xaclient.systems.event.impl.window;

import lombok.Generated;
import moscow.xaclient.systems.event.EventCancellable;

public class ChatSendEvent extends EventCancellable {
   private String message;

   public ChatSendEvent(String message) {
      this.message = message;
   }

   @Generated
   public String getMessage() {
      return this.message;
   }

   @Generated
   public void setMessage(String message) {
      this.message = message;
   }
}
