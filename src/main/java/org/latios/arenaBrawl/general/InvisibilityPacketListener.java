package org.latios.arenaBrawl.general;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import org.latios.arenaBrawl.abilities.utility.ShadowCloakManager;

import java.util.ArrayList;
import java.util.List;

public class InvisibilityPacketListener extends PacketListenerAbstract {

    private final ShadowCloakManager manager;

    public InvisibilityPacketListener(ShadowCloakManager manager) {
        super(PacketListenerPriority.HIGH);
        this.manager = manager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        int entityId = wrapper.getEntityId();

        if (!manager.isCloakedEntity(entityId)) return;
        if (event.getUser().getEntityId() == entityId) return;

        List<Equipment> hidden = new ArrayList<>();
        for (Equipment eq : wrapper.getEquipment()) {
            hidden.add(new Equipment(eq.getSlot(), ItemStack.EMPTY));
        }
        wrapper.setEquipment(hidden);
        event.markForReEncode(true);
    }
}