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
import javax.sound.midi.Track;

import com.digero.common.midi.ExtensionMidiInstrument;
import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.Util;
import com.digero.maestro.abc.TimingInfo;
import com.sun.media.sound.MidiUtils;

public class SequenceDataCache implements MidiConstants, ITempoCache, IBarNumberCache
{
	private final int tickResolution;
	private final float divisionType;
	private final int primaryTempoMPQ;
	private final int minTempoMPQ;
	private final int maxTempoMPQ;
	private final TimeSignature timeSignature;
	private NavigableMap<Long, TempoEvent> tempo = new TreeMap<Long, TempoEvent>();

	private final long songLengthTicks;

	private MapByChannel instruments = new MapByChannel(DEFAULT_INSTRUMENT);
	private MapByChannel volume = new MapByChannel(DEFAULT_CHANNEL_VOLUME);
	private MapByChannel pitchBendCoarse = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_SEMITONES);
	private MapByChannel pitchBendFine = new MapByChannel(DEFAULT_PITCH_BEND_RANGE_CENTS);
	private MapByChannel mapMSB = new MapByChannel(0);
	private MapByChannel mapLSB = new MapByChannel(0);
	private MapByChannel mapPatch = new MapByChannel(0);
	private int[] brandDrumBanks;// 1 = XG drums, 2 = GS Drums, 3 = normal drums, 4 = GM2 drums
	private String standard = "GM";
	private boolean[] rolandDrumChannels = null;
	private boolean gm2DrumsOn11 = false;
	private boolean[] yamahaDrumChannels = null;

	public SequenceDataCache(Sequence song, String standard, boolean[] rolandDrumChannels, ArrayList<TreeMap<Long, Boolean>> yamahaDrumSwitches, boolean gm2DrumsOn11, boolean[] yamahaDrumChannels, ArrayList<TreeMap<Long, Boolean>> mmaDrumSwitches)
	{
		Map<Integer, Long> tempoLengths = new HashMap<Integer, Long>();
		
		this.standard = standard;
		this.rolandDrumChannels = rolandDrumChannels;
		this.yamahaDrumChannels = yamahaDrumChannels;
		this.gm2DrumsOn11 = gm2DrumsOn11;
		
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

		Track[] tracks = song.getTracks();
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
						if (rolandDrumChannels != null && rolandDrumChannels[ch] == true && ch != DRUM_CHANNEL && standard == "GS") {
							brandDrumBanks[iTrack] = 2;// GS Drums
						} else if (brandDrumBanks[iTrack] != 1 && standard == "XG" && yamahaDrumSwitches != null && yamahaDrumSwitches.get(ch).floorEntry(tick) != null && yamahaDrumSwitches.get(ch).floorEntry(tick).getValue() == true) {
							brandDrumBanks[iTrack] = 1;// XG drums
						} else if (brandDrumBanks[iTrack] != 4 && standard == "GM2" && mmaDrumSwitches != null && mmaDrumSwitches.get(ch).floorEntry(tick) != null && mmaDrumSwitches.get(ch).floorEntry(tick).getValue() == true) {
							brandDrumBanks[iTrack] = 4;// GM2 drums
						} else if (ch == DRUM_CHANNEL && (rolandDrumChannels == null || standard != "GS" || rolandDrumChannels[ch] == true) && (yamahaDrumChannels == null || standard != "XG" || yamahaDrumChannels[ch] == true)) {
							brandDrumBanks[iTrack] = 3;// Normal drums on channel #10
						} else if (ch == DRUM_CHANNEL+1 && gm2DrumsOn11) {
							brandDrumBanks[iTrack] = 4;// GM2 drums
							//System.err.println("GM2 drums on 11");
						} else if (yamahaDrumChannels != null && yamahaDrumChannels[ch] == true && ch != DRUM_CHANNEL && standard == "XG") {
							brandDrumBanks[iTrack] = 1;// XG drums
						}
					} else if (cmd == ShortMessage.PROGRAM_CHANGE)
					{
						if ((
								(ch != DRUM_CHANNEL && rolandDrumChannels == null && yamahaDrumChannels == null)
								|| ((rolandDrumChannels == null || standard != "GS" || rolandDrumChannels[ch] == false) && (yamahaDrumChannels == null || standard != "XG" || yamahaDrumChannels[ch] == false))
								)
								&& (standard != "XG" || yamahaDrumSwitches == null || yamahaDrumSwitches.get(ch).floorEntry(tick) == null || yamahaDrumSwitches.get(ch).floorEntry(tick).getValue() == false)
								&& (standard != "GM2" || mmaDrumSwitches == null || mmaDrumSwitches.get(ch).floorEntry(tick) == null || mmaDrumSwitches.get(ch).floorEntry(tick).getValue() == false))
						{
							instruments.put(ch, tick, m.getData1());
						}
						mapPatch.put(ch, tick, m.getData1());
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE)
					{
						switch (m.getData1())
						{
						case CHANNEL_VOLUME_CONTROLLER_COARSE:
							volume.put(ch, tick, m.getData2());
							break;
						case REGISTERED_PARAMETER_NUMBER_MSB:
							rpn[ch] = (rpn[ch] & 0x7F) | ((m.getData2() & 0x7F) << 7);
							break;
						case REGISTERED_PARAMETER_NUMBER_LSB:
							rpn[ch] = (rpn[ch] & (0x7F << 7)) | (m.getData2() & 0x7F);
							break;
						case DATA_ENTRY_COARSE:
							if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendCoarse.put(ch, tick, m.getData2());
							break;
						case DATA_ENTRY_FINE:
							if (rpn[ch] == REGISTERED_PARAM_PITCH_BEND_RANGE)
								pitchBendFine.put(ch, tick, m.getData2());
							break;
						case BANK_SELECT_MSB:
							mapMSB.put(ch, tick, m.getData2());
							//if(ch==DRUM_CHANNEL) System.err.println("Bank select MSB "+m.getData2()+"  "+tick);
							break;
						case BANK_SELECT_LSB:
							mapLSB.put(ch, tick, m.getData2());
							//if(ch==DRUM_CHANNEL) System.err.println("Bank select LSB "+m.getData2()+"  "+tick);
							break;
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
				else if (msg instanceof MetaMessage)
				{
					MetaMessage m = (MetaMessage) msg;
					if (m.getType() == META_TIME_SIGNATURE && timeSignature == null)
					{
						timeSignature = new TimeSignature(m);
					}
				}
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

	public int getInstrument(int channel, long tick)
	{
		return instruments.get(channel, tick);
	}
	
	public String getInstrumentExt(int channel, long tick, boolean drumKit)
	{
		int type = 0;
		boolean rhythmChannel = false;
		if (standard == "XG") {
			type = ExtensionMidiInstrument.XG;
			rhythmChannel = yamahaDrumChannels[channel];
		} else if (standard == "GS" && !drumKit) {
			type = ExtensionMidiInstrument.GS;
		} else if (standard == "GS" && drumKit) {
			type = ExtensionMidiInstrument.GSK;
			rhythmChannel = rolandDrumChannels[channel];
		} else if (standard == "GM2") {
			type = ExtensionMidiInstrument.GM2;
			rhythmChannel = channel == DRUM_CHANNEL+1 && gm2DrumsOn11?true:channel == DRUM_CHANNEL;
		} else {
			type = ExtensionMidiInstrument.GM;
			rhythmChannel = channel == DRUM_CHANNEL;
		}
		long patchTick = mapPatch.getEntryTick(channel, tick);
		if (patchTick == -1) {
			return null;
		}
		
		String value = ExtensionMidiInstrument.getInstance().fromId(type, (byte)mapMSB.get(channel, patchTick), (byte)mapLSB.get(channel, patchTick), (byte)mapPatch.get(channel, tick),drumKit, rhythmChannel);
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
		public final long micros;
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
				map[channel] = new TreeMap<Long, Integer>();

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
				return -1;

			Entry<Long, Integer> entry = map[channel].floorEntry(tick);
			if (entry == null) // No changes before this tick
				return -1;

			return entry.getKey();
		}
	}
}
