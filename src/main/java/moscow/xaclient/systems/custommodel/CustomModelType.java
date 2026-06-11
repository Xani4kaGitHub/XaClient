package moscow.xaclient.systems.custommodel;

import java.util.function.Function;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.custommodel.model.AbstractCustomModel;
import moscow.xaclient.systems.custommodel.model.AmogusModel;
import moscow.xaclient.systems.custommodel.model.DemonModel;
import moscow.xaclient.systems.custommodel.model.FreddyModel;
import moscow.xaclient.systems.custommodel.model.RabbitModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.util.Identifier;
import java.util.function.Supplier;

public enum CustomModelType {
   CRAZY_RABBIT("custom_models.crazy_rabbit", "textures/models/rabbit.png", RabbitModel::data, RabbitModel::new),
   WHITE_DEMON("custom_models.white_demon", "textures/models/whitedemon.png", DemonModel::data, DemonModel::new),
   RED_DEMON("custom_models.red_demon", "textures/models/reddemon.png", DemonModel::data, DemonModel::new),
   FREDDY_BEAR("custom_models.freddy_bear", "textures/models/freddy.png", FreddyModel::data, FreddyModel::new),
   AMOGUS("custom_models.amogus", "textures/models/amogus.png", AmogusModel::data, AmogusModel::new);

   private final String key;
   private final Identifier texture;
   private final Supplier<TexturedModelData> dataSupplier;
   private final Function<ModelPart, AbstractCustomModel> factory;
   private AbstractCustomModel model;

   CustomModelType(String key, String texturePath, Supplier<TexturedModelData> dataSupplier, Function<ModelPart, AbstractCustomModel> factory) {
      this.key = key;
      this.texture = XaClient.id(texturePath);
      this.dataSupplier = dataSupplier;
      this.factory = factory;
   }

   public AbstractCustomModel getModel() {
      if (this.model == null) {
         this.model = this.factory.apply(this.dataSupplier.get().createModel());
      }

      return this.model;
   }

   public String getKey() {
      return this.key;
   }

   public Identifier getTexture() {
      return this.texture;
   }
}
