package moscow.xaclient.systems.capes;

import moscow.xaclient.XaClient;
import net.minecraft.util.Identifier;

public record Cape(String id, String displayKey, Identifier texture) {
   public static Cape of(String id, String displayKey, String texturePath) {
      return new Cape(id, displayKey, XaClient.id(texturePath));
   }
}
