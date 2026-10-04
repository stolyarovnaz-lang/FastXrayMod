package dev.fastxray;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastXrayClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("fastxray");

    private static KeyBinding xrayKey;
    private static KeyBinding hitboxKey;

    @Override
    public void onInitializeClient() {
        FastXrayConfig.load();

        xrayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fastxray.xray", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_0, "category.fastxray"));
        hitboxKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fastxray.hitboxes", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_KP_1, "category.fastxray"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (hitboxKey.wasPressed()) {
                toggleHitboxes(client);
            }
            while (xrayKey.wasPressed()) {
                XrayToggler.toggle(client);
            }
        });
    }

    private static void toggleHitboxes(MinecraftClient client) {
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        boolean enable = !dispatcher.shouldRenderHitboxes();
        dispatcher.setRenderHitboxes(enable);
        XrayToggler.say(client, "Hitboxes: " + (enable ? "ON" : "OFF"));
    }
}
