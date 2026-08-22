// debuffs/PolymorphEffectListener.java
package org.latios.arenaBrawl.debuffs;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.entity.Player;

public class PolymorphEffectListener implements DebuffListener {

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.POLYMORPH) return;

        MobDisguise disguise = new MobDisguise(DisguiseType.SHEEP);
        disguise.setReplaceSounds(true);

        disguise.setViewSelfDisguise(false);
        disguise.setHideArmorFromSelf(true);

        DisguiseAPI.disguiseToAll(player, disguise);
    }
    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.POLYMORPH) return;

        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
    }
}