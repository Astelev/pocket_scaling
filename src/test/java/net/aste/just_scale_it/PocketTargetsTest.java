package net.aste.just_scale_it;
import java.io.*;
import java.nio.file.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
/** Inspect the shipped jars and mapped runtime bytecode without booting Minecraft. */
public final class PocketTargetsTest {
    private static ClassNode read(InputStream in)throws Exception {try(in){var n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;}}
    private static ClassNode jar(JarFile jar,String name)throws Exception{return read(jar.getInputStream(jar.getJarEntry(name+".class")));}
    private static ClassNode runtime(String name)throws Exception{return read(PocketTargetsTest.class.getClassLoader().getResourceAsStream(name+".class"));}
    private static MethodNode method(ClassNode n,String name,String desc){return n.methods.stream().filter(m->m.name.equals(name)&&m.desc.equals(desc)).findFirst().orElseThrow(()->new AssertionError(n.name+"."+name+desc));}
    private static void calls(MethodNode m,String owner,String name,String desc) {
        for(var i:m.instructions)if(i instanceof MethodInsnNode call&&call.owner.equals(owner)&&call.name.equals(name)&&call.desc.equals(desc))return;
        throw new AssertionError("Missing live call "+m.name+" -> "+owner+"."+name+desc);
    }
    public static void main(String[] args)throws Exception {
        try(var create=new JarFile(args[0]);var pym=new JarFile(args[1]);var sable=new JarFile(args[2])) {
            String be="Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;";
            var network=jar(create,"com/simibubi/create/content/kinetics/KineticNetwork");
            method(network,"getActualCapacityOf","("+be+")F");method(network,"getActualStressOf","("+be+")F");
            calls(method(network,"calculateCapacity","()F"),network.name,"getActualCapacityOf","("+be+")F");
            calls(method(network,"calculateStress","()F"),network.name,"getActualStressOf","("+be+")F");
            var pipeline=jar(sable,"dev/ryanhcode/sable/api/physics/PhysicsPipeline");
            method(pipeline,"applyLinearAndAngularImpulse","(Ldev/ryanhcode/sable/api/physics/PhysicsPipelineBody;Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;Z)V");
            var container=jar(sable,"dev/ryanhcode/sable/api/sublevel/SubLevelContainer");
            method(container,"allocateNewSubLevel","(Ldev/ryanhcode/sable/companion/math/Pose3d;)Ldev/ryanhcode/sable/sublevel/SubLevel;");
            method(jar(sable,"dev/ryanhcode/sable/api/SubLevelAssemblyHelper"),"markAndNotifyBlock","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;II)V");
            method(jar(sable,"dev/ryanhcode/sable/sublevel/ServerSubLevel"),"updateMergedMassData","(F)V");
            var context=jar(sable,"dev/ryanhcode/sable/api/schematic/SubLevelSchematicSerializationContext");
            method(context,"setCurrentContext","(Ldev/ryanhcode/sable/api/schematic/SubLevelSchematicSerializationContext;)V");
            method(context,"getMappings","()Ljava/util/Map;");
            var mapping=jar(sable,"dev/ryanhcode/sable/api/schematic/SubLevelSchematicSerializationContext$SchematicMapping");
            method(mapping,"newUUID","()Ljava/util/UUID;");method(mapping,"transform","()Lit/unimi/dsi/fastutil/Function;");
            var exporter=jar(create,"com/simibubi/create/content/schematics/SchematicExport");
            calls(method(exporter,"saveSchematic","(Ljava/nio/file/Path;Ljava/lang/String;ZLnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Lcom/simibubi/create/content/schematics/SchematicExport$SchematicExportResult;"),"net/minecraft/nbt/NbtIo","writeCompressed","(Lnet/minecraft/nbt/CompoundTag;Ljava/io/OutputStream;)V");
            method(jar(create,"com/simibubi/create/content/schematics/SchematicItem"),"loadSchematic","(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;");
            method(jar(create,"com/simibubi/create/content/schematics/packet/SchematicPlacePacket"),"handle","(Lnet/minecraft/server/level/ServerPlayer;)V");
            var renderer=jar(sable,"dev/ryanhcode/sable/neoforge/mixin/compatibility/create/schematics/SchematicRendererMixin");
            boolean tessellates=false;for(var m:renderer.methods)for(var ins:m.instructions)if(ins instanceof MethodInsnNode c&&c.owner.equals("net/minecraft/client/renderer/block/ModelBlockRenderer")&&c.name.equals("tesselateBlock"))tessellates=true;
            if(!tessellates)throw new AssertionError("Sable child schematic drawing path changed");
            var request=jar(pym,"com/misterblusky9/pym/api/ResizeRequest");
            method(request,"check","()Lcom/misterblusky9/pym/api/ResizeResult;");
            method(request,"immediate","()Lcom/misterblusky9/pym/api/ResizeRequest;");
            method(request,"submit","()Lcom/misterblusky9/pym/api/ResizeResult;");
            for(String field:new String[]{"subLevel","scale","immediate"})if(request.fields.stream().noneMatch(f->f.name.equals(field)))throw new AssertionError("Resize cost hook field "+field);
            method(jar(pym,"com/misterblusky9/pym/api/ScaleQuery"),"target","(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;)D");
            String tooltip="(Ljava/util/List;Z)Z";
            var kinetic=jar(create,"com/simibubi/create/content/kinetics/base/KineticBlockEntity");
            var generator=jar(create,"com/simibubi/create/content/kinetics/base/GeneratingKineticBlockEntity");
            calls(method(kinetic,"addToGoggleTooltip",tooltip),kinetic.name,"calculateStressApplied","()F");
            calls(method(generator,"addToGoggleTooltip",tooltip),generator.name,"calculateAddedStressCapacity","()F");
            method(network,"addSilently","("+be+"FF)V");
            method(jar(create,"com/simibubi/create/content/kinetics/base/KineticBlockEntity"),"hasNetwork","()Z");
            method(jar(pym,"com/misterblusky9/pym/api/spi/ResizePolicy"),"refuse","(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;DD)Ljava/lang/String;");
            method(jar(pym,"com/misterblusky9/pym/api/ScaleQuery"),"at","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)D");
            method(jar(pym,"com/misterblusky9/pym/api/event/ScaleTickEvent"),"container","()Ldev/ryanhcode/sable/api/sublevel/ServerSubLevelContainer;");
            method(jar(sable,"dev/ryanhcode/sable/sublevel/plot/LevelPlot"),"getLoadedChunks","()Ljava/util/Collection;");
        }
        try(var addition=new JarFile(args[3])) {
            var alternator=jar(addition,"com/mrh0/createaddition/blocks/alternator/AlternatorBlockEntity");
            var motor=jar(addition,"com/mrh0/createaddition/blocks/electric_motor/ElectricMotorBlockEntity");
            String tooltip="(Ljava/util/List;Z)Z";
            calls(method(alternator,"addToGoggleTooltip",tooltip),alternator.name,"getEnergyProductionRate","(I)I");
            calls(method(motor,"addToGoggleTooltip",tooltip),motor.name,"getEnergyConsumptionRate","(F)I");
            var connector=jar(addition,"com/mrh0/createaddition/blocks/connector/base/AbstractConnectorBlockEntity");
            method(connector,"getEnergyStorage","(Lnet/minecraft/core/Direction;)Lnet/neoforged/neoforge/energy/IEnergyStorage;");
            var io=jar(addition,"com/mrh0/createaddition/blocks/connector/base/AbstractConnectorBlockEntity$InterfaceEnergyHandler");
            method(io,"receiveEnergy","(IZ)I");method(io,"extractEnergy","(IZ)I");
            // These rates are nominal inputs to internal storage; they must NOT also be scaled in tick.
            calls(method(alternator,"tick","()V"),alternator.name,"getEnergyProductionRate","(I)I");
            calls(method(motor,"tick","()V"),motor.name,"getEnergyConsumptionRate","(F)I");
        }
        method(runtime("net/neoforged/neoforge/capabilities/BlockCapability"),"getCapability","(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Ljava/lang/Object;)Ljava/lang/Object;");
        var fluid=runtime("net/neoforged/neoforge/fluids/capability/templates/FluidTank");
        method(fluid,"setCapacity","(I)Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;");
        method(fluid,"readFromNBT","(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;");
        method(fluid,"setFluid","(Lnet/neoforged/neoforge/fluids/FluidStack;)V");
        method(fluid,"fill","(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I");
        for(String field:new String[]{"capacity","fluid"})if(fluid.fields.stream().noneMatch(f->f.name.equals(field)))throw new AssertionError(field);
        try(var create=new JarFile(args[0])){
            var combined=jar(create,"com/simibubi/create/foundation/fluid/CombinedTankWrapper");
            if(combined.fields.stream().noneMatch(f->f.name.equals("itemHandler")&&f.desc.equals("[Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;")))throw new AssertionError("Combined fluid children");
        }
        var integration=runtime("net/aste/just_scale_it/pocket/PocketIntegration");
        calls(method(integration,"place","(Lnet/neoforged/neoforge/event/level/BlockEvent$EntityPlaceEvent;)V"),integration.name,"incompatibility","(Lnet/minecraft/world/level/block/entity/BlockEntity;D)Ljava/lang/String;");
        method(runtime("net/neoforged/neoforge/common/CommonHooks"),"onPlaceItemIntoWorld","(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;");
        var entity=runtime("net/minecraft/world/level/block/entity/BlockEntity");
        String nbt="(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V";
        method(entity,"loadAdditional",nbt);method(entity,"saveAdditional",nbt);
        var ticker=method(runtime("net/minecraft/world/level/chunk/LevelChunk$BoundTickingBlockEntity"),"tick","()V");
        boolean tickCall=false;
        for(var insn:ticker.instructions)if(insn instanceof MethodInsnNode call&&call.owner.equals("net/minecraft/world/level/block/entity/BlockEntityTicker")&&call.name.equals("tick"))tickCall=true;
        if(!tickCall)throw new AssertionError("Missing real block-entity tick context call");
        var storage=runtime("net/neoforged/neoforge/energy/EnergyStorage");
        method(storage,"receiveEnergy","(IZ)I");method(storage,"extractEnergy","(IZ)I");
        method(storage,"getEnergyStored","()I");method(storage,"getMaxEnergyStored","()I");
        method(storage,"canReceive","()Z");method(storage,"canExtract","()Z");
        calls(method(storage,"serializeNBT","(Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/nbt/Tag;"),storage.name,"getEnergyStored","()I");
        for(String field:new String[]{"energy","capacity"})if(storage.fields.stream().noneMatch(f->f.name.equals(field)&&f.desc.equals("I")))throw new AssertionError(field);
        System.out.println("Pocket hooks: Create live/unloaded network paths, Pym 0.5.1 policy/scale events, Sable loaded plots, SU tooltip/live network call sites, Create Additions 1.6.0 wire/FE methods, physical FluidTank and Create combined children, placement gate, mapped tick/capability/NBT and direct standard FE methods verified");
    }
}
