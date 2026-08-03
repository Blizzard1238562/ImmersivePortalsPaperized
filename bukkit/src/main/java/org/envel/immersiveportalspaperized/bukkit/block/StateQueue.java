package org.envel.immersiveportalspaperized.bukkit.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import org.envel.immersiveportalspaperized.shared.logging.Logger;

/**
 * StateQueue.
 */
public class StateQueue {
   private List<IViewableBlockInfo> viewableStates = null;
   private int size = 0;
   private final LinkedBlockingQueue<List<IViewableBlockInfo>> newStateQueue = new LinkedBlockingQueue<>();
   private volatile boolean hasFinishedInit = false;
   private final Logger logger;

   public StateQueue(Logger logger) {
      this.logger = logger;
   }

   public synchronized List<IViewableBlockInfo> getViewableStates() {
      if (!this.hasFinishedInit) {
         this.logger.fine("Init not finished");
         return Collections.emptyList();
      } else {
         while (!this.newStateQueue.isEmpty()) {
            this.logger.fine("Adding queued states");

            try {
               List<IViewableBlockInfo> newStates = this.newStateQueue.take();
               this.size = this.size + newStates.size();
               this.viewableStates.addAll(newStates);
            } catch (InterruptedException var2) {
               throw new RuntimeException(var2);
            }
         }

         return new ArrayList<>(this.viewableStates);
      }
   }

   public synchronized void addStatesInitially(List<IViewableBlockInfo> blockInfoList) {
      if (this.hasFinishedInit) {
         throw new IllegalStateException("Cannot add initial states multiple times");
      } else {
         this.viewableStates = blockInfoList;
         this.size = blockInfoList.size();
         this.hasFinishedInit = true;
      }
   }

   public synchronized void enqueueStates(List<IViewableBlockInfo> blockInfoList) {
      this.logger.fine("Enqueueing states");
      this.newStateQueue.add(blockInfoList);
   }

   public synchronized int stateCount() {
      return this.size;
   }
}


