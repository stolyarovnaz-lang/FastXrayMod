package dev.fastxray;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class XrayToggler {
    private static boolean busy = false;
    private static List<String> saved = null;

    private XrayToggler() {}

    public static void say(MinecraftClient mc, String text) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(text), false);
        }
    }

    public static void toggle(MinecraftClient mc) {
        if (busy || mc.getOverlay() != null) {
            return;
        }

        Path source = FastXrayConfig.packPath;
        Path fileName = source.getFileName();
        if (fileName == null) {
            say(mc, "X-Ray pack not found!");
            return;
        }
        String id = "file/" + fileName;

        ResourcePackManager mgr = mc.getResourcePackManager();
        boolean currentlyOn = mgr.getEnabledNames().contains(id);
        List<String> target;

        if (currentlyOn) {
            if (saved != null) {
                target = new ArrayList<>(saved);
            } else {
                target = new ArrayList<>(mgr.getEnabledNames());
                target.remove(id);
            }
        } else {
            Path packsDir = mc.runDirectory.toPath().resolve("resourcepacks");
            if (!ensureInstalled(mc, source, packsDir.resolve(fileName.toString()))) {
                return;
            }
            mgr.scanPacks();
            if (mgr.getProfile(id) == null) {
                FastXrayClient.LOGGER.error("Pack '{}' not found by ResourcePackManager after scan", id);
                say(mc, "X-Ray pack not found!");
                return;
            }
            saved = new ArrayList<>(mgr.getEnabledNames());
            target = new ArrayList<>(saved);
            target.add(id);
        }

        applyAndReload(mc, mgr, target, id, !currentlyOn);
    }

    private static void applyAndReload(MinecraftClient mc, ResourcePackManager mgr,
                                       List<String> target, String id, boolean wantOn) {
        List<String> before = new ArrayList<>(mgr.getEnabledNames());
        busy = true;
        try {
            mgr.setEnabledProfiles(target);
            mc.reloadResources().whenComplete((v, error) -> mc.execute(() -> {
                boolean isOn = mgr.getEnabledNames().contains(id);
                if (error != null || isOn != wantOn) {
                    FastXrayClient.LOGGER.error("X-Ray reload failed, rolling back", error);
                    rollback(mc, mgr, before);
                } else {
                    if (!wantOn) {
                        saved = null;
                    }
                    busy = false;
                    say(mc, "X-Ray: " + (wantOn ? "ON" : "OFF"));
                }
            }));
        } catch (Throwable t) {
            FastXrayClient.LOGGER.error("Failed to start resource reload", t);
            try {
                mgr.setEnabledProfiles(before);
            } catch (Throwable ignored) {
            }
            busy = false;
            say(mc, "X-Ray: error, see log");
        }
    }

    private static void rollback(MinecraftClient mc, ResourcePackManager mgr, List<String> before) {
        try {
            mgr.setEnabledProfiles(before);
            mc.reloadResources().whenComplete((v, e) -> mc.execute(() -> {
                if (e != null) {
                    FastXrayClient.LOGGER.error("Rollback reload failed", e);
                }
                saved = null;
                busy = false;
                say(mc, "X-Ray: error, previous packs restored");
            }));
        } catch (Throwable t) {
            FastXrayClient.LOGGER.error("Rollback failed", t);
            saved = null;
            busy = false;
        }
    }

    private static boolean ensureInstalled(MinecraftClient mc, Path source, Path dest) {
        try {
            boolean sourceExists = Files.isRegularFile(source);
            boolean destExists = Files.isRegularFile(dest);

            if (!sourceExists && !destExists) {
                FastXrayClient.LOGGER.error("X-Ray pack not found: {}", source);
                say(mc, "X-Ray pack not found!");
                return false;
            }
            if (sourceExists && !(destExists && Files.isSameFile(source, dest))) {
                boolean needCopy = !destExists
                        || Files.size(source) != Files.size(dest)
                        || Files.getLastModifiedTime(source).compareTo(Files.getLastModifiedTime(dest)) > 0;
                if (needCopy) {
                    Files.createDirectories(dest.getParent());
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return true;
        } catch (IOException e) {
            FastXrayClient.LOGGER.error("Failed to install X-Ray pack", e);
            say(mc, "X-Ray: cannot copy pack, see log");
            return false;
        }
    }
}
