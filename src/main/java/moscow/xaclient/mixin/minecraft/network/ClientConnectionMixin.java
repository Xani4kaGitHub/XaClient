package moscow.xaclient.mixin.minecraft.network;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.impl.network.ReceivePacketEvent;
import moscow.xaclient.systems.event.impl.network.SendPacketEvent;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin implements IMinecraft {
   @Unique
   private static boolean stackOverflowFix;

   @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
   private static <T extends PacketListener> void triggerReceivePacketEvent(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
      ReceivePacketEvent event = new ReceivePacketEvent(packet);
      XaClient.getInstance().getEventManager().triggerEvent(event);
      if (event.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   public void triggerSendPacketEvent(Packet<?> packet, CallbackInfo ci) {
      SendPacketEvent event = new SendPacketEvent(packet);
      if (!stackOverflowFix) {
         XaClient.getInstance().getEventManager().triggerEvent(event);
         if (event.isCancelled()) {
            ci.cancel();
         }

         Packet<?> newPacket = event.getPacket();
         if (newPacket != packet) {
            ci.cancel();
            stackOverflowFix = true;
            mc.getNetworkHandler().sendPacket(newPacket);
            stackOverflowFix = false;
         }
      }
   }
}
