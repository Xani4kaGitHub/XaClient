package moscow.xaclient.mixin.minecraft.client.gui.screen;

import moscow.xaclient.XaClient;
import moscow.xaclient.framework.base.CustomDrawContext;
import moscow.xaclient.systems.event.impl.render.ScreenRenderEvent;
import moscow.xaclient.systems.event.impl.window.ContainerClickEvent;
import moscow.xaclient.systems.event.impl.window.ContainerReleaseEvent;
import moscow.xaclient.systems.modules.modules.player.InvUtils;
import moscow.xaclient.utility.interfaces.IMinecraft;
import moscow.xaclient.utility.time.Timer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin implements IMinecraft {
   @Unique
   private final Timer timer = new Timer();

   @Shadow
   protected abstract boolean isPointOverSlot(Slot var1, double var2, double var4);

   @Shadow
   protected abstract void onMouseClick(Slot var1, int var2, int var3, SlotActionType var4);

   @Inject(method = "render", at = @At("TAIL"))
   private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
      CustomDrawContext customDrawContext = CustomDrawContext.of(context);
      XaClient.getInstance().getEventManager().triggerEvent(new ScreenRenderEvent(customDrawContext, mouseX, mouseY, delta));

      for (Slot slot : mc.player.currentScreenHandler.slots) {
         InvUtils invUtils = XaClient.getInstance().getModuleManager().getModule(InvUtils.class);
         if (this.isPointOverSlot(slot, mouseX, mouseY)
            && slot.isEnabled()
            && invUtils.isEnabled()
            && invUtils.getScroller().isSelected()
            && this.timer.finished((long)invUtils.getScrollDelay().getCurrentValue())
            && InputUtil.isKeyPressed(mc.getWindow().getHandle(), 340)
            && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
            this.onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
            this.timer.reset();
         }
      }
   }

   @Inject(method = "mouseClicked", at = @At("HEAD"))
   private void onMouseClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      XaClient.getInstance().getEventManager().triggerEvent(new ContainerClickEvent((float)mouseX, (float)mouseY, button));
   }

   @Inject(method = "mouseReleased", at = @At("HEAD"))
   public void mouseReleased(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      XaClient.getInstance().getEventManager().triggerEvent(new ContainerReleaseEvent((float)mouseX, (float)mouseY, button));
   }
}
