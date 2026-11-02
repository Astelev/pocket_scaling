package net.aste.just_scale_it.mixin.pocket;
import net.aste.just_scale_it.pocket.PocketRules;
import net.aste.just_scale_it.schematic.PocketSchematics;
import net.minecraft.server.level.ServerPlayer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value=com.simibubi.create.content.schematics.packet.SchematicPlacePacket.class,remap=false,priority=900)
public abstract class PocketSchematicPlaceMixin {
    @WrapMethod(method="handle")
    private void just_scale_it$place(ServerPlayer player,Operation<Void> original){
        if(!PocketRules.present()||player==null||!player.isCreative()){original.call(player);return;}
        try(var session=PocketSchematics.paste(player)){original.call(player);session.placed();}
    }
}
