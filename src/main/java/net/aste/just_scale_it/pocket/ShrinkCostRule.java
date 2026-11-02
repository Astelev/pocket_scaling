package net.aste.just_scale_it.pocket;
import java.util.*;
import java.util.function.Predicate;
/** Config parsing and first-match selection, independent of Minecraft registries. */
public record ShrinkCostRule(String group,List<String> blocks,String item,int count) {
    private static boolean id(String s){return s.matches("[a-z0-9_.-]+:[a-z0-9/._-]+");}
    public static ShrinkCostRule parse(String text){
        String[] p=text.split("\\|",-1);if(p.length!=4)throw new IllegalArgumentException("Expected group|blocks|item|count");
        for(int i=0;i<p.length;i++)p[i]=p[i].trim();
        if(p[0].isEmpty()||!id(p[2]))throw new IllegalArgumentException("Invalid group/item");
        var selectors=Arrays.stream(p[1].split(",",-1)).map(String::trim).toList();
        for(var s:selectors)if(!s.equals("*")&&!s.equals("@mechanical")&&!id(s.startsWith("#")?s.substring(1):s))throw new IllegalArgumentException("Invalid block selector");
        int n=Integer.parseInt(p[3]);if(n<0||n>4096)throw new IllegalArgumentException("Count must be 0..4096");
        return new ShrinkCostRule(p[0],selectors,p[2],n);
    }
    public static boolean valid(Object value){try{parse((String)value);return true;}catch(RuntimeException ex){return false;}}
    public static ShrinkCostRule first(List<ShrinkCostRule> rules,String block,Predicate<String> tag){
        for(var r:rules)for(var s:r.blocks)if(s.equals("*")||s.equals(block)||s.equals("@mechanical")&&tag.test(s)||s.startsWith("#")&&tag.test(s.substring(1)))return r;
        return null;
    }
}
