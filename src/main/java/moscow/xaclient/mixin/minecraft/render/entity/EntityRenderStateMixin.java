package moscow.xaclient.mixin.minecraft.render.entity;

import moscow.xaclient.utility.mixins.EntityRenderStateAddition;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements EntityRenderStateAddition {
   @Unique
   private Entity xaclient$entity;

   @Unique
   @Override
   public void xaclient$setEntity(Entity entity) {
      this.xaclient$entity = entity;
   }

   @Unique
   @Override
   public Entity xaclient$getEntity() {
      return this.xaclient$entity;
   }
}
