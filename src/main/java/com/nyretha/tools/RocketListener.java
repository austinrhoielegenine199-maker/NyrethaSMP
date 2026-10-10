package com.nyretha.tools;

import org.bukkit.event.Listener;
/** Integration scaffold for RocketListener; event behavior is pending porting. */
public class RocketListener implements Listener {
 private final Tools plugin;
 public RocketListener(Tools plugin){this.plugin=plugin;}
 public Tools getPlugin(){return plugin;}
}
