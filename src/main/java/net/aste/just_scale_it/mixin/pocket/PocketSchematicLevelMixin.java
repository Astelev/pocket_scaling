package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.schematic.PocketSchematicData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.*;
@Pseudo
@Mixin(targets="net.createmod.catnip.levelWrappers.SchematicLevel",remap=false)
public abstract class PocketSchematicLevelMixin implements PocketSchematicData {
    @Unique private CompoundTag just_scale_it$pocketData=new CompoundTag();
    public CompoundTag just_scale_it$pocketSchematic(){return just_scale_it$pocketData;}
    public void just_scale_it$pocketSchematic(CompoundTag value){just_scale_it$pocketData=value.copy();}
}
