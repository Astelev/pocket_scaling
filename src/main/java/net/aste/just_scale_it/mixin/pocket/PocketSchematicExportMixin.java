package net.aste.just_scale_it.mixin.pocket;
import java.io.OutputStream;
import java.nio.file.Path;
import net.aste.just_scale_it.pocket.PocketRules;
import net.aste.just_scale_it.schematic.PocketSchematics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import com.simibubi.create.content.schematics.SchematicExport;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=SchematicExport.class,remap=false,priority=900)
public abstract class PocketSchematicExportMixin {
    @WrapMethod(method="saveSchematic")
    private static SchematicExport.SchematicExportResult just_scale_it$session(Path folder,String name,boolean overwrite,Level level,BlockPos from,BlockPos to,Operation<SchematicExport.SchematicExportResult> original){
        if(!PocketRules.present())return original.call(folder,name,overwrite,level,from,to);
        try(var session=PocketSchematics.export(level,from,to)){return original.call(folder,name,overwrite,level,from,to);}
    }
    @WrapOperation(method="saveSchematic",at=@At(value="INVOKE",target="Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V",remap=true))
    private static void just_scale_it$metadata(CompoundTag tag,OutputStream stream,Operation<Void> original){
        if(PocketRules.present())PocketSchematics.saving(tag);original.call(tag,stream);
    }
}
