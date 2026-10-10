package com.nyretha.tpa.gui;

import com.nyretha.tpa.TPA;
import com.nyretha.tpa.Manager.TPARequest;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class TPAHereAcceptGUI implements Listener {
   private final TPA plugin;
   private final Player target;
   private final TPARequest request;
   public TPAHereAcceptGUI(TPA plugin, Player target, TPARequest request) { this.plugin=plugin; this.target=target; this.request=request; }
   public void open() {
      Player sender=org.bukkit.Bukkit.getPlayer(request.getSender());
      if(sender==null) { target.sendMessage(plugin.getLanguageManager().getMessage("sender-offline")); return; }
      new TPARequestGUI(plugin,target,sender,target,request,"tpa_here_accept_gui",false,true).open();
   }
}