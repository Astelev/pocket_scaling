package net.aste.just_scale_it.pocket;
import java.util.IdentityHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
/** Per-block lifetime and identity cache: shared faces never apply conversion twice. */
public final class PocketEnergyData {
    public double savedFactor=0;
    public boolean unsupported;
    public boolean unsupportedFluid;
    private IdentityHashMap<IEnergyStorage,EnergyScaleAdapter> wrappers;
    private IdentityHashMap<net.neoforged.neoforge.fluids.capability.IFluidHandler,Boolean> fluids;
    public void resetProbes(){unsupported=false;unsupportedFluid=false;if(fluids!=null)fluids.clear();}
    public boolean bindFluid(BlockEntity owner,net.neoforged.neoforge.fluids.capability.IFluidHandler handler){
        if(fluids!=null&&fluids.containsKey(handler))return true;
        boolean ok=PocketFluids.bind(owner,handler);unsupportedFluid|=!ok;
        if(ok&&handler.getTanks()>0){if(fluids==null)fluids=new IdentityHashMap<>();fluids.put(handler,true);}return ok;
    }
    public IEnergyStorage wrap(BlockEntity owner,IEnergyStorage raw) {
        if(raw instanceof EnergyScaleAdapter)return raw;
        if(raw instanceof EnergyFields fields&&fields.just_scale_it$pocketAdapter()!=null
                &&fields.just_scale_it$pocketOwner() instanceof BlockEntity previous&&!previous.isRemoved()
                &&PocketScale.sameBody(previous,owner))return fields.just_scale_it$pocketAdapter();
        if(wrappers==null)wrappers=new IdentityHashMap<>();
        var wrapped=wrappers.computeIfAbsent(raw,key->owner instanceof WorldEnergyTransport?new EnergyScaleAdapter(key,()->1,()->PocketScale.blocked(owner),n->{},owner::setChanged,()->false,1):key instanceof net.aste.just_scale_it.api.BatteryDock?new BatteryDockEnergy(owner,key):new ScaledEnergyStorage(owner,key,savedFactor));
        if(wrapped.supported&&raw instanceof EnergyFields fields)fields.just_scale_it$pocketBind(wrapped,owner);
        unsupported|=!wrapped.supported;return wrapped;
    }
    public void clear(double saved) {savedFactor=Double.isFinite(saved)&&saved>0&&saved<=1?saved:0;if(wrappers!=null)for(var wrapper:wrappers.values())wrapper.resetFactor(savedFactor);}
}
