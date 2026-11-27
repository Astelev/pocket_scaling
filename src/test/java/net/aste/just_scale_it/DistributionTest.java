package net.aste.just_scale_it;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import com.google.gson.JsonParser;
/** Distribution boundary: fail if the extracted mod accidentally pulls assembly/bee/AeroUtils code back in. */
public final class DistributionTest {
    public static void main(String[] args)throws Exception{
        try(var jar=new JarFile(args[0])){
            if(jar.getJarEntry("just_scale_it.mixins.json")==null)throw new AssertionError("Missing mixin config");
            if(jar.getJarEntry("META-INF/neoforge.mods.toml")==null)throw new AssertionError("Missing metadata");
            var config = com.google.gson.JsonParser.parseString(new String(
                    jar.getInputStream(jar.getJarEntry("just_scale_it.mixins.json")).readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
            String prefix = config.get("package").getAsString().replace('.', '/') + "/";
            var configEntry=jar.getJarEntry("just_scale_it.mixins.json");
            var config=JsonParser.parseString(new String(jar.getInputStream(configEntry).readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            String prefix=config.get("package").getAsString().replace('.', '/')+"/";
            int mixins=0;
            for(String side:new String[]{"mixins","client","server"}){
                if(!config.has(side))continue;
                for(var value:config.getAsJsonArray(side)){
                    String path=prefix+value.getAsString().replace('.', '/')+".class";
                    if(jar.getJarEntry(path)==null)throw new AssertionError("Declared mixin missing from mod JAR: "+path);
                    mixins++;
                }
            }
            if(mixins==0)throw new AssertionError("No mixins declared");
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
