package net.aste.just_scale_it.pocket;
import java.util.*;
import net.aste.just_scale_it.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
public final class PocketRules {
    private static volatile List<? extends String> previous;
    private static Set<ResourceLocation> blocks=Set.of();
    private static List<TagKey<Block>> tags=List.of();
    public static boolean present() {return ModList.get().isLoaded("pocket");}
    public static boolean active() {return present()&&Config.POCKET_ENABLED.get();}
    public static boolean forbidden(BlockState state) {
        if(!active())return false;
        var list=Config.POCKET_BLACKLIST.get();if(list!=previous)reload(list);
        if(blocks.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock())))return true;
        for(var tag:tags)if(state.is(tag))return true;
        return false;
    }
    private static synchronized void reload(List<? extends String> list) {
        var ids=new HashSet<ResourceLocation>();var keys=new ArrayList<TagKey<Block>>();
        for(String e:list)if(e.startsWith("#"))keys.add(TagKey.create(Registries.BLOCK,ResourceLocation.parse(e.substring(1))));else ids.add(ResourceLocation.parse(e));
        blocks=Set.copyOf(ids);tags=List.copyOf(keys);previous=list;
    }
    private PocketRules() {}
}
