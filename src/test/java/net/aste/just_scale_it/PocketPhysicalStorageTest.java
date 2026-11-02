package net.aste.just_scale_it;
import net.aste.just_scale_it.pocket.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
public final class PocketPhysicalStorageTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    private static final class Item implements IEnergyStorage {
        int energy=1000,capacity=1000,rate=100;boolean output=true;
        public int getEnergyStored(){return energy;}public int getMaxEnergyStored(){return capacity;}
        public boolean canReceive(){return true;}public boolean canExtract(){return output;}
        public int receiveEnergy(int n,boolean sim){int v=Math.max(0,Math.min(n,Math.min(rate,capacity-energy)));if(!sim)energy+=v;return v;}
        public int extractEnergy(int n,boolean sim){int v=output?Math.max(0,Math.min(n,Math.min(rate,energy))):0;if(!sim)energy-=v;return v;}
    }
    public static void main(String[] args){
        var item=new Item();double[] factor={.125};var dock=new CappedWorldEnergy(item,()->factor[0]);
        check(dock.trim()&&item.energy==125,"Shrinking a dock removes excess real charge from the item");
        check(dock.getEnergyStored()==125&&dock.getMaxEnergyStored()==125,"No nominal charge is exported through a removable battery");
        int before=item.energy;check(dock.extractEnergy(100,true)==12&&item.energy==before,"World extraction simulation is non-mutating");
        check(dock.extractEnergy(100,false)==12&&item.energy==113,"12 exported FE cost 12 real item FE, not 96");
        factor[0]=1;check(dock.getEnergyStored()==113,"Removing/enlarging a battery cannot multiply charge");
        factor[0]=.125;item.energy=0;check(dock.receiveEnergy(100,true)==12&&item.energy==0,"Dock input simulation");
        check(dock.receiveEnergy(100,false)==12&&item.energy==12,"Reduced input writes real FE into the removable item");
        item.output=false;item.energy=1000;check(!dock.canFit(.125),"A receive-only opaque item requires discharging before shrink");
        item.output=true;item.capacity=1000000;item.energy=1000000;item.rate=1;check(!dock.trim()&&item.energy==999936,"Excess removal has a bounded work budget");
        check(dock.extractEnergy(100,false)==0,"Hidden over-capacity charge cannot be exported while trimming");
        // Exercise many physical fluid resize paths: no resize creates mB and normal drain stays unscaled.
        int liquid=8000,nominal=8000;var random=new java.util.Random(31);
        for(int i=0;i<10000;i++){int old=liquid;double f=Math.pow(.0625+random.nextDouble()*.9375,3);
            int cap=PhysicalCapacity.capacity(nominal,f);liquid=PhysicalCapacity.contents(liquid,cap);
            check(liquid<=old&&liquid<=cap&&liquid>=0,"Resize conserves physical mB except explicit overflow loss");}
        liquid=PhysicalCapacity.contents(8000,PhysicalCapacity.capacity(8000,.125));
        check(liquid==1000,"Half-size 8 bucket tank holds one bucket");
        liquid-=100;check(liquid==900,"Engine consumption remains 100 mB, without a scale conversion");
        liquid=PhysicalCapacity.contents(liquid,PhysicalCapacity.capacity(8000,1));check(liquid==900,"Enlarging adds empty capacity only");
        // Passive wire input converts tiny producer units once; active connector ticks use world units.
        var network=new Item();network.energy=0;
        var world=new EnergyScaleAdapter(network,()->1,()->false,n->network.energy=n,()->{},()->false,1);
        double old=PocketEnergyContext.enter(.125);try{check(world.receiveEnergy(100,false)==96&&network.energy==12,"Wire passive IO stays in world FE");}finally{PocketEnergyContext.restore(old);}
        check(world.extractEnergy(12,false)==12,"Active wire IO does not scale a shared network buffer");
        System.out.println("Pocket physical storage: removable batteries, bounded overflow, passive/active world wire IO, physical fluid capacity and unchanged consumption passed");
    }
}
