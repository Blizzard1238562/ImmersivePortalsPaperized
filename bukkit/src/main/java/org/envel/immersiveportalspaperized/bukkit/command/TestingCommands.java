package org.envel.immersiveportalspaperized.bukkit.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Axis;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.envel.immersiveportalspaperized.api.IntVector;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Argument;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Arguments;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Command;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.Path;
import org.envel.immersiveportalspaperized.bukkit.command.framework.annotations.RequiresPlayer;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityInfo;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.IEntityPacketManipulator;
import org.envel.immersiveportalspaperized.bukkit.net.IPortalClient;
import org.envel.immersiveportalspaperized.bukkit.net.requests.TestForwardedRequest;
import org.envel.immersiveportalspaperized.bukkit.nms.BlockDataUtil;
import org.envel.immersiveportalspaperized.bukkit.portal.spawning.NewPortalChecker;
import org.envel.immersiveportalspaperized.bukkit.util.MaterialUtil;
import org.envel.immersiveportalspaperized.bukkit.util.performance.OperationTimer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.envel.immersiveportalspaperized.shared.net.RequestException;

/**
 * TestingCommands.
 */
@Singleton
public class TestingCommands {
   private final NewPortalChecker spawnChecker;
   private final IEntityPacketManipulator entityPacketManipulator;
   private final IPortalClient portalClient;
   private List<Integer> storedData;

   @Inject
   public TestingCommands(NewPortalChecker spawnChecker, IEntityPacketManipulator entityPacketManipulator, IPortalClient portalClient) {
      this.spawnChecker = spawnChecker;
      this.entityPacketManipulator = entityPacketManipulator;
      this.portalClient = portalClient;
   }

   @Command
   @Path("immersiveportalspaperized/test/portalBlock")
   @Argument(
      name = "dataValue"
   )
   @RequiresPlayer
   public boolean createTestPortalBlock(Player sender, byte dataValue) {
      BlockState state = sender.getLocation().getBlock().getState();
      state.setType(MaterialUtil.PORTAL_MATERIAL);
      if (state.getBlockData() instanceof Orientable orientable) {
         orientable.setAxis(dataValue == 2 ? Axis.Z : Axis.X);
         state.setBlockData(orientable);
      }

      state.update(true, false);
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/isValid")
   @Arguments({@Argument(
         name = "direction"
      ), @Argument(
         name = "sizeX"
      ), @Argument(
         name = "sizeY"
      )})
   @RequiresPlayer
   public boolean testIsValidSpawnPos(Player player, PortalDirection direction, int sizeX, int sizeY) {
      Vector size = new Vector(sizeX, sizeY, 0.0);
      player.sendMessage(String.valueOf(this.spawnChecker.isValidPortalSpawnPosition(player.getLocation().subtract(0.0, 1.0, 0.0), direction, size)));
      return true;
   }

   public EntityInfo getEntityInfo(Player player) {
      Entity nearestEntity = null;

      for (Entity entity : player.getNearbyEntities(10.0, 10.0, 10.0)) {
         if (!(entity instanceof Player)) {
            nearestEntity = entity;
            break;
         }
      }

      if (nearestEntity == null) {
         throw new IllegalStateException("No non-player entity found nearby");
      }

      return new EntityInfo(nearestEntity);
   }

   @Command
   @Path("immersiveportalspaperized/test/hideEntity")
   @RequiresPlayer
   public boolean hideEntity(Player player) {
      this.entityPacketManipulator.hideEntity(this.getEntityInfo(player), player);
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/showEntity")
   @RequiresPlayer
   public boolean showEntity(Player player) {
      this.entityPacketManipulator.showEntity(this.getEntityInfo(player), player);
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/smallTeleport")
   @RequiresPlayer
   public boolean testTeleport(Player player) {
      player.teleportAsync(player.getLocation().add(0.1, 0.0, 0.0));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/serializeBlocks")
   @RequiresPlayer
   public boolean doTestSerialization(Player player) {
      List<Integer> result = new ArrayList<>();
      OperationTimer timer = new OperationTimer();

      for (int x = -10; x < 10; x++) {
         for (int y = -10; y < 10; y++) {
            for (int z = -10; z < 10; z++) {
               BlockData blockData = player.getLocation().add(x, y, z).getBlock().getBlockData();
               result.add(BlockDataUtil.getCombinedId(blockData));
            }
         }
      }

      this.storedData = result;
      player.sendMessage(String.format("Serialized block data of %d blocks. Time taken: %.03f", result.size(), timer.getTimeTakenMillis()));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/restoreSerializedBlocks")
   @RequiresPlayer
   public boolean doTestDeserialization(Player player) {
      OperationTimer timer = new OperationTimer();
      int i = 0;

      for (int x = -10; x < 10; x++) {
         for (int y = -10; y < 10; y++) {
            for (int z = -10; z < 10; z++) {
               int storedCombinedId = this.storedData.get(i);
               BlockData blockData = BlockDataUtil.getByCombinedId(storedCombinedId);
               player.getLocation().add(x, y, z).getBlock().setBlockData(blockData);
               i++;
            }
         }
      }

      player.sendMessage(String.format("Successfully restored %d blocks. Time taken: %.03f", this.storedData.size(), timer.getTimeTakenMillis()));
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/forwardRequest")
   @Argument(
      name = "serverName"
   )
   public boolean sendForwardedRequest(CommandSender sender, String serverName) {
      TestForwardedRequest request = new TestForwardedRequest();
      request.setTestField(new IntVector(5, 10, 5));
      this.portalClient.sendRequestToServer(request, serverName, response -> {
         try {
            IntVector result = (IntVector)response.getResult();
            sender.sendMessage(result.toString());
         } catch (RequestException var3x) {
            var3x.printStackTrace();
         }
      });
      return true;
   }

   @Command
   @Path("immersiveportalspaperized/test/refresh")
   @RequiresPlayer
   public boolean refreshChunk(Player sender) {
      Chunk senderChunk = sender.getLocation().getChunk();
      sender.getWorld().refreshChunk(senderChunk.getX(), senderChunk.getZ());
      return true;
   }
}


