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

public class DemonModel extends AbstractCustomModel {
   private final ModelPart head7;
   private final ModelPart body7;
   private final ModelPart leftArm7;
   private final ModelPart rightArm7;
   private final ModelPart leftLeg7;
   private final ModelPart rightLeg7;

   public DemonModel(ModelPart root) {
      super(root);
      this.head7 = root.getChild("head7");
      this.body7 = root.getChild("body7");
      this.leftArm7 = root.getChild("left_arm7");
      this.rightArm7 = root.getChild("right_arm7");
      this.leftLeg7 = root.getChild("left_leg7");
      this.rightLeg7 = root.getChild("right_leg7");
   }

   public static TexturedModelData data() {
      ModelData md = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData root = md.getRoot();

      ModelPartData head7 = root.addChild(
         "head7", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.3F)), ModelTransform.pivot(0.0F, -6.0F, -1.0F)
      );
      head7.addChild(
         "left_horn",
         ModelPartBuilder.create()
            .uv(32, 8).cuboid(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
            .uv(0, 0).cuboid(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
         ModelTransform.of(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F)
      );
      head7.addChild(
         "right_horn",
         ModelPartBuilder.create().mirrored()
            .uv(32, 8).cuboid(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new Dilation(0.1F))
            .uv(0, 0).cuboid(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new Dilation(0.1F)),
         ModelTransform.of(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F)
      );

      ModelPartData body7 = root.addChild(
         "body7", ModelPartBuilder.create().uv(0, 16).cuboid(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F), ModelTransform.of(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F)
      );
      body7.addChild(
         "left_wing",
         ModelPartBuilder.create().uv(40, 12).cuboid(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
         ModelTransform.of(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F)
      );
      body7.addChild(
         "right_wing",
         ModelPartBuilder.create().mirrored().uv(40, 12).cuboid(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.0F),
         ModelTransform.of(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F)
      );

      root.addChild(
         "left_arm7", ModelPartBuilder.create().uv(24, 16).cuboid(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F), ModelTransform.of(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F)
      );
      root.addChild(
         "right_arm7",
         ModelPartBuilder.create().mirrored().uv(24, 16).cuboid(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F),
         ModelTransform.of(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F)
      );

      ModelPartData leftLeg7 = root.addChild(
         "left_leg7", ModelPartBuilder.create().uv(48, 22).cuboid(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(3.0F, 10.0F, 0.0F)
      );
      ModelPartData leftLeg1 = leftLeg7.addChild(
         "left_leg1", ModelPartBuilder.create().uv(34, 34).cuboid(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F), ModelTransform.of(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F)
      );
      leftLeg1.addChild(
         "bone2",
         ModelPartBuilder.create()
            .uv(26, 0).cuboid(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
            .uv(40, 0).cuboid(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
         ModelTransform.of(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F)
      );
      ModelPartData bone3 = leftLeg1.addChild("bone3", ModelPartBuilder.create(), ModelTransform.of(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F));
      bone3.addChild(
         "bone7",
         ModelPartBuilder.create()
            .uv(16, 34).cuboid(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
            .uv(0, 32).cuboid(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
         ModelTransform.pivot(1.9F, 12.0F, 0.25F)
      );

      ModelPartData rightLeg7 = root.addChild(
         "right_leg7", ModelPartBuilder.create().mirrored().uv(48, 22).cuboid(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F), ModelTransform.pivot(-3.0F, 10.0F, 0.0F)
      );
      ModelPartData rightLeg3 = rightLeg7.addChild(
         "right_leg3",
         ModelPartBuilder.create().mirrored().uv(34, 34).cuboid(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F),
         ModelTransform.of(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F)
      );
      rightLeg3.addChild(
         "bone4",
         ModelPartBuilder.create().mirrored()
            .uv(26, 0).cuboid(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F)
            .uv(40, 0).cuboid(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F),
         ModelTransform.of(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F)
      );
      ModelPartData bone5 = rightLeg3.addChild("bone5", ModelPartBuilder.create(), ModelTransform.of(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F));
      bone5.addChild(
         "bone6",
         ModelPartBuilder.create().mirrored()
            .uv(16, 34).cuboid(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F)
            .uv(0, 32).cuboid(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F),
         ModelTransform.pivot(-1.9F, 12.0F, 0.25F)
      );

      return TexturedModelData.of(md, 64, 64);
   }

   @Override
   public void setAngles(PlayerEntityRenderState state) {
      super.setAngles(state);
      copyAngles(this.head, this.head7);
      copyAngles(this.rightLeg, this.rightLeg7);
      copyAngles(this.leftLeg, this.leftLeg7);
      copyAngles(this.leftArm, this.leftArm7);
      copyAngles(this.rightArm, this.rightArm7);
   }

   public void renderCustom(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
      matrices.push();
      this.head7.render(matrices, vertices, light, overlay, color);
      this.body7.render(matrices, vertices, light, overlay, color);
      this.leftArm7.render(matrices, vertices, light, overlay, color);
      this.rightArm7.render(matrices, vertices, light, overlay, color);
      this.leftLeg7.render(matrices, vertices, light, overlay, color);
      this.rightLeg7.render(matrices, vertices, light, overlay, color);
      matrices.pop();
   }
}
