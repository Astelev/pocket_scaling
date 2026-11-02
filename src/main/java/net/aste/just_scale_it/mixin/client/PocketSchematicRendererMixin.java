package net.aste.just_scale_it.mixin.client;
import net.aste.just_scale_it.schematic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import com.mojang.blaze3d.vertex.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
/** Also visits Sable's earlier-injected sublevel drawing handler, whose meshes use the same call. */
@Mixin(value=com.simibubi.create.content.schematics.client.SchematicRenderer.class,remap=false,priority=900)
public abstract class PocketSchematicRendererMixin {
    @Shadow private BlockPos anchor;
    @WrapOperation(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZLnet/minecraft/util/RandomSource;JILnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)V",remap=true))
    private void just_scale_it$scale(ModelBlockRenderer renderer,BlockAndTintGetter level,BakedModel model,BlockState state,BlockPos pos,PoseStack pose,VertexConsumer buffer,boolean sides,RandomSource random,long seed,int overlay,net.neoforged.neoforge.client.model.data.ModelData data,RenderType layer,Operation<Void> original){
        double scale=level instanceof PocketSchematicData meta?meta.just_scale_it$pocketSchematic().getDouble("scale"):1;
        if(!PocketSchematicCodec.scale(scale)||scale==1){original.call(renderer,level,model,state,pos,pose,buffer,sides,random,seed,overlay,data,layer);return;}
        int x=pos.getX()-anchor.getX(),y=pos.getY()-anchor.getY(),z=pos.getZ()-anchor.getZ();
        pose.pushPose();try{
            pose.translate(-x,-y,-z);pose.scale((float)scale,(float)scale,(float)scale);pose.translate(x,y,z);
            original.call(renderer,level,model,state,pos,pose,buffer,sides,random,seed,overlay,data,layer);
        }finally{pose.popPose();}
    }
}
