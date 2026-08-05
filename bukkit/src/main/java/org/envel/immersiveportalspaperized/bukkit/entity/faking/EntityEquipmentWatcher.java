package org.envel.immersiveportalspaperized.bukkit.entity.faking;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class EntityEquipmentWatcher {
   private final LivingEntity entity;
   private ItemStack mainHand;
   private ItemStack offHand;
   private ItemStack helmet;
   private ItemStack chestplate;
   private ItemStack leggings;
   private ItemStack boots;

   public EntityEquipmentWatcher(LivingEntity entity) {
      this.entity = entity;
   }

   public Map<EquipmentSlot, ItemStack> checkForChanges() {
      Map<EquipmentSlot, ItemStack> result = new HashMap<>();
      EntityEquipment current = this.entity.getEquipment();
      if (current == null) {
         return result;
      } else {
         if (this.isStateDifferent(this.mainHand, current.getItemInMainHand())) {
            this.mainHand = current.getItemInMainHand();
            result.put(EquipmentSlot.HAND, this.mainHand);
         }

         if (this.isStateDifferent(this.offHand, current.getItemInOffHand())) {
            this.offHand = current.getItemInOffHand();
            result.put(EquipmentSlot.OFF_HAND, this.offHand);
         }

         if (this.isStateDifferent(this.helmet, current.getHelmet())) {
            this.helmet = current.getHelmet();
            result.put(EquipmentSlot.HEAD, this.helmet);
         }

         if (this.isStateDifferent(this.chestplate, current.getChestplate())) {
            this.chestplate = current.getChestplate();
            result.put(EquipmentSlot.CHEST, this.chestplate);
         }

         if (this.isStateDifferent(this.leggings, current.getLeggings())) {
            this.leggings = current.getLeggings();
            result.put(EquipmentSlot.LEGS, this.leggings);
         }

         if (this.isStateDifferent(this.boots, current.getBoots())) {
            this.boots = current.getBoots();
            result.put(EquipmentSlot.FEET, this.boots);
         }

         return result;
      }
   }

   private boolean isStateDifferent(ItemStack a, ItemStack b) {
      return a != null && b != null ? !a.equals(b) : a != b;
   }
}
