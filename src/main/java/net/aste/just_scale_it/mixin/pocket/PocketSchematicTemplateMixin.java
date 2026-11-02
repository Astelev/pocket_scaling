package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.schematic.*;
import net.aste.just_scale_it.pocket.PocketRules;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(StructureTemplate.class)
public abstract class PocketSchematicTemplateMixin implements PocketSchematicData {
    @Unique private CompoundTag just_scale_it$pocketData=new CompoundTag();
    public CompoundTag just_scale_it$pocketSchematic(){return just_scale_it$pocketData;}
    public void just_scale_it$pocketSchematic(CompoundTag value){just_scale_it$pocketData=value.copy();}
    @Inject(method="load",at=@At("HEAD"))
    private void just_scale_it$load(HolderGetter<Block> blocks,CompoundTag tag,CallbackInfo ci){just_scale_it$pocketData=tag.getCompound(PocketSchematicCodec.KEY).copy();}
    @Inject(method="save",at=@At("RETURN"))
    private void just_scale_it$save(CompoundTag tag,CallbackInfoReturnable<CompoundTag> cir){if(!just_scale_it$pocketData.isEmpty())cir.getReturnValue().put(PocketSchematicCodec.KEY,just_scale_it$pocketData.copy());}
    @Inject(method="placeInWorld",at=@At("HEAD"))
    private void just_scale_it$level(ServerLevelAccessor level,BlockPos pos,BlockPos pivot,StructurePlaceSettings settings,RandomSource random,int flags,CallbackInfoReturnable<Boolean> cir){
        if(PocketRules.present()&&level instanceof PocketSchematicData data)data.just_scale_it$pocketSchematic(just_scale_it$pocketData);
    }
}
