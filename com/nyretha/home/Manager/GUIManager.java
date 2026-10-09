package com.nyretha.home.Manager;

import org.bukkit.entity.Player;
import com.nyretha.home.GUI.MainGUI;

public final class GUIManager {
    public void openMain(Player player) { player.openInventory(MainGUI.create(player, 5)); }
}
