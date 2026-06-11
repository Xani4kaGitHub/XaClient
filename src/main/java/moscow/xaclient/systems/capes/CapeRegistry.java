package moscow.xaclient.systems.capes;

import java.util.ArrayList;
import java.util.List;

public final class CapeRegistry {
   private static final List<Cape> CAPES = new ArrayList<>();

   static {
      register(Cape.of("xaclient", "capes.xaclient", "textures/capes/xaclient.png"));
      for (int i = 2; i <= 12; i++) {
         register(Cape.of("capes" + i, "capes.capes" + i, "textures/capes/capes" + i + ".png"));
      }
   }

   public static void register(Cape cape) {
      CAPES.add(cape);
   }

   public static List<Cape> getCapes() {
      return CAPES;
   }

   public static Cape byId(String id) {
      for (Cape cape : CAPES) {
         if (cape.id().equalsIgnoreCase(id)) {
            return cape;
         }
      }

      return null;
   }

   private CapeRegistry() {
   }
}
