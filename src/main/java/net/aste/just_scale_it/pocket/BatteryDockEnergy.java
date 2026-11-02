package net.aste.just_scale_it.pocket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
public final class BatteryDockEnergy extends EnergyScaleAdapter {
    private final CappedWorldEnergy physical;
    private final BlockEntity owner;
    private IEnergyStorage backing;
    public BatteryDockEnergy(BlockEntity owner,IEnergyStorage raw){this(owner,new CappedWorldEnergy(raw,()->PocketScale.factor(owner)));backing=raw;}
    private BatteryDockEnergy(BlockEntity owner,CappedWorldEnergy physical){
        super(physical,()->1,()->PocketScale.blocked(owner),n->{},owner::setChanged,()->false,1);this.physical=physical;this.owner=owner;
    }
    @Override public double sync(){if(owner.getLevel()!=null&&!owner.getLevel().isClientSide){
        if(backing instanceof net.aste.just_scale_it.api.BatteryDock dock)dock.limitFallback(PocketScale.factor(owner));physical.trim();}return 1;}
    public boolean canFit(double factor){return physical.canFit(factor);}
}
