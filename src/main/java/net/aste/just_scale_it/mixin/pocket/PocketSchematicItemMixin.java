package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.PocketRules;
import net.aste.just_scale_it.schematic.PocketSchematics;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=com.simibubi.create.content.schematics.SchematicItem.class,remap=false)
public abstract class PocketSchematicItemMixin {
    @Inject(method="loadSchematic",at=@At("RETURN"))
    private static void just_scale_it$loaded(Level level,ItemStack stack,CallbackInfoReturnable<StructureTemplate> cir){if(PocketRules.present()&&cir.getReturnValue()!=null)PocketSchematics.loaded(cir.getReturnValue());}
}
