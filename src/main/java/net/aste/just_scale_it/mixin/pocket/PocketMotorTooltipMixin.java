package net.aste.just_scale_it.mixin.pocket;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.aste.just_scale_it.pocket.PocketScale;
import net.aste.just_scale_it.pocket.ScalePower;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo
@Mixin(targets="com.mrh0.createaddition.blocks.electric_motor.ElectricMotorBlockEntity",remap=false)
public abstract class PocketMotorTooltipMixin {
    @ModifyExpressionValue(method="addToGoggleTooltip",at=@At(value="INVOKE",target="Lcom/mrh0/createaddition/blocks/electric_motor/ElectricMotorBlockEntity;getEnergyConsumptionRate(F)I"))
    private int just_scale_it$shownRate(int value){return ScalePower.ceil(value*PocketScale.factor((BlockEntity)(Object)this));}
}
