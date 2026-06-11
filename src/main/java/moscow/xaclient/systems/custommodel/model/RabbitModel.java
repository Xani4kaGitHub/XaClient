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

public class RabbitModel extends AbstractCustomModel {
   private final ModelPart bone;
   private final ModelPart rabbitHead;
   private final ModelPart rabbitLarm;
   private final ModelPart rabbitRarm;
   private final ModelPart rabbitRleg;
   private final ModelPart rabbitLleg;

   public RabbitModel(ModelPart root) {
      super(root);
      this.bone = root.getChild("rabbitBone");
      this.rabbitHead = this.bone.getChild("rabbitHead");
      this.rabbitLarm = this.bone.getChild("rabbitLarm");
      this.rabbitRarm = this.bone.getChild("rabbitRarm");
      this.rabbitRleg = this.bone.getChild("rabbitRleg");
      this.rabbitLleg = this.bone.getChild("rabbitLleg");
   }

   public static TexturedModelData data() {
      ModelData md = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData root = md.getRoot();
      ModelPartData bone = root.addChild(
         "rabbitBone", ModelPartBuilder.create().uv(28, 45).cuboid(-5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F), ModelTransform.pivot(0.0F, 24.0F, 0.0F)
      );
      bone.addChild("rabbitRleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), ModelTransform.pivot(-3.0F, -2.0F, -1.0F));
      bone.addChild(
         "rabbitLarm", ModelPartBuilder.create().uv(0, 0).cuboid(0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), ModelTransform.of(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F)
      );
      bone.addChild(
         "rabbitRarm", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F), ModelTransform.of(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F)
      );
      bone.addChild("rabbitLleg", ModelPartBuilder.create().uv(0, 0).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F), ModelTransform.pivot(3.0F, -2.0F, -1.0F));
      bone.addChild(
         "rabbitHead",
         ModelPartBuilder.create()
            .uv(0, 0).cuboid(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F)
            .uv(56, 0).cuboid(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F)
            .uv(56, 0).mirrored().cuboid(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F).mirrored(false)
            .uv(0, 45).cuboid(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F)
            .uv(46, 0).cuboid(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F)
            .uv(46, 0).cuboid(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F),
         ModelTransform.pivot(0.0F, -14.0F, -1.0F)
      );
      return TexturedModelData.of(md, 64, 64);
   }

   @Override
   public void setAngles(PlayerEntityRenderState state) {
      super.setAngles(state);
      copyAngles(this.head, this.rabbitHead);
      this.rabbitLarm.pitch = this.leftArm.pitch;
      this.rabbitLarm.yaw = this.leftArm.yaw;
      this.rabbitLarm.roll = this.leftArm.roll - 0.0873F;
      this.rabbitRarm.pitch = this.rightArm.pitch;
      this.rabbitRarm.yaw = this.rightArm.yaw;
      this.rabbitRarm.roll = this.rightArm.roll + 0.0873F;
      copyAngles(this.rightLeg, this.rabbitRleg);
      copyAngles(this.leftLeg, this.rabbitLleg);
   }

   public void renderCustom(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
      matrices.push();
      matrices.scale(1.25F, 1.25F, 1.25F);
      matrices.translate(0.0F, -0.3F, 0.0F);
      this.bone.render(matrices, vertices, light, overlay, color);
      matrices.pop();
   }
}
