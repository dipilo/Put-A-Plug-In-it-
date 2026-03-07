$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$portsRoot = Join-Path $root 'ports'
$iconSource = 'C:\Users\sgibe\Downloads\PAPI.png'

if (Test-Path $portsRoot) {
    Get-ChildItem -Force $portsRoot | ForEach-Object {
        try {
            Remove-Item -Recurse -Force $_.FullName -ErrorAction Stop
        } catch {
            Write-Warning "Skipping locked path during cleanup: $($_.FullName)"
        }
    }
}
New-Item -ItemType Directory -Force -Path $portsRoot | Out-Null

function Write-TextFile {
    param(
        [Parameter(Mandatory = $true)] [string]$Path,
        [Parameter(Mandatory = $true)] [string]$Content
    )
    $dir = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function Add-WrapperFiles {
    param(
        [Parameter(Mandatory = $true)] [string]$ProjectDir,
        [Parameter(Mandatory = $true)] [string]$GradleVersion
    )

    $gradlewSrc = Join-Path $root 'gradlew'
    $gradlewBatSrc = Join-Path $root 'gradlew.bat'
    $wrapperJarSrc = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'

    Copy-Item -Force $gradlewSrc (Join-Path $ProjectDir 'gradlew')
    Copy-Item -Force $gradlewBatSrc (Join-Path $ProjectDir 'gradlew.bat')

    $wrapperDir = Join-Path $ProjectDir 'gradle\wrapper'
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
    Copy-Item -Force $wrapperJarSrc (Join-Path $wrapperDir 'gradle-wrapper.jar')

    $wrapperProps = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@

    Write-TextFile -Path (Join-Path $wrapperDir 'gradle-wrapper.properties') -Content $wrapperProps
}

function Add-GradleProperties {
    param(
        [Parameter(Mandatory = $true)] [string]$ProjectDir,
        [string]$JavaHomeOverride
    )

    $lines = @('org.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8')
    if (-not [string]::IsNullOrWhiteSpace($JavaHomeOverride)) {
        $normalizedJavaHome = $JavaHomeOverride -replace '\\', '/'
        $lines += "org.gradle.java.home=$normalizedJavaHome"
    }
    Write-TextFile -Path (Join-Path $ProjectDir 'gradle.properties') -Content ($lines -join "`n")
}

function Add-IconFiles {
    param(
        [Parameter(Mandatory = $true)] [string]$ProjectDir
    )

    if (-not (Test-Path $iconSource)) {
        Write-Warning "Icon source not found: $iconSource"
        return
    }

    $resDir = Join-Path $ProjectDir 'src\main\resources'
    New-Item -ItemType Directory -Force -Path $resDir | Out-Null
        Copy-Item -Force $iconSource (Join-Path $resDir 'papi.png')

    $fabricIconDir = Join-Path $resDir 'assets\papi'
    New-Item -ItemType Directory -Force -Path $fabricIconDir | Out-Null
    Copy-Item -Force $iconSource (Join-Path $fabricIconDir 'icon.png')
}

function Resolve-Java17Home {
    if ($env:JAVA17_HOME -and (Test-Path (Join-Path $env:JAVA17_HOME 'bin\java.exe'))) {
        return $env:JAVA17_HOME
    }

    $roots = @(
        'C:\Program Files\Eclipse Adoptium',
        'C:\Program Files\Java',
        'C:\Program Files\Microsoft'
    )

    foreach ($rootPath in $roots) {
        if (-not (Test-Path $rootPath)) {
            continue
        }
        $candidate = Get-ChildItem -Path $rootPath -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk[-_.]?17|temurin[-_.]?17|openjdk[-_.]?17' } |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($null -ne $candidate -and (Test-Path (Join-Path $candidate.FullName 'bin\java.exe'))) {
            return $candidate.FullName
        }
    }

    return $null
}

