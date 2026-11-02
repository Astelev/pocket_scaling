package net.aste.just_scale_it;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Server-side balancing rules, synchronized to connected clients. */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER=new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue POCKET_ENABLED;
    public static final ModConfigSpec.DoubleValue POCKET_POWER_EXPONENT;
    public static final ModConfigSpec.DoubleValue POCKET_FLUID_EXPONENT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> POCKET_BLACKLIST;
    static {
        BUILDER.push("pocket_balance");
        POCKET_ENABLED=BUILDER.comment("Ограничения уменьшенных конструкций Pocket Sized/Pym. Обычные и увеличенные блоки не усиливаются.").define("enabled",true);
        POCKET_POWER_EXPONENT=BUILDER.comment("Множитель SU и FE = min(1, масштаб)^показатель. 3 — по объёму: размер 1/2 даёт 1/8 мощности и потребления.",
                "RPM не меняются. FE ёмкость/приём/отдача и внутренний номинальный расход пересчитываются совместно.")
                .defineInRange("power_exponent",3.0,0.0,6.0);
        POCKET_FLUID_EXPONENT=BUILDER.comment("Вместимость жидкостных баков = базовая * масштаб^показатель. Миллибакеты и расход топлива не масштабируются. Избыток при уменьшении теряется.").defineInRange("fluid_capacity_exponent",3.0,0.0,6.0);
        POCKET_BLACKLIST=BUILDER.comment("Запрещённые блоки: namespace:block или #namespace:tag. Нельзя уменьшить конструкцию с ними или поставить их на уже уменьшенную.",
                "FE-аккумуляторы не запрещены по умолчанию: их эффективная ёмкость уже масштабируется. Предметные хранилища сохраняют запрет; жидкостным уменьшается вместимость.",
                "Существующие запрещённые блоки не удаляются; их тик, capability и ручное взаимодействие блокируются до увеличения.",
                "Нестандартные FE-хранилища, не совместимые с базовым EnergyStorage, не допускаются к уменьшению без отдельного адаптера.")
                .defineListAllowEmpty("blacklist",java.util.List.of(
                    "create:mechanical_saw","create:mechanical_drill","create:mechanical_mixer","create:mechanical_press",
                    "create:deployer","create:mechanical_crafter","create:millstone","create:crushing_wheel","create:spout",
                    "create:basin","create:depot","create:item_drain","create:item_vault",
                    "create:creative_crate",
                    "minecraft:chest","minecraft:trapped_chest","minecraft:ender_chest","minecraft:barrel",
                    "minecraft:hopper","minecraft:dispenser","minecraft:dropper","#minecraft:shulker_boxes",
                    "#c:chests","#c:barrels","#c:shulker_boxes"),()->"create:mechanical_press",
                    value->value instanceof String text&&net.minecraft.resources.ResourceLocation.tryParse(text.startsWith("#")?text.substring(1):text)!=null);
        BUILDER.pop();
    }

    public static final ModConfigSpec.BooleanValue SHRINK_COST_ENABLED;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> SHRINK_COST_RULES;
    static {
        BUILDER.push("shrink_cost");
        SHRINK_COST_ENABLED=BUILDER.comment("Дополнительный расход предметов при создании/размещении маленьких блоков и уменьшении готовых конструкций. Креатив освобождён.").define("enabled",true);
        SHRINK_COST_RULES=BUILDER.comment("Группы: имя|ID блоков и #теги через запятую|ID предмета-ресурса|количество за один блок.",
                "@mechanical — все вращающиеся блоки Create/аддонов (IRotate или кинетическая BlockEntity). Остальные блоки по умолчанию бесплатны.",
                "Первая совпавшая группа имеет приоритет. * означает все остальные блоки: держите такую группу последней.",
                "Количество 0 освобождает группу от оплаты. Пустой список отключает стоимость. Неизвестный ресурс запрещает платное действие.",
                "Нужно хранить ресурсы в инвентаре; используемый для размещения стек не считается запасом ресурса.",
                "При повторном уменьшении цена взимается снова; увеличение/перенос не требуют оплаты и не возвращают ресурсы.")
            .defineListAllowEmpty("groups",java.util.List.of(
                "mechanical|@mechanical,create:steam_engine|create:precision_mechanism|1"),()->"group|minecraft:stone|minecraft:iron_nugget|1",
                net.aste.just_scale_it.pocket.ShrinkCostRule::valid);
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC=BUILDER.build();
    private Config(){}
}
