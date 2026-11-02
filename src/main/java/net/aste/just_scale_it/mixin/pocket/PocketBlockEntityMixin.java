package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BlockEntity.class)
public abstract class PocketBlockEntityMixin implements PocketEnergyOwner {
    @Unique private PocketEnergyData just_scale_it$pocketData;
    @Unique private boolean just_scale_it$bindingsChecked;
    @Override public void just_scale_it$pocketEnsureBindings() {
        if(just_scale_it$bindingsChecked)return;
        just_scale_it$bindingsChecked=true;
        PocketIntegration.syncEnergy((BlockEntity)(Object)this);
    }
    @Override public PocketEnergyData just_scale_it$pocketEnergy() {if(just_scale_it$pocketData==null)just_scale_it$pocketData=new PocketEnergyData();return just_scale_it$pocketData;}
    @Inject(method="loadAdditional",at=@At("HEAD"))
    private void just_scale_it$readPocket(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        just_scale_it$bindingsChecked=false;
        if(just_scale_it$pocketData!=null||tag.contains("AeroutilsPocketFE"))just_scale_it$pocketEnergy().clear(tag.contains("AeroutilsPocketFE")?tag.getDouble("AeroutilsPocketFE"):0);
    }
    @Inject(method="saveAdditional",at=@At("RETURN"))
    private void just_scale_it$writePocket(CompoundTag tag,HolderLookup.Provider registries,CallbackInfo ci) {
        if(!PocketRules.present()||just_scale_it$pocketData==null)return;
        PocketIntegration.syncEnergy((BlockEntity)(Object)this);
        tag.putDouble("AeroutilsPocketFE",just_scale_it$pocketData.savedFactor);
    }
}
