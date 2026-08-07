package org.envel.immersiveportalspaperized.shared.net.requests;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Requests that a player be teleported to a specific destination on login or portal entry.
 */
@Getter
@Setter
public class TeleportRequest extends Request {
   private static final long serialVersionUID = 1L;
   private UUID playerId;
   private String destServer;
   private UUID destWorldId;
   private String destWorldName;
   private double destX;
   private double destY;
   private double destZ;
   private float destPitch;
   private float destYaw;
   private boolean flying;
   private boolean gliding;
   private double destVelX;
   private double destVelY;
   private double destVelZ;
}
