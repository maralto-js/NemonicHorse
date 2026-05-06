package com.nemonichorse.util;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;

public final class AttributeUtil {

    private AttributeUtil() {}

    public static void setBase(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(value);
        }
    }
}
