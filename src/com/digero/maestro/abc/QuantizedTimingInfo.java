package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.IBarNumberCache;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.TimeSignature;
import com.digero.common.midiutils.MidiUtils;
import com.digero.common.util.Util;
import com.digero.maestro.midi.NoteEvent;
import com.digero.maestro.midi.SequenceDataCache;
import com.digero.maestro.midi.SequenceDataCache.TempoEvent;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.common.midi.MidiConstants;

public class QuantizedTimingInfo implements ITempoCache, IBarNumberCache
{
	// Tick => TimingInfoEvent
	private final NavigableMap<Long, TimingInfoEvent> timingInfoByTick = new TreeMap<Long, TimingInfoEvent>();
	private final HashMap<AbcPart, NavigableMap<Long, TimingInfoEvent>> oddTimingInfoByTick = new HashMap<AbcPart, NavigableMap<Long, TimingInfoEvent>>();

	private NavigableSet<Long> barStartTicks = null;
	private Long[] barStartTickByBar = null;
	private final long songLengthTicks;
	private final int tickResolution;

	private final int primaryTempoMPQ;
	private final float exportTempoFactor;
	private final TimeSignature meter;
	private final boolean tripletTiming;
	private final boolean oddsAndEnds;
	public final static int COMBINE_PRIORITY_MULTIPLIER = 4;// Do not change this number without exposing the int in UI. Since old projects will have 4 saved in msx.

