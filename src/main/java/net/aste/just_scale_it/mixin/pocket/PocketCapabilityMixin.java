package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=BlockCapability.class,remap=false)
public abstract class PocketCapabilityMixin {
    @Inject(method="getCapability",at=@At("RETURN"),cancellable=true)
    private void just_scale_it$pocketCapability(Level level,BlockPos pos,BlockState state,BlockEntity supplied,Object context,CallbackInfoReturnable<Object> cir) {
        if(cir.getReturnValue()==null||!PocketRules.present())return;
        var owner=supplied!=null?supplied:level.getBlockEntity(pos);if(owner==null)return;
        if(PocketScale.blocked(owner)) {cir.setReturnValue(null);return;}
        if((Object)this==Capabilities.FluidHandler.BLOCK&&cir.getReturnValue() instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler fluid){
            if(!((PocketEnergyOwner)owner).just_scale_it$pocketEnergy().bindFluid(owner,fluid)
                    &&PocketScale.at(level,pos)<1-1e-7){cir.setReturnValue(null);return;}
        }
        if((Object)this==Capabilities.EnergyStorage.BLOCK&&cir.getReturnValue() instanceof IEnergyStorage energy)
            cir.setReturnValue(((PocketEnergyOwner)owner).just_scale_it$pocketEnergy().wrap(owner,energy));
    }
}
