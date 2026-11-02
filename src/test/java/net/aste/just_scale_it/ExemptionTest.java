package net.aste.just_scale_it;
import net.aste.just_scale_it.api.ShrinkCostExemption;
public final class ExemptionTest {
    public static void main(String[] args)throws Exception{
        if(ShrinkCostExemption.active())throw new AssertionError("Leaked initial scope");
        try(var outer=ShrinkCostExemption.enter()){
            try(var inner=ShrinkCostExemption.enter()){inner.close();if(!ShrinkCostExemption.active())throw new AssertionError("Lost outer scope");}
            if(!ShrinkCostExemption.active())throw new AssertionError("Double-close changed depth");
            var error=new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread t=new Thread(()->{if(ShrinkCostExemption.active())error.set(new AssertionError("Cross-thread exemption"));});t.start();t.join();if(error.get()!=null)throw new AssertionError(error.get());
        }
        try(var scope=ShrinkCostExemption.enter()){throw new IllegalStateException("expected");}catch(IllegalStateException expected){}
        if(ShrinkCostExemption.active())throw new AssertionError("Exception leaked exemption");
        System.out.println("Construction exemption nesting, idempotent close and thread isolation passed");
    }
}
