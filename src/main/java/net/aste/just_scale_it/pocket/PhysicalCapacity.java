package net.aste.just_scale_it.pocket;
/** Physical mB/FE remain real units: resize clamps excess, enlargement only creates empty space. */
public final class PhysicalCapacity {
    public static int capacity(int nominal,double factor){return ScalePower.floor(Math.max(0,nominal)*Math.max(0,Math.min(1,factor)));}
    public static int contents(int amount,int capacity){return Math.max(0,Math.min(amount,Math.max(0,capacity)));}
    private PhysicalCapacity(){}
}
