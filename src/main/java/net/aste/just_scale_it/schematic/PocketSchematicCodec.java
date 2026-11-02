package net.aste.just_scale_it.schematic;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Matrix3dc;
/** Only schematic-local IDs/coordinates are stored: never live plot or weld UUIDs. */
public final class PocketSchematicCodec {
    public static final String KEY="aeroutils_pocket";
    public static final int MAX_PARTS=4096,MAX_BONDS=16384;
    public record Part(UUID id,double scale){}
    public record End(UUID part,BlockPos pos,Direction face,Vector3d anchor){}
    public record Bond(End small,End big,double smallSpan,double bigSpan){}
    public record Data(List<Part> parts,List<Bond> bonds){}
    public static boolean scale(double s){return Double.isFinite(s)&&s>=1.0/1024&&s<=64;}
    public static CompoundTag write(Data data){
        var tag=new CompoundTag();tag.putInt("version",1);var parts=new ListTag();var bonds=new ListTag();
        for(var p:data.parts){var t=new CompoundTag();t.putUUID("id",p.id);t.putDouble("scale",p.scale);parts.add(t);}
        for(var b:data.bonds){var t=new CompoundTag();t.put("small",end(b.small));t.put("big",end(b.big));t.putDouble("small_span",b.smallSpan);t.putDouble("big_span",b.bigSpan);bonds.add(t);}
        tag.put("parts",parts);tag.put("bonds",bonds);return tag;
    }
    public static Data read(CompoundTag tag){
        var parts=new ArrayList<Part>();var bonds=new ArrayList<Bond>();
        if(tag.getInt("version")!=1)return new Data(parts,bonds);
        var ps=tag.getList("parts",Tag.TAG_COMPOUND);var bs=tag.getList("bonds",Tag.TAG_COMPOUND);
        if(ps.size()>MAX_PARTS||bs.size()>MAX_BONDS)return new Data(parts,bonds);
        var ids=new HashSet<UUID>();
        for(var raw:ps){var t=(CompoundTag)raw;if(t.hasUUID("id")&&scale(t.getDouble("scale"))&&ids.add(t.getUUID("id")))parts.add(new Part(t.getUUID("id"),t.getDouble("scale")));}
        for(var raw:bs){var t=(CompoundTag)raw;var a=end(t.getCompound("small"));var b=end(t.getCompound("big"));double x=t.getDouble("small_span"),y=t.getDouble("big_span");
            if(a!=null&&b!=null&&(a.part!=null||b.part!=null)&&(a.part==null||ids.contains(a.part))&&(b.part==null||ids.contains(b.part))&&!Objects.equals(a.part,b.part)
                    &&Double.isFinite(x)&&Double.isFinite(y)&&x>0&&y>0&&x<=64&&y<=64)bonds.add(new Bond(a,b,x,y));
        }
        return new Data(List.copyOf(parts),List.copyOf(bonds));
    }
    private static CompoundTag end(End e){var t=new CompoundTag();if(e.part!=null)t.putUUID("part",e.part);else t.putBoolean("root",true);
        t.putIntArray("pos",new int[]{e.pos.getX(),e.pos.getY(),e.pos.getZ()});t.putInt("face",e.face.get3DDataValue());
        t.putDouble("x",e.anchor.x);t.putDouble("y",e.anchor.y);t.putDouble("z",e.anchor.z);return t;}
    private static End end(CompoundTag t){
        int[] p=t.getIntArray("pos");int f=t.getInt("face");if(p.length!=3||!t.contains("face")||f<0||f>5||!t.hasUUID("part")&&!t.getBoolean("root"))return null;
        var a=new Vector3d(t.getDouble("x"),t.getDouble("y"),t.getDouble("z"));
        if(!t.contains("x")||!t.contains("y")||!t.contains("z")||!Double.isFinite(a.x)||!Double.isFinite(a.y)||!Double.isFinite(a.z)
                ||Math.abs(a.x-p[0])>2||Math.abs(a.y-p[1])>2||Math.abs(a.z-p[2])>2)return null;
        return new End(t.hasUUID("part")?t.getUUID("part"):null,new BlockPos(p[0],p[1],p[2]),Direction.from3DDataValue(f),a);
    }
    /** Block-coordinate transforms rotate unit cubes around their centers, rather than their corners. */
    public static Vector3d point(Vector3dc blockOrigin,Matrix3dc axes,Vector3dc value){
        return new Vector3d(value).sub(.5,.5,.5).mul(axes).add(blockOrigin).add(.5,.5,.5);
    }
    private PocketSchematicCodec(){}
}
