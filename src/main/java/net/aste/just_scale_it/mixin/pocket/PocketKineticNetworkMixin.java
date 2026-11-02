package net.aste.just_scale_it.mixin.pocket;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.aste.just_scale_it.pocket.PocketScale;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=KineticNetwork.class,remap=false)
public abstract class PocketKineticNetworkMixin {
    @Inject(method={"getActualCapacityOf","getActualStressOf"},at=@At("RETURN"),cancellable=true)
    private void just_scale_it$scaledStress(KineticBlockEntity be,CallbackInfoReturnable<Float> cir) {cir.setReturnValue(PocketScale.blocked(be)?0:(float)(cir.getReturnValue()*PocketScale.factor(be)));}
    @ModifyVariable(method="addSilently",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float just_scale_it$loadedCapacity(float value,KineticBlockEntity be,float capacity,float stress) {return (float)(value*PocketScale.factor(be));}
    @ModifyVariable(method="addSilently",at=@At("HEAD"),argsOnly=true,ordinal=1)
    private float just_scale_it$loadedStress(float value,KineticBlockEntity be,float capacity,float stress) {return (float)(value*PocketScale.factor(be));}
}
