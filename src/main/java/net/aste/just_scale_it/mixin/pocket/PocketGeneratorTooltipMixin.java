package net.aste.just_scale_it.mixin.pocket;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.aste.just_scale_it.pocket.PocketScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=GeneratingKineticBlockEntity.class,remap=false)
public abstract class PocketGeneratorTooltipMixin {
    @ModifyExpressionValue(method="addToGoggleTooltip",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/base/GeneratingKineticBlockEntity;calculateAddedStressCapacity()F"))
    private float just_scale_it$shownCapacity(float value){var be=(GeneratingKineticBlockEntity)(Object)this;return PocketScale.blocked(be)?0:(float)(value*PocketScale.factor(be));}
}
