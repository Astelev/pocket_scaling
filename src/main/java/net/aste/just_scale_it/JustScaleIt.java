package net.aste.just_scale_it;
import com.mojang.logging.LogUtils;
import net.aste.just_scale_it.pocket.PocketIntegration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
@Mod(JustScaleIt.MOD_ID)
public final class JustScaleIt {
    public static final String MOD_ID="just_scale_it";
    public static final Logger LOGGER=LogUtils.getLogger();
    public JustScaleIt(IEventBus bus,ModContainer container){
        container.registerConfig(ModConfig.Type.SERVER,Config.SPEC);
        bus.addListener(this::setup);
    }
    private void setup(FMLCommonSetupEvent event){event.enqueueWork(PocketIntegration::register);}
}
