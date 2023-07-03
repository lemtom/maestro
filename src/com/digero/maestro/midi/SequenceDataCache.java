package com.digero.maestro.midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.ExtensionMidiInstrument;
import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.TimeSignature;
import com.digero.common.midiutils.MidiUtils;
import com.digero.common.util.Util;
import com.digero.maestro.abc.TimingInfo;

public class SequenceDataCache implements MidiConstants, ITempoCache, IBarNumberCache
{
	private final int tickResolution;
	private final float divisionType;
	private final int primaryTempoMPQ;
	private final int minTempoMPQ;
	private final int maxTempoMPQ;
	private final TimeSignature timeSignature;
	private NavigableMap<Long, TempoEvent> tempo = new TreeMap<>();

	private final long songLengthTicks;
	private final static int NO_RESULT = -250;

	private MapByChannelPort instruments = new MapByChannelPort(DEFAULT_INSTRUMENT);
	private MapByChannel volume = new MapByChannel(DEFAULT_CHANNEL_VOLUME);
	private MapByChannel pitchBendCoarse = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_SEMITONES);
	private MapByChannel pitchBendFine = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_CENTS);
	private MapByChannel mapMSB = new MapByChannel(0);
	private MapByChannel mapLSB = new MapByChannel(0);
	private MapByChannel mapPatch = new MapByChannel(0);
	private int[] brandDrumBanks;// 1 = XG drums, 2 = GS Drums, 3 = normal drums, 4 = GM2 drums
	private String standard = "GM";
	private boolean[] rolandDrumChannels = null;
	private boolean[] yamahaDrumChannels = null;

	public SequenceDataCache(Sequence song, String standard, boolean[] rolandDrumChannels, ArrayList<TreeMap<Long, Boolean>> yamahaDrumSwitches, boolean[] yamahaDrumChannels, ArrayList<TreeMap<Long, Boolean>> mmaDrumSwitches, TreeMap<Integer, Integer> portMap)
	{
		Map<Integer, Long> tempoLengths = new HashMap<>();
		
		this.standard = standard;
		this.rolandDrumChannels = rolandDrumChannels;
		this.yamahaDrumChannels = yamahaDrumChannels;
		
		brandDrumBanks = new int[song.getTracks().length];
		
		tempo.put(0L, TempoEvent.DEFAULT_TEMPO);
		int minTempoMPQ = Integer.MAX_VALUE;
		int maxTempoMPQ = Integer.MIN_VALUE;
		TimeSignature timeSignature = null;

		divisionType = song.getDivisionType();
		tickResolution = song.getResolution();

		// Keep track of the active registered paramater number for pitch bend range
		int[] rpn = new int[CHANNEL_COUNT];
		Arrays.fill(rpn, REGISTERED_PARAM_NONE);

		/*
		 * We need to be able to know which tracks have drum notes.
		 * We also need to know what instrument voices are used in each track,
		 * so we build maps of voice changes that TrackInfo later can use
		 * to build strings of instruments for each track.
		 * 
		 * This among other things we will find out by iterating through all
		 * MidiEvents.
		 * 
		 */		
		Track[] tracks = song.getTracks();
		for (int iiTrack = 0; iiTrack < tracks.length; iiTrack++)
		{
			Track track = tracks[iiTrack];
			int port = 0;
			portMap.put(iiTrack, port);
			
			for (int jj = 0, sz1 = track.size(); jj < sz1; jj++)
			{
				MidiEvent evt = track.get(jj);
				MidiMessage msg = evt.getMessage();
				long tick = evt.getTick();
				if (msg instanceof MetaMessage m)
				{
					if (m.getType() == META_PORT_CHANGE)	{
						byte[] portChange = m.getData();
						if (portChange.length == 1 && tick == 0) {
							// Support for (non-midi-standard) port assignments used by Cakewalk and Musescore. 
							// We only support this for GM, and only super well-formed (tick == 0).
							port = (int) portChange[0];
							//System.out.println("Port change on track "+iiTrack+"  tick "+tick+"  port "+formatBytes(portChange));
							portMap.put(iiTrack, port);
						}
					}
				}
			}
		}
		long lastTick = 0;
		for (int iTrack = 0; iTrack < tracks.length; iTrack++)
		{
			Track track = tracks[iTrack];
			
			for (int j = 0, sz = track.size(); j < sz; j++)
			{
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				long tick = evt.getTick();
				if (tick > lastTick)
					lastTick = tick;

				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();
					int ch = m.getChannel();
					
					if (cmd == ShortMessage.NOTE_ON) {
						if (rolandDrumChannels != null && rolandDrumChannels[ch] && standard == "GS") {
							brandDrumBanks[iTrack] = 2;// GS Drums
						} else if (brandDrumBanks[iTrack] != 1 && standard == "XG" && yamahaDrumSwitches != null && yamahaDrumSwitches.get(ch).floorEntry(tick) != null && yamahaDrumSwitches.get(ch).floorEntry(tick).getValue()) {
							brandDrumBanks[iTrack] = 1;// XG drums
						} else if (brandDrumBanks[iTrack] != 4 && standard == "GM2" && mmaDrumSwitches != null && mmaDrumSwitches.get(ch).floorEntry(tick) != null && mmaDrumSwitches.get(ch).floorEntry(tick).getValue()) {
							brandDrumBanks[iTrack] = 4;// GM2 drums
						} else if (ch == DRUM_CHANNEL && (standard == "GM" || standard == "ABC")) {
							brandDrumBanks[iTrack] = 3;// GM drums on channel #10
						}
					} else if (cmd == ShortMessage.PROGRAM_CHANGE)
					{
						if ((
								(ch != DRUM_CHANNEL && rolandDrumChannels == null && yamahaDrumChannels == null)
								|| ((rolandDrumChannels == null || standard != "GS" || !rolandDrumChannels[ch]) && (yamahaDrumChannels == null || standard != "XG" || !yamahaDrumChannels[ch]))
								)
								&& (standard != "XG" || yamahaDrumSwitches == null || yamahaDrumSwitches.get(ch).floorEntry(tick) == null || !yamahaDrumSwitches.get(ch).floorEntry(tick).getValue())
								&& (standard != "GM2" || mmaDrumSwitches == null || mmaDrumSwitches.get(ch).floorEntry(tick) == null || !mmaDrumSwitches.get(ch).floorEntry(tick).getValue()))
						{
							instruments.put(portMap.get(iTrack), ch, tick, m.getData1());
						}
						mapPatch.put(ch, tick, m.getData1());
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE)
					{
						switch (m.getData1()) {
							case CHANNEL_VOLUME_CONTROLLER_COARSE -> volume.put(ch, tick, m.getData2());
							case REGISTERED_PARAMETER_NUMBER_MSB ->
									rpn[ch] = (rpn[ch] & 0x7F) | ((m.getData2() & 0x7F) << 7);
							case REGISTERED_PARAMETER_NUMBER_LSB ->
									rpn[ch] = (rpn[ch] & (0x7F << 7)) | (m.getData2() & 0x7F);
							case DATA_ENTRY_COARSE -> {
								if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
									pitchBendCoarse.put(ch, tick, m.getData2());
							}
							case DATA_ENTRY_FINE -> {
								if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
									pitchBendFine.put(ch, tick, m.getData2());
							}
							case BANK_SELECT_MSB -> {
								if (ch != DRUM_CHANNEL || standard != "XG" || m.getData2() == 126 || m.getData2() == 127) {
									// Due to XG drum part protect mode being ON, drum channel 9 only can switch between MSB 126 & 127.
									mapMSB.put(ch, tick, m.getData2());
								} else if (ch == DRUM_CHANNEL && standard == "XG" && m.getData2() != 126 && m.getData2() != 127) {
									System.err.println("XG Drum Part Protect Mode prevented bank select MSB.");
								}
							}
							//if(ch==DRUM_CHANNEL) System.err.println("Bank select MSB "+m.getData2()+"  "+tick);
							case BANK_SELECT_LSB -> mapLSB.put(ch, tick, m.getData2());

							//if(ch==DRUM_CHANNEL) System.err.println("Bank select LSB "+m.getData2()+"  "+tick);
						}
					}
				} else if (msg instanceof SysexMessage sysex) {
					byte[] message = sysex.getMessage();
					if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
						&& (message[4] & 0xFF) == 0x08 && (message[8] & 0xFF) == 0xF7) {
				    	String bank = message[6]==1?"MSB":(message[6]==2?"LSB":(message[6]==3?"Patch":""));
				    	if (standard == "XG" && bank != "" && message[5] < 16 && message[5] > -1 && message[7] < 128 && message[7] > -1) {
							switch (bank) {
								case "MSB" ->
									// XG Drum Part Protect Mode does not apply to sysex bank changes.
										mapMSB.put((int) message[5], tick, (int) message[7]);
								case "Patch" -> mapPatch.put((int) message[5], tick, (int) message[7]);
								case "LSB" -> mapLSB.put((int) message[5], tick, (int) message[7]);
							}
				    	}
					}
			    }
				else if (iTrack == 0 && (divisionType == Sequence.PPQ) && MidiUtils.isMetaTempo(msg))
				{
					TempoEvent te = getTempoEventForTick(tick);
					long elapsedMicros = MidiUtils.ticks2microsec(tick - te.tick, te.tempoMPQ, tickResolution);
					tempoLengths.put(te.tempoMPQ, elapsedMicros + Util.valueOf(tempoLengths.get(te.tempoMPQ), 0));
					tempo.put(tick, new TempoEvent(MidiUtils.getTempoMPQ(msg), tick, te.micros + elapsedMicros));

					if (te.tempoMPQ < minTempoMPQ)
						minTempoMPQ = te.tempoMPQ;
					if (te.tempoMPQ > maxTempoMPQ)
						maxTempoMPQ = te.tempoMPQ;
				}
				else if (msg instanceof MetaMessage m)
				{
					if (m.getType() == META_TIME_SIGNATURE && timeSignature == null)
					{
						timeSignature = new TimeSignature(m);
					}
				}
			}
		}
		
		// Setup default banks for extensions:
		for (int i = 0; i < 16;i++) {
			mapPatch.put(i, -1, 0);
			mapLSB.put(i, -1, 0);
		}
		if (standard == "XG" && yamahaDrumChannels != null) {
			// Bank 127 is implicit the default on drum channels in XG.
			for (int i = 0; i < 16;i++) {
				if (yamahaDrumChannels[i]) mapMSB.put(i, -1, 127);
				else mapMSB.put(i, -1, 0);
			}
		} else if (standard == "GM2") {
			// Bank 120 is implicit the default on drum channel in GM2.
			// Bank 121 is implicit the default on all other channels in GM2.			
			mapMSB.put(0, -1, 121);
			mapMSB.put(1, -1, 121);
			mapMSB.put(2, -1, 121);
			mapMSB.put(3, -1, 121);
			mapMSB.put(4, -1, 121);
			mapMSB.put(5, -1, 121);
			mapMSB.put(6, -1, 121);
			mapMSB.put(7, -1, 121);
			mapMSB.put(8, -1, 121);
			mapMSB.put(DRUM_CHANNEL, -1, 120);
			mapMSB.put(10, -1, 121);
			mapMSB.put(11, -1, 121);
			mapMSB.put(12, -1, 121);
			mapMSB.put(13, -1, 121);
			mapMSB.put(14, -1, 121);
			mapMSB.put(15, -1, 121);
		} else {
			for (int i = 0; i < 16;i++) {
				mapMSB.put(i, -1, 0);
			}
		}

		// Account for the duration of the final tempo
		TempoEvent te = getTempoEventForTick(lastTick);
		long elapsedMicros = MidiUtils.ticks2microsec(lastTick - te.tick, te.tempoMPQ, tickResolution);
		tempoLengths.put(te.tempoMPQ, elapsedMicros + Util.valueOf(tempoLengths.get(te.tempoMPQ), 0));

		Entry<Integer, Long> max = null;
		for (Entry<Integer, Long> entry : tempoLengths.entrySet())
		{
			if (max == null || entry.getValue() > max.getValue())
				max = entry;
		}
		primaryTempoMPQ = (max == null) ? DEFAULT_TEMPO_MPQ : max.getKey();

		this.minTempoMPQ = (minTempoMPQ == Integer.MAX_VALUE) ? DEFAULT_TEMPO_MPQ : minTempoMPQ;
		this.maxTempoMPQ = (maxTempoMPQ == Integer.MIN_VALUE) ? DEFAULT_TEMPO_MPQ : maxTempoMPQ;
		this.timeSignature = (timeSignature == null) ? TimeSignature.FOUR_FOUR : timeSignature;

		songLengthTicks = lastTick;
	}
	
	private String formatBytes(byte[] portChange) {
		StringBuilder str = new StringBuilder();
		for (byte by : portChange) {
			str.append((int) by).append(" ");
		}
		StringBuilder sb = new StringBuilder();
	    for (byte b : portChange) {
	        sb.append(String.format("%02X ", b));
	    }				    				    
	    str.append("[ ").append(sb).append("]");
		return str.toString();
	}

	public boolean isXGDrumsTrack (int track) {
		if (track >= brandDrumBanks.length) return false;
		return brandDrumBanks[track] == 1;
	}
	
	public boolean isGSDrumsTrack (int track) {
		if (track >= brandDrumBanks.length) return false;
		return brandDrumBanks[track] == 2;
	}
	
	public boolean isDrumsTrack (int track) {
		if (track >= brandDrumBanks.length) return false;
		return brandDrumBanks[track] == 3;
	}
	
	public boolean isGM2DrumsTrack (int track) {
		if (track >= brandDrumBanks.length) return false;
		return brandDrumBanks[track] == 4;
	}

	public int getInstrument(int port, int channel, long tick)
	{
		return instruments.get(port, channel, tick);
	}
	
	public String getInstrumentExt(int channel, long tick, boolean drumKit)
	{
		int type = 0;
		boolean rhythmChannel = channel == DRUM_CHANNEL;
		if (standard == "XG") {
			type = ExtensionMidiInstrument.XG;
			rhythmChannel = yamahaDrumChannels[channel];
		} else if (standard == "GS") {
			type = ExtensionMidiInstrument.GS;
			rhythmChannel = rolandDrumChannels[channel];
		} else if (standard == "GM2") {
			type = ExtensionMidiInstrument.GM2;
		} else {
			type = ExtensionMidiInstrument.GM;
		}
		long patchTick = mapPatch.getEntryTick(channel, tick);
		if (patchTick == NO_RESULT) {
			return null;
		}
		
		String value = ExtensionMidiInstrument.getInstance().fromId(type, (byte)mapMSB.get(channel, patchTick), (byte)mapLSB.get(channel, patchTick), (byte)mapPatch.get(channel, tick), drumKit, rhythmChannel);
		//if (value == null && drumKit) {
		//	System.out.println(mapMSB.get(channel, patchTick)+","+ mapLSB.get(channel, patchTick)+","+mapPatch.get(channel, tick)+"  "+ rhythmChannel);
		//}
		return value;
	}

	public int getVolume(int channel, long tick)
	{
		return volume.get(channel, tick);
	}

	public double getPitchBendRange(int channel, long tick)
	{
		return pitchBendCoarse.get(channel, tick) + (pitchBendFine.get(channel, tick) / 100.0);
	}

	public long getSongLengthTicks()
	{
		return songLengthTicks;
	}

	@Override public long tickToMicros(long tick)
	{
		if (divisionType != Sequence.PPQ)
			return (long) (TimingInfo.ONE_SECOND_MICROS * ((double) tick / (double) (divisionType * tickResolution)));

		TempoEvent te = getTempoEventForTick(tick);
		return te.micros + MidiUtils.ticks2microsec(tick - te.tick, te.tempoMPQ, tickResolution);
	}

	@Override public long microsToTick(long micros)
	{
		if (divisionType != Sequence.PPQ)
			return (long) (divisionType * tickResolution * micros / (double) TimingInfo.ONE_SECOND_MICROS);

		TempoEvent te = getTempoEventForMicros(micros);
		return te.tick + MidiUtils.microsec2ticks(micros - te.micros, te.tempoMPQ, tickResolution);
	}

	public int getTempoMPQ(long tick)
	{
		return getTempoEventForTick(tick).tempoMPQ;
	}

	public int getTempoBPM(long tick)
	{
		return (int) Math.round(MidiUtils.convertTempo(getTempoMPQ(tick)));
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public int getMinTempoMPQ()
	{
		return minTempoMPQ;
	}

	public int getMinTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getMinTempoMPQ()));
	}

	public int getMaxTempoMPQ()
	{
		return maxTempoMPQ;
	}

	public int getMaxTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getMaxTempoMPQ()));
	}

	public int getTickResolution()
	{
		return tickResolution;
	}

	public TimeSignature getTimeSignature()
	{
		return timeSignature;
	}

	public long getBarLengthTicks()
	{
		// tickResolution is in ticks per quarter note
		return 4L * tickResolution * timeSignature.numerator / timeSignature.denominator;
	}
	
	public long getBarToTick(int bar) {
		return getBarLengthTicks()*(bar-1);
	}

	@Override public int tickToBarNumber(long tick)
	{
		return (int) (tick / getBarLengthTicks());
	}

	public NavigableMap<Long, TempoEvent> getTempoEvents()
	{
		return tempo;
	}

	/**
	 * Tempo Handling
	 */
	public static class TempoEvent
	{
		private TempoEvent(int tempoMPQ, long startTick, long startMicros)
		{
			this.tempoMPQ = tempoMPQ;
			this.tick = startTick;
			this.micros = startMicros;
		}

		public static final TempoEvent DEFAULT_TEMPO = new TempoEvent(DEFAULT_TEMPO_MPQ, 0, 0);

		public final int tempoMPQ;
		public final long tick;
		public long micros;
	}
	
	public TempoEvent getATempoEvent(int tempoMPQ, long startTick, long startMicros) {
		return new TempoEvent(tempoMPQ, startTick, startMicros);
	}

	public TempoEvent getTempoEventForTick(long tick)
	{
		Entry<Long, TempoEvent> entry = tempo.floorEntry(tick);
		if (entry != null)
			return entry.getValue();

		return TempoEvent.DEFAULT_TEMPO;
	}

	public TempoEvent getTempoEventForMicros(long micros)
	{
		TempoEvent prev = TempoEvent.DEFAULT_TEMPO;
		for (TempoEvent event : tempo.values())
		{
			if (event.micros > micros)
				break;

			prev = event;
		}
		return prev;
	}

	/**
	 * Map by channel
	 */
	private static class MapByChannel
	{
		private NavigableMap<Long, Integer>[] map;
		private int defaultValue;

		@SuppressWarnings("unchecked")//
		public MapByChannel(int defaultValue)
		{
			map = new NavigableMap[CHANNEL_COUNT];
			this.defaultValue = defaultValue;
		}

		public void put(int channel, long tick, Integer value)
		{
			if (map[channel] == null)
				map[channel] = new TreeMap<>();

			map[channel].put(tick, value);
		}

		public int get(int channel, long tick)
		{
			if (map[channel] == null)
				return defaultValue;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return defaultValue;

			return entry.getValue();
		}
		
		public long getEntryTick(int channel, long tick)
		{
			if (map[channel] == null)
				return NO_RESULT;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return NO_RESULT;

			return entry.getKey();
		}
	}
	
	/**
	 * Map by channel
	 */
	private static class MapByChannelPort
	{
		private NavigableMap<Long, Integer>[][] map;
		private int defaultValue;

		@SuppressWarnings("unchecked")//
		public MapByChannelPort(int defaultValue)
		{
			map = new NavigableMap[PORT_COUNT][CHANNEL_COUNT];
			this.defaultValue = defaultValue;
		}

		public void put(int port, int channel, long tick, Integer value)
		{
			if (map[port][channel] == null)
				map[port][channel] = new TreeMap<>();

			map[port][channel].put(tick, value);
		}

		public int get(int port, int channel, long tick)
		{
			if (map[port][channel] == null)
				return defaultValue;

			Entry<Long, Integer> entry = map[port][channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return defaultValue;

			return entry.getValue();
		}
		
		public long getEntryTick(int port, int channel, long tick)
		{
			if (map[port][channel] == null)
				return NO_RESULT;

			Entry<Long, Integer> entry = map[port][channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return NO_RESULT;

			return entry.getKey();
		}
	}
	
	public boolean isGM () {
		return "GM".equals(standard);
	}
}
