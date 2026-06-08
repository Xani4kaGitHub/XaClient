package moscow.xaclient.utility.rotations;

import moscow.xaclient.XaClient;
import moscow.xaclient.systems.event.EventListener;
import moscow.xaclient.systems.event.impl.player.ClientPlayerTickEvent;
import moscow.xaclient.systems.event.impl.player.InputEvent;
import moscow.xaclient.systems.event.impl.render.Render3DEvent;

public class RotationUpdateListener {
   private final EventListener<ClientPlayerTickEvent> onTick = event -> XaClient.getInstance().getRotationHandler().update();
   private final EventListener<Render3DEvent> onRender = event -> XaClient.getInstance().getRotationHandler().updateRender(event.getTickDelta());
   private final EventListener<InputEvent> onInputEvent = event -> {
      RotationHandler rotationHandler = XaClient.INSTANCE.getRotationHandler();
      RotationTask currentTask = rotationHandler.getCurrentTask();
      if (!rotationHandler.isIdling() && currentTask != null && currentTask.getMoveCorrection() == MoveCorrection.SILENT) {
         event.setYaw(XaClient.getInstance().getRotationHandler().getCurrentRotation().getYaw());
      }
   };

   public RotationUpdateListener() {
      XaClient.getInstance().getEventManager().subscribe(this);
   }
}
