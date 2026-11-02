package net.aste.just_scale_it.pocket;
public interface EnergyFields {
    void just_scale_it$pocketSetEnergy(int value);
    EnergyScaleAdapter just_scale_it$pocketAdapter();
    Object just_scale_it$pocketOwner();
    default boolean just_scale_it$pocketExternal(){return just_scale_it$pocketAdapter()!=null&&!PocketEnergyContext.isRaw(this)&&!PocketEnergyContext.isOwner(just_scale_it$pocketOwner());}
    void just_scale_it$pocketBind(EnergyScaleAdapter adapter,Object owner);
}
