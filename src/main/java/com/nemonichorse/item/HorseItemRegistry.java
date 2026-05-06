package com.nemonichorse.item;

import com.nemonichorse.NemonicHorse;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Creates and identifies horse items via PDC — never via display name.
 * ItemsAdder items that exist already are identified by their IA namespace
 * stored in PDC by ItemsAdder itself (itemsadder:id key).
 */
public final class HorseItemRegistry {

    // PDC key stamped on every horse item: value = item type string
    public static final String TYPE_SPUR    = "spur";
    public static final String TYPE_NAME_TAG = "name_tag";
    public static final String TYPE_ELIXIR   = "elixir";

    private final NamespacedKey keyItemType;

    // ItemsAdder PDC key — set automatically by IA on every custom item
    private static final String IA_ID_KEY = "itemsadder:id";
    private static final String IA_SPUR_ID    = "nemonicorp:horse_magic_spurs";
    private static final String IA_NAME_TAG_ID = "nemonicorp:horse_name_tag";
    private static final String IA_ELIXIR_ID   = "nemonicorp:horse_training_elixir";

    public HorseItemRegistry(NemonicHorse plugin) {
        this.keyItemType = new NamespacedKey(plugin, "item_type");
    }

    /** Detect item type. Returns null if not a horse item. */
    @Nullable
    public String getItemType(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Check our own PDC key first (for vanilla/fallback items)
        if (pdc.has(keyItemType, PersistentDataType.STRING)) {
            return pdc.get(keyItemType, PersistentDataType.STRING);
        }

        // Check ItemsAdder's ID key (present on all IA custom items)
        if (pdc.has(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING)) {
            String iaId = pdc.get(new NamespacedKey("itemsadder", "id"), PersistentDataType.STRING);
            if (IA_SPUR_ID.equals(iaId))     return TYPE_SPUR;
            if (IA_NAME_TAG_ID.equals(iaId)) return TYPE_NAME_TAG;
            if (IA_ELIXIR_ID.equals(iaId))   return TYPE_ELIXIR;
        }

        return null;
    }

    public boolean isSpur(@Nullable ItemStack item)    { return TYPE_SPUR.equals(getItemType(item)); }
    public boolean isNameTag(@Nullable ItemStack item) { return TYPE_NAME_TAG.equals(getItemType(item)); }
    public boolean isElixir(@Nullable ItemStack item)  { return TYPE_ELIXIR.equals(getItemType(item)); }

    // ── Vanilla fallback item builders (used when ItemsAdder is absent) ──

    public ItemStack buildSpurItem() {
        return buildItem(Material.IRON_NUGGET, "&bEsporas Mágicas", TYPE_SPUR,
                List.of("&8Item de Cavalaria", "", "&7Ativa a habilidade equipada.", "&eShift+Clique &7montado"));
    }

    public ItemStack buildNameTagItem() {
        return buildItem(Material.NAME_TAG, "&6Placa de Identificação", TYPE_NAME_TAG,
                List.of("&8Item de Cavalaria", "", "&7Dê um nome ao seu cavalo.", "&eShift+Clique &7no cavalo"));
    }

    public ItemStack buildElixirItem() {
        return buildItem(Material.EXPERIENCE_BOTTLE, "&aElixir de Treinamento", TYPE_ELIXIR,
                List.of("&8Item de Cavalaria", "", "&7Concede &f+100 XP &7ao cavalo.", "&eShift+Clique &7no cavalo"));
    }

    private ItemStack buildItem(Material material, String name, String typeValue, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();
        meta.displayName(ser.deserialize(name));
        meta.lore(lore.stream().map(ser::deserialize).toList());
        meta.getPersistentDataContainer().set(keyItemType, PersistentDataType.STRING, typeValue);
        item.setItemMeta(meta);
        return item;
    }
}
