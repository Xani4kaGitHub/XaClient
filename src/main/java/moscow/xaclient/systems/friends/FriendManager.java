package moscow.xaclient.systems.friends;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.localization.Localizator;
import moscow.xaclient.utility.game.EntityUtility;
import moscow.xaclient.utility.game.MessageUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import ru.kotopushka.compiler.sdk.annotations.Compile;

public class FriendManager implements IMinecraft {
   private final List<String> friends = new ArrayList<>();

   public void add(String name) {
      if (XaClient.getInstance().getTargetManager().getTarget().contains(name)) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.target")));
      } else if (this.friends.contains(name)) {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.exists", name)));
      } else if (name.equalsIgnoreCase(mc.getSession().getUsername())) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.self")));
      } else {
         this.friends.add(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.added", name)));
         if (EntityUtility.isInGame()) {
            XaClient.getInstance().getFileManager().writeFile("client");
         }
      }
   }

   public void remove(String name) {
      if (this.friends.contains(name)) {
         this.friends.remove(name);
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.removed", name)));
      } else {
         MessageUtility.info(Text.of(Localizator.translate("commands.friends.not_exists", name)));
      }

      XaClient.getInstance().getFileManager().writeFile("client");
   }

   @Compile
   public void clear() {
      if (this.friends.isEmpty()) {
         MessageUtility.error(Text.of(Localizator.translate("commands.friends.empty")));
      } else {
         this.friends.clear();
         MessageUtility.info(Text.of("Список друзей успешно очищен!"));
         XaClient.getInstance().getFileManager().writeFile("client");
      }
   }

   public List<String> listFriends() {
      return Collections.unmodifiableList(this.friends);
   }

   public boolean isFriend(String name) {
      return this.friends.contains(name);
   }
}
