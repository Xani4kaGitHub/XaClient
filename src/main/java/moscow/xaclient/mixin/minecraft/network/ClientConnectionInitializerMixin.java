package moscow.xaclient.mixin.minecraft.network;

import io.netty.channel.Channel;
import moscow.xaclient.XaClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.ClientConnection$1")
public class ClientConnectionInitializerMixin {
   @Inject(method = "initChannel", at = @At("HEAD"))
   private void applyProxy(Channel channel, CallbackInfo ci) {
      XaClient.getInstance().getProxyManager().apply(channel.pipeline());
   }
}
