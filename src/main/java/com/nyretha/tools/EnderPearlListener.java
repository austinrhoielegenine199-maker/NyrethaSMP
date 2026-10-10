package com.nyretha.tools;

import org.bukkit.event.Listener;
/** Integration scaffold for EnderPearlListener; event behavior is pending porting. */
public class EnderPearlListener implements Listener {
 private final Tools plugin;
 public EnderPearlListener(Tools plugin){this.plugin=plugin;}
 public Tools getPlugin(){return plugin;}
}
