package net.aste.just_scale_it.pocket;
import java.util.*;
/** Pure, all-or-nothing inventory allocation. Callers omit protected/actively used stacks. */
public final class ShrinkPaymentPlan {
    public record Stock<T>(int slot,T item,int count){}
    public record Take(int slot,int count){}
    public record Result<T>(List<Take> takes,T missing,long required){public boolean accepted(){return missing==null;}}
    public static <T> Result<T> allocate(Map<T,Long> bill,List<Stock<T>> stock){
        var takes=new ArrayList<Take>();
        for(var e:bill.entrySet()){
            if(e.getValue()<0)throw new IllegalArgumentException("Negative price");
            long remaining=e.getValue();
            for(var s:stock)if(remaining>0&&Objects.equals(s.item,e.getKey())){
                int n=(int)Math.min(remaining,Math.max(0,s.count));if(n>0)takes.add(new Take(s.slot,n));remaining-=n;
            }
            if(remaining>0)return new Result<>(List.of(),e.getKey(),e.getValue());
        }
        return new Result<>(List.copyOf(takes),null,0);
    }
    private ShrinkPaymentPlan(){}
}
