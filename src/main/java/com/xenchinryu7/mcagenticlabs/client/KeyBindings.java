package com.xenchinryu7.mcagenticlabs.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final KeyMapping.Category KEY_CATEGORY = new KeyMapping.Category(ResourceLocation.parse("key.categories.mc-agenticlabs"));

    public static KeyMapping TOGGLE_GUI;

    public static void init() {
        TOGGLE_GUI = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.mc-agenticlabs.toggle_gui",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, // K key
            KEY_CATEGORY
        ));
    }
}

