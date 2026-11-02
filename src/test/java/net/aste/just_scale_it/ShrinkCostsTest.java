package net.aste.just_scale_it;
import java.util.*;
import net.aste.just_scale_it.pocket.ShrinkCostRule;
import net.aste.just_scale_it.pocket.ShrinkPaymentPlan;
public final class ShrinkCostsTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        var rules=List.of(ShrinkCostRule.parse("motor|create:steam_engine,#c:engines|create:precision_mechanism|2"),
            ShrinkCostRule.parse("free|minecraft:dirt|minecraft:iron_nugget|0"),
            ShrinkCostRule.parse("rest|*|minecraft:iron_nugget|1"));
        check(ShrinkCostRule.first(rules,"create:steam_engine",t->false).count()==2,"Exact motor group overrides fallback");
        check(ShrinkCostRule.first(rules,"addon:motor",t->t.equals("c:engines")).item().equals("create:precision_mechanism"),"Tags extend a group to addon engines");
        check(ShrinkCostRule.first(rules,"minecraft:dirt",t->false).count()==0,"Explicit free group wins over paid fallback");
        check(ShrinkCostRule.first(rules,"minecraft:stone",t->false).group().equals("rest"),"Unlisted blocks have the fallback price");
        check(ShrinkCostRule.first(List.of(),"minecraft:stone",t->true)==null,"Empty list is free");
        for(String invalid:List.of("x|*|minecraft:iron_nugget|-1","x|*|minecraft:iron_nugget|4097","x||minecraft:iron_nugget|1","x|*|BAD|1","x|*|minecraft:stone|1|extra"))check(!ShrinkCostRule.valid(invalid),"Reject malformed configuration: "+invalid);
        var mechanical=List.of(ShrinkCostRule.parse("mechanical|@mechanical,create:steam_engine|create:precision_mechanism|1"));
        check(ShrinkCostRule.first(mechanical,"addon:kinetic",t->t.equals("@mechanical"))!=null,"Mechanical classification includes addon blocks without maintaining an ID list");
        check(ShrinkCostRule.first(mechanical,"minecraft:stone",t->false)==null,"Ordinary blocks default to free");
        var stock=List.of(new ShrinkPaymentPlan.Stock<>(1,"precision",1),new ShrinkPaymentPlan.Stock<>(2,"precision",2),new ShrinkPaymentPlan.Stock<>(3,"nugget",5));
        var paid=ShrinkPaymentPlan.allocate(Map.of("precision",3L,"nugget",4L),stock);
        check(paid.accepted()&&paid.takes().stream().mapToInt(ShrinkPaymentPlan.Take::count).sum()==7,"Combine fragmented stacks and different resources");
        var bill=new LinkedHashMap<String,Long>();bill.put("precision",2L);bill.put("nugget",6L);
        var failed=ShrinkPaymentPlan.allocate(bill,stock);
        check(!failed.accepted()&&failed.takes().isEmpty(),"Shortage of the final resource must not take earlier resources");
        check(ShrinkPaymentPlan.allocate(Map.of("precision",Long.MAX_VALUE),stock).takes().isEmpty(),"Large totals cannot overflow into an affordable price");
        check(ShrinkPaymentPlan.allocate(Map.of("precision",0L),List.<ShrinkPaymentPlan.Stock<String>>of()).accepted(),"Zero price needs no stock");
        check(!ShrinkPaymentPlan.allocate(Map.of("precision",1L),List.<ShrinkPaymentPlan.Stock<String>>of()).accepted(),"Protected placement stack omitted from stock cannot fund its own fee");
        check(stock.get(0).count()==1&&stock.get(2).count()==5,"Planning never mutates inventory");
        System.out.println("Shrink costs: group/tag/first-match rules, fallback/free groups, fragmented inventory, atomic shortage and overflow checks passed");
    }
}
