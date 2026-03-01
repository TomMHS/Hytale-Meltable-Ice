package de.tommhs.meltable.ice.world;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Resolves numeric fluid ids from the server's fluid asset registry.
 *
 * <p>This avoids hardcoding ids (which can change across builds).</p>
 */
@SuppressWarnings("SpellCheckingInspection")
public final class FluidIdResolver {

    private FluidIdResolver() {
    }

    /**
     * Attempts to resolve a numeric id for a fluid key (e.g. {@code "Water_Source"}).
     *
     * @param fluidKey The fluid key.
     * @return The numeric id, or {@link Integer#MIN_VALUE} if not resolvable.
     */
    public static int resolveFluidId(@Nonnull String fluidKey) {
        Integer id = tryResolveFromKnownClasses(fluidKey);
        if (id != null) {
            return id;
        }

        Integer scanned = tryResolveByScanningServerJar(fluidKey);
        return scanned != null ? scanned : Integer.MIN_VALUE;
    }

    @Nullable
    private static Integer tryResolveFromKnownClasses(@Nonnull String fluidKey) {
        for (String className : knownFluidClassNames()) {
            Integer id = tryResolveViaAssetMap(className, fluidKey);
            if (id != null && id != Integer.MIN_VALUE) {
                return id;
            }
        }
        return null;
    }

    @Nullable
    private static Integer tryResolveByScanningServerJar(@Nonnull String fluidKey) {
        Path jarPath = findServerJarPath();
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return null;
        }

        List<String> candidates = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (!name.endsWith(".class")) {
                    continue;
                }

                // Heuristic: likely fluid asset classes live in these packages.
                if (!name.startsWith("com/hypixel/hytale/server/core/asset/type/fluid/")) {
                    continue;
                }

                // Prefer classes that look like asset definitions/maps.
                if (!name.contains("config") && !name.contains("Fluid")) {
                    continue;
                }

                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                candidates.add(className);
            }
        } catch (Exception ignored) {
            return null;
        }

        // Try "best guesses" first to reduce class loading.
        candidates.sort((a, b) -> score(b) - score(a));

        for (String className : candidates) {
            Integer id = tryResolveViaAssetMap(className, fluidKey);
            if (id != null && id != Integer.MIN_VALUE) {
                return id;
            }
        }

        return null;
    }

    private static int score(@Nonnull String className) {
        int s = 0;
        String n = className.toLowerCase();
        if (n.endsWith(".fluid")) s += 10;
        if (n.endsWith(".fluidtype")) s += 8;
        if (n.contains(".config.")) s += 5;
        if (n.contains("assetmap")) s += 3;
        return s;
    }

    @Nullable
    private static Integer tryResolveViaAssetMap(@Nonnull String className, @Nonnull String key) {
        try {
            Class<?> clazz = Class.forName(className);

            // Prefer a static getAssetMap(): similar pattern as BlockType.
            Method getAssetMap = findStaticNoArg(clazz, "getAssetMap");
            Object assetMap;
            if (getAssetMap != null) {
                assetMap = getAssetMap.invoke(null);
            } else {
                // Fallback: getAssetStore() -> getAssetMap()
                Method getAssetStore = findStaticNoArg(clazz, "getAssetStore");
                if (getAssetStore == null) {
                    return null;
                }
                Object store = getAssetStore.invoke(null);
                Method storeGetAssetMap = findNoArg(store.getClass(), "getAssetMap");
                if (storeGetAssetMap == null) {
                    return null;
                }
                assetMap = storeGetAssetMap.invoke(store);
            }

            Method getIndex = findMethod(assetMap.getClass(), "getIndex", String.class);
            if (getIndex == null) {
                return null;
            }

            Object result = getIndex.invoke(assetMap, key);
            if (!(result instanceof Integer)) {
                return null;
            }

            return (Integer) result;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Method findStaticNoArg(@Nonnull Class<?> clazz, @Nonnull String name) {
        try {
            Method m = clazz.getMethod(name);
            return (m.getParameterCount() == 0) ? m : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static Method findNoArg(@Nonnull Class<?> clazz, @Nonnull String name) {
        try {
            Method m = clazz.getMethod(name);
            return (m.getParameterCount() == 0) ? m : null;
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static Method findMethod(@Nonnull Class<?> clazz, @Nonnull String name, @Nonnull Class<?> param) {
        try {
            return clazz.getMethod(name, param);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Nullable
    private static Path findServerJarPath() {
        // Use a known server class that must come from the server runtime.
        try {
            Class<?> worldClass = Class.forName("com.hypixel.hytale.server.core.universe.world.World");
            URL url = worldClass.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) {
                return null;
            }
            URI uri = url.toURI();
            File f = new File(uri);
            if (f.isDirectory()) {
                // In case of an exploded classpath, scanning is not implemented here.
                return null;
            }
            return f.toPath();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String[] knownFluidClassNames() {
        return new String[] {
                "com.hypixel.hytale.server.core.asset.type.fluid.config.Fluid",
                "com.hypixel.hytale.server.core.asset.type.fluid.config.FluidType",
                "com.hypixel.hytale.server.core.asset.type.fluid.config.FluidDefinition",
                "com.hypixel.hytale.server.core.asset.type.fluid.Fluid",
                "com.hypixel.hytale.server.core.asset.type.fluid.FluidType"
        };
    }
}