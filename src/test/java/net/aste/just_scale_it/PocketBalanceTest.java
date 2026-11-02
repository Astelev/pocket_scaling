package net.aste.just_scale_it;
import net.aste.just_scale_it.pocket.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
public final class PocketBalanceTest {
    private static void check(boolean condition,String why) {if(!condition)throw new AssertionError(why);}
    private static final class Buffer implements IEnergyStorage {
        int energy,capacity=1000,rate=100;
        public int receiveEnergy(int n,boolean simulate) {int a=Math.max(0,Math.min(Math.min(n,rate),capacity-energy));if(!simulate)energy+=a;return a;}
        public int extractEnergy(int n,boolean simulate) {int a=Math.max(0,Math.min(Math.min(n,rate),energy));if(!simulate)energy-=a;return a;}
        public int getEnergyStored(){return energy;}public int getMaxEnergyStored(){return capacity;}
        public boolean canExtract(){return true;}public boolean canReceive(){return true;}
    }
    /** Emulates a standard storage with the same shared routing rule used by EnergyStorageMixin. */
    private static final class BoundBuffer implements IEnergyStorage,EnergyFields {
        int energy,capacity=10000,rate=1000;
        Object owner;EnergyScaleAdapter adapter;
        public void just_scale_it$pocketSetEnergy(int n){energy=Math.max(0,Math.min(capacity,n));}
        public EnergyScaleAdapter just_scale_it$pocketAdapter(){return adapter;}
        public Object just_scale_it$pocketOwner(){return owner;}
        public void just_scale_it$pocketBind(EnergyScaleAdapter a,Object o){adapter=a;owner=o;}
        public int getEnergyStored(){return energy;}
        public int getMaxEnergyStored(){return capacity;}
        public boolean canReceive(){return true;}public boolean canExtract(){return true;}
        public int receiveEnergy(int n,boolean simulate){
            if(just_scale_it$pocketExternal())return adapter.receiveEnergy(n,simulate);
            int a=Math.max(0,Math.min(Math.min(n,rate),capacity-energy));if(!simulate)energy+=a;return a;
        }
        public int extractEnergy(int n,boolean simulate){
            if(just_scale_it$pocketExternal())return adapter.extractEnergy(n,simulate);
            int a=Math.max(0,Math.min(Math.min(n,rate),energy));if(!simulate)energy-=a;return a;
        }
    }
    private static void directStorageRegression() {
        Object machine=new Object(),other=new Object();var raw=new BoundBuffer();
        var adapter=new EnergyScaleAdapter(raw,()->.125,()->false,raw::just_scale_it$pocketSetEnergy,()->{},()->true,.125);
        raw.just_scale_it$pocketBind(adapter,machine);
        double before=PocketEnergyContext.enter(.125);Object old=PocketEnergyContext.enterOwner(machine);
        try {
            check(raw.receiveEnergy(1000,false)==1000&&raw.energy==1000,"Internal generation uses nominal units once");
            check(raw.getEnergyStored()==1000,"Owner tick uses nominal charge for its ordinary thresholds");
            check(raw.extractEnergy(100,false)==100,"Owner internal consumption stays nominal");
            check(raw.energy==900,"Internal consumption changes actual backing storage");
        }finally{PocketEnergyContext.restoreOwner(old);PocketEnergyContext.restore(before);}
        check(adapter.getEnergyStored()==112&&adapter.getMaxEnergyStored()==1250,"Public capability getters show world FE");
        int saved=raw.energy;
        check(raw.extractEnergy(1000,true)==112&&raw.energy==saved,"Direct raw simulation cannot bypass scaled extraction");
        check(raw.extractEnergy(1000,false)==112&&raw.energy==4,"Direct raw export obeys the same budget as capability export");
        check(raw.receiveEnergy(1000,false)==125&&raw.energy==1004,"Direct raw import rate is scaled once");
        old=PocketEnergyContext.enterOwner(other);before=PocketEnergyContext.enter(.5);
        try {check(raw.extractEnergy(1000,false)==250,"Different machine's nominal units convert from world FE exactly once");}
        finally{PocketEnergyContext.restoreOwner(old);PocketEnergyContext.restore(before);}
        check(!PocketEnergyContext.isOwner(machine)&&!PocketEnergyContext.isRaw(raw),"Owner/raw scopes are restored after nested transfers");
        // Full-size generator really exports 8x more FE than a half-size one.
        var big=new BoundBuffer();Object bigOwner=new Object();
        var bigAdapter=new EnergyScaleAdapter(big,()->1,()->false,big::just_scale_it$pocketSetEnergy,()->{},()->true,1);
        big.just_scale_it$pocketBind(bigAdapter,bigOwner);big.energy=1000;
        raw.energy=1000;
        check(big.extractEnergy(1000,false)==1000&&raw.extractEnergy(1000,false)==125,"Actual standard-storage output is reduced, not just its displayed capacity");
        // Full-size and miniature consumers both spend 1000 nominal units of internally stored charge.
        big.energy=1000;raw.energy=1000;old=PocketEnergyContext.enterOwner(machine);before=PocketEnergyContext.enter(.125);
        try{raw.extractEnergy(1000,false);}finally{PocketEnergyContext.restoreOwner(old);PocketEnergyContext.restore(before);}
        check(adapter.getEnergyStored()==0&&bigAdapter.getEnergyStored()==1000,"Miniature internal consumption drains 125 world FE rather than 1000");
        check(raw.getEnergyStored()==raw.energy&&raw.getMaxEnergyStored()==raw.capacity,"Native fields/getters remain nominal for save and non-tick internal checks");
        adapter.resetFactor(.125);check(adapter.getMaxEnergyStored()==1250,"NBT factor reset retains direct-storage binding without recursion");
    }
    public static void main(String[] args) {
        directStorageRegression();
        check(ScalePower.factor(.5,3)==.125,"Half linear size is 1/8 SU/FE");
        check(ScalePower.factor(2,3)==1,"No enlargement generator bonus");
        check(ScalePower.ceil(1e-20)==1,"Tiny charge cannot be accepted free");
        var raw=new Buffer();double[] factor={.125};boolean[] blocked={false};
        var scaled=new EnergyScaleAdapter(raw,()->factor[0],()->blocked[0],n->raw.energy=n,()->{},()->true,.125);
        check(scaled.getMaxEnergyStored()==125,"Capacity scales too");
        int simulated=scaled.receiveEnergy(100,true);check(simulated==13&&raw.energy==0,"Receive simulation is non-mutating and rounds costs up");
        check(scaled.receiveEnergy(100,false)==simulated&&raw.energy==100,"Actual receive matches simulation");
        int before=raw.energy;check(scaled.extractEnergy(100,true)==12&&raw.energy==before,"Extract simulation is non-mutating");
        check(scaled.extractEnergy(100,false)==12&&raw.energy==4,"Only paid whole FE can be exported");
        check(scaled.extractEnergy(100,false)==0&&raw.energy==4,"Do not burn a sub-FE residue");
        raw.energy=1000;factor[0]=1;scaled.sync();check(raw.energy==125,"Enlarging a tiny charged engine cannot multiply energy");
        int initial=scaled.getEnergyStored();
        for(int i=0;i<100;i++){factor[0]=.125;scaled.sync();factor[0]=1;scaled.sync();}
        check(scaled.getEnergyStored()<=initial,"Repeated resize cannot generate charge");
        factor[0]=.125;scaled.sync();raw.energy=100;blocked[0]=true;
        check(scaled.receiveEnergy(100,false)==0&&scaled.extractEnergy(100,false)==0,"Forbidden existing block cannot transfer FE");
        blocked[0]=false;
        var unsupported=new EnergyScaleAdapter(raw,()->factor[0],()->false,null,()->{},()->true,.125);
        check(unsupported.extractEnergy(100,false)==0,"Unsupported shrunk storage fails closed");
        factor[0]=1;check(unsupported.extractEnergy(10,false)==10,"Ordinary custom storage retains its contract");
        var destination=new Buffer();
        var normal=new EnergyScaleAdapter(destination,()->1,()->false,n->destination.energy=n,()->{},()->true,1);
        double previous=PocketEnergyContext.enter(.125);
        try {
            check(normal.receiveEnergy(100,true)==96&&destination.energy==0,"Miniature generator push is converted even when destination is normal-sized");
            check(normal.receiveEnergy(100,false)==96&&destination.energy==12,"100 miniature nominal FE cannot become 100 world FE");
        }finally{PocketEnergyContext.restore(previous);}
        check(normal.getEnergyStored()==12,"External network sees world FE");
        var micro=new Buffer();
        var microAdapter=new EnergyScaleAdapter(micro,()->1.0/4096,()->false,n->micro.energy=n,()->{},()->true,1.0/4096);
        check(microAdapter.receiveEnergy(1,false)==0&&micro.energy==0,"Zero visible capacity cannot accept charge");
        System.out.println("Pocket balance: volume scaling, FE simulations/rounding, rate/capacity, resize conservation, unsupported adapters and forbidden blocks passed");
    }
}
