package net.aste.just_scale_it;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
/** Distribution boundary: fail if the extracted mod accidentally pulls assembly/bee/AeroUtils code back in. */
public final class DistributionTest {
    public static void main(String[] args)throws Exception{
        try(var jar=new JarFile(args[0])){
            if(jar.getJarEntry("just_scale_it.mixins.json")==null)throw new AssertionError("Missing mixin config");
            if(jar.getJarEntry("META-INF/neoforge.mods.toml")==null)throw new AssertionError("Missing metadata");
            var entries=jar.entries();int classes=0;
            while(entries.hasMoreElements()){
                var e=entries.nextElement();String name=e.getName();
                if(name.endsWith(".jar")||name.contains("/assembly/")||name.contains("/bees/"))throw new AssertionError("Unexpected bundled content "+name);
                if(!name.endsWith(".class"))continue;classes++;
                if(!name.startsWith("net/aste/just_scale_it/"))throw new AssertionError("Unexpected package "+name);
                String pool=new String(jar.getInputStream(e).readAllBytes(),StandardCharsets.ISO_8859_1);
                for(String ref:new String[]{"net/aste/aeroutils","net.aste.aeroutils","de/devin/cbbees","CreativeModeTab","DeferredRegister"})
                    if(pool.contains(ref))throw new AssertionError("Standalone dependency/registry leak "+name+" -> "+ref);
            }
            if(classes==0)throw new AssertionError("Empty distribution");
            System.out.println("Standalone distribution: "+classes+" own classes; no AeroUtils/bee/tool dependencies, registries or bundled jars");
        }
    }
}
