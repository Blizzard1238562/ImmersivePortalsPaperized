package org.envel.immersiveportalspaperized.bukkit.portal.selection;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerData;
import org.envel.immersiveportalspaperized.bukkit.player.IPlayerDataManager;
import org.envel.immersiveportalspaperized.bukkit.util.SchedulerUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * SelectionVisualizer.
 */
@Singleton
public class SelectionVisualizer implements Runnable {
   private final IPlayerDataManager playerDataManager;
   private SchedulerUtil.PortalTask visualizerTask;
   private static final long TIMEOUT_MS = 60000L;
   private static final double VISUALIZER_RADIUS_SQUARED = 10000.0;
   private static final double PARTICLES_PER_BLOCK = 2.0;

   @Inject
   public SelectionVisualizer(IPlayerDataManager playerDataManager) {
      this.playerDataManager = playerDataManager;
   }

   public void start() {
      if (this.visualizerTask != null) {
         this.visualizerTask.cancel();
      }

      this.visualizerTask = SchedulerUtil.runTaskTimer(this, 0L, 10L);
   }

   public void stop() {
      if (this.visualizerTask != null) {
         this.visualizerTask.cancel();
         this.visualizerTask = null;
      }
   }

   @Override
   public void run() {
      long now = System.currentTimeMillis();

      for (IPlayerData playerData : this.playerDataManager.getPlayers()) {
         Player player = playerData.getPlayer();
         if (player != null && player.isOnline()) {
            ISelectionManager selection = playerData.getSelection();
            if (selection != null && now - selection.getLastActivityTime() <= TIMEOUT_MS) {
               this.drawSelectionBorder(player, selection.getCurrentlySelecting(), Particle.HAPPY_VILLAGER);
               if (selection.getOriginSelection() != null) {
                  this.drawSelectionBorder(player, selection.getOriginSelection(), Particle.HEART);
               }

               if (selection.getDestSelection() != null) {
                  this.drawSelectionBorder(player, selection.getDestSelection(), Particle.PORTAL);
               }
            }
         }
      }
   }

   private void drawSelectionBorder(Player player, IPortalSelection selection, Particle particle) {
      if (selection != null && selection.isValid()) {
         Location locA = selection.getPosA();
         Location locB = selection.getPosB();
         if (locA != null && locB != null) {
            World world = locA.getWorld();
            if (world != null && world.equals(player.getWorld())) {
               double minX = locA.getX();
               double minY = locA.getY();
               double minZ = locA.getZ();
               double maxX = locB.getX() + 1.0;
               double maxY = locB.getY() + 1.0;
               double maxZ = locB.getZ() + 1.0;
               Location pLoc = player.getLocation();
               if (!(pLoc.distanceSquared(locA) > VISUALIZER_RADIUS_SQUARED)) {
                  this.drawEdge(player, world, minX, minY, minZ, maxX, minY, minZ, particle);
                  this.drawEdge(player, world, minX, minY, minZ, minX, maxY, minZ, particle);
                  this.drawEdge(player, world, minX, minY, minZ, minX, minY, maxZ, particle);
                  this.drawEdge(player, world, maxX, maxY, maxZ, minX, maxY, maxZ, particle);
                  this.drawEdge(player, world, maxX, maxY, maxZ, maxX, minY, maxZ, particle);
                  this.drawEdge(player, world, maxX, maxY, maxZ, maxX, maxY, minZ, particle);
                  this.drawEdge(player, world, minX, maxY, minZ, maxX, maxY, minZ, particle);
                  this.drawEdge(player, world, minX, maxY, minZ, minX, maxY, maxZ, particle);
                  this.drawEdge(player, world, maxX, minY, minZ, maxX, maxY, minZ, particle);
                  this.drawEdge(player, world, maxX, minY, minZ, maxX, minY, maxZ, particle);
                  this.drawEdge(player, world, minX, minY, maxZ, maxX, minY, maxZ, particle);
                  this.drawEdge(player, world, minX, minY, maxZ, minX, maxY, maxZ, particle);
               }
            }
         }
      }
   }

   private void drawEdge(Player player, World world, double x1, double y1, double z1, double x2, double y2, double z2, Particle particle) {
      double dist = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2) + (z1 - z2) * (z1 - z2));
      int points = (int)(dist * PARTICLES_PER_BLOCK);
      if (points == 0) {
         points = 1;
      }

      double stepX = (x2 - x1) / points;
      double stepY = (y2 - y1) / points;
      double stepZ = (z2 - z1) / points;

      for (int i = 0; i <= points; i++) {
         double px = x1 + i * stepX;
         double py = y1 + i * stepY;
         double pz = z1 + i * stepZ;
         player.spawnParticle(particle, px, py, pz, 1, 0.0, 0.0, 0.0, 0.0);
      }
   }
}


