package net.aste.just_scale_it.mixin.pocket;
import com.misterblusky9.pym.api.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.aste.just_scale_it.pocket.ShrinkCosts;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="com.misterblusky9.pym.api.ResizeRequest",remap=false)
public abstract class PocketResizeCostMixin {
    @Shadow @Final private ServerSubLevel subLevel;
    @Shadow private double scale;
    @Shadow private boolean immediate;
    @WrapMethod(method="submit")
    private ResizeResult just_scale_it$cost(Operation<ResizeResult> original){
        if(!net.aste.just_scale_it.pocket.PocketRules.present())return original.call();
        return ShrinkCosts.resize((ResizeRequest)(Object)this,subLevel,scale,immediate,original::call);
    }
}
