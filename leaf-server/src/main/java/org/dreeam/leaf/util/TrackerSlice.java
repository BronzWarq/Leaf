package org.dreeam.leaf.util;

import net.minecraft.world.entity.Entity;

public record TrackerSlice(Entity[] array, int start, int end) {

    public TrackerSlice(final Entity[] entities) {
        this(entities, 0, entities.length);
    }

    public int size() {
        return end - start;
    }

    public TrackerSlice[] splitEvenly(final int parts) {
        final TrackerSlice[] result = new TrackerSlice[parts];
        splitEvenly(parts, result);
        return result;
    }

    public int splitEvenly(final int parts, final TrackerSlice[] result) {
        if (parts == 1) {
            result[0] = this;
            return 1;
        }

        final int sliceSize = size();
        final int base = sliceSize / parts;
        final int remainder = sliceSize % parts;

        int curr = start;
        for (int i = 0; i < parts; i++) {
            final int endIdx = curr + base + (i < remainder ? 1 : 0);
            result[i] = new TrackerSlice(array, curr, endIdx);
            curr = endIdx;
        }
        return parts;
    }

    public TrackerSlice[] chunks(final int chunkSize) {
        final int len = size() == 0 ? 0 : (size() - 1) / chunkSize + 1;
        final TrackerSlice[] result = new TrackerSlice[len];
        chunks(chunkSize, result);
        return result;
    }

    public int chunks(final int chunkSize, final TrackerSlice[] result) {
        final int len = size() == 0 ? 0 : (size() - 1) / chunkSize + 1;
        int curr = start;
        for (int i = 0; i < len; i++) {
            final int endIdx = Math.min(curr + chunkSize, end);
            result[i] = new TrackerSlice(array, curr, endIdx);
            curr = endIdx;
        }
        return len;
    }
}
