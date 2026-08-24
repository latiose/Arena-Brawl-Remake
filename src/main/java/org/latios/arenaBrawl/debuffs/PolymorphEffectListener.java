// debuffs/PolymorphEffectListener.java
package org.latios.arenaBrawl.debuffs;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher;
import org.bukkit.entity.Player;
import org.latios.arenaBrawl.general.PlayerHealthManager;
import org.latios.arenaBrawl.team.TeamManager;

public class PolymorphEffectListener implements DebuffListener {

    private final TeamManager teamManager;
    private final PlayerHealthManager healthManager;

    public PolymorphEffectListener(TeamManager teamManager, PlayerHealthManager healthManager) {
        this.teamManager = teamManager;
        this.healthManager = healthManager;
    }

    @Override
    public void onApplied(Player player, DebuffType type) {
        if (type != DebuffType.POLYMORPH) return;

        MobDisguise disguise = new MobDisguise(DisguiseType.SHEEP);
        disguise.setReplaceSounds(true);
        disguise.setViewSelfDisguise(false);
        disguise.setHideArmorFromSelf(true);

        if (disguise.getWatcher() instanceof LivingWatcher watcher) {
            watcher.setCustomName(buildNameWithHealth(player));
            watcher.setCustomNameVisible(true);
        }

        DisguiseAPI.disguiseToAll(player, disguise);
    }

    @Override
    public void onExpired(Player player, DebuffType type) {
        if (type != DebuffType.POLYMORPH) return;

        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }
    }

    private String buildNameWithHealth(Player player) {
        int hp = (int) healthManager.getHealth(player);
        return "§f" + player.getName() + " §7- §c" + hp + " HP";
    }
}