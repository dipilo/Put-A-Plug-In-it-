package com.dipilodopilasaurus.putapluginit;

import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;

public final class TargetEntityLeakFix {
    private static final Field TARGET_ENTITY_FIELD = findField("crosshairPickEntity", "pointedEntity");
    private static final Field HIT_RESULT_FIELD = findField("hitResult", "objectMouseOver");

    private TargetEntityLeakFix() {
    }

    public static void onClientTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }
        clearField(TARGET_ENTITY_FIELD, minecraft);
        clearField(HIT_RESULT_FIELD, minecraft);
    }

    private static Field findField(String... names) {
        for (String name : names) {
            try {
                Field field = Minecraft.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static void clearField(Field field, Object target) {
        if (field == null) {
            return;
        }
        try {
            field.set(target, null);
        } catch (IllegalAccessException ignored) {
        }
    }
}
