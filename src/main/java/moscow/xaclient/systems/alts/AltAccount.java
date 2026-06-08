package moscow.xaclient.systems.alts;

import java.util.UUID;
import lombok.Generated;

public class AltAccount {
   private final String username;
   private final UUID uuid;

   public AltAccount(String username, UUID uuid) {
      this.username = username;
      this.uuid = uuid;
   }

   @Generated
   public String getUsername() {
      return this.username;
   }

   @Generated
   public UUID getUuid() {
      return this.uuid;
   }
}
