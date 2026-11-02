package net.aste.just_scale_it.pocket;
import java.util.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
public final class PocketFluids {
    public static boolean bind(BlockEntity be,IFluidHandler handler){
        if(handler instanceof PocketFluidTank tank){boolean ok=tank.just_scale_it$bindFluid(be);if(ok)tank.just_scale_it$syncFluid();return ok;}
        return bind(be,handler,Collections.newSetFromMap(new IdentityHashMap<>()));
    }
    private static boolean bind(BlockEntity be,IFluidHandler handler,Set<IFluidHandler> seen){
        if(!seen.add(handler))return true;
        if(handler instanceof PocketFluidTank tank){boolean ok=tank.just_scale_it$bindFluid(be);if(ok)tank.just_scale_it$syncFluid();return ok;}
        if(handler instanceof PocketTankChildren group){boolean ok=true;for(var child:group.just_scale_it$fluidChildren())ok&=bind(be,child,seen);return ok;}
        return handler.getTanks()==0;
    }
    private PocketFluids(){}
}
