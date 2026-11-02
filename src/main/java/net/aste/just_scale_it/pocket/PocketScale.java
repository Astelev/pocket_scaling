package net.aste.just_scale_it.pocket;
import com.misterblusky9.pym.api.Pym;
import net.aste.just_scale_it.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
public final class PocketScale {
    public static double at(Level level,BlockPos pos) {return PocketRules.active()&&level!=null?Pym.scale().at(level,pos):1;}
    public static double factor(BlockEntity be) {return !PocketRules.active()?1:ScalePower.factor(at(be.getLevel(),be.getBlockPos()),Config.POCKET_POWER_EXPONENT.get());}
    public static boolean blocked(BlockEntity be) {return at(be.getLevel(),be.getBlockPos())<1-1e-7&&PocketRules.forbidden(be.getBlockState());}
    public static double fluidFactor(BlockEntity be){return !PocketRules.active()?1:ScalePower.factor(at(be.getLevel(),be.getBlockPos()),net.aste.just_scale_it.Config.POCKET_FLUID_EXPONENT.get());}
    public static boolean sameBody(BlockEntity a,BlockEntity b){return a.getLevel()==b.getLevel()&&a.getLevel()!=null&&dev.ryanhcode.sable.Sable.HELPER.getContaining(a.getLevel(),a.getBlockPos())==dev.ryanhcode.sable.Sable.HELPER.getContaining(b.getLevel(),b.getBlockPos());}
    private PocketScale() {}
}
