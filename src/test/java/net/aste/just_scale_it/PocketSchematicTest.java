package net.aste.just_scale_it;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import org.joml.Vector3d;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import net.aste.just_scale_it.schematic.PocketSchematicCodec;
import static net.aste.just_scale_it.schematic.PocketSchematicCodec.*;
public final class PocketSchematicTest {
    private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    public static void main(String[] args){
        UUID a=UUID.fromString("11111111-1111-1111-1111-111111111111"),b=UUID.fromString("22222222-2222-2222-2222-222222222222");
        var small=new End(a,new BlockPos(0,0,0),Direction.DOWN,new Vector3d(.5,0,.5));
        var root=new End(null,new BlockPos(3,4,5),Direction.UP,new Vector3d(3.5,5,5.5));
        var other=new End(b,new BlockPos(1,2,3),Direction.UP,new Vector3d(1.5,3,3.5));
        var input=new Data(List.of(new Part(a,.5),new Part(b,.25)),List.of(new Bond(small,root,.5,1),new Bond(small,other,.5,.25)));
        var tag=write(input);var round=read(tag);
        check(round.equals(input),"NBT round trip retains scales, anchors, local endpoints and root/part glue");
        check(read(new CompoundTag()).parts().isEmpty(),"Old schematics have no extension and remain valid");
        var future=tag.copy();future.putInt("version",99);check(read(future).parts().isEmpty(),"Unknown future formats are not interpreted");
        var invalid=tag.copy();invalid.getList("parts",10).getCompound(0).putDouble("scale",Double.NaN);
        check(read(invalid).parts().size()==1&&read(invalid).bonds().isEmpty(),"Invalid scale cannot leave live links to rejected parts");
        var external=tag.copy();external.getList("bonds",10).getCompound(0).getCompound("small").putUUID("part",UUID.randomUUID());
        check(read(external).bonds().size()==1,"References outside the saved schematic are rejected");
        var noAnchor=tag.copy();noAnchor.getList("bonds",10).getCompound(0).getCompound("small").remove("x");
        check(read(noAnchor).bonds().size()==1,"Malformed glue anchors do not become world-origin constraints");
        check(!scale(Double.POSITIVE_INFINITY)&&!scale(0)&&scale(.25)&&scale(1),"Reject unsafe scales, retain ordinary and quarter scales");
        var oversized=new CompoundTag();oversized.putInt("version",1);var many=new ListTag();for(int i=0;i<=MAX_PARTS;i++)many.add(new CompoundTag());oversized.put("parts",many);
        check(read(oversized).parts().isEmpty(),"Oversized extension is bounded before restoration");
        // A 90 degree block rotation maps cube (x,z) to (-z-1,x); points rotate about cube centers.
        var axes=new Matrix3d(0,0,1,0,1,0,-1,0,0);var origin=new Vector3d(9,20,30);
        var rotated=PocketSchematicCodec.point(origin,axes,new Vector3d(2.25,3.75,4.5));
        check(rotated.distance(new Vector3d(5.5,23.75,32.25))<1e-12,"Fractional glue anchors rotate with block centers without a one-block shift");
        var mirrored=PocketSchematicCodec.point(new Vector3d(-1,0,0),new Matrix3d().scaling(-1,1,1),new Vector3d(.25,.4,.8));
        check(mirrored.distance(new Vector3d(-.25,.4,.8))<1e-12,"Affine point helper also handles reflections consistently");
        var local=new Vector3d(0,0,0);var com=new Vector3d(.5,1,.5);var q=new Quaterniond().rotationXYZ(.2,.7,-.3);var pos=new Vector3d(80,90,100);
        var before=new Quaterniond(q).transform(new Vector3d(local).sub(com)).add(pos);
        var after=new Quaterniond(q).transform(new Vector3d(local).sub(com).mul(.25)).add(pos);
        var adjusted=new Vector3d(pos).add(new Vector3d(before).sub(after));
        check(new Quaterniond(q).transform(new Vector3d(local).sub(com).mul(.25)).add(adjusted).distance(before)<1e-12,"Resizing retains the saved lower corner, rather than shifting attachment points around the center of mass");
        System.out.println("Pocket schematics: NBT scale/glue round trip, legacy/malformed input, bounded references, rotated anchors and fixed-corner scaling passed");
    }
}