$commonLeakFix = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class SophisticatedCoreLeakFix {
    private static final String GLOBAL_FIXED_VERSION = "1.4.6.1504";
    private static final String SOPHISTICATED_CORE_MODID = "sophisticatedcore";
    private static final String ITEMSTACKKEY_CLASS = "net.p3pp3rf1y.sophisticatedcore.inventory.ItemStackKey";
    private static final String CACHE_FIELD = "CACHE";

    private static volatile boolean initTried = false;
    private static volatile boolean available = false;
    private static Field cacheField;
    private static int clientTickCounter = 0;

    private SophisticatedCoreLeakFix() {
    }

    @SuppressWarnings("java:S3011")
    private static void allowReflectiveAccess(Field field) {
        field.setAccessible(true);
    }

    public static void onClientTickEnd() {
        clientTickCounter++;
        if (clientTickCounter % 20 != 0) {
            return;
        }
        clearIfLargeEnough(10_000);
    }

    public static void onServerTickEnd() {
        // Left as opt-in hook if needed in future
    }

    private static void clearIfLargeEnough(int minSize) {
        initIfNeeded();
        if (!available || cacheField == null) {
            return;
        }

        try {
            Object cacheObj = cacheField.get(null);
            if (cacheObj instanceof Map<?, ?> map && map.size() >= minSize) {
                map.clear();
            }
        } catch (Exception ignored) {
            available = false;
        }
    }

    private static void initIfNeeded() {
        if (initTried) {
            return;
        }
        synchronized (SophisticatedCoreLeakFix.class) {
            if (initTried) {
                return;
            }
            initTried = true;

            String scVersion = detectSophisticatedCoreVersion();
            if (scVersion == null || !shouldApplyItemStackKeyFix(scVersion)) {
                available = false;
                return;
            }

            try {
                Class<?> keyClass = Class.forName(ITEMSTACKKEY_CLASS, false, SophisticatedCoreLeakFix.class.getClassLoader());
                Field field = keyClass.getDeclaredField(CACHE_FIELD);
                allowReflectiveAccess(field);
                cacheField = field;
                available = true;
            } catch (Exception ignored) {
                available = false;
            }
        }
    }

    private static String detectSophisticatedCoreVersion() {
        String fromForge = detectForgeLikeVersion("net.minecraftforge.fml.ModList");
        if (fromForge != null) {
            return fromForge;
        }

        String fromNeoForge = detectForgeLikeVersion("net.neoforged.fml.ModList");
        if (fromNeoForge != null) {
            return fromNeoForge;
        }

        return detectFabricVersion();
    }

    private static String detectForgeLikeVersion(String modListClassName) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, SophisticatedCoreLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            boolean loaded = (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, SOPHISTICATED_CORE_MODID);
            if (!loaded) {
                return null;
            }

            Object maybeContainer = modListClass.getMethod("getModContainerById", String.class)
                    .invoke(modList, SOPHISTICATED_CORE_MODID);
            if (!(maybeContainer instanceof Optional<?> optional) || optional.isEmpty()) {
                return "";
            }

            Object container = optional.get();
            Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
            Object version = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
            return version.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String detectFabricVersion() {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, SophisticatedCoreLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);

            boolean loaded = (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, SOPHISTICATED_CORE_MODID);
            if (!loaded) {
                return null;
            }

            Object maybeContainer = loaderClass.getMethod("getModContainer", String.class).invoke(loader, SOPHISTICATED_CORE_MODID);
            if (!(maybeContainer instanceof Optional<?> optional) || optional.isEmpty()) {
                return "";
            }

            Object container = optional.get();
            Method getMetadata = container.getClass().getMethod("getMetadata");
            Object metadata = getMetadata.invoke(container);
            Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
            return version.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean shouldApplyItemStackKeyFix(String version) {
        ParsedVersion parsed = parseVersion(version);
        if (parsed == null) {
            return true;
        }

        if (compareDotVersion(parsed.scVersion(), GLOBAL_FIXED_VERSION) >= 0) {
            return false;
        }

        String mcThreshold = getMcSpecificFixedVersion(parsed.mcVersion());
        return mcThreshold == null || compareDotVersion(parsed.scVersion(), mcThreshold) < 0;
    }

    private static ParsedVersion parseVersion(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }

        String[] parts = version.trim().toLowerCase().split("-", 2);
        if (parts.length != 2) {
            return null;
        }
        return new ParsedVersion(parts[0], parts[1]);
    }

    private static String getMcSpecificFixedVersion(String mcVersion) {
        return switch (mcVersion) {
            case "1.20.1" -> "1.3.5.1505";
            case "1.21.1" -> "1.4.5.1499";
            case "1.21.4" -> "1.4.5.1500";
            case "1.21.5" -> "1.4.5.1501";
            case "1.21.8" -> "1.4.5.1502";
            case "1.21.10" -> "1.4.6.1503";
            case "1.21.11" -> GLOBAL_FIXED_VERSION;
            default -> null;
        };
    }

    private static int compareDotVersion(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        int len = Math.max(l.length, r.length);
        for (int i = 0; i < len; i++) {
            int li = i < l.length ? parsePart(l[i]) : 0;
            int ri = i < r.length ? parsePart(r[i]) : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int parsePart(String value) {
        String digits = value.replaceAll("\\D.*$", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private record ParsedVersion(String mcVersion, String scVersion) {
    }
}
'@

$commonImmediatelyFastCompat = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.lwjgl.opengl.GL11C;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Method;
import java.util.Optional;

public final class ImmediatelyFastCompat {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String IF_MODID = "immediatelyfast";
    private static final String MLF_MODID = "memoryleakfix";

    private static boolean patched = false;

    private ImmediatelyFastCompat() {
    }

    public static void onClientTickEnd() {
        if (patched) {
            return;
        }
        patched = true;

        if (!isModLoaded(IF_MODID)) {
            return;
        }

        Path cfg = Paths.get("config", "immediatelyfast.json");
        if (!Files.exists(cfg)) {
            return;
        }

        try {
            JsonObject root = readJson(cfg);
            if (root == null) {
                return;
            }

            boolean changed = false;
            if (isIntelUhd()) {
                changed |= setBoolean(root, "experimental_screen_batching", false);
            }

            if (isModLoaded(MLF_MODID)) {
                changed |= setBoolean(root, "experimental_screen_batching", false);
            }

            if (changed) {
                writeJson(cfg, root);
            }
        } catch (IOException | RuntimeException ignored) {
            // Best-effort compatibility patch: ignore malformed or transiently unavailable config state.
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject()) {
                return null;
            }
            return element.getAsJsonObject();
        }
    }

    private static void writeJson(Path path, JsonObject root) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
    }

    private static boolean setBoolean(JsonObject root, String key, boolean value) {
        if (root.has(key) && root.get(key).isJsonPrimitive() && root.get(key).getAsBoolean() == value) {
            return false;
        }
        root.addProperty(key, value);
        return true;
    }

    private static boolean isIntelUhd() {
        try {
            String vendor = GL11C.glGetString(GL11C.GL_VENDOR);
            String renderer = GL11C.glGetString(GL11C.GL_RENDERER);
            String v = vendor == null ? "" : vendor.toLowerCase();
            String r = renderer == null ? "" : renderer.toLowerCase();
            return (v.contains("intel") || r.contains("intel"))
                    && (r.contains("uhd") || r.contains("iris xe") || r.contains("iris(r) xe"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isModLoaded(String modId) {
        return isForgeLikeLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeLoaded("net.neoforged.fml.ModList", modId)
                || isFabricLoaded(modId);
    }

    private static boolean isForgeLikeLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, ImmediatelyFastCompat.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, ImmediatelyFastCompat.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);

            boolean loaded = (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
            if (!loaded) {
                return false;
            }

            Method getModContainer = loaderClass.getMethod("getModContainer", String.class);
            Object maybeContainer = getModContainer.invoke(loader, modId);
            return maybeContainer instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
'@

$commonForgeEntityMemoriesLeakFix = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

public final class EntityMemoriesLeakFix {
    private static final String MLF_MODID = "memoryleakfix";

    private static boolean checkedEnabled = false;
    private static boolean enabled = false;

    private EntityMemoriesLeakFix() {
    }

    public static void onEntityRemoved(Object entity) {
        if (!isEnabled() || !isLivingEntity(entity)) {
            return;
        }
        clearLivingEntityBrainMemories(entity);
    }

    private static boolean isEnabled() {
        if (checkedEnabled) {
            return enabled;
        }
        checkedEnabled = true;
        enabled = isAffectedMinecraftVersion() && !isModLoaded(MLF_MODID);
        return enabled;
    }

    private static boolean isAffectedMinecraftVersion() {
        try {
            String versionName = resolveMinecraftVersionName();
            return compareSimpleVersion(versionName, "1.19.0") >= 0 && compareSimpleVersion(versionName, "1.19.4") < 0;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String resolveMinecraftVersionName() {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", false, EntityMemoriesLeakFix.class.getClassLoader());
            Object worldVersion = invokeFirstNoArgMethod(sharedConstantsClass, "getCurrentVersion", "getGameVersion", "createGameVersion");
            return extractVersionName(worldVersion);
        } catch (ReflectiveOperationException ignored) {
            return "";
        }
    }

    private static Object invokeFirstNoArgMethod(Class<?> owner, String... methodNames) throws ReflectiveOperationException {
        for (String methodName : methodNames) {
            try {
                Method method = owner.getMethod(methodName);
                return method.invoke(null);
            } catch (NoSuchMethodException ignored) {
                // Try next candidate.
            }
        }
        throw new NoSuchMethodException("No matching SharedConstants version accessor found");
    }

    private static String extractVersionName(Object worldVersion) {
        if (worldVersion == null) {
            return "";
        }
        try {
            Method getName = worldVersion.getClass().getMethod("getName");
            Object value = getName.invoke(worldVersion);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            try {
                Method getId = worldVersion.getClass().getMethod("getId");
                Object value = getId.invoke(worldVersion);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException ignoredAgain) {
                return worldVersion.toString();
            }
        }
    }

    private static int compareSimpleVersion(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int len = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < len; i++) {
            int li = i < leftParts.length ? parseIntPrefix(leftParts[i]) : 0;
            int ri = i < rightParts.length ? parseIntPrefix(rightParts[i]) : 0;
            if (li != ri) {
                return Integer.compare(li, ri);
            }
        }
        return 0;
    }

    private static int parseIntPrefix(String value) {
        String digits = value.replaceAll("\\D.*$", "");
        if (digits.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static boolean isModLoaded(String modId) {
        if (isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)) {
            return true;
        }
        if (isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)) {
            return true;
        }
        return isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, EntityMemoriesLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, EntityMemoriesLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "java:S3011"})
    private static void clearLivingEntityBrainMemories(Object livingEntity) {
        Object brain = getBrain(livingEntity);
        if (brain == null || !hasBrainType(brain)) {
            return;
        }

        Field memoriesField = findMemoriesField(brain.getClass());
        if (memoriesField == null) {
            return;
        }

        try {
            memoriesField.setAccessible(true);
            Object memoriesObj = memoriesField.get(brain);
            if (!(memoriesObj instanceof Map<?, ?> memoriesMap)) {
                return;
            }

            Method setMemoryMethod = findSetMemoryMethod(brain.getClass());
            if (setMemoryMethod == null) {
                return;
            }

            for (Object key : new ArrayList<>(memoriesMap.keySet())) {
                try {
                    setMemoryMethod.invoke(brain, key, Optional.empty());
                } catch (ReflectiveOperationException ignored) {
                    // Best-effort cleanup for each memory entry.
                }
            }
        } catch (IllegalAccessException | RuntimeException ignored) {
            // Best-effort cleanup; if internals differ in a patch, skip silently.
        }
    }

    private static boolean isLivingEntity(Object entity) {
        if (entity == null) {
            return false;
        }
        String className = entity.getClass().getName();
        return "net.minecraft.world.entity.LivingEntity".equals(className)
                || "net.minecraft.entity.LivingEntity".equals(className)
                || isSubclassNamed(entity.getClass(), "net.minecraft.world.entity.LivingEntity")
                || isSubclassNamed(entity.getClass(), "net.minecraft.entity.LivingEntity");
    }

    private static boolean isSubclassNamed(Class<?> clazz, String fqcn) {
        Class<?> current = clazz;
        while (current != null) {
            if (fqcn.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private static Object getBrain(Object livingEntity) {
        try {
            Method getBrain = livingEntity.getClass().getMethod("getBrain");
            return getBrain.invoke(livingEntity);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean hasBrainType(Object brain) {
        String className = brain.getClass().getName();
        return "net.minecraft.world.entity.ai.Brain".equals(className)
                || "net.minecraft.entity.ai.brain.Brain".equals(className)
                || isSubclassNamed(brain.getClass(), "net.minecraft.world.entity.ai.Brain")
                || isSubclassNamed(brain.getClass(), "net.minecraft.entity.ai.brain.Brain");
    }

    private static Method findSetMemoryMethod(Class<?> brainClass) {
        for (Method method : brainClass.getMethods()) {
            if ("setMemory".equals(method.getName()) && method.getParameterCount() == 2) {
                return method;
            }
        }
        return null;
    }

    private static Field findMemoriesField(Class<?> brainClass) {
        for (Field field : brainClass.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }
}
'@

$commonTagKeyLeakFix = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class TagKeyLeakFix {

    private static boolean attempted;

    private TagKeyLeakFix() {
    }

    public static void applyIfNeeded() {
        if (attempted) {
            return;
        }
        attempted = true;

        if (!isExactly1182()) {
            return;
        }

        try {
            Class<?> tagKeyClass = Class.forName("net.minecraft.tags.TagKey", false, TagKeyLeakFix.class.getClassLoader());
            Field internerField = findStaticInternerField(tagKeyClass);
            if (internerField == null) {
                return;
            }

            internerField.setAccessible(true);
            removeFinalModifier(internerField);
            internerField.set(null, Interners.newWeakInterner());
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Best-effort: if internals differ for a patch, skip silently.
        }
    }

    private static boolean isExactly1182() {
        try {
            Object worldVersion = resolveWorldVersion();
            String versionName = extractVersionName(worldVersion);
            return "1.18.2".equals(versionName);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Object resolveWorldVersion() {
        try {
            Class<?> sharedConstantsClass = Class.forName("net.minecraft.SharedConstants", false, TagKeyLeakFix.class.getClassLoader());
            for (String methodName : new String[]{"getCurrentVersion", "getGameVersion", "createGameVersion"}) {
                try {
                    Method method = sharedConstantsClass.getMethod(methodName);
                    return method.invoke(null);
                } catch (ReflectiveOperationException ignored) {
                    // Try next candidate.
                }
            }
        } catch (ClassNotFoundException ignored) {
            return null;
        }
        return null;
    }

    private static String extractVersionName(Object worldVersion) {
        if (worldVersion == null) {
            return "";
        }
        try {
            Method getName = worldVersion.getClass().getMethod("getName");
            Object value = getName.invoke(worldVersion);
            return value == null ? "" : value.toString();
        } catch (ReflectiveOperationException ignored) {
            try {
                Method getId = worldVersion.getClass().getMethod("getId");
                Object value = getId.invoke(worldVersion);
                return value == null ? "" : value.toString();
            } catch (ReflectiveOperationException ignoredAgain) {
                return worldVersion.toString();
            }
        }
    }

    private static Field findStaticInternerField(Class<?> tagKeyClass) {
        for (Field field : tagKeyClass.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Interner.class.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }

    @SuppressWarnings("java:S3011")
    private static void removeFinalModifier(Field field) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (ReflectiveOperationException ignored) {
            // Java version dependent; if unavailable, leave as-is and try direct set.
        }
    }
}
'@

$commonDrownedNavigationLeakFix = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class DrownedNavigationLeakFix {

    private static final String MLF_MODID = "memoryleakfix";
    private static final Map<Object, Boolean> CLEANED_ENTITIES = new WeakHashMap<>();
    private static boolean checkedEnabled;
    private static boolean enabled;

    private DrownedNavigationLeakFix() {
    }

    public static void onEntityRemoved(Object entity, Object level) {
        if (!isEnabled() || entity == null || level == null) {
            return;
        }
        synchronized (CLEANED_ENTITIES) {
            if (CLEANED_ENTITIES.put(entity, Boolean.TRUE) != null) {
                return;
            }
        }
        // Drowned navigation leak fix is only relevant to 1.16.3-1.16.5.
        // This generated helper intentionally remains a no-op outside that range.
    }

    private static boolean isEnabled() {
        if (checkedEnabled) {
            return enabled;
        }
        checkedEnabled = true;
        enabled = !isModLoaded(MLF_MODID);
        return enabled;
    }

    private static boolean isModLoaded(String modId) {
        return isForgeLikeModLoaded("net.minecraftforge.fml.ModList", modId)
                || isForgeLikeModLoaded("net.neoforged.fml.ModList", modId)
                || isFabricModLoaded(modId);
    }

    private static boolean isForgeLikeModLoaded(String modListClassName, String modId) {
        try {
            Class<?> modListClass = Class.forName(modListClassName, false, DrownedNavigationLeakFix.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, DrownedNavigationLeakFix.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
'@

$forgeEntry = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

${entity_leave_import}
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(PutAPlugInIt.MODID)
public class PutAPlugInIt {
    public static final String MODID = "papi";

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TickHooks {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                TagKeyLeakFix.applyIfNeeded();
                SophisticatedCoreLeakFix.onClientTickEnd();
                ImmediatelyFastCompat.onClientTickEnd();
            }
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                SophisticatedCoreLeakFix.onServerTickEnd();
            }
        }
${entity_leave_hook}
    }
}
'@

$neoEntry = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(PutAPlugInIt.MODID)
public class PutAPlugInIt {
    public static final String MODID = "papi";

    @SuppressWarnings({"unused", "java:S1118"})
    public PutAPlugInIt(IEventBus modBus, Dist dist, ModContainer container) {
        TagKeyLeakFix.applyIfNeeded();
        NeoForge.EVENT_BUS.addListener(PutAPlugInIt::onClientTickEnd);
        NeoForge.EVENT_BUS.addListener(PutAPlugInIt::onServerTickEnd);
    }

    private static void onClientTickEnd(ClientTickEvent.Post event) {
        SophisticatedCoreLeakFix.onClientTickEnd();
        ImmediatelyFastCompat.onClientTickEnd();
    }

    private static void onServerTickEnd(ServerTickEvent.Post event) {
        SophisticatedCoreLeakFix.onServerTickEnd();
    }
}
'@

$neoLegacyEntry = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;

@Mod(PutAPlugInIt.MODID)
public class PutAPlugInIt {
    public static final String MODID = "papi";

    @SuppressWarnings("java:S1118")
    public PutAPlugInIt(IEventBus modBus) {
        TagKeyLeakFix.applyIfNeeded();
        NeoForge.EVENT_BUS.addListener(PutAPlugInIt::onClientTickEnd);
        NeoForge.EVENT_BUS.addListener(PutAPlugInIt::onServerTickEnd);
    }

    private static void onClientTickEnd(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SophisticatedCoreLeakFix.onClientTickEnd();
            ImmediatelyFastCompat.onClientTickEnd();
        }
    }

    private static void onServerTickEnd(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SophisticatedCoreLeakFix.onServerTickEnd();
        }
    }
}
'@

$modsToml = @'
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="MIT"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
logoFile="papi.png"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
modId="forge"
mandatory=true
versionRange="${forge_or_neo_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
mandatory=true
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
'@

$neoModsToml = @'
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="MIT"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"
logoFile="papi.png"
authors="${mod_authors}"
description='''${mod_description}'''

[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="${forge_or_neo_version_range}"
ordering="NONE"
side="BOTH"

[[dependencies.${mod_id}]]
modId="minecraft"
type="required"
versionRange="${minecraft_version_range}"
ordering="NONE"
side="BOTH"
'@

$packMcmeta = @'
{
  "pack": {
    "description": "Put A Plug In it resources",
    "pack_format": ${pack_format}
  }
}
'@

$fabricEntrypoint = @'
/*
 * This file is part of Put A Plug In it! - https://github.com/dipilo/Put-A-Plug-In-it
 * Copyright (C) 2023-2026 dipilo and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dipilodopilasaurus.putapluginit;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class PutAPlugInIt implements ModInitializer, ClientModInitializer {
    public static final String MODID = "papi";

    @Override
    public void onInitialize() {
        TagKeyLeakFix.applyIfNeeded();
        ServerTickEvents.END_SERVER_TICK.register(server -> SophisticatedCoreLeakFix.onServerTickEnd());
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            EntityMemoriesLeakFix.onEntityRemoved(entity);
            DrownedNavigationLeakFix.onEntityRemoved(entity, world);
        });
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SophisticatedCoreLeakFix.onClientTickEnd();
            ImmediatelyFastCompat.onClientTickEnd();
        });
    }
}
'@

$fabricModJson = @'
{
  "schemaVersion": 1,
  "id": "papi",
  "version": "${version}",
  "name": "Put A Plug In it!",
  "description": "Client leak mitigation for Sophisticated Core ItemStackKey cache.",
  "authors": ["Dipilodopilasaurus"],
    "license": "LGPL-3.0-or-later",
    "icon": "assets/papi/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.dipilodopilasaurus.putapluginit.PutAPlugInIt"],
    "client": ["com.dipilodopilasaurus.putapluginit.PutAPlugInIt"]
  },
  "depends": {
    "fabricloader": ">=${loader_version}",
    "fabric-api": ">=${fabric_api_version}",
    "minecraft": "~${minecraft_version}",
    "java": ">=${java_version}"
  }
}
'@

function New-ForgeProfile {
    param(
        [string]$Name,
        [string]$MinecraftVersion,
        [string]$ForgeVersion,
        [string]$ForgeGradleVersion,
        [string]$MinecraftRange,
        [string]$ForgeRange,
        [string]$LoaderRange,
        [int]$PackFormat,
        [int]$JavaVersion,
        [string]$GradleVersion,
        [string]$JavaHomeOverride = '',
        [bool]$IncludeEntityMemoriesFix = $false
    )

    $dir = Join-Path $portsRoot $Name

    $buildGradle = @"
plugins {
    id 'java'
    id 'eclipse'
    id 'idea'
    id 'net.minecraftforge.gradle' version '$ForgeGradleVersion'
}

group = 'com.dipilodopilasaurus'
version = "${MinecraftVersion}-forge-1.0.2"
base { archivesName = 'papi' }

java {
    toolchain.languageVersion = JavaLanguageVersion.of($JavaVersion)
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft "net.minecraftforge:forge:$MinecraftVersion-$ForgeVersion"
}

minecraft {
    mappings channel: 'official', version: '$MinecraftVersion'

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.console.level', 'debug'
            mods {
                papi {
                    source sourceSets.main
                }
            }
        }
        client {}
        server { args '--nogui' }
    }
}

tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
        mod_id: 'papi',
        mod_name: 'Put A Plug In it!',
        mod_authors: 'Dipilodopilasaurus',
        mod_version: project.version,
        mod_description: 'Client leak mitigation for Sophisticated Core ItemStackKey cache.',
        loader_version_range: '$LoaderRange',
        minecraft_version_range: '$MinecraftRange',
        forge_or_neo_version_range: '$ForgeRange'
    ]
    inputs.properties replaceProperties
    filesMatching(['META-INF/mods.toml']) {
        expand replaceProperties
    }
    filesMatching(['pack.mcmeta']) {
        expand([pack_format: $PackFormat])
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
"@

    $settingsGradle = @"
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net' }
        maven { url = 'https://maven.neoforged.net/releases' }
        maven { url = 'https://maven.fabricmc.net' }
        mavenCentral()
    }
}

rootProject.name = 'papi-$Name'
"@

    $entityLeaveImport = ''
    $entityLeaveHook = ''
    if ($IncludeEntityMemoriesFix) {
        $entityLeaveImport = 'import net.minecraftforge.event.entity.EntityLeaveLevelEvent;'
        $entityLeaveHook = @'

        @SubscribeEvent
        public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
            EntityMemoriesLeakFix.onEntityRemoved(event.getEntity());
            DrownedNavigationLeakFix.onEntityRemoved(event.getEntity(), event.getLevel());
        }
'@
    }

    $resolvedForgeEntry = $forgeEntry
    $resolvedForgeEntry = $resolvedForgeEntry.Replace('${entity_leave_import}', $entityLeaveImport)
    $resolvedForgeEntry = $resolvedForgeEntry.Replace('${entity_leave_hook}', $entityLeaveHook)

    Write-TextFile -Path (Join-Path $dir 'settings.gradle') -Content $settingsGradle
    Write-TextFile -Path (Join-Path $dir 'build.gradle') -Content $buildGradle
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/PutAPlugInIt.java') -Content $resolvedForgeEntry
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/SophisticatedCoreLeakFix.java') -Content $commonLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/ImmediatelyFastCompat.java') -Content $commonImmediatelyFastCompat
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/TagKeyLeakFix.java') -Content $commonTagKeyLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/DrownedNavigationLeakFix.java') -Content $commonDrownedNavigationLeakFix
    if ($IncludeEntityMemoriesFix) {
        Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/EntityMemoriesLeakFix.java') -Content $commonForgeEntityMemoriesLeakFix
    }
    Write-TextFile -Path (Join-Path $dir 'src/main/resources/META-INF/mods.toml') -Content $modsToml
    Write-TextFile -Path (Join-Path $dir 'src/main/resources/pack.mcmeta') -Content $packMcmeta
    Add-IconFiles -ProjectDir $dir
    Add-WrapperFiles -ProjectDir $dir -GradleVersion $GradleVersion
    Add-GradleProperties -ProjectDir $dir -JavaHomeOverride $JavaHomeOverride
}

function New-NeoProfile {
    param(
        [string]$Name,
        [string]$MinecraftVersion,
        [string]$NeoVersion,
        [string]$NeoPluginVersion,
        [string]$MinecraftRange,
        [string]$NeoRange,
        [string]$LoaderRange,
        [int]$PackFormat,
        [int]$JavaVersion,
        [string]$PluginId,
        [string]$GradleVersion
    )

    $dir = Join-Path $portsRoot $Name

    $buildGradle = @"
plugins {
    id 'java'
    id 'idea'
    id '$PluginId' version '$NeoPluginVersion'
}

group = 'com.dipilodopilasaurus'
version = "${MinecraftVersion}-neoforge-1.0.2"
base { archivesName = 'papi' }

java {
    toolchain.languageVersion = JavaLanguageVersion.of($JavaVersion)
}

"@

    if ($PluginId -eq 'net.neoforged.moddev') {
        $buildGradle += @"
neoForge {
    version = '$NeoVersion'

    runs {
        client { client() }
        server {
            server()
            programArgument '--nogui'
        }
    }

    mods {
        papi {
            sourceSet(sourceSets.main)
        }
    }
}
"@
    }
    else {
        $buildGradle += @"
runs {
    configureEach {
        modSource project.sourceSets.main
    }
    client {}
    server {
        programArgument '--nogui'
    }
}

def scriptAtFile = layout.buildDirectory.file('neoForge/neoForgeJoined$NeoVersion/unpacked/ats/_script_neoforge.cfg')
def resourceAtFile = layout.buildDirectory.file('neoForge/neoForgeJoined$NeoVersion/unpacked/ats/accesstransformer.cfg')

tasks.register('prepareScriptNeoforgeAt') {
    outputs.files(scriptAtFile, resourceAtFile)
    doLast {
        [scriptAtFile.get().asFile, resourceAtFile.get().asFile].each { f ->
            f.parentFile.mkdirs()
            if (!f.exists()) {
                f.text = ''
            }
        }
    }
}

tasks.matching { it.name.startsWith('neoFormForgesAccessTransformerProvider') }.configureEach {
    dependsOn(tasks.named('prepareScriptNeoforgeAt'))
}
"@
    }

    $buildGradle += @"

repositories {
    mavenCentral()
}

dependencies {
    implementation "net.neoforged:neoforge:$NeoVersion"
}

tasks.named('processResources', ProcessResources).configure {
    def replaceProperties = [
        mod_id: 'papi',
        mod_name: 'Put A Plug In it!',
        mod_authors: 'Dipilodopilasaurus',
        mod_version: project.version,
        mod_description: 'Client leak mitigation for Sophisticated Core ItemStackKey cache.',
        loader_version_range: '$LoaderRange',
        minecraft_version_range: '$MinecraftRange',
        forge_or_neo_version_range: '$NeoRange'
    ]
    inputs.properties replaceProperties
    filesMatching(['META-INF/neoforge.mods.toml', 'META-INF/mods.toml']) {
        expand replaceProperties
    }
    filesMatching(['pack.mcmeta']) {
        expand([pack_format: $PackFormat])
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
"@

    $settingsGradle = @"
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net' }
        maven { url = 'https://maven.neoforged.net/releases' }
        maven { url = 'https://maven.fabricmc.net' }
        mavenCentral()
    }
}

rootProject.name = 'papi-$Name'
"@

    Write-TextFile -Path (Join-Path $dir 'settings.gradle') -Content $settingsGradle
    Write-TextFile -Path (Join-Path $dir 'build.gradle') -Content $buildGradle
    if ($PluginId -eq 'net.neoforged.moddev') {
        Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/PutAPlugInIt.java') -Content $neoEntry
    } else {
        Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/PutAPlugInIt.java') -Content $neoLegacyEntry
    }
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/SophisticatedCoreLeakFix.java') -Content $commonLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/ImmediatelyFastCompat.java') -Content $commonImmediatelyFastCompat
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/TagKeyLeakFix.java') -Content $commonTagKeyLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/DrownedNavigationLeakFix.java') -Content $commonDrownedNavigationLeakFix
    if ($PluginId -eq 'net.neoforged.moddev') {
        Write-TextFile -Path (Join-Path $dir 'src/main/resources/META-INF/neoforge.mods.toml') -Content $neoModsToml
    } else {
        Write-TextFile -Path (Join-Path $dir 'src/main/resources/META-INF/mods.toml') -Content $neoModsToml
    }
    Write-TextFile -Path (Join-Path $dir 'src/main/resources/pack.mcmeta') -Content $packMcmeta
    Add-IconFiles -ProjectDir $dir
    Add-WrapperFiles -ProjectDir $dir -GradleVersion $GradleVersion
    Add-GradleProperties -ProjectDir $dir -JavaHomeOverride ''
}

function New-FabricProfile {
    param(
        [string]$Name,
        [string]$MinecraftVersion,
        [string]$LoaderVersion,
        [string]$FabricApiVersion,
        [string]$LoomVersion,
        [string]$YarnMappings,
        [int]$PackFormat,
        [int]$JavaVersion,
        [string]$GradleVersion
    )

    $dir = Join-Path $portsRoot $Name

    $buildGradle = @"
plugins {
    id 'java'
    id 'fabric-loom' version '$LoomVersion'
}

group = 'com.dipilodopilasaurus'
version = "${MinecraftVersion}-fabric-1.0.2"
base { archivesName = 'papi' }

repositories {
    mavenCentral()
    maven { url = 'https://maven.fabricmc.net/' }
}

dependencies {
    minecraft "com.mojang:minecraft:$MinecraftVersion"
    mappings "net.fabricmc:yarn:${YarnMappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:$LoaderVersion"
    modImplementation "net.fabricmc.fabric-api:fabric-api:$FabricApiVersion"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of($JavaVersion)
}

tasks.named('processResources', ProcessResources).configure {
    inputs.property 'version', project.version
    inputs.property 'loader_version', '$LoaderVersion'
    inputs.property 'fabric_api_version', '$FabricApiVersion'
    inputs.property 'minecraft_version', '$MinecraftVersion'
    inputs.property 'java_version', '$JavaVersion'

    filesMatching('fabric.mod.json') {
        expand([
            version: project.version,
            loader_version: '$LoaderVersion',
            fabric_api_version: '$FabricApiVersion',
            minecraft_version: '$MinecraftVersion',
            java_version: '$JavaVersion'
        ])
    }
    filesMatching('pack.mcmeta') {
        expand([pack_format: $PackFormat])
    }
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
"@

    $settingsGradle = @"
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net' }
        maven { url = 'https://maven.neoforged.net/releases' }
        maven { url = 'https://maven.fabricmc.net' }
        mavenCentral()
    }
}

rootProject.name = 'papi-$Name'
"@

    Write-TextFile -Path (Join-Path $dir 'settings.gradle') -Content $settingsGradle
    Write-TextFile -Path (Join-Path $dir 'build.gradle') -Content $buildGradle
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/PutAPlugInIt.java') -Content $fabricEntrypoint
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/SophisticatedCoreLeakFix.java') -Content $commonLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/ImmediatelyFastCompat.java') -Content $commonImmediatelyFastCompat
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/EntityMemoriesLeakFix.java') -Content $commonForgeEntityMemoriesLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/TagKeyLeakFix.java') -Content $commonTagKeyLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/java/com/dipilodopilasaurus/putapluginit/DrownedNavigationLeakFix.java') -Content $commonDrownedNavigationLeakFix
    Write-TextFile -Path (Join-Path $dir 'src/main/resources/fabric.mod.json') -Content $fabricModJson
    Write-TextFile -Path (Join-Path $dir 'src/main/resources/pack.mcmeta') -Content $packMcmeta
    Add-IconFiles -ProjectDir $dir
    Add-WrapperFiles -ProjectDir $dir -GradleVersion $GradleVersion
    Add-GradleProperties -ProjectDir $dir -JavaHomeOverride ''
}

$profiles = @()

$java17Home = Resolve-Java17Home
if ([string]::IsNullOrWhiteSpace($java17Home)) {
    Write-Warning 'Java 17 home was not found. Legacy Forge profiles will be generated per-version and not consolidated.'
} else {
    Write-Output "Using Java 17 for legacy Forge profiles: $java17Home"
}

# Forge targets
New-ForgeProfile -Name 'forge-1.18.2' -MinecraftVersion '1.18.2' -ForgeVersion '40.1.30' -ForgeGradleVersion '5.1.+' -MinecraftRange '[1.18.2,1.19)' -ForgeRange '[40,)' -LoaderRange '[40,)' -PackFormat 9 -JavaVersion 17 -GradleVersion '7.6.4' -JavaHomeOverride $java17Home -IncludeEntityMemoriesFix $false
$profiles += [pscustomobject]@{ profile = 'forge-1.18.2'; loader = 'forge'; minecraft = '1.18.2' }

if ([string]::IsNullOrWhiteSpace($java17Home)) {
    New-ForgeProfile -Name 'forge-1.19.0' -MinecraftVersion '1.19' -ForgeVersion '41.1.0' -ForgeGradleVersion '5.1.+' -MinecraftRange '[1.19,1.19.1)' -ForgeRange '[41,)' -LoaderRange '[41,)' -PackFormat 9 -JavaVersion 17 -GradleVersion '7.6.4' -JavaHomeOverride '' -IncludeEntityMemoriesFix $true
    $profiles += [pscustomobject]@{ profile = 'forge-1.19.0'; loader = 'forge'; minecraft = '1.19' }

    New-ForgeProfile -Name 'forge-1.19.1' -MinecraftVersion '1.19.1' -ForgeVersion '42.0.9' -ForgeGradleVersion '5.1.+' -MinecraftRange '[1.19.1,1.19.2)' -ForgeRange '[42,)' -LoaderRange '[42,)' -PackFormat 9 -JavaVersion 17 -GradleVersion '7.6.4' -JavaHomeOverride '' -IncludeEntityMemoriesFix $true
    $profiles += [pscustomobject]@{ profile = 'forge-1.19.1'; loader = 'forge'; minecraft = '1.19.1' }

    New-ForgeProfile -Name 'forge-1.19.2' -MinecraftVersion '1.19.2' -ForgeVersion '43.2.13' -ForgeGradleVersion '5.1.+' -MinecraftRange '[1.19.2,1.20)' -ForgeRange '[43,)' -LoaderRange '[43,)' -PackFormat 9 -JavaVersion 17 -GradleVersion '7.6.4' -JavaHomeOverride '' -IncludeEntityMemoriesFix $true
    $profiles += [pscustomobject]@{ profile = 'forge-1.19.2'; loader = 'forge'; minecraft = '1.19.2' }
} else {
    New-ForgeProfile -Name 'forge-1.19.x' -MinecraftVersion '1.19.2' -ForgeVersion '43.2.13' -ForgeGradleVersion '5.1.+' -MinecraftRange '[1.19,1.20)' -ForgeRange '[41,)' -LoaderRange '[41,)' -PackFormat 9 -JavaVersion 17 -GradleVersion '7.6.4' -JavaHomeOverride $java17Home -IncludeEntityMemoriesFix $true
    $profiles += [pscustomobject]@{ profile = 'forge-1.19.x'; loader = 'forge'; minecraft = '1.19-1.19.4' }
}

New-ForgeProfile -Name 'forge-1.20.1' -MinecraftVersion '1.20.1' -ForgeVersion '47.4.16' -ForgeGradleVersion '[6.0.16,6.2)' -MinecraftRange '[1.20.1,1.21)' -ForgeRange '[47,)' -LoaderRange '[47,)' -PackFormat 15 -JavaVersion 17 -GradleVersion '8.12' -IncludeEntityMemoriesFix $false
$profiles += [pscustomobject]@{ profile = 'forge-1.20.1'; loader = 'forge'; minecraft = '1.20.1' }

# NeoForge targets
New-NeoProfile -Name 'neoforge-1.20.4' -MinecraftVersion '1.20.4' -NeoVersion '20.4.223' -NeoPluginVersion '7.0.101' -PluginId 'net.neoforged.gradle.userdev' -MinecraftRange '[1.20.4,1.21)' -NeoRange '[20.4,)' -LoaderRange '[2,)' -PackFormat 22 -JavaVersion 17 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.20.4'; loader = 'neoforge'; minecraft = '1.20.4' }

New-NeoProfile -Name 'neoforge-1.21.0-1.21.1' -MinecraftVersion '1.21.1' -NeoVersion '21.1.129' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21,1.21.2)' -NeoRange '[21.0.0,21.2.0)' -LoaderRange '[4,)' -PackFormat 34 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.0-1.21.1'; loader = 'neoforge'; minecraft = '1.21-1.21.1' }

New-NeoProfile -Name 'neoforge-1.21.4' -MinecraftVersion '1.21.4' -NeoVersion '21.4.136' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21.4,1.21.5)' -NeoRange '[21.4.0,)' -LoaderRange '[4,)' -PackFormat 46 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.4'; loader = 'neoforge'; minecraft = '1.21.4' }

New-NeoProfile -Name 'neoforge-1.21.5' -MinecraftVersion '1.21.5' -NeoVersion '21.5.80' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21.5,1.21.6)' -NeoRange '[21.5.0,)' -LoaderRange '[4,)' -PackFormat 55 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.5'; loader = 'neoforge'; minecraft = '1.21.5' }

New-NeoProfile -Name 'neoforge-1.21.8' -MinecraftVersion '1.21.8' -NeoVersion '21.8.29' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21.8,1.21.9)' -NeoRange '[21.6.0,21.9.0)' -LoaderRange '[4,)' -PackFormat 64 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.8'; loader = 'neoforge'; minecraft = '1.21.8' }

New-NeoProfile -Name 'neoforge-1.21.10' -MinecraftVersion '1.21.10' -NeoVersion '21.10.52-beta' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21.10,1.21.11)' -NeoRange '[21.10.0,21.11.0)' -LoaderRange '[4,)' -PackFormat 69 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.10'; loader = 'neoforge'; minecraft = '1.21.10' }

New-NeoProfile -Name 'neoforge-1.21.11' -MinecraftVersion '1.21.11' -NeoVersion '21.11.13-beta' -NeoPluginVersion '2.0.134' -PluginId 'net.neoforged.moddev' -MinecraftRange '[1.21.11,1.21.12)' -NeoRange '[21.11.0,21.12.0)' -LoaderRange '[4,)' -PackFormat 70 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'neoforge-1.21.11'; loader = 'neoforge'; minecraft = '1.21.11' }

# Fabric targets
New-FabricProfile -Name 'fabric-1.19.2' -MinecraftVersion '1.19.2' -LoaderVersion '0.15.7' -FabricApiVersion '0.77.0+1.19.2' -LoomVersion '1.10.+' -YarnMappings '1.19.2+build.28' -PackFormat 9 -JavaVersion 17 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'fabric-1.19.2'; loader = 'fabric'; minecraft = '1.19.2' }

New-FabricProfile -Name 'fabric-1.19.4' -MinecraftVersion '1.19.4' -LoaderVersion '0.15.7' -FabricApiVersion '0.87.2+1.19.4' -LoomVersion '1.10.+' -YarnMappings '1.19.4+build.2' -PackFormat 13 -JavaVersion 17 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'fabric-1.19.4'; loader = 'fabric'; minecraft = '1.19.4' }

New-FabricProfile -Name 'fabric-1.20.1' -MinecraftVersion '1.20.1' -LoaderVersion '0.16.9' -FabricApiVersion '0.92.2+1.20.1' -LoomVersion '1.10.+' -YarnMappings '1.20.1+build.10' -PackFormat 15 -JavaVersion 17 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'fabric-1.20.1'; loader = 'fabric'; minecraft = '1.20.1' }

New-FabricProfile -Name 'fabric-1.20.4' -MinecraftVersion '1.20.4' -LoaderVersion '0.16.5' -FabricApiVersion '0.97.2+1.20.4' -LoomVersion '1.10.+' -YarnMappings '1.20.4+build.3' -PackFormat 22 -JavaVersion 17 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'fabric-1.20.4'; loader = 'fabric'; minecraft = '1.20.4' }

New-FabricProfile -Name 'fabric-1.21.1' -MinecraftVersion '1.21.1' -LoaderVersion '0.16.9' -FabricApiVersion '0.114.0+1.21.1' -LoomVersion '1.10.+' -YarnMappings '1.21.1+build.3' -PackFormat 34 -JavaVersion 21 -GradleVersion '8.12'
$profiles += [pscustomobject]@{ profile = 'fabric-1.21.1'; loader = 'fabric'; minecraft = '1.21.1' }

$index = [ordered]@{
    generatedAtUtc = [DateTime]::UtcNow.ToString('o')
    root = 'ports'
    profiles = $profiles
}

Write-TextFile -Path (Join-Path $portsRoot 'profiles.json') -Content ($index | ConvertTo-Json -Depth 10)

$readme = @'
# Put A Plug In it - Port Matrix

This directory was generated by `tools/generate-ports.ps1`.

Each folder under `ports/` is a standalone mod project for one target profile.

## Build

From this repository root:

```powershell
pwsh -File .\tools\generate-ports.ps1
cd .\ports\forge-1.20.1
.\gradlew.bat build
```

(If wrapper is missing in a generated profile, run the build with a local Gradle install.)

## Profiles

See `ports/profiles.json`.
'@

Write-TextFile -Path (Join-Path $portsRoot 'README.md') -Content $readme

Write-Output "Generated $($profiles.Count) profiles under: $portsRoot"
