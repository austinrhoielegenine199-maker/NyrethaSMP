package com.nyretha.tpa.gui;

import com.nyretha.tpa.TPA;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class TPAHereSendGUI implements Listener {
   private final TPA plugin;
   private final Player sender;
   private final Player target;
   public TPAHereSendGUI(TPA plugin, Player sender, Player target) { this.plugin=plugin; this.sender=sender; this.target=target; }
   public void open() { new TPARequestGUI(plugin,sender,sender,target,null,"tpa_here_send_gui",true,true).open(); }
}