	public QuantizedTimingInfo(SequenceInfo source, float exportTempoFactor, TimeSignature meter,
			boolean useTripletTiming, int abcSongBPM, AbcSong song, boolean oddsAndEnds) throws AbcConversionException
	{
		double exportPrimaryTempoMPQ = TimingInfo.roundTempoMPQ(source.getPrimaryTempoMPQ() / exportTempoFactor);
		this.primaryTempoMPQ = (int) Math.round(exportPrimaryTempoMPQ * exportTempoFactor);
		this.exportTempoFactor = exportTempoFactor;
		this.meter = meter;
		this.tripletTiming = useTripletTiming;
		this.tickResolution = source.getDataCache().getTickResolution();
		this.songLengthTicks = source.getDataCache().getSongLengthTicks();
		final int resolution = source.getDataCache().getTickResolution();

		TimingInfo defaultTiming = new TimingInfo(source.getPrimaryTempoMPQ(), resolution, exportTempoFactor, meter,
				useTripletTiming, abcSongBPM);
		TimingInfo defaultOddTiming = new TimingInfo(source.getPrimaryTempoMPQ(), resolution, exportTempoFactor, meter,
				!useTripletTiming, abcSongBPM);
		timingInfoByTick.put(0L, new TimingInfoEvent(0, 0, 0, defaultTiming, defaultOddTiming));
		//System.out.println("even"+defaultTiming.toString());
		//System.out.println("odd"+defaultOddTiming.toString());
		
		Collection<TimingInfoEvent> reversedEvents = timingInfoByTick.descendingMap().values();

		/* Go through the tempo events from the MIDI file and quantize them so each event starts at
		 * an integral multiple of the previous event's MinNoteLengthTicks. This ensures that we can
		 * split notes at each tempo change without creating a note that is shorter than
		 * MinNoteLengthTicks. */
		Collection<SequenceDataCache.TempoEvent> origTempos = source.getDataCache().getTempoEvents().values();
		
		TreeMap<Long, Integer> changeTree = song.getTuneTempoChanges();
		ArrayList<SequenceDataCache.TempoEvent> combinedTempos = new ArrayList<SequenceDataCache.TempoEvent>();
		
		for (SequenceDataCache.TempoEvent midiTempo : origTempos) {
			// Modify the orig midi tempos by tune editor amount
			long tick = midiTempo.tick;
			Entry<Long, Integer> midiEntry = changeTree.floorEntry(tick);
			if (midiEntry != null && midiEntry.getValue() != 0) {
				int newTempo = (int) MidiUtils.convertTempo(Math.max(1.0d, MidiUtils.convertTempo(midiTempo.tempoMPQ) + midiEntry.getValue()));
				SequenceDataCache.TempoEvent te = source.getDataCache().getATempoEvent(newTempo, midiTempo.tick, midiTempo.micros);
				combinedTempos.add(te);
			} else {
				combinedTempos.add(source.getDataCache().getATempoEvent(midiTempo.tempoMPQ, midiTempo.tick, midiTempo.micros));
			}
		}
		for (Entry<Long, Integer> tuneTempo : changeTree.entrySet()) {
			// Add in tune editor tempo changes where there was not a midi tempo change
			long tick = tuneTempo.getKey();
			SequenceDataCache.TempoEvent oldTempo = source.getDataCache().getTempoEvents().get(tick);
			if (oldTempo == null) {
				Entry<Long, TempoEvent> prevTempo = source.getDataCache().getTempoEvents().floorEntry(tick);
				if (prevTempo != null) {
					int mpq = prevTempo.getValue().tempoMPQ;
					if (tuneTempo.getValue() != 0) {
						mpq = (int) MidiUtils.convertTempo(Math.max(1.0d, MidiUtils.convertTempo(mpq) + tuneTempo.getValue()));
					}
					SequenceDataCache.TempoEvent te = source.getDataCache().getATempoEvent(mpq, tick, prevTempo.getValue().micros);
					combinedTempos.add(te);
				} else {
					int mpq = MidiConstants.DEFAULT_TEMPO_MPQ;
					if (tuneTempo.getValue() != 0) {
						mpq = (int) MidiUtils.convertTempo(Math.max(1.0d, MidiUtils.convertTempo(mpq) + tuneTempo.getValue()));
					}
					SequenceDataCache.TempoEvent te = source.getDataCache().getATempoEvent(mpq, tick, SequenceDataCache.TempoEvent.DEFAULT_TEMPO.micros);
					combinedTempos.add(te);
				}
			}
		}		
		Comparator<SequenceDataCache.TempoEvent> rator = new Comparator<SequenceDataCache.TempoEvent>() {
			@Override public int compare(TempoEvent o1, TempoEvent o2) {
				if (o1.tick == o2.tick) {
					return 0;
				} else if (o1.tick > o2.tick) {
					return 1;
				}
				return -1;
			}
		};
		combinedTempos.sort(rator);
		calcNewMicros(combinedTempos);
		for (SequenceDataCache.TempoEvent sourceEvent : combinedTempos)
		{
			long tick = 0;
			long micros = 0;
			double barNumber = 0;
			TimingInfo info = new TimingInfo(sourceEvent.tempoMPQ, resolution, exportTempoFactor, meter,
					useTripletTiming, abcSongBPM);
			TimingInfo infoOdd = new TimingInfo(sourceEvent.tempoMPQ, resolution, exportTempoFactor, meter,
					!useTripletTiming, abcSongBPM);
			
			// Iterate over the existing events in reverse order
			Iterator<TimingInfoEvent> reverseIterator = reversedEvents.iterator();
			while (reverseIterator.hasNext())
			{
				TimingInfoEvent prev = reverseIterator.next();
				assert prev.tick <= sourceEvent.tick;

				long gridUnitTicks = prev.info.getMinNoteLengthTicks();

				// Quantize the tick length to the floor multiple of gridUnitTicks
				long lengthTicks = Util.floorGrid(sourceEvent.tick - prev.tick, gridUnitTicks);

				/* If the new event has a coarser timing grid than prev, then it's possible that the
				 * bar splits will not align to the grid. To avoid this, adjust the length so that
				 * the new event starts at a time that will allow the bar to land on the
				 * quantization grid. */
				while (lengthTicks > 0)
				{
					double barNumberTmp = prev.barNumber + lengthTicks / ((double) prev.info.getBarLengthTicks());
					double gridUnitsRemaining = ((Math.ceil(barNumberTmp) - barNumberTmp) * info.getBarLengthTicks())
							/ info.getMinNoteLengthTicks();

					final double epsilon = TimingInfo.MIN_TEMPO_BPM / (2.0 * TimingInfo.MAX_TEMPO_BPM);
					if (Math.abs(gridUnitsRemaining - Math.round(gridUnitsRemaining)) <= epsilon)
						break; // Ok, the bar ends on the grid

					lengthTicks -= gridUnitTicks;
				}

				if (lengthTicks <= 0)
				{
					// The prev tempo event was quantized to zero-length; remove it
					reverseIterator.remove();
					continue;
				}

				tick = prev.tick + lengthTicks;
				micros = prev.micros + MidiUtils.ticks2microsec(lengthTicks, prev.info.getTempoMPQ(), resolution);
				barNumber = prev.barNumber + lengthTicks / ((double) prev.info.getBarLengthTicks());
				break;
			}

			TimingInfoEvent event = new TimingInfoEvent(tick, micros, barNumber, info, infoOdd);

			timingInfoByTick.put(tick, event);
		}
		int parts = song.getParts().size();
		this.oddsAndEnds = oddsAndEnds;
		if (!oddsAndEnds) return;
		// default means timing is to laid out like the tripletcheckbox selected grid.
		// odd means timing is to laid out the opposite of tripletcheckbox selected grid.
		//System.err.println("  Odds And Ends:");
		int tracks = song.getSequenceInfo().getTrackCount();
		TimingInfoEvent[] timings = (TimingInfoEvent[]) timingInfoByTick.values().toArray(new TimingInfoEvent[0]);
		//long totalSwing = 0;
		//long totalEven = 0;
		
		for (int part = 0; part < parts; part++) {
			// calculate for all parts
			AbcPart abcPart = song.getParts().get(part);
			TreeMap<Long, TimingInfoEvent> partMap = new TreeMap<Long, TimingInfoEvent>();
			oddTimingInfoByTick.put(abcPart, partMap);
			
			// Lets us build an array of all notes in this part
			// Combine-priorities means some notes might be added several times.
			ArrayList<NoteEvent> eventList = new ArrayList<NoteEvent>();  
			for (int t = 0; t < tracks; t++) {
				if(abcPart.isTrackEnabled(t)) {
					int scoreMultiplier = (song.isPriorityActive() && abcPart.getEnabledTrackCount() > 1 && abcPart.isTrackPriority(t)) ? COMBINE_PRIORITY_MULTIPLIER : 1;
					if (abcPart.sectionsModified.get(t) == null && abcPart.nonSection.get(t) == null) {
						eventList.addAll(abcPart.getTrackEvents(t));
						for(NoteEvent note : abcPart.getTrackEvents(t)) {
							note.combinePrioritiesScoreMultiplier = scoreMultiplier;
						}
					} else {
						for(NoteEvent note : abcPart.getTrackEvents(t)) {
							if (abcPart.getAudible(t, note.getStartTick())) {
								note.combinePrioritiesScoreMultiplier = scoreMultiplier;
								eventList.add(note);
							}
						}
					}
				}
			}
			
			for (int j = 0; j < timings.length; j++ ) {
				// calculate for all tempochanges
				TimingInfoEvent tempoChange = timings[j];
				TimingInfoEvent nextTempoChange = null;
				if (j+1 < timings.length) {
					nextTempoChange = timings[j+1];
				}
				partMap.put(tempoChange.tick, tempoChange);
				
				//Now calculate duration of sixGrid sections. They will always end and start on quantized grid for both odd and even timing
				//I call it sixGrid due to durations of 3 and 2 will always coincide each 6th duration.
				//Its really just LCM (Least Common Multiple)
				long sixTicks = 0;
				boolean evenShortest = tempoChange.info.getMinNoteLengthTicks() < tempoChange.infoOdd.getMinNoteLengthTicks();
				int loopCount = 1;
				long longest = 0;
				long shortest = 0;
				if (evenShortest) {
					shortest = tempoChange.info.getMinNoteLengthTicks();
					longest = tempoChange.infoOdd.getMinNoteLengthTicks();
				} else {
					shortest = tempoChange.infoOdd.getMinNoteLengthTicks();
					longest = tempoChange.info.getMinNoteLengthTicks();
				}					
				sixTicks = longest;
				while(sixTicks % shortest != 0 && loopCount < shortest) {
					sixTicks += longest;
					loopCount++;
				}
				
				assert sixTicks % tempoChange.info.getMinNoteLengthTicks() == 0;
				assert sixTicks % tempoChange.infoOdd.getMinNoteLengthTicks() == 0;
				
				// Max possible number of sixGrid before song ending +1
				int maxSixths = (int) ((this.songLengthTicks-tempoChange.tick+sixTicks)/sixTicks);
				
				ArrayList<Integer> sixGridsOdds = new ArrayList<Integer>(maxSixths);
				for (int k = 0; k < maxSixths; k++) {
					sixGridsOdds.add(null);
				}
				
				int highest = -1;
				for (NoteEvent ne : eventList) {
					if (ne.getStartTick() > tempoChange.tick && (nextTempoChange == null || ne.getStartTick() < nextTempoChange.tick)) {
						// Note starting scores
						// The note starts after current tempo change and either is last tempochange or note starts before next tempo change
						long q = tempoChange.tick + Util.roundGrid(ne.getStartTick() - tempoChange.tick, tempoChange.info.getMinNoteLengthTicks());
						long qOdd = tempoChange.tick + Util.roundGrid(ne.getStartTick() - tempoChange.tick, tempoChange.infoOdd.getMinNoteLengthTicks());
						int odd = (int)(Math.abs(ne.getStartTick() - q) - Math.abs(ne.getStartTick() - qOdd));
						// determine which sixGrid we are in
						int sixGrid = (int) ((ne.getStartTick()-tempoChange.tick)/sixTicks);
						
						if (sixGrid >= maxSixths) continue;
						if (sixGrid > highest) highest = sixGrid;
						// Add a point to this sixGrid odd vs. default list.
						int oddScore = odd*2*ne.combinePrioritiesScoreMultiplier;
						if (sixGridsOdds.get(sixGrid) != null) {
							sixGridsOdds.set(sixGrid, sixGridsOdds.get(sixGrid)+oddScore);
						} else {
							sixGridsOdds.set(sixGrid, oddScore);
						}
					}
					if (!abcPart.getInstrument().equals(LotroInstrument.BASIC_DRUM) && ne.getEndTick() > tempoChange.tick && (nextTempoChange == null || ne.getEndTick() < nextTempoChange.tick)) {
						// Note ending scores
						// Do not evaluate note endings for drum
						// The note ends after current tempo change and either is last tempochange or note ends before next tempo change
						long q = tempoChange.tick + Util.roundGrid(ne.getEndTick() - tempoChange.tick, tempoChange.info.getMinNoteLengthTicks());
						long qOdd = tempoChange.tick + Util.roundGrid(ne.getEndTick() - tempoChange.tick, tempoChange.infoOdd.getMinNoteLengthTicks());
						int odd = (int)(Math.abs(ne.getEndTick() - q) - Math.abs(ne.getEndTick() - qOdd));
						// determine which sixGrid we are in
						int sixGrid = (int) ((ne.getEndTick()-tempoChange.tick)/sixTicks);
						
						if (sixGrid >= maxSixths) continue;
						if (sixGrid > highest) highest = sixGrid;
						// Add a point to this sixGrid odd vs. default list.
						int oddScore = odd*1*ne.combinePrioritiesScoreMultiplier;
						if (sixGridsOdds.get(sixGrid) != null) {
							sixGridsOdds.set(sixGrid, sixGridsOdds.get(sixGrid)+oddScore);
						} else {
							sixGridsOdds.set(sixGrid, oddScore);
						}
					}
				}
				boolean prevOdd = false;
				for (int i = 0; i <= highest; i++) {
					long tck = sixTicks*i;
					long micros = tempoChange.micros + MidiUtils.ticks2microsec(tck, tempoChange.info.getTempoMPQ(), resolution);
					tck += tempoChange.tick;
					assert (nextTempoChange == null || tck < nextTempoChange.tick);
					if (sixGridsOdds.get(i) != null && sixGridsOdds.get(i) > 0 && (nextTempoChange == null || tck <= nextTempoChange.tick-sixTicks)) {
						//if (useTripletTiming) totalEven += MidiUtils.ticks2microsec(sixTicks, tempoChange.info.getTempoMPQ(), tempoChange.info.getResolutionPPQ());
						//else if (!useTripletTiming) totalSwing += MidiUtils.ticks2microsec(sixTicks, tempoChange.info.getTempoMPQ(), tempoChange.info.getResolutionPPQ());
						if (!prevOdd) {
							TimingInfoEvent newTempoChange = new TimingInfoEvent(tck, micros, tempoChange.barNumber, tempoChange.infoOdd, null);
							partMap.remove(tck);
							partMap.put(tck, newTempoChange);
						}
						prevOdd = true;
					} else {
						//if (!useTripletTiming && sixGridsOdds.get(i) != null && sixGridsOdds.get(i) != 0) totalEven += MidiUtils.ticks2microsec(sixTicks, tempoChange.info.getTempoMPQ(), tempoChange.info.getResolutionPPQ());
						//else if (useTripletTiming && sixGridsOdds.get(i) != null && sixGridsOdds.get(i) != 0) totalSwing += MidiUtils.ticks2microsec(sixTicks, tempoChange.info.getTempoMPQ(), tempoChange.info.getResolutionPPQ());
						if (prevOdd) {
							TimingInfoEvent newTempoChange = new TimingInfoEvent(tck, micros, tempoChange.barNumber, tempoChange.info, null);
							partMap.putIfAbsent(tck, newTempoChange);
						}
						prevOdd = false;
					}
				}
				if (prevOdd) {
					int i = highest+1;
					// Make sure tempo section ends with a default tempoevent.
					long tck = sixTicks*i;
					long micros = tempoChange.micros + MidiUtils.ticks2microsec(tck, tempoChange.info.getTempoMPQ(), resolution);
					tck += tempoChange.tick;
					if (nextTempoChange == null || tck < nextTempoChange.tick) {
						TimingInfoEvent newTempoChange = new TimingInfoEvent(tck, micros, tempoChange.barNumber, tempoChange.info, null);
						partMap.put(tck, newTempoChange);
					}
				}
			}
		}
		//if (totalEven+totalSwing > 0) {
		//	System.err.println("Mix Timing: "+(int)(100*totalSwing/(float)(totalEven+totalSwing))+"% of abc song is swing/triplet timing.");
		//}
	}

