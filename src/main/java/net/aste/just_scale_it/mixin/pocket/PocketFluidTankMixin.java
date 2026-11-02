package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=FluidTank.class,remap=false)
public abstract class PocketFluidTankMixin implements PocketFluidTank {
    @Shadow protected int capacity;
    @Shadow protected FluidStack fluid;
    @Shadow protected abstract void onContentsChanged();
    @Unique private int just_scale_it$nominal=-1;
    @Unique private BlockEntity just_scale_it$owner;
    @Unique private boolean just_scale_it$syncing;
    @Override public boolean just_scale_it$bindFluid(BlockEntity owner){
        if(just_scale_it$owner!=null&&just_scale_it$owner!=owner&&!just_scale_it$owner.isRemoved()&&!PocketScale.sameBody(just_scale_it$owner,owner))return false;
        if(just_scale_it$owner==null||just_scale_it$owner.isRemoved())just_scale_it$owner=owner;
        if(just_scale_it$nominal<0)just_scale_it$nominal=capacity;return true;
    }
    @Override public void just_scale_it$syncFluid(){
        if(just_scale_it$owner==null||just_scale_it$syncing)return;just_scale_it$syncing=true;
        try{capacity=PhysicalCapacity.capacity(just_scale_it$nominal,PocketScale.fluidFactor(just_scale_it$owner));
            if(just_scale_it$owner.getLevel()!=null&&!just_scale_it$owner.getLevel().isClientSide&&fluid.getAmount()>capacity){fluid.setAmount(PhysicalCapacity.contents(fluid.getAmount(),capacity));onContentsChanged();just_scale_it$owner.setChanged();}
        }finally{just_scale_it$syncing=false;}
    }
    @ModifyVariable(method="setCapacity",at=@At("HEAD"),argsOnly=true)
    private int just_scale_it$capacity(int value){if(just_scale_it$owner==null)return value;just_scale_it$nominal=value;return PhysicalCapacity.capacity(value,PocketScale.fluidFactor(just_scale_it$owner));}
    @Inject(method={"setCapacity","readFromNBT"},at=@At("RETURN"))
    private void just_scale_it$afterWrite(CallbackInfoReturnable<FluidTank> ci){just_scale_it$syncFluid();}
    @Inject(method="setFluid",at=@At("RETURN"))
    private void just_scale_it$afterSet(CallbackInfo ci){just_scale_it$syncFluid();}
    @Inject(method={"fill","drain","getCapacity","getFluidAmount","getFluid","getSpace"},at=@At("HEAD"))
    private void just_scale_it$beforeRead(CallbackInfoReturnable<?> cir){just_scale_it$syncFluid();}
}
