package wtf.wyvern.client.modules.impl.combat.rotation;

import net.minecraft.entity.LivingEntity;
import wtf.wyvern.utility.component.RotationComponent;
import wtf.wyvern.utility.game.player.rotation.Rotation;

public class ReallyWorldRotation extends RotationBase {
    @Override
    public void update(Rotation targetAngle, boolean elytraVisual) {
        this.update(null, targetAngle, elytraVisual);
    }

    public void update(LivingEntity target, Rotation targetAngle, boolean elytraVisual) {

        RotationComponent.update(targetAngle, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, elytraVisual);
        this.lastYaw = targetAngle.getYaw();
        this.lastPitch = targetAngle.getPitch();
    }
}