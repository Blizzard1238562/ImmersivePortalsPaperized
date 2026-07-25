package org.envel.immersiveportalspaperized.bukkit.player.view.entity;

public interface IPlayerEntityView {
   void update();

   void onDeactivate(boolean shouldResetEntities);
}
