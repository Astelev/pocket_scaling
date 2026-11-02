package net.aste.just_scale_it.api;
/** Synchronous scoped exemption for trusted construction integrations. Never retain across ticks. */
public final class ShrinkCostExemption implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH=ThreadLocal.withInitial(()->0);
    private boolean closed;
    private ShrinkCostExemption(){DEPTH.set(DEPTH.get()+1);}
    public static ShrinkCostExemption enter(){return new ShrinkCostExemption();}
    public static boolean active(){return DEPTH.get()>0;}
    @Override public void close(){if(!closed){closed=true;int n=DEPTH.get()-1;if(n<=0)DEPTH.remove();else DEPTH.set(n);}}
}
