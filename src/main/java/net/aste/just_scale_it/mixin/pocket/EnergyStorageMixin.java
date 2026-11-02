package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.*;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Covers direct references as well as capability wrappers. Own internal tick stays nominal. */
@Mixin(value=EnergyStorage.class,remap=false)
public abstract class EnergyStorageMixin implements EnergyFields {
    @Shadow protected int energy;
    @Shadow protected int capacity;
    @Unique private EnergyScaleAdapter just_scale_it$adapter;
    @Unique private Object just_scale_it$owner;
    @Override public void just_scale_it$pocketSetEnergy(int value) {energy=Math.max(0,Math.min(capacity,value));}
    @Override public EnergyScaleAdapter just_scale_it$pocketAdapter(){return just_scale_it$adapter;}
    @Override public Object just_scale_it$pocketOwner(){return just_scale_it$owner;}
    @Override public void just_scale_it$pocketBind(EnergyScaleAdapter adapter,Object owner){just_scale_it$adapter=adapter;just_scale_it$owner=owner;}
    @Inject(method="receiveEnergy",at=@At("HEAD"),cancellable=true)
    private void just_scale_it$receive(int n,boolean simulate,CallbackInfoReturnable<Integer> cir){if(just_scale_it$pocketExternal())cir.setReturnValue(just_scale_it$adapter.receiveEnergy(n,simulate));}
    @Inject(method="extractEnergy",at=@At("HEAD"),cancellable=true)
    private void just_scale_it$extract(int n,boolean simulate,CallbackInfoReturnable<Integer> cir){if(just_scale_it$pocketExternal())cir.setReturnValue(just_scale_it$adapter.extractEnergy(n,simulate));}
    @Inject(method="canExtract",at=@At("HEAD"),cancellable=true)
    private void just_scale_it$canExtract(CallbackInfoReturnable<Boolean> cir){if(just_scale_it$pocketExternal())cir.setReturnValue(just_scale_it$adapter.canExtract());}
    @Inject(method="canReceive",at=@At("HEAD"),cancellable=true)
    private void just_scale_it$canReceive(CallbackInfoReturnable<Boolean> cir){if(just_scale_it$pocketExternal())cir.setReturnValue(just_scale_it$adapter.canReceive());}
}
