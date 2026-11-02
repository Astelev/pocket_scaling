package net.aste.just_scale_it.mixin.pocket;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.aste.just_scale_it.pocket.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class PocketTickMixin {
    @WrapOperation(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"))
    private void just_scale_it$scaledTick(BlockEntityTicker<BlockEntity> ticker,Level level,BlockPos pos,BlockState state,BlockEntity be,Operation<Void> original) {
        if(!PocketRules.present()){original.call(ticker,level,pos,state,be);return;}
        ((PocketEnergyOwner)be).just_scale_it$pocketEnsureBindings();
        double factor=PocketScale.factor(be);
        if(PocketScale.blocked(be)||(PocketScale.at(level,pos)<1-1e-7&&(((PocketEnergyOwner)be).just_scale_it$pocketEnergy().unsupported||((PocketEnergyOwner)be).just_scale_it$pocketEnergy().unsupportedFluid)))return;
        double previous=PocketEnergyContext.enter(be instanceof WorldEnergyTransport?1:factor);
        Object owner=PocketEnergyContext.enterOwner(be);
        try{original.call(ticker,level,pos,state,be);}finally{PocketEnergyContext.restoreOwner(owner);PocketEnergyContext.restore(previous);}
    }
}
