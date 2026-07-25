package org.envel.immersiveportalspaperized.bukkit.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerUtil {
   private static JavaPlugin plugin;
   private static boolean isFolia;
   private static final Set<SchedulerUtil.PortalTask> activeTasks = ConcurrentHashMap.newKeySet();

   public static void init(JavaPlugin pl) {
      plugin = pl;

      try {
         Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
         isFolia = true;
      } catch (ClassNotFoundException var2) {
         isFolia = false;
      }
   }

   public static boolean isFolia() {
      return isFolia;
   }

   public static void cancelAll() {
      for (SchedulerUtil.PortalTask task : new ArrayList<>(activeTasks)) {
         try {
            task.cancel();
         } catch (Exception var3) {
         }
      }

      activeTasks.clear();
   }

   private static SchedulerUtil.PortalTask trackOneShot(Function<Runnable, SchedulerUtil.PortalTask> scheduler, Runnable runnable) {
      SchedulerUtil.PortalTask[] taskRef = new SchedulerUtil.PortalTask[1];
      Runnable wrapped = () -> {
         try {
            runnable.run();
         } finally {
            activeTasks.remove(taskRef[0]);
         }
      };
      SchedulerUtil.PortalTask task = scheduler.apply(wrapped);
      SchedulerUtil.PortalTask wrappedTask = () -> {
         activeTasks.remove(taskRef[0]);
         task.cancel();
      };
      taskRef[0] = wrappedTask;
      activeTasks.add(wrappedTask);
      return wrappedTask;
   }

   private static SchedulerUtil.PortalTask trackRepeating(Function<Runnable, SchedulerUtil.PortalTask> scheduler, Runnable runnable) {
      SchedulerUtil.PortalTask[] taskRef = new SchedulerUtil.PortalTask[1];
      SchedulerUtil.PortalTask task = scheduler.apply(runnable);
      SchedulerUtil.PortalTask wrappedTask = () -> {
         activeTasks.remove(taskRef[0]);
         task.cancel();
      };
      taskRef[0] = wrappedTask;
      activeTasks.add(wrappedTask);
      return wrappedTask;
   }

   public static SchedulerUtil.PortalTask runTask(Runnable runnable) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().run(plugin, t -> wrapped.run());
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, wrapped);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runTaskLater(Runnable runnable, long delayTicks) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> wrapped.run(), delayTicks);
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, wrapped, delayTicks);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runTaskTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
      return trackRepeating(wrapped -> {
         if (isFolia) {
            long delay = Math.max(1L, initialDelayTicks);
            ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> wrapped.run(), delay, periodTicks);
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, wrapped, initialDelayTicks, periodTicks);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runAsync(Runnable runnable) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = Bukkit.getAsyncScheduler().runNow(plugin, t -> wrapped.run());
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, wrapped);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runTimerAsync(Runnable runnable, long initialDelayTicks, long periodTicks) {
      return trackRepeating(
         wrapped -> {
            if (isFolia) {
               ScheduledTask task = Bukkit.getAsyncScheduler()
                  .runAtFixedRate(plugin, t -> wrapped.run(), initialDelayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
               return () -> {
                  if (task != null) {
                     task.cancel();
                  }
               };
            } else {
               BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, wrapped, initialDelayTicks, periodTicks);
               return task::cancel;
            }
         },
         runnable
      );
   }

   public static SchedulerUtil.PortalTask runAtLocation(Location location, Runnable runnable) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, location, t -> wrapped.run());
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, wrapped);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runAtLocation(World world, int blockX, int blockZ, Runnable runnable) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = Bukkit.getRegionScheduler().run(plugin, world, blockX >> 4, blockZ >> 4, t -> wrapped.run());
            return () -> {
               if (task != null) {
                  task.cancel();
               }
            };
         } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, wrapped);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runForEntity(Entity entity, Runnable runnable) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = entity.getScheduler().run(plugin, t -> wrapped.run(), null);
            if (task == null) {
               wrapped.run();
               return () -> {};
            } else {
               return task::cancel;
            }
         } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, wrapped);
            return task::cancel;
         }
      }, runnable);
   }

   public static SchedulerUtil.PortalTask runForEntityLater(Entity entity, Runnable runnable, long delayTicks) {
      return trackOneShot(wrapped -> {
         if (isFolia) {
            ScheduledTask task = entity.getScheduler().runDelayed(plugin, t -> wrapped.run(), null, delayTicks);
            if (task == null) {
               wrapped.run();
               return () -> {};
            } else {
               return task::cancel;
            }
         } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, wrapped, delayTicks);
            return task::cancel;
         }
      }, runnable);
   }

   public interface PortalTask {
      void cancel();
   }
}
