package net.aste.just_scale_it.pocket;
import java.util.function.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
/** FE accounting independent of Minecraft. Raw nominal units are never exposed to external energy networks. */
public class EnergyScaleAdapter implements IEnergyStorage {
    protected final IEnergyStorage source;
    private final DoubleSupplier factor;
    private final BooleanSupplier blocked,mayRebase;
    private final IntConsumer setEnergy;
    private final Runnable changed;
    public final boolean supported;
    private double lastFactor;
    public EnergyScaleAdapter(IEnergyStorage source,DoubleSupplier factor,BooleanSupplier blocked,IntConsumer setEnergy,
            Runnable changed,BooleanSupplier mayRebase,double previous) {
        this.source=source;this.factor=factor;this.blocked=blocked;this.setEnergy=setEnergy;this.changed=changed;
        this.mayRebase=mayRebase;supported=setEnergy!=null;lastFactor=previous>0?previous:factor.getAsDouble();
    }
    public void resetFactor(double previous) {lastFactor=previous>0?previous:factor.getAsDouble();}
    private int rawStored(){Object old=PocketEnergyContext.enterRaw(source);try{return source.getEnergyStored();}finally{PocketEnergyContext.restoreRaw(old);}}
    private int rawCapacity(){Object old=PocketEnergyContext.enterRaw(source);try{return source.getMaxEnergyStored();}finally{PocketEnergyContext.restoreRaw(old);}}
    private int rawReceive(int n,boolean simulate){Object old=PocketEnergyContext.enterRaw(source);try{return source.receiveEnergy(n,simulate);}finally{PocketEnergyContext.restoreRaw(old);}}
    private int rawExtract(int n,boolean simulate){Object old=PocketEnergyContext.enterRaw(source);try{return source.extractEnergy(n,simulate);}finally{PocketEnergyContext.restoreRaw(old);}}
    private boolean rawCanExtract(){Object old=PocketEnergyContext.enterRaw(source);try{return source.canExtract();}finally{PocketEnergyContext.restoreRaw(old);}}
    private boolean rawCanReceive(){Object old=PocketEnergyContext.enterRaw(source);try{return source.canReceive();}finally{PocketEnergyContext.restoreRaw(old);}}
    public double sync() {
        double f=factor.getAsDouble();
        if(supported&&mayRebase.getAsBoolean()&&Double.compare(lastFactor,f)!=0) {
            setEnergy.accept(ScalePower.rebase(rawStored(),rawCapacity(),lastFactor,f));
            lastFactor=f;changed.run();
        }
        return f;
    }
    private double usable() {double f=sync();return blocked.getAsBoolean()||(!supported&&f<1-1e-7)?0:f;}
    private int receiveWorld(int maximum,boolean simulate) {
        double f=usable();if(maximum<=0||f<=0||ScalePower.floor(rawCapacity()*f)==0)return 0;
        int possible=rawReceive(ScalePower.floor(maximum/f),true);if(possible<=0)return 0;
        int accepted=simulate?possible:rawReceive(possible,false);
        return Math.min(maximum,ScalePower.ceil(accepted*f));
    }
    private int extractWorld(int maximum,boolean simulate) {
        double f=usable();if(maximum<=0||f<=0)return 0;
        int possible=rawExtract(ScalePower.floor(maximum/f),true),visible=ScalePower.floor(possible*f);
        if(visible<=0)return 0;
        int debit=ScalePower.ceil(visible/f);
        return Math.min(maximum,ScalePower.floor((simulate?debit:rawExtract(debit,false))*f));
    }
    private int storedWorld() {double f=usable();return ScalePower.floor(rawStored()*f);}
    private int capacityWorld() {double f=usable();return ScalePower.floor(rawCapacity()*f);}
    @Override public int receiveEnergy(int maximum,boolean simulate) {
        if(maximum<=0)return 0;
        double caller=PocketEnergyContext.factor();if(caller<=0)return 0;
        if(caller==1)return receiveWorld(maximum,simulate);
        return Math.min(maximum,ScalePower.ceil(receiveWorld(ScalePower.floor(maximum*caller),simulate)/caller));
    }
    @Override public int extractEnergy(int maximum,boolean simulate) {
        if(maximum<=0)return 0;
        double caller=PocketEnergyContext.factor();if(caller<=0)return 0;
        if(caller==1)return extractWorld(maximum,simulate);
        return Math.min(maximum,ScalePower.floor(extractWorld(ScalePower.floor(maximum*caller),simulate)/caller));
    }
    @Override public int getEnergyStored() {double c=PocketEnergyContext.factor();return c<=0?0:ScalePower.floor(storedWorld()/c);}
    @Override public int getMaxEnergyStored() {double c=PocketEnergyContext.factor();return c<=0?0:ScalePower.floor(capacityWorld()/c);}
    @Override public boolean canExtract() {return usable()>0&&rawCanExtract();}
    @Override public boolean canReceive() {return usable()>0&&rawCanReceive();}
}
