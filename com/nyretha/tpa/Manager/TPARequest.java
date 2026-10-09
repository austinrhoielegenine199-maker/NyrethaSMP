package com.nyretha.tpa.Manager;

import java.util.UUID;

public class TPARequest {
   private UUID sender;
   private UUID target;
   private long timestamp;
   private boolean isTpaHere;

   public TPARequest(UUID sender, UUID target, boolean isTpaHere) {
      this.sender = sender;
      this.target = target;
      this.timestamp = System.currentTimeMillis();
      this.isTpaHere = isTpaHere;
   }
   public UUID getSender() { return this.sender; }
   public UUID getTarget() { return this.target; }
   public long getTimestamp() { return this.timestamp; }
   public boolean isTpaHere() { return this.isTpaHere; }
   public boolean isExpired() { return System.currentTimeMillis() - this.timestamp > 30000L; }
}