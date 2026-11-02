package net.aste.just_scale_it.schematic;
import java.util.*;
import java.util.function.Function;
import com.misterblusky9.pym.api.Pym;
import com.misterblusky9.pocket.compat.simulated.*;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.*;
import net.aste.just_scale_it.JustScaleIt;
import net.aste.just_scale_it.pocket.ShrinkCosts;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Matrix3d;
import static net.aste.just_scale_it.schematic.PocketSchematicCodec.*;
/** Extends Sable's own schematic UUID/coordinate remapping, rather than serializing live plot data. */
public final class PocketSchematics {
    private static final ThreadLocal<Session> SESSION=new ThreadLocal<>();
    public static final class Session implements AutoCloseable {
        final Session previous;final Level level;final ServerPlayer player;final SubLevel sourceRoot;
        final SubLevelSchematicSerializationContext originalContext;
        SubLevelSchematicSerializationContext context;CompoundTag metadata=new CompoundTag();
        Session(Level level,ServerPlayer player,BlockPos from,BlockPos to){
            previous=SESSION.get();this.level=level;this.player=player;
            originalContext=SubLevelSchematicSerializationContext.getCurrentContext();
            sourceRoot=from==null?null:Sable.HELPER.getContaining(level,new Vector3d((from.getX()+(double)to.getX()+1)/2,(from.getY()+(double)to.getY()+1)/2,(from.getZ()+(double)to.getZ()+1)/2));
            SESSION.set(this);
        }
        public void placed(){if(player!=null&&context!=null)restore(this);}
        @Override public void close(){
            try{if(SubLevelSchematicSerializationContext.getCurrentContext()!=originalContext)SubLevelSchematicSerializationContext.setCurrentContext(originalContext);}
            finally{SESSION.set(previous);}
        }
    }
    public static Session export(Level level,BlockPos from,BlockPos to){return new Session(level,null,from,to);}
    public static Session paste(ServerPlayer player){return new Session(player.serverLevel(),player,null,null);}
    public static void contextChanging(SubLevelSchematicSerializationContext next){
        var s=SESSION.get();var old=SubLevelSchematicSerializationContext.getCurrentContext();
        if(s!=null&&next==null&&old!=null)s.context=old;
    }
    public static void loaded(StructureTemplate template){
        var s=SESSION.get();if(s!=null&&s.player!=null)s.metadata=((PocketSchematicData)template).just_scale_it$pocketSchematic().copy();
    }
    public static void saving(CompoundTag root){
        var s=SESSION.get();if(s==null||s.player!=null)return;
        var ctx=s.context!=null?s.context:SubLevelSchematicSerializationContext.getCurrentContext();
        if(ctx==null||ctx.getType()!=SubLevelSchematicSerializationContext.Type.SAVE)return;
        var container=SubLevelContainer.getContainer(s.level);if(container==null)return;
        double rootScale=s.sourceRoot==null?1:Pym.scale().of(s.sourceRoot);
        var parts=new ArrayList<Part>();var bonds=new ArrayList<Bond>();var included=new HashSet<UUID>();
        var entries=root.getList("sub_levels",Tag.TAG_COMPOUND);var byId=new HashMap<UUID,CompoundTag>();
        for(var raw:entries){var t=(CompoundTag)raw;if(t.hasUUID("uuid"))byId.put(t.getUUID("uuid"),t);}
        for(var e:ctx.getMappings().entrySet()){
            var body=container.getSubLevel(e.getKey());if(body==null||body.isRemoved())continue;
            var id=e.getValue().newUUID();double factor=Pym.scale().of(body)/rootScale;
            if(!byId.containsKey(id)||!scale(factor)||parts.size()>=MAX_PARTS)continue;
            parts.add(new Part(id,factor));included.add(e.getKey());
            var tag=new CompoundTag();tag.putDouble("scale",factor);byId.get(id).put(KEY,tag);
        }
        for(var weld:CrossScaleWelds.records(s.level)){
            if(bonds.size()>=MAX_BONDS)break;
            var a=saveEnd(s,ctx,included,weld.smallSubLevel(),weld.smallPos(),weld.smallFacing(),weld.smallAnchor());
            var b=saveEnd(s,ctx,included,weld.bigSubLevel(),weld.bigPos(),weld.bigFacing(),weld.bigAnchor());
            if(a!=null&&b!=null&&(a.part()!=null||b.part()!=null))bonds.add(new Bond(a,b,weld.smallSpan()/rootScale,weld.bigSpan()/rootScale));
        }
        root.put(KEY,write(new Data(parts,bonds)));
    }
    private static End saveEnd(Session s,SubLevelSchematicSerializationContext ctx,Set<UUID> included,UUID id,BlockPos pos,Direction face,Vector3d anchor){
        if(id!=null&&included.contains(id)){
            var mapping=ctx.getMapping(id);return new End(mapping.newUUID(),mapping.transform().apply(pos),face,point(mapping.transform()::apply,anchor));
        }
        if(id==null?s.sourceRoot!=null:s.sourceRoot==null||!id.equals(s.sourceRoot.getUniqueId()))return null;
        var bounds=ctx.getBoundingBox();
        if(pos.getX()<bounds.minX()||pos.getX()>bounds.maxX()||pos.getY()<bounds.minY()||pos.getY()>bounds.maxY()||pos.getZ()<bounds.minZ()||pos.getZ()>bounds.maxZ())return null;
        var transform=ctx.getPlaceTransform();return new End(null,transform.apply(pos),face,point(transform::apply,anchor));
    }
    private record Resolved(SubLevel body,BlockPos pos,Direction face,Vector3d anchor){}
    private static void restore(Session s){
        if(!(s.level instanceof ServerLevel level)||!s.player.isCreative()||s.context.getType()!=SubLevelSchematicSerializationContext.Type.PLACE)return;
        var data=read(s.metadata);if(data.parts().isEmpty())return;
        var container=SubLevelContainer.getContainer(level);if(container==null)return;
        var ctx=s.context;
        Function<BlockPos,BlockPos> rootTransform=p->ctx.getPlaceTransform().apply(ctx.getSetupTransform().apply(p));
        var rootBody=Sable.HELPER.getContaining(level,rootTransform.apply(BlockPos.ZERO));double rootScale=rootBody==null?1:Pym.scale().of(rootBody);
        var restored=new HashSet<UUID>();boolean failed=false;
        for(var part:data.parts()){
            var mapping=ctx.getMapping(part.id());if(mapping==null)continue;
            if(!(container.getSubLevel(mapping.newUUID()) instanceof ServerSubLevel body)||body.isRemoved())continue;
            try{
                double target=part.scale()*rootScale;if(!scale(target))throw new IllegalArgumentException("Unsupported schematic scale");
                var origin=body.getPlot().getCenterBlock();var corner=new Vector3d(origin.getX(),origin.getY(),origin.getZ());
                var worldCorner=new Vector3d(Pym.frames().toWorld(body,corner));
                body.buildMassTracker();body.updateMergedMassData((float)container.physicsSystem().getPartialPhysicsTick());
                if(body.getMassTracker().getCenterOfMass()==null||body.getMassTracker().getMass()<=0)throw new IllegalStateException("Schematic body has no physical mass");
                var check=Pym.resize().request(body).scaleTo(target).propagate(false).check();
                if(!check.accepted())throw new IllegalStateException(check.describe());
                var result=ShrinkCosts.tool(()->Pym.resize().request(body).scaleTo(target).immediate().submit());
                if(!result.accepted())throw new IllegalStateException(result.describe());
                // Native Sable stores the plot's lower corner, not its center of mass: keep that corner fixed.
                body.logicalPose().position().add(worldCorner.sub(Pym.frames().toWorld(body,corner)));
                container.physicsSystem().getPipeline().teleport(body,body.logicalPose().position(),body.logicalPose().orientation());
                body.updateLastPose();body.forceUpdateGlobalBounds();restored.add(part.id());
            }catch(RuntimeException ex){failed=true;JustScaleIt.LOGGER.warn("Could not restore schematic Pocket scale for {}",part.id(),ex);}
        }
        if(!restoreBonds(level,s.metadata,ctx,restored))failed=true;
        if(failed)s.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("just_scale_it.schematic.partial"),false);
    }
    /** Shared by creative paste and bee construction; all endpoints resolve only through this copy's map. */
    public static boolean restoreBonds(ServerLevel level,CompoundTag metadata,SubLevelSchematicSerializationContext ctx,Set<UUID> restored){
        var data=read(metadata);Function<BlockPos,BlockPos> rootTransform=p->ctx.getPlaceTransform().apply(ctx.getSetupTransform().apply(p));
        var rootBody=Sable.HELPER.getContaining(level,rootTransform.apply(BlockPos.ZERO));double rootScale=rootBody==null?1:Pym.scale().of(rootBody);
        int added=0;boolean success=true;
        for(var bond:data.bonds())try{
            var a=resolve(level,ctx,bond.small(),restored,rootTransform);var b=resolve(level,ctx,bond.big(),restored,rootTransform);
            if(a==null||b==null||a.body==b.body){success=false;continue;}
            double smallSpan=bond.smallSpan()*rootScale,bigSpan=bond.bigSpan()*rootScale;
            if(a.body==null){var swap=a;a=b;b=swap;double span=smallSpan;smallSpan=bigSpan;bigSpan=span;}
            if(a.body==null){success=false;continue;}
            var record=new WeldRecord(UUID.randomUUID(),a.body.getUniqueId(),b.body==null?null:b.body.getUniqueId(),a.pos,b.pos,a.face,b.face,
                a.anchor,b.anchor,smallSpan,bigSpan,CrossScaleWelds.relativeOrientation(a.body,b.body));
            WeldStore.get(level).add(record);added++;
        }catch(RuntimeException ex){success=false;JustScaleIt.LOGGER.warn("Could not restore schematic Pocket glue",ex);}
        if(added>0){CrossScaleWelds.invalidate(level);CrossScaleWelds.refreshLoaded(level);}return success;
    }
    private static Resolved resolve(Level level,SubLevelSchematicSerializationContext ctx,End end,Set<UUID> restored,Function<BlockPos,BlockPos> rootTransform){
        var container=SubLevelContainer.getContainer(level);SubLevel body;Function<BlockPos,BlockPos> transform;Direction face=end.face();
        if(end.part()==null){transform=rootTransform;var zero=transform.apply(BlockPos.ZERO);var direction=transform.apply(new BlockPos(face.getStepX(),face.getStepY(),face.getStepZ())).subtract(zero);
            face=Direction.getNearest((float)direction.getX(),(float)direction.getY(),(float)direction.getZ());body=Sable.HELPER.getContaining(level,transform.apply(end.pos()));
        }else{
            if(!restored.contains(end.part()))return null;var mapping=ctx.getMapping(end.part());if(mapping==null)return null;
            body=container.getSubLevel(mapping.newUUID());if(body==null||body.isRemoved())return null;transform=mapping.transform()::apply;
        }
        var pos=transform.apply(end.pos());if(!level.hasChunkAt(pos)||level.getBlockState(pos).isAir())return null;
        return new Resolved(body,pos,face,point(transform,end.anchor()));
    }
    public static Vector3d point(Function<BlockPos,BlockPos> transform,Vector3dc value){
        var o=transform.apply(BlockPos.ZERO);var x=transform.apply(new BlockPos(1,0,0)).subtract(o);var y=transform.apply(new BlockPos(0,1,0)).subtract(o);var z=transform.apply(new BlockPos(0,0,1)).subtract(o);
        var axes=new Matrix3d(x.getX(),x.getY(),x.getZ(),y.getX(),y.getY(),y.getZ(),z.getX(),z.getY(),z.getZ());
        return PocketSchematicCodec.point(new Vector3d(o.getX(),o.getY(),o.getZ()),axes,value);
    }
    private PocketSchematics(){}
}
