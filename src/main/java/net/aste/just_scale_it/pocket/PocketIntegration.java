package net.aste.just_scale_it.pocket;

import java.util.*;
import com.misterblusky9.pym.api.Pym;
import com.misterblusky9.pym.api.event.ScaleTickEvent;
import com.misterblusky9.pym.api.event.SubLevelScaleEvent;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.aste.just_scale_it.Config;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

public final class PocketIntegration {
    private static final Direction[] SIDES=Direction.values();
    private static final Map<ServerSubLevel,Double> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    private static Object blacklist;
    private static double powerExponent=-1,fluidExponent=-1;
    private static final ThreadLocal<Boolean> SYNCING=ThreadLocal.withInitial(()->false);
    public static void register() {
        Pym.extensions().register((com.misterblusky9.pym.api.spi.ResizePolicy)(body,from,to)-> {
            if(!PocketRules.active()||to>=1-1e-7||to>=from)return null; // Always allow enlargement/recovery.
            for(var holder:body.getPlot().getLoadedChunks()) {
                var chunk=holder.getChunk();if(chunk==null)continue;
                for(var section:chunk.getSections()) {
                    if(section.hasOnlyAir()||!section.maybeHas(PocketRules::forbidden))continue;
                    for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++) {
                        var state=section.getBlockState(x,y,z);
                        if(PocketRules.forbidden(state))return "Нельзя уменьшить: запрещённый блок "+BuiltInRegistries.BLOCK.getKey(state.getBlock());
                    }
                }
                for(var be:chunk.getBlockEntities().values()) {
                    String reason=incompatibility(be,ScalePower.factor(to,Config.POCKET_POWER_EXPONENT.get()));
                    if(reason!=null)return "Нельзя уменьшить: "+reason;
                }
            }
            return null;
        });
        ShrinkCosts.register();
        NeoForge.EVENT_BUS.addListener(PocketIntegration::scaleTick);
        NeoForge.EVENT_BUS.addListener(PocketIntegration::settled);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,PocketIntegration::place);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST,PocketIntegration::interact);
        NeoForge.EVENT_BUS.addListener(PocketIntegration::stopped);
    }
    public static void syncEnergy(BlockEntity be) {
        if(SYNCING.get()||!PocketRules.present()||be.getLevel()==null||be.getLevel().isClientSide)return;
        SYNCING.set(true);
        try {
            var data=((PocketEnergyOwner)be).just_scale_it$pocketEnergy();
            data.resetProbes();
            boolean energy=false;
            for(Direction side:SIDES)energy|=syncSide(be,side);
            energy|=syncSide(be,null);
            if(!energy)return;
            data.savedFactor=PocketScale.factor(be);
        }finally{SYNCING.set(false);}
    }
    private static boolean syncSide(BlockEntity be,Direction side) {
        var storage=be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),side);
        var fluid=be.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,be.getBlockPos(),side);
        if(fluid!=null)((PocketEnergyOwner)be).just_scale_it$pocketEnergy().bindFluid(be,fluid);
        if(storage instanceof EnergyScaleAdapter scaled){scaled.sync();return true;}
        return false;
    }
    private static void scaleTick(ScaleTickEvent event) {
        var current=Config.POCKET_BLACKLIST.get();
        if(current!=blacklist||powerExponent!=Config.POCKET_POWER_EXPONENT.get()||fluidExponent!=Config.POCKET_FLUID_EXPONENT.get()) {
            blacklist=current;powerExponent=Config.POCKET_POWER_EXPONENT.get();fluidExponent=Config.POCKET_FLUID_EXPONENT.get();LAST.clear();}
        for(var body:event.container().getAllSubLevels()) {
            double factor=PocketRules.active()?Pym.scale().of(body):1;
            Double old=LAST.put(body,factor);
            if(old==null&&factor==1)continue;
            if(old==null||Double.compare(old,factor)!=0)refresh(body);
        }
    }
    private static void settled(SubLevelScaleEvent.Settled event) {refresh(event.subLevel());}
    private static void refresh(ServerSubLevel body) {
        var networks=new HashSet<KineticNetwork>();
        for(var holder:body.getPlot().getLoadedChunks()) {
            var chunk=holder.getChunk();if(chunk==null)continue;
            for(var be:chunk.getBlockEntities().values()) {
                be.getLevel().invalidateCapabilities(be.getBlockPos());
                syncEnergy(be);
                if(be instanceof KineticBlockEntity kinetic&&kinetic.hasNetwork())networks.add(kinetic.getOrCreateNetwork());
            }
        }
        for(var network:networks)network.updateNetwork();
    }
    private static void place(BlockEvent.EntityPlaceEvent event) {
        if(!(event.getLevel() instanceof ServerLevel level))return;
        var snapshots=event instanceof BlockEvent.EntityMultiPlaceEvent multi?multi.getReplacedBlockSnapshots():List.of(event.getBlockSnapshot());
        for(var snapshot:snapshots) {
            var state=snapshot==event.getBlockSnapshot()?event.getPlacedBlock():snapshot.getCurrentState();
            if(PocketScale.at(level,snapshot.getPos())>=1-1e-7)continue;
            String reason=PocketRules.forbidden(state)?"запрещённый блок":null;
            var be=level.getBlockEntity(snapshot.getPos());
            if(reason==null&&be!=null)reason=incompatibility(be,PocketScale.factor(be));
            if(reason!=null){event.setCanceled(true);level.invalidateCapabilities(snapshot.getPos());
                if(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)
                    player.displayClientMessage(Component.literal("Нельзя разместить на уменьшенной конструкции: "+reason),true);
                return;
            }
        }
    }
    private static String incompatibility(BlockEntity be,double targetFactor){
        syncEnergy(be);
        for(int i=0;i<=SIDES.length;i++){
            var storage=be.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,be.getBlockPos(),i==SIDES.length?null:SIDES[i]);
            if(storage instanceof EnergyScaleAdapter scaled&&!scaled.supported)
                return "FE-хранилище "+BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())+" требует адаптера";
            if(storage instanceof BatteryDockEnergy dock&&!dock.canFit(targetFactor))
                return "вставленная батарея не позволяет убрать избыток заряда; сначала разрядите или выньте её";
        }
        if(((PocketEnergyOwner)be).just_scale_it$pocketEnergy().unsupportedFluid)
            return "жидкостное хранилище "+BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())+" требует адаптера";
        return null;
    }
    private static void interact(PlayerInteractEvent.RightClickBlock event) {
        var be=event.getLevel().getBlockEntity(event.getPos());
        if(be!=null&&PocketScale.blocked(be)) {
            event.setCanceled(true);
            if(!event.getLevel().isClientSide)event.getEntity().displayClientMessage(Component.literal("Запрещённый блок недоступен до увеличения конструкции."),true);
        }
    }
    private static void stopped(ServerStoppedEvent event) {LAST.clear();}
    private PocketIntegration() {}
}
