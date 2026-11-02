package net.aste.just_scale_it.pocket;
/** Conservative integer FE conversion; enlargement is never a power bonus. */
public final class ScalePower {
    private ScalePower() {}
    public static double factor(double scale,double exponent) {return !Double.isFinite(scale)||scale<=0?0:Math.pow(Math.min(1,scale),exponent);}
    public static int floor(double n) {return !Double.isFinite(n)?(n>0?Integer.MAX_VALUE:0):(int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.floor(n)));}
    public static int ceil(double n) {return !Double.isFinite(n)?(n>0?Integer.MAX_VALUE:0):(int)Math.max(0,Math.min(Integer.MAX_VALUE,Math.ceil(n)));}
    public static int rebase(int stored,int capacity,double oldFactor,double newFactor) {return oldFactor<=0||newFactor<=0?0:Math.min(capacity,floor(floor(stored*oldFactor)/newFactor));}
}
