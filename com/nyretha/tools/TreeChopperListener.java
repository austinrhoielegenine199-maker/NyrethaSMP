package com.nyretha.tools;
import org.bukkit.event.Listener;
/** Integration scaffold for TreeChopper block events; behavior is pending porting. */
public class TreeChopperListener implements Listener {
 private final Tools plugin;
 public TreeChopperListener(Tools plugin){this.plugin=plugin;}
 public Tools getPlugin(){return plugin;}
}
