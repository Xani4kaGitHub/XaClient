package moscow.xaclient.protection.client;

import moscow.xaclient.XaClient;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kotopushka.compiler.sdk.annotations.VMProtect;
import ru.kotopushka.compiler.sdk.enums.VMProtectType;

public class MinecraftClientMixinProtection {
   @VMProtect(type = VMProtectType.MUTATION)
   public static void init() {
      XaClient.INSTANCE.initialize();
   }

   @VMProtect(type = VMProtectType.MUTATION)
   public static void shutdown() {
      XaClient.INSTANCE.shutdown();
   }

   public static void updateTitle(CallbackInfoReturnable<String> cir) {
      if (!XaClient.INSTANCE.isPanic()) {
         String title = "%s %s (%s)".formatted("XaClient", "2.0", "Beta");
         cir.setReturnValue(title);
      }
   }
}
