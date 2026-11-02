package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.ShrinkCosts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=net.neoforged.neoforge.common.CommonHooks.class,remap=false)
public abstract class PocketPlacementCostMixin {
    @WrapMethod(method="onPlaceItemIntoWorld")
    private static InteractionResult just_scale_it$cost(UseOnContext context,Operation<InteractionResult> original){
        if(!net.aste.just_scale_it.pocket.PocketRules.present()||!ShrinkCosts.active()||!(context.getPlayer() instanceof ServerPlayer player))return original.call(context);
        try(var scope=ShrinkCosts.placement(player,context.getItemInHand())){
            var result=original.call(context);if(result.consumesAction())scope.success();return result;
        }
    }
}
