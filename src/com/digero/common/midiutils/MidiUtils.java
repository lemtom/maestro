package com.digero.common.midiutils;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
public class MidiUtils {
    public final static int DEFAULT_TEMPO_MPQ = 500000; // 120bpm
    public final static int META_END_OF_TRACK_TYPE = 0x2F;
    public final static int META_TEMPO_TYPE = 0x51;
    /**
     * Given a microsecond time, convert to tick.
     * returns tempo at the given time in cache.getCurrTempoMPQ
     */
    public static long microsecond2tick(Sequence seq, long micros, TempoCache cache) {
        if (seq.getDivisionType() != Sequence.PPQ ) {
            double dTick = ( ((double) micros)
                           * ((double) seq.getDivisionType())
                           * ((double) seq.getResolution()))
                           / ((double) 1000000);
            long tick = (long) dTick;
            if (cache != null) {
                cache.currTempo = (int) cache.getTempoMPQAt(tick);
            }
            return tick;
        }

        if (cache == null) {
            cache = new TempoCache(seq);
        }
        long[] ticks = cache.ticks;
        int[] tempos = cache.tempos; // in MPQ
        int cacheCount = tempos.length;

        int resolution = seq.getResolution();

        long us = 0; long tick = 0; int newReadPos = 0; int i = 1;

        // walk through all tempo changes and add time for the respective blocks
        // to find the right tick
        if (micros > 0 && cacheCount > 0) {
            // this loop requires that the first tempo Event is at time 0
            while (i < cacheCount) {
                long nextTime = us + ticks2microsec(ticks[i] - ticks[i - 1],
                                                    tempos[i - 1], resolution);
                if (nextTime > micros) {
                    break;
                }
                us = nextTime;
                i++;
            }
            tick = ticks[i - 1] + microsec2ticks(micros - us, tempos[i - 1], resolution);
        }
        cache.currTempo = tempos[i - 1];
        return tick;
    }
    
    /**
     * Given a tick, convert to microsecond
     * @param cache tempo info and current tempo
     */
    public static long tick2microsecond(Sequence seq, long tick, TempoCache cache) {
        if (seq.getDivisionType() != Sequence.PPQ ) {
            double seconds = ((double)tick / (double)(seq.getDivisionType() * seq.getResolution()));
            return (long) (1000000 * seconds);
        }

        if (cache == null) {
            cache = new TempoCache(seq);
        }

        int resolution = seq.getResolution();

        long[] ticks = cache.ticks;
        int[] tempos = cache.tempos; // in MPQ
        int cacheCount = tempos.length;

        // optimization to not always go through entire list of tempo events
        int snapshotIndex = cache.snapshotIndex;
        int snapshotMicro = cache.snapshotMicro;

        // walk through all tempo changes and add time for the respective blocks
        long us = 0; // microsecond

        if (snapshotIndex <= 0
            || snapshotIndex >= cacheCount
            || ticks[snapshotIndex] > tick) {
            snapshotMicro = 0;
            snapshotIndex = 0;
        }
        if (cacheCount > 0) {
            // this implementation needs a tempo event at tick 0!
            int i = snapshotIndex + 1;
            while (i < cacheCount && ticks[i] <= tick) {
                snapshotMicro += ticks2microsec(ticks[i] - ticks[i - 1], tempos[i - 1], resolution);
                snapshotIndex = i;
                i++;
            }
            us = snapshotMicro
                + ticks2microsec(tick - ticks[snapshotIndex],
                                 tempos[snapshotIndex],
                                 resolution);
        }
        cache.snapshotIndex = snapshotIndex;
        cache.snapshotMicro = snapshotMicro;
        return us;
    }
    
    /**
     * convert tick to microsecond with given tempo.
     * Does not take tempo changes into account.
     * Does not work for SMPTE timing!
     */
    public static long ticks2microsec(long tick, double tempoMPQ, int resolution) {
        return (long) (((double) tick) * tempoMPQ / resolution);
    }
    
    /**
     * convert tempo to microsecond with given tempo
     * Does not take tempo changes into account.
     * Does not work for SMPTE timing!
     */
    public static long microsec2ticks(long us, double tempoMPQ, int resolution) {
        // do not round to nearest tick
        //return (long) Math.round((((double)us) * resolution) / tempoMPQ);
        return (long) ((((double)us) * resolution) / tempoMPQ);
    }
    
    /**
     * converts<br>
     * 1 - MPQ-Tempo to BPM tempo<br>
     * 2 - BPM tempo to MPQ tempo<br>
     */
    public static double convertTempo(double tempo) {
        if (tempo <= 0) {
            tempo = 1;
        }
        return ((double) 60000000l) / tempo;
    }
    
    /** return if the given message is a meta tempo message */
    public static boolean isMetaTempo(MidiMessage midiMsg) {
        // first check if it is a META message at all
        if (midiMsg.getLength() != 6
            || midiMsg.getStatus() != MetaMessage.META) {
            return false;
        }
        // now get message and check for tempo
        byte[] msg = midiMsg.getMessage();
        // meta type must be 0x51, and data length must be 3
        return ((msg[1] & 0xFF) == META_TEMPO_TYPE) && (msg[2] == 3);
    }
    
    /** parses this message for a META tempo message and returns
     * the tempo in MPQ, or -1 if this isn't a tempo message
     */
    public static int getTempoMPQ(MidiMessage midiMsg) {
        // first check if it is a META message at all
        if (midiMsg.getLength() != 6
            || midiMsg.getStatus() != MetaMessage.META) {
            return -1;
        }
        byte[] msg = midiMsg.getMessage();
        if (((msg[1] & 0xFF) != META_TEMPO_TYPE) || (msg[2] != 3)) {
            return -1;
        }
        int tempo =    (msg[5] & 0xFF)
                    | ((msg[4] & 0xFF) << 8)
                    | ((msg[3] & 0xFF) << 16);
        return tempo;
    }
    
    /** return true if the passed message is Meta End Of Track */
    public static boolean isMetaEndOfTrack(MidiMessage midiMsg) {
        // first check if it is a META message at all
        if (midiMsg.getLength() != 3
            || midiMsg.getStatus() != MetaMessage.META) {
            return false;
        }
        // now get message and check for end of track
        byte[] msg = midiMsg.getMessage();
        return ((msg[1] & 0xFF) == META_END_OF_TRACK_TYPE) && (msg[2] == 0);
    }

}
