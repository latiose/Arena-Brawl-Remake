package org.latios.arenaBrawl.abilities.structures;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class CobwebVisibilityFilter extends PacketListenerAbstract {

    private static final int AIR_ID = 0;

    private static final Map<UUID, Set<Long>> HIDDEN = new ConcurrentHashMap<>();

    public CobwebVisibilityFilter() {
        super(PacketListenerPriority.HIGH);
    }


    public static void hide(UUID viewer, Collection<Long> positions) {
        HIDDEN.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()).addAll(positions);
    }

    public static void show(UUID viewer, Collection<Long> positions) {
        Set<Long> set = HIDDEN.get(viewer);
        if (set == null) return;
        set.removeAll(positions);
        if (set.isEmpty()) HIDDEN.remove(viewer, set);
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    private static int unpackX(long p) { return (int) (p >> 38); }
    private static int unpackY(long p) { return (int) (p << 52 >> 52); }
    private static int unpackZ(long p) { return (int) (p << 26 >> 38); }

    private static boolean isCobweb(int blockId) {
        return WrappedBlockState.getByGlobalId(blockId).getType() == StateTypes.COBWEB;
    }


    @Override
    public void onPacketSend(PacketSendEvent event) {
        UUID id = event.getUser().getUUID();
        if (id == null) return;

        Set<Long> hidden = HIDDEN.get(id);
        if (hidden == null || hidden.isEmpty()) return;

        if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
            WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
            Vector3i pos = wrapper.getBlockPosition();

            if (hidden.contains(pack(pos.getX(), pos.getY(), pos.getZ()))
                    && isCobweb(wrapper.getBlockId())) {
                wrapper.setBlockID(AIR_ID);
                event.markForReEncode(true);
            }

        } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
            WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);
            boolean changed = false;

            for (WrapperPlayServerMultiBlockChange.EncodedBlock block : wrapper.getBlocks()) {
                if (hidden.contains(pack(block.getX(), block.getY(), block.getZ()))
                        && isCobweb(block.getBlockId())) {
                    block.setBlockId(AIR_ID);
                    changed = true;
                }
            }
            if (changed) event.markForReEncode(true);

        } else if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA) {
            WrapperPlayServerChunkData wrapper = new WrapperPlayServerChunkData(event);
            Column column = wrapper.getColumn();
            BaseChunk[] sections = column.getChunks();
            int minY = event.getUser().getMinWorldHeight();
            boolean changed = false;

            for (long packed : hidden) {
                int x = unpackX(packed);
                int y = unpackY(packed);
                int z = unpackZ(packed);

                if ((x >> 4) != column.getX() || (z >> 4) != column.getZ()) continue;

                int index = (y - minY) >> 4;
                if (index < 0 || index >= sections.length || sections[index] == null) continue;

                BaseChunk section = sections[index];
                int lx = x & 15, ly = y & 15, lz = z & 15;

                if (isCobweb(section.getBlockId(lx, ly, lz))) {
                    section.set(lx, ly, lz, AIR_ID);
                    changed = true;
                }
            }
            if (changed) event.markForReEncode(true);
        }
    }
}