	/**
	 * Recalculate all the microseconds in the ABC timing events.
	 * This will modify the TempoEvents, so make sure that you
	 * do not send the original midi tempo events to this method.
	 * 
	 * @param combinedTempos Sorted list of tempo events
	 */
	private void calcNewMicros(ArrayList<TempoEvent> combinedTempos) {
		int lastTempo = MidiConstants.DEFAULT_TEMPO_MPQ;
		long lastTick = 0L;
		long lastMicros = 0L;
		if(!combinedTempos.isEmpty()) {
			TempoEvent first = combinedTempos.get(0);
			if (first.tick < 0L) {
				// since the first is going to have negative micros from start
				// those micros should be calced from its own tempo
				lastTempo = first.tempoMPQ;
			}
		}
		for (TempoEvent event : combinedTempos) {
			if (event.tick == 0) {
				event.micros = 0L;
				continue;
			}
		    long newMicros = lastMicros + MidiUtils.ticks2microsec(event.tick-lastTick, lastTempo, tickResolution);
		    event.micros = newMicros;
		    lastTick   = event.tick;
		    lastMicros = event.micros;
		    lastTempo  = event.tempoMPQ;
		}		
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public int getPrimaryExportTempoMPQ()
	{
		return (int) Math.round(primaryTempoMPQ / exportTempoFactor);
	}

	public int getPrimaryExportTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo((double) primaryTempoMPQ / exportTempoFactor));
	}

	public float getExportTempoFactor()
	{
		return exportTempoFactor;
	}

	public TimeSignature getMeter()
	{
		return meter;
	}

	public boolean isTripletTiming()
	{
		return tripletTiming;
	}
	
	public boolean isMixTiming()
	{
		return oddsAndEnds;
	}

	public TimingInfo getTimingInfo(long tick, AbcPart part)
	{
		return getTimingEventForTick(tick, part).info;
	}

	public long quantize(long tick, AbcPart part)
	{
		TimingInfoEvent e = getTimingEventForTick(tick, part);
		return e.tick + Util.roundGrid(tick - e.tick, e.info.getMinNoteLengthTicks());
	}

	/**
	 *  Microseconds to tick. Does not take export tempo change into consideration.
	 */
	@Override public long tickToMicros(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return e.micros + MidiUtils.ticks2microsec(tick - e.tick, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	/**
	 *  Tick to microseconds. Does not take export tempo change into consideration.
	 */
	@Override public long microsToTick(long micros)
	{
		TimingInfoEvent e = getTimingEventForMicros(micros);
		return e.tick + MidiUtils.microsec2ticks(micros - e.micros, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}
	
	/**
	 *  Microseconds to tick. Does take export tempo change into consideration.
	 *  Returns micros in the ABC song.
	 */
	public long tickToMicrosABC(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return (long) ((e.micros + MidiUtils.ticks2microsec(tick - e.tick, e.info.getTempoMPQ(), e.info.getResolutionPPQ()))*getExportTempoFactor());
	}

	/**
	 *  Tick to microseconds. Does take export tempo change into consideration.
	 *  The micro is in the ABC song.
	 */
	public long microsToTickABC(long micros)
	{
		micros = (long)(micros / getExportTempoFactor());
		TimingInfoEvent e = getTimingEventForMicros(micros);
		return e.tick + MidiUtils.microsec2ticks(micros - e.micros, e.info.getTempoMPQ(), e.info.getResolutionPPQ());
	}

	@Override public int tickToBarNumber(long tick)
	{
		TimingInfoEvent e = getTimingEventForTick(tick);
		return (int) Math.floor(e.barNumber + (tick - e.tick) / ((double) e.info.getBarLengthTicks()));
	}

	public long tickToBarStartTick(long tick)
	{
		if (barStartTicks == null)
			calcBarStarts();

		if (tick <= barStartTicks.last())
			return barStartTicks.floor(tick);

		return barNumberToBarStartTick(tickToBarNumber(tick));
	}

	public long tickToBarEndTick(long tick)
	{
		if (barStartTicks == null)
			calcBarStarts();

		Long endTick = barStartTicks.higher(tick);
		if (endTick != null)
			return endTick;

		return barNumberToBarEndTick(tickToBarNumber(tick));
	}

	public long barNumberToBarStartTick(int barNumber)
	{
		if (barStartTickByBar == null)
			calcBarStarts();

		if (barNumber < barStartTickByBar.length)
			return barStartTickByBar[barNumber];

		TimingInfoEvent e = timingInfoByTick.lastEntry().getValue();
		return e.tick + Math.round((barNumber - e.barNumber) * e.info.getBarLengthTicks());
	}

	public long barNumberToBarEndTick(int barNumber)
	{
		return barNumberToBarStartTick(barNumber + 1);
	}

	public long barNumberToMicrosecond(int barNumber)
	{
		return tickToMicros(barNumberToBarStartTick(barNumber));
	}

	public int getMidiResolution()
	{
		return tickResolution;
	}

	private void calcBarStarts()
	{
		barStartTicks = new TreeSet<Long>();
		barStartTicks.add(0L);
		TimingInfoEvent prev = null;
		for (TimingInfoEvent event : timingInfoByTick.values())
		{
			if (prev != null)
			{
				// Calculate the start time for all bars that start between prev and event
				long barStart = prev.tick
						+ Math.round((Math.ceil(prev.barNumber) - prev.barNumber) * prev.info.getBarLengthTicks());
				while (barStart < event.tick)
				{
					barStartTicks.add(barStart);
					barStart += prev.info.getBarLengthTicks();
				}
			}
			prev = event;
		}

		// Calculate bar starts for all bars after the last tempo change
		long barStart = prev.tick
				+ Math.round((Math.ceil(prev.barNumber) - prev.barNumber) * prev.info.getBarLengthTicks());
		while (barStart <= songLengthTicks)
		{
			barStartTicks.add(barStart);
			barStart += prev.info.getBarLengthTicks();
		}
		barStartTicks.add(barStart);

		barStartTickByBar = barStartTicks.toArray(new Long[0]);
	}

	TimingInfoEvent getTimingEventForTick(long tick, AbcPart part)
	{
		if (oddsAndEnds)
			return oddTimingInfoByTick.get(part).floorEntry(tick).getValue();
		return timingInfoByTick.floorEntry(tick).getValue();
	}
	
	TimingInfoEvent getTimingEventForTick(long tick)
	{
		return timingInfoByTick.floorEntry(tick).getValue();
	}

	TimingInfoEvent getTimingEventForMicros(long micros)
	{
		TimingInfoEvent retVal = timingInfoByTick.firstEntry().getValue();
		for (TimingInfoEvent event : timingInfoByTick.values())
		{
			if (event.micros > micros)
				break;

			retVal = event;
		}
		return retVal;
	}

	TimingInfoEvent getNextTimingEvent(long tick, AbcPart part)
	{
		if (oddsAndEnds) {
			Map.Entry<Long, TimingInfoEvent> entry = oddTimingInfoByTick.get(part).higherEntry(tick);
			return (entry == null) ? null : entry.getValue();
		} else {
			Map.Entry<Long, TimingInfoEvent> entry = timingInfoByTick.higherEntry(tick);
			return (entry == null) ? null : entry.getValue();
		}
	}

	NavigableMap<Long, TimingInfoEvent> getTimingInfoByTick()
	{
		return timingInfoByTick;
	}

	class TimingInfoEvent
	{
		public final long tick;
		public final long micros;
		public final double barNumber; // May start in the middle of a bar

		public final TimingInfo info;
		public final TimingInfo infoOdd;

		public TimingInfoEvent(long tick, long micros, double barNumber, TimingInfo info, TimingInfo infoOdd)
		{
			this.tick = tick;
			this.micros = micros;
			this.barNumber = barNumber;
			this.info = info;
			this.infoOdd = infoOdd;
		}
	}
}
