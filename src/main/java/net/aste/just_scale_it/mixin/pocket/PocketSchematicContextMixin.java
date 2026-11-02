package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.PocketRules;
import net.aste.just_scale_it.schematic.PocketSchematics;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets="dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext",remap=false)
public abstract class PocketSchematicContextMixin {
    @Inject(method="setCurrentContext",at=@At("HEAD"))
    private static void just_scale_it$capture(SubLevelSchematicSerializationContext next,CallbackInfo ci){if(PocketRules.present())PocketSchematics.contextChanging(next);}
}
