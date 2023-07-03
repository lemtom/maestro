package com.digero.common.midiutils;

import java.util.ArrayList;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

public final class TempoCache {
    public static final int DEFAULT_TEMPO_MPQ = 500000; // 120bpm
    public static final int META_TEMPO_TYPE = 0x51;

    long[] ticks;
    int[] tempos; // in MPQ
    // index in ticks/tempos at the snapshot
    int snapshotIndex = 0;
    // microsecond at the snapshot
    int snapshotMicro = 0;

    int currTempo; // MPQ, used as return value for microsecond2tick

    private boolean firstTempoIsFake = false;

    public TempoCache() {
        // just some defaults, to prevents weird stuff
        ticks = new long[1];
        tempos = new int[1];
        tempos[0] = DEFAULT_TEMPO_MPQ;
        snapshotIndex = 0;
        snapshotMicro = 0;
    }

    public TempoCache(Sequence seq) {
        this();
        refresh(seq);
    }

    public synchronized void refresh(Sequence seq) {
        ArrayList<MidiEvent> list = new ArrayList<>();
        Track[] tracks = seq.getTracks();
        if (tracks.length > 0) {
            // tempo events only occur in track 0
            Track track = tracks[0];
            int c = track.size();
            for (int i = 0; i < c; i++) {
                MidiEvent ev = track.get(i);
                MidiMessage msg = ev.getMessage();
                if (isMetaTempo(msg)) {
                    // found a tempo event. Add it to the list
                    list.add(ev);
                }
            }
        }
        int size = list.size() + 1;
        firstTempoIsFake = true;
        if ((size > 1)
            && (list.get(0).getTick() == 0)) {
            // do not need to add an initial tempo event at the beginning
            size--;
            firstTempoIsFake = false;
        }
        ticks  = new long[size];
        tempos = new int[size];
        int e = 0;
        if (firstTempoIsFake) {
            // add tempo 120 at beginning
            ticks[0] = 0;
            tempos[0] = DEFAULT_TEMPO_MPQ;
            e++;
        }
        for (int i = 0; i < list.size(); i++, e++) {
            MidiEvent evt = list.get(i);
            ticks[e] = evt.getTick();
            tempos[e] = getTempoMPQ(evt.getMessage());
        }
        snapshotIndex = 0;
        snapshotMicro = 0;
    }

    public int getCurrTempoMPQ() {
        return currTempo;
    }

    float getTempoMPQAt(long tick) {
        return getTempoMPQAt(tick, -1.0f);
    }

    synchronized float getTempoMPQAt(long tick, float startTempoMPQ) {
        for (int i = 0; i < ticks.length; i++) {
            if (ticks[i] > tick) {
                if (i > 0) i--;
                if (startTempoMPQ > 0 && i == 0 && firstTempoIsFake) {
                    return startTempoMPQ;
                }
                return (float) tempos[i];
            }
        }
        return tempos[tempos.length - 1];
    }
    
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
}