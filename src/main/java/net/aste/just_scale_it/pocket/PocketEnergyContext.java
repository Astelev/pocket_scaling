package net.aste.just_scale_it.pocket;
/** Nominal machine quantities are scoped to the owner tick; all other access uses world FE. */
public final class PocketEnergyContext {
    private static final class Frame {double factor=1;Object owner,raw;}
    private static final ThreadLocal<Frame> FRAME=ThreadLocal.withInitial(Frame::new);
    public static double factor(){return FRAME.get().factor;}
    public static double enter(double value){var frame=FRAME.get();double old=frame.factor;frame.factor=value;return old;}
    public static void restore(double value){FRAME.get().factor=value;}
    public static Object enterOwner(Object owner){var f=FRAME.get();Object old=f.owner;f.owner=owner;return old;}
    public static void restoreOwner(Object owner){FRAME.get().owner=owner;}
    public static boolean isOwner(Object owner){return owner!=null&&FRAME.get().owner==owner;}
    public static Object enterRaw(Object raw){var f=FRAME.get();Object old=f.raw;f.raw=raw;return old;}
    public static void restoreRaw(Object raw){FRAME.get().raw=raw;}
    public static boolean isRaw(Object raw){return FRAME.get().raw==raw;}
    private PocketEnergyContext(){}
}
