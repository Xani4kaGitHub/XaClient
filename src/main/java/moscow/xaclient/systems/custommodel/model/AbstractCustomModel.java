package moscow.xaclient.systems.custommodel.model;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public abstract class AbstractCustomModel extends BipedEntityModel<PlayerEntityRenderState> {
   public AbstractCustomModel(ModelPart root) {
      super(root);
   }

   public abstract void renderCustom(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color);

   protected static void copyAngles(ModelPart from, ModelPart to) {
      to.pitch = from.pitch;
      to.yaw = from.yaw;
      to.roll = from.roll;
   }
}
