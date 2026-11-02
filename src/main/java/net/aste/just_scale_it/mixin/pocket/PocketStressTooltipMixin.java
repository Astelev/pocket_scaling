package net.aste.just_scale_it.mixin.pocket;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.aste.just_scale_it.pocket.PocketScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=KineticBlockEntity.class,remap=false)
public abstract class PocketStressTooltipMixin {
    @ModifyExpressionValue(method="addToGoggleTooltip",at=@At(value="INVOKE",target="Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;calculateStressApplied()F"))
    private float just_scale_it$shownStress(float value){var be=(KineticBlockEntity)(Object)this;return PocketScale.blocked(be)?0:(float)(value*PocketScale.factor(be));}
}
