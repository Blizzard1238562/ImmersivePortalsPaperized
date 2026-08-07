package org.envel.immersiveportalspaperized.bukkit.portal;

import java.util.Collection;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.World;
import org.envel.immersiveportalspaperized.api.PortalPosition;
import org.envel.immersiveportalspaperized.api.PortalDirection;
import org.envel.immersiveportalspaperized.bukkit.config.MiscConfig;
import org.envel.immersiveportalspaperized.bukkit.entity.faking.EntityTrackingManager;
import org.envel.immersiveportalspaperized.bukkit.portal.predicate.IPortalPredicateManager;
import org.envel.immersiveportalspaperized.shared.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PortalManagerTest {
   private PortalManager portalManager;
   private MiscConfig miscConfig;
   private IPortalPredicateManager predicateManager;
   private IPortalActivityManager activityManager;
   private EntityTrackingManager entityTrackingManager;
   private Logger logger;
   private World world;

   @BeforeEach
   public void setUp() {
      logger = mock(Logger.class);
      miscConfig = mock(MiscConfig.class);
      predicateManager = mock(IPortalPredicateManager.class);
      activityManager = mock(IPortalActivityManager.class);
      entityTrackingManager = mock(EntityTrackingManager.class);
      world = mock(World.class);
      when(miscConfig.getPortalActivationDistance()).thenReturn(5.0);
      when(miscConfig.isPreventDuplicatePortals()).thenReturn(true);

      portalManager = new PortalManager(logger, predicateManager, activityManager, miscConfig, entityTrackingManager);
   }

   @Test
   public void testRegisterAndGetPortalById() {
      IPortal portal = mock(IPortal.class);
      PortalPosition originPos = new PortalPosition(new Location(world, 0, 64, 0), PortalDirection.NORTH);
      when(portal.getOriginPos()).thenReturn(originPos);
      when(portal.getId()).thenReturn(UUID.randomUUID());

      portalManager.registerPortal(portal);
      assertNotNull(portalManager.getPortalById(portal.getId()));
   }

   @Test
   public void testRemovePortalById() {
      IPortal portal = mock(IPortal.class);
      PortalPosition originPos = new PortalPosition(new Location(world, 0, 64, 0), PortalDirection.NORTH);
      when(portal.getOriginPos()).thenReturn(originPos);
      UUID id = UUID.randomUUID();
      when(portal.getId()).thenReturn(id);

      portalManager.registerPortal(portal);
      assertTrue(portalManager.removePortalById(id));
      assertNull(portalManager.getPortalById(id));
   }

   @Test
   public void testDuplicatePortalGuard() {
      IPortal portal1 = mock(IPortal.class);
      IPortal portal2 = mock(IPortal.class);
      PortalPosition originPos = new PortalPosition(new Location(world, 0, 64, 0), PortalDirection.NORTH);
      PortalPosition destPos = new PortalPosition(new Location(world, 0, 64, -1), PortalDirection.NORTH);
      when(portal1.getOriginPos()).thenReturn(originPos);
      when(portal1.getDestPos()).thenReturn(destPos);
      when(portal1.getId()).thenReturn(UUID.randomUUID());
      when(portal2.getOriginPos()).thenReturn(originPos);
      when(portal2.getDestPos()).thenReturn(destPos);
      when(portal2.getId()).thenReturn(UUID.randomUUID());

      portalManager.registerPortal(portal1);
      portalManager.registerPortal(portal2);
      assertEquals(1, portalManager.getAllPortals().size());
   }

   @Test
   public void testRemovePortalsAt() {
      IPortal portal = mock(IPortal.class);
      PortalPosition originPos = new PortalPosition(new Location(world, 0, 64, 0), PortalDirection.NORTH);
      when(portal.getOriginPos()).thenReturn(originPos);
      UUID id = UUID.randomUUID();
      when(portal.getId()).thenReturn(id);

      portalManager.registerPortal(portal);
      assertEquals(1, portalManager.removePortalsAt(originPos.getLocation()));
      assertNull(portalManager.getPortalById(id));
   }
}
