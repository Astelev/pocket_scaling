package net.aste.just_scale_it.mixin.pocket;
import com.simibubi.create.foundation.fluid.CombinedTankWrapper;
import net.aste.just_scale_it.pocket.PocketTankChildren;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.*;
@Mixin(value=CombinedTankWrapper.class,remap=false)
public abstract class PocketCombinedTankMixin implements PocketTankChildren {
    @Shadow @Final protected IFluidHandler[] itemHandler;
    @Override public IFluidHandler[] just_scale_it$fluidChildren(){return itemHandler;}
}
