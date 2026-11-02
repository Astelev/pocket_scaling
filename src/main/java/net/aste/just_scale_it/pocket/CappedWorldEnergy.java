package net.aste.just_scale_it.pocket;
import java.util.function.DoubleSupplier;
import net.neoforged.neoforge.energy.IEnergyStorage;
/** Item batteries contain real FE. Changing their dock size changes capacity, never the charge unit. */
public final class CappedWorldEnergy implements IEnergyStorage {
    private final IEnergyStorage source;private final DoubleSupplier factor;
    public CappedWorldEnergy(IEnergyStorage source,DoubleSupplier factor){this.source=source;this.factor=factor;}
    private double scale(){return Math.max(0,Math.min(1,factor.getAsDouble()));}
    public int capacity(){return ScalePower.floor(source.getMaxEnergyStored()*scale());}
    /** Bounded work even for exotic items with a 1 FE extraction limit. No unbounded resize loop. */
    public boolean trim(){Object old=PocketEnergyContext.enterRaw(source);try{
        int cap=capacity();for(int i=0;i<64;i++){int excess=source.getEnergyStored()-cap;if(excess<=0)return true;
            if(source.extractEnergy(excess,false)<=0)return false;}
        return source.getEnergyStored()<=cap;
    }finally{PocketEnergyContext.restoreRaw(old);}}
    public boolean canFit(double target){Object old=PocketEnergyContext.enterRaw(source);try{
        int excess=source.getEnergyStored()-ScalePower.floor(source.getMaxEnergyStored()*target);
        return excess<=0||source.extractEnergy(excess,true)>0;
    }finally{PocketEnergyContext.restoreRaw(old);}}
    @Override public int getEnergyStored(){return Math.min(source.getEnergyStored(),capacity());}
    @Override public int getMaxEnergyStored(){return capacity();}
    @Override public int receiveEnergy(int n,boolean simulate){Object old=PocketEnergyContext.enterRaw(source);try{
        int room=capacity()-source.getEnergyStored();double f=scale();if(n<=0||room<=0||f<=0)return 0;
        int rate=ScalePower.floor(source.receiveEnergy(ScalePower.floor(n/f),true)*f);
        return source.receiveEnergy(Math.min(n,Math.min(room,rate)),simulate);
    }finally{PocketEnergyContext.restoreRaw(old);}}
    @Override public int extractEnergy(int n,boolean simulate){Object old=PocketEnergyContext.enterRaw(source);try{
        double f=scale();if(n<=0||f<=0||source.getEnergyStored()>capacity())return 0;
        int rate=ScalePower.floor(source.extractEnergy(ScalePower.floor(n/f),true)*f);
        return source.extractEnergy(Math.min(n,rate),simulate);
    }finally{PocketEnergyContext.restoreRaw(old);}}
    @Override public boolean canReceive(){Object old=PocketEnergyContext.enterRaw(source);try{return source.canReceive();}finally{PocketEnergyContext.restoreRaw(old);}}
    @Override public boolean canExtract(){Object old=PocketEnergyContext.enterRaw(source);try{return source.canExtract();}finally{PocketEnergyContext.restoreRaw(old);}}
}
