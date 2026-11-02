package net.aste.just_scale_it.pocket;
import java.util.function.IntConsumer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
/** Only the standard backing charge layout is modified during resize. Custom storage requires an adapter. */
public final class ScaledEnergyStorage extends EnergyScaleAdapter {
    public ScaledEnergyStorage(BlockEntity owner,IEnergyStorage source,double previous) {
        super(source,()->PocketScale.factor(owner),()->PocketScale.blocked(owner),setter(source,owner),owner::setChanged,
                ()->owner.getLevel()!=null&&!owner.getLevel().isClientSide,previous);
    }
    private static IntConsumer setter(IEnergyStorage source,Object owner) {
        if(source instanceof EnergyStorage&&source instanceof EnergyFields fields
                &&(fields.just_scale_it$pocketOwner()==null||fields.just_scale_it$pocketOwner()==owner))try {
            if(source.getClass().getMethod("getEnergyStored").getDeclaringClass()==EnergyStorage.class
                    &&source.getClass().getMethod("getMaxEnergyStored").getDeclaringClass()==EnergyStorage.class)
                return fields::just_scale_it$pocketSetEnergy;
        }catch(ReflectiveOperationException ignored){}
        return null;
    }
}
