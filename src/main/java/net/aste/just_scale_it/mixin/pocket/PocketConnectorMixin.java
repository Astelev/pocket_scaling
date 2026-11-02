package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
@Pseudo
@Mixin(targets="com.mrh0.createaddition.blocks.connector.base.AbstractConnectorBlockEntity",remap=false)
public abstract class PocketConnectorMixin implements WorldEnergyTransport {
    @Inject(method="getEnergyStorage",at=@At("RETURN"),cancellable=true)
    private void just_scale_it$worldNetwork(Direction side,CallbackInfoReturnable<IEnergyStorage> cir){
        if(cir.getReturnValue()!=null&&PocketRules.present()){
            var owner=(BlockEntity)(Object)this;
            cir.setReturnValue(((PocketEnergyOwner)owner).just_scale_it$pocketEnergy().wrap(owner,cir.getReturnValue()));
        }
    }
}
