package moscow.xaclient.mixin.minecraft.client;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.impl.window.MouseEvent;
import moscow.xaclient.systems.event.impl.window.MouseScrollEvent;
import moscow.xaclient.utility.game.cursor.CursorType;
import moscow.xaclient.utility.game.cursor.CursorUtility;
import moscow.xaclient.utility.interfaces.IMinecraft;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin implements IMinecraft {
   @Inject(method = "tick()V", at = @At("RETURN"))
   private void tick(CallbackInfo ci) {
      if (CursorUtility.getCurrentType() != CursorUtility.getPrev()) {
         GLFW.glfwSetCursor(mc.getWindow().getHandle(), CursorUtility.getCurrentType().getCode());
      }

      CursorUtility.setPrev(CursorUtility.getCurrentType());
      CursorUtility.set(CursorType.DEFAULT);
   }

   @Inject(method = "onMouseButton", at = @At("HEAD"))
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      if (action == 1) {
         XaClient.getInstance().getEventManager().triggerEvent(new MouseEvent(button, action));
      }
   }

   @Inject(method = "onMouseScroll", at = @At("HEAD"))
   private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
      if (vertical != 0.0) {
         XaClient.getInstance().getEventManager().triggerEvent(new MouseScrollEvent(vertical));
      }
   }
}
