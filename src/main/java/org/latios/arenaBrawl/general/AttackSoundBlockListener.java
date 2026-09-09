package org.latios.arenaBrawl.general;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;

import java.util.Set;

public class AttackSoundBlockListener extends PacketListenerAbstract {

    private static final Set<String> BLOCKED_SOUND_KEYS = Set.of(
            "minecraft:entity.player.attack.weak",
            "minecraft:entity.player.attack.strong",
            "minecraft:entity.player.attack.crit",
            "minecraft:entity.player.attack.sweep",
            "minecraft:entity.player.attack.knockback",
            "minecraft:entity.player.attack.nodamage"
    );

    @Override
    public void onPacketSend(PacketSendEvent event) {

        if (event.getPacketType() == PacketType.Play.Server.SOUND_EFFECT) {

            WrapperPlayServerSoundEffect wrapper =
                    new WrapperPlayServerSoundEffect(event);

            if (isBlockedSound(wrapper.getSound())) {
                event.setCancelled(true);
            }

        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_SOUND_EFFECT) {

            WrapperPlayServerEntitySoundEffect wrapper =
                    new WrapperPlayServerEntitySoundEffect(event);

            if (isBlockedSound(wrapper.getSound())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isBlockedSound(Sound sound) {

        if (sound == null || sound.getSoundId() == null) {
            return false;
        }

        String soundId = sound.getSoundId().toString();

      //  System.out.println("[ArenaBrawl] SOUND: " + soundId);

        return BLOCKED_SOUND_KEYS.contains(soundId);
    }
}