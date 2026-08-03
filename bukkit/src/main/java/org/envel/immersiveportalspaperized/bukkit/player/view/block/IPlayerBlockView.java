package org.envel.immersiveportalspaperized.bukkit.player.view.block;

/**
 * IPlayerBlockView.
 */
public interface IPlayerBlockView {
   void update(boolean refresh);

   void onDeactivate(boolean shouldResetStates);

   void finishReset();
}


