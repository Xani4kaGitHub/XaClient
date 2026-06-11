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

public class FreddyModel extends AbstractCustomModel {
   private static final float DEG1 = (float)Math.PI / 180.0F;
   private static final float DEG2 = (float)Math.PI / 90.0F;

   private final ModelPart fredbody;
   private final ModelPart fredhead;
   private final ModelPart armLeft;
   private final ModelPart armRight;
   private final ModelPart legLeft;
   private final ModelPart legRight;

   public FreddyModel(ModelPart root) {
      super(root);
      this.fredbody = root.getChild("fredbody");
      this.fredhead = this.fredbody.getChild("fredhead");
      this.armLeft = this.fredbody.getChild("armLeft");
      this.armRight = this.fredbody.getChild("armRight");
      this.legLeft = this.fredbody.getChild("legLeft");
      this.legRight = this.fredbody.getChild("legRight");
   }

   public static TexturedModelData data() {
      ModelData md = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
      ModelPartData root = md.getRoot();

      ModelPartData fredbody = root.addChild(
         "fredbody", ModelPartBuilder.create().uv(0, 0).cuboid(-1.0F, -14.0F, -1.0F, 2.0F, 24.0F, 2.0F), ModelTransform.pivot(0.0F, -9.0F, 0.0F)
      );

      fredbody.addChild("torso", ModelPartBuilder.create().uv(8, 0).cuboid(-6.0F, -9.0F, -4.0F, 12.0F, 18.0F, 8.0F), ModelTransform.of(0.0F, 0.0F, 0.0F, DEG1, 0.0F, 0.0F));
      fredbody.addChild("crotch", ModelPartBuilder.create().uv(56, 0).cuboid(-5.5F, 0.0F, -3.5F, 11.0F, 3.0F, 7.0F), ModelTransform.pivot(0.0F, 9.5F, 0.0F));

      ModelPartData armRight = fredbody.addChild(
         "armRight", ModelPartBuilder.create().uv(48, 0).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.of(-6.5F, -8.0F, 0.0F, 0.0F, 0.0F, 0.2617994F)
      );
      armRight.addChild("armRightpad", ModelPartBuilder.create().uv(70, 10).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      ModelPartData armRight2 = armRight.addChild(
         "armRight2", ModelPartBuilder.create().uv(90, 20).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F)
      );
      armRight2.addChild("armRightpad2", ModelPartBuilder.create().uv(0, 26).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      armRight2.addChild("handRight", ModelPartBuilder.create().uv(20, 26).cuboid(-2.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, -0.05235988F));

      ModelPartData armLeft = fredbody.addChild(
         "armLeft", ModelPartBuilder.create().uv(62, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.of(6.5F, -8.0F, 0.0F, 0.0F, 0.0F, -0.2617994F)
      );
      armLeft.addChild("armLeftpad", ModelPartBuilder.create().uv(38, 54).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 9.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      ModelPartData armLeft2 = armLeft.addChild(
         "armLeft2", ModelPartBuilder.create().uv(90, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.of(0.0F, 9.6F, 0.0F, -0.17453292F, 0.0F, 0.0F)
      );
      armLeft2.addChild("handLeft", ModelPartBuilder.create().uv(58, 56).cuboid(-1.0F, 0.0F, -2.5F, 4.0F, 4.0F, 5.0F), ModelTransform.of(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.05235988F));
      armLeft2.addChild("armLeftpad2", ModelPartBuilder.create().uv(0, 58).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 7.0F, 5.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

      ModelPartData legRight = fredbody.addChild(
         "legRight", ModelPartBuilder.create().uv(90, 8).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.pivot(-3.3F, 12.5F, 0.0F)
      );
      legRight.addChild("legRightpad", ModelPartBuilder.create().uv(73, 33).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      ModelPartData legRight2 = legRight.addChild(
         "legRight2", ModelPartBuilder.create().uv(20, 35).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.of(0.0F, 9.6F, 0.0F, DEG2, 0.0F, 0.0F)
      );
      legRight2.addChild("footRight", ModelPartBuilder.create().uv(22, 39).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), ModelTransform.of(0.0F, 8.0F, 0.0F, -DEG2, 0.0F, 0.0F));
      legRight2.addChild("legRightpad2", ModelPartBuilder.create().uv(0, 39).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));

      ModelPartData legLeft = fredbody.addChild(
         "legLeft", ModelPartBuilder.create().uv(54, 10).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F), ModelTransform.pivot(3.3F, 12.5F, 0.0F)
      );
      legLeft.addChild("legLeftpad", ModelPartBuilder.create().uv(48, 39).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 9.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      ModelPartData legLeft2 = legLeft.addChild(
         "legLeft2", ModelPartBuilder.create().uv(72, 48).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), ModelTransform.of(0.0F, 9.6F, 0.0F, DEG2, 0.0F, 0.0F)
      );
      legLeft2.addChild("legLeftpad2", ModelPartBuilder.create().uv(16, 50).cuboid(-2.5F, 0.0F, -3.0F, 5.0F, 7.0F, 6.0F), ModelTransform.pivot(0.0F, 0.5F, 0.0F));
      legLeft2.addChild("footLeft", ModelPartBuilder.create().uv(72, 50).cuboid(-2.5F, 0.0F, -6.0F, 5.0F, 3.0F, 8.0F), ModelTransform.of(0.0F, 8.0F, 0.0F, -DEG2, 0.0F, 0.0F));

      ModelPartData fredhead = fredbody.addChild(
         "fredhead", ModelPartBuilder.create().uv(39, 22).cuboid(-5.5F, -8.0F, -4.5F, 11.0F, 8.0F, 9.0F), ModelTransform.pivot(0.0F, -13.0F, -0.5F)
      );
      fredhead.addChild("frednose", ModelPartBuilder.create().uv(17, 67).cuboid(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 3.0F), ModelTransform.pivot(0.0F, -2.0F, -4.5F));
      fredhead.addChild("jaw", ModelPartBuilder.create().uv(49, 65).cuboid(-5.0F, 0.0F, -4.5F, 10.0F, 3.0F, 9.0F), ModelTransform.of(0.0F, 0.5F, 0.0F, 0.08726646F, 0.0F, 0.0F));
      ModelPartData earRight = fredhead.addChild(
         "earRight", ModelPartBuilder.create().uv(8, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F), ModelTransform.of(-4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, -1.0471976F)
      );
      earRight.addChild("earRightpad", ModelPartBuilder.create().uv(85, 0).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), ModelTransform.pivot(0.0F, -1.0F, 0.0F));
      ModelPartData earLeft = fredhead.addChild(
         "earLeft", ModelPartBuilder.create().uv(40, 0).cuboid(-1.0F, -3.0F, -0.5F, 2.0F, 3.0F, 1.0F), ModelTransform.of(4.5F, -5.5F, 0.0F, 0.05235988F, 0.0F, 1.0471976F)
      );
      earLeft.addChild("earRightpad_1", ModelPartBuilder.create().uv(40, 39).cuboid(-2.0F, -5.0F, -1.0F, 4.0F, 4.0F, 2.0F), ModelTransform.pivot(0.0F, -1.0F, 0.0F));
      ModelPartData hat = fredhead.addChild(
         "hat", ModelPartBuilder.create().uv(70, 24).cuboid(-3.0F, -0.5F, -3.0F, 6.0F, 1.0F, 6.0F), ModelTransform.of(0.0F, -8.4F, 0.0F, -DEG1, 0.0F, 0.0F)
      );
      hat.addChild("hat2", ModelPartBuilder.create().uv(78, 61).cuboid(-2.0F, -4.0F, -2.0F, 4.0F, 4.0F, 4.0F), ModelTransform.of(0.0F, 0.1F, 0.0F, -DEG1, 0.0F, 0.0F));

      return TexturedModelData.of(md, 100, 80);
   }

   @Override
   public void setAngles(PlayerEntityRenderState state) {
      super.setAngles(state);
      copyAngles(this.head, this.fredhead);
      copyAngles(this.leftArm, this.armLeft);
      copyAngles(this.rightLeg, this.legRight);
      copyAngles(this.leftLeg, this.legLeft);
      copyAngles(this.rightArm, this.armRight);
   }

   public void renderCustom(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, int color) {
      matrices.push();
      matrices.scale(0.75F, 0.65F, 0.75F);
      matrices.translate(0.0F, 0.85F, 0.0F);
      this.fredbody.render(matrices, vertices, light, overlay, color);
      matrices.pop();
   }
}
