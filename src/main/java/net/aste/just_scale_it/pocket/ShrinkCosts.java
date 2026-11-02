package net.aste.just_scale_it.pocket;

import java.util.*;
import java.util.function.Supplier;
import com.misterblusky9.pym.api.*;
import com.misterblusky9.pocket.scale.ResizeActor;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.aste.just_scale_it.Config;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-side payments, reserved atomically and refunded if the surrounding operation fails. */
public final class ShrinkCosts {
    private static volatile Object config;
    private static List<ShrinkCostRule> rules=List.of();
    private static final ThreadLocal<Scope> PLACEMENT=new ThreadLocal<>();
    private static final ThreadLocal<Map<String,Long>> COLLECT=new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TOOL=ThreadLocal.withInitial(()->false);
    public static boolean active(){return PocketRules.present()&&Config.SHRINK_COST_ENABLED.get();}
    public static void register(){
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST,ShrinkCosts::place);
        Pym.extensions().register((com.misterblusky9.pym.api.spi.ResizePolicy)(body,from,to)->{collect(body,from,to);return null;});
    }
    private static List<ShrinkCostRule> rules(){
        var list=Config.SHRINK_COST_RULES.get();
        // Migrate only the exact previous defaults; custom groups retain their prices/order.
        if(list.equals(List.of(
            "engines|create:steam_engine,create:windmill_bearing,create:water_wheel,create:large_water_wheel,createaddition:electric_motor,aeronautics:propeller_bearing|create:precision_mechanism|1",
            "transmission|create:shaft,create:cogwheel,create:large_cogwheel,create:gearbox,create:vertical_gearbox|create:andesite_alloy|1",
            "other|*|minecraft:iron_nugget|1"))){
            Config.SHRINK_COST_RULES.set(List.of("mechanical|@mechanical,create:steam_engine|create:precision_mechanism|1"));
            Config.SHRINK_COST_RULES.save();list=Config.SHRINK_COST_RULES.get();
        }
        if(config!=list){rules=list.stream().map(ShrinkCostRule::parse).toList();config=list;}return rules;
    }
    private static void quote(Map<String,Long> bill,BlockState state){
        if(state.isAir())return;
        var r=ShrinkCostRule.first(rules(),BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),
            tag->tag.equals("@mechanical")?mechanical(state):state.is(TagKey.create(Registries.BLOCK,ResourceLocation.parse(tag))));
        if(r!=null&&r.count()>0)bill.merge(r.item(),(long)r.count(),Math::addExact);
    }
    private static boolean mechanical(BlockState state){
        var block=state.getBlock();
        return block instanceof com.simibubi.create.content.kinetics.base.IRotate
            ||block instanceof com.simibubi.create.foundation.block.IBE<?> ibe
                &&com.simibubi.create.content.kinetics.base.KineticBlockEntity.class.isAssignableFrom(ibe.getBlockEntityClass());
    }
    private static void collect(ServerSubLevel body,double from,double to){
        var bill=COLLECT.get();
        // Reissuing an existing target while it animates must not bill the same resize again.
        if(bill==null||to>=1-1e-7||to>=Math.min(from,Pym.scale().target(body))-1e-7)return;
        for(var holder:body.getPlot().getLoadedChunks()){
            var chunk=holder.getChunk();if(chunk==null)continue;
            for(var section:chunk.getSections())if(!section.hasOnlyAir())
                for(int y=0;y<16;y++)for(int z=0;z<16;z++)for(int x=0;x<16;x++)quote(bill,section.getBlockState(x,y,z));
        }
    }
    public static final class Scope implements AutoCloseable {
        private final Scope previous;private final ServerPlayer player;private final ItemStack excluded;
        private final List<Receipt> payments=new ArrayList<>();private final List<BlockEvent.EntityPlaceEvent> events=new ArrayList<>();
        private boolean success,denied;
        private Scope(ServerPlayer player,ItemStack excluded){this.player=player;this.excluded=excluded;previous=PLACEMENT.get();PLACEMENT.set(this);}
        public void success(){success=true;}
        public boolean denied(){return denied;}
        @Override public void close(){
            PLACEMENT.set(previous);
            if(!success||events.stream().anyMatch(BlockEvent.EntityPlaceEvent::isCanceled))for(var r:payments)r.refund();
        }
    }
    public static Scope placement(ServerPlayer player,ItemStack excluded){return new Scope(player,excluded);}
    public static <T> T tool(Supplier<T> action){boolean prior=TOOL.get();TOOL.set(true);try{return action.get();}finally{TOOL.set(prior);}}
    private static void place(BlockEvent.EntityPlaceEvent event){
        if(!active()||net.aste.just_scale_it.api.ShrinkCostExemption.active()||!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level))return;
        if(event.getEntity() instanceof ServerPlayer p&&p.isCreative())return;
        var bill=new LinkedHashMap<String,Long>();var seen=new HashSet<net.minecraft.core.BlockPos>();
        var snapshots=event instanceof BlockEvent.EntityMultiPlaceEvent multi?multi.getReplacedBlockSnapshots():List.of(event.getBlockSnapshot());
        for(var snapshot:snapshots)if(seen.add(snapshot.getPos())&&Pym.scale().at(level,snapshot.getPos())<1-1e-7)
            quote(bill,snapshot==event.getBlockSnapshot()?event.getPlacedBlock():snapshot.getCurrentState());
        if(bill.isEmpty())return;
        var scope=PLACEMENT.get();
        if(scope==null||event.getEntity()!=scope.player){
            event.setCanceled(true);if(event.getEntity() instanceof ServerPlayer p)message(p,"just_scale_it.cost.no_context");return;
        }
        var payment=reserve(scope.player,bill,scope.excluded);
        if(payment==null){scope.denied=true;event.setCanceled(true);return;}
        scope.payments.add(payment);scope.events.add(event);
    }
    /** Invoked only around an actual submit; preview check() never consumes inventory. */
    public static ResizeResult resize(ResizeRequest request,ServerSubLevel body,double target,boolean immediate,Supplier<ResizeResult> submit){
        if(!active()||TOOL.get()||net.aste.just_scale_it.api.ShrinkCostExemption.active())return submit.get(); // Tool creation pays once through its placement event.
        var actor=ResizeActor.current();if(actor!=null&&actor.isCreative())return submit.get();
        var bill=new LinkedHashMap<String,Long>();var previous=COLLECT.get();COLLECT.set(bill);
        try{
            if(immediate)collect(body,Pym.scale().of(body),target);
            else {var check=request.check();if(!check.accepted())return check;}
        }finally{COLLECT.set(previous);}
        if(bill.isEmpty())return submit.get();
        if(!(actor instanceof ServerPlayer player))return ResizeResult.refused(ResizeResult.Status.BLOCKED,Component.translatable("just_scale_it.cost.no_player").getString());
        var payment=reserve(player,bill,player.getUseItem().isEmpty()?player.getMainHandItem():player.getUseItem());
        if(payment==null)return ResizeResult.refused(ResizeResult.Status.BLOCKED,Component.translatable("just_scale_it.cost.shortage").getString());
        boolean accepted=false;
        try{var result=submit.get();accepted=result.accepted();return result;}finally{if(!accepted)payment.refund();}
    }
    private record Taken(int slot,ItemStack stack){}
    private static final class Receipt {
        final ServerPlayer player;final List<Taken> taken;
        Receipt(ServerPlayer player,List<Taken> taken){this.player=player;this.taken=taken;}
        void refund(){var inventory=player.getInventory();
            for(var t:taken){var rest=t.stack.copy();var slot=inventory.getItem(t.slot);
                if(slot.isEmpty()){inventory.setItem(t.slot,rest);continue;}
                if(ItemStack.isSameItemSameComponents(slot,rest)){
                    int n=Math.min(rest.getCount(),Math.max(0,slot.getMaxStackSize()-slot.getCount()));slot.grow(n);rest.shrink(n);
                }
                if(!rest.isEmpty()&&!inventory.add(rest))player.drop(rest,false);
            }inventory.setChanged();player.inventoryMenu.broadcastChanges();
        }
    }
    private static Receipt reserve(ServerPlayer player,Map<String,Long> bill,ItemStack excluded){
        var inventory=player.getInventory();var taken=new ArrayList<Taken>();var remaining=new LinkedHashMap<Item,Long>();
        for(var e:bill.entrySet()){
            var id=ResourceLocation.parse(e.getKey());
            if(!BuiltInRegistries.ITEM.containsKey(id)||BuiltInRegistries.ITEM.get(id)==Items.AIR){message(player,"just_scale_it.cost.bad_item",e.getKey());return null;}
            remaining.merge(BuiltInRegistries.ITEM.get(id),e.getValue(),Math::addExact);
        }
        // Plan the whole deduction before changing any slot. Armor is not a consumable resource pool.
        var stock=new ArrayList<ShrinkPaymentPlan.Stock<Item>>();
        for(int slot=0;slot<inventory.getContainerSize();slot++){
            if(slot>=36&&slot<40)continue;var stack=inventory.getItem(slot);
            if(stack!=excluded&&!stack.isEmpty())stock.add(new ShrinkPaymentPlan.Stock<>(slot,stack.getItem(),stack.getCount()));
        }
        var plan=ShrinkPaymentPlan.allocate(remaining,stock);
        if(!plan.accepted()){message(player,"just_scale_it.cost.missing",plan.required(),plan.missing().getDescription());return null;}
        for(var t:plan.takes())taken.add(new Taken(t.slot(),inventory.getItem(t.slot()).copyWithCount(t.count())));
        for(var t:taken)inventory.getItem(t.slot).shrink(t.stack.getCount());
        inventory.setChanged();player.inventoryMenu.broadcastChanges();return new Receipt(player,taken);
    }
    private static void message(ServerPlayer p,String key,Object... args){p.displayClientMessage(Component.translatable(key,args),true);}
    private ShrinkCosts(){}
}
