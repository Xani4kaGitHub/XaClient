package moscow.xaclient.systems.custommodel.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class AmogusModel extends AbstractCustomModel {
   private final ModelPart body;
   private final ModelPart eye;
   private final ModelPart leftLegPart;
   private final ModelPart rightLegPart;

   public AmogusModel(ModelPart root) {
      super(root);
      this.body = root.getChild("amogusBody");
      this.eye = root.getChild("amogusEye");
      this.leftLegPart = root.getChild("amogusLeftLeg");
      this.rightLegPart = root.getChild("amogusRightLeg");
   }

   public static TexturedModelData data() {
      ModelData md = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData root = md.getRoot();
      root.addChild(
         "amogusBody",
         ModelPartBuilder.create()
            .uv(34, 8).cuboid(-4.0F, 6.0F, -3.0F, 8.0F, 12.0F, 6.0F)
            .uv(15, 10).cuboid(-3.0F, 9.0F, 3.0F, 6.0F, 8.0F, 3.0F)
            .uv(26, 0).cuboid(-3.0F, 5.0F, -3.0F, 6.0F, 1.0F, 6.0F),
         ModelTransform.pivot(0.0F, 0.0F, 0.0F)
      );
      root.addChild("amogusEye", ModelPartBuilder.create().uv(0, 10).cuboid(-3.0F, 7.0F, -4.0F, 6.0F, 4.0F, 1.0F), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
      root.addChild("amogusLeftLeg", ModelPartBuilder.create().uv(0, 0).cuboid(2.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(-2.0F, 18.0F, 0.0F));
      root.addChild("amogusRightLeg", ModelPartBuilder.create().uv(13, 0).cuboid(-5.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(2.0F, 18.0F, 0.0F));
      return TexturedModelData.of(md, 64, 64);
   }

   @Override
   public void setAngles(PlayerEntityRenderState state) {
      super.setAngles(state);
   }

   public void renderCustom(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
      matrices.push();
      matrices.translate(0.0F, -0.5F, 0.0F);
      this.body.render(matrices, vertices, light, overlay, color);
      this.eye.render(matrices, vertices, light, overlay, color);
      this.leftLegPart.render(matrices, vertices, light, overlay, color);
      this.rightLegPart.render(matrices, vertices, light, overlay, color);
      matrices.pop();
   }
}
