package com.nyretha.tools;

import org.bukkit.event.Listener;
/** Integration scaffold for DrillListener; event behavior is pending porting. */
public class DrillListener implements Listener {
 private final Tools plugin;
 public DrillListener(Tools plugin){this.plugin=plugin;}
 public Tools getPlugin(){return plugin;}
}
