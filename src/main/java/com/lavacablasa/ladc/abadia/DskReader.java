package com.lavacablasa.ladc.abadia;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class for extracting data from extended disk images.
 */
public class DskReader {
    private byte[] data;         // Disk data
    private int numTracks;       // number of tracks of the disk
    private int numSides;        // number of sides of the disk

    public DskReader(byte[] dskData) {
        this.data = Objects.requireNonNull(dskData);

        // check if it's a valid extended dsk file
        String name = new String(Arrays.copyOfRange(data, 0x00, 0x15), StandardCharsets.US_ASCII);
        if (!"EXTENDED CPC DSK File".equals(name)) {
            throw new IllegalArgumentException("Invalid disk data " + name);
        }

        // get disk information
        numTracks = data[0x30] & 0xff;
        numSides = data[0x31] & 0xff;
    }

    public void getTrackData(int numTrack, byte[] buffer, int bufferPos, int size) {
        // gets a pointer to the track's starting address
        int start = getTrackOffset(numTrack);

        // get track information
        int sectorSize = (data[start + 0x14] & 0xff) * 256;
        int numSectors = (data[start + 0x15] & 0xff);

        // check the length of the data to be copied
        int trackSize = numSectors * sectorSize;
        size = Math.min(trackSize, size);
        if (bufferPos + size > buffer.length) {
            throw new IllegalArgumentException("Insufficient buffer capacity");
        }

        // copy all sectors for this track
        System.arraycopy(data, start + 0x100, buffer, bufferPos, size);
    }

    private int getTrackOffset(int numTrack) {
        if (numTrack < 0 || numTrack >= numTracks) {
            throw new IllegalArgumentException("Invalid track number " + numTrack);
        }

        int offset = 0x00000100;
        for (int i = 0; i < numTrack; i++) {
            offset += (data[0x34 + i] & 0xff) * 256;
        }

        return offset;
    }
}
