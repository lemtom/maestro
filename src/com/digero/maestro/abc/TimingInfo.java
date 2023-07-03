package com.digero.maestro.abc;

import com.digero.common.midi.TimeSignature;
import com.digero.common.midiutils.MidiUtils;

public class TimingInfo
{
	public static final long ONE_SECOND_MICROS = 1000000;
	public static final long ONE_MINUTE_MICROS = 60 * ONE_SECOND_MICROS;
	//public static final long SHORTEST_NOTE_MICROS = 60001;
	public static final long LONGEST_NOTE_MICROS = ONE_MINUTE_MICROS / 12;// reduced to 5s from 6s due to some samples are shorter than 6s.
	public static final int MAX_TEMPO_BPM = (int) (ONE_MINUTE_MICROS / getShortestNoteMicros(125));
	public static final int MIN_TEMPO_BPM = (int) ((ONE_MINUTE_MICROS + (ONE_MINUTE_MICROS /10) / 2) / (ONE_MINUTE_MICROS /10)); // Round up

	private final int tempoMPQ;
	private final int resolutionPPQ;
	private final float exportTempoFactor;

	private final TimeSignature meter;

	private final int defaultDivisor;
	private final int minNoteDivisor;
	private final long minNoteLengthTicks;
	private final long maxNoteLengthTicks;
	
	@Override
	public String toString() {
		String str = "  TimingInfo:\n";
		str += meter.toString()+"\n";
		str += resolutionPPQ+"\n";
		str += tempoMPQ+"\n";
		str += exportTempoFactor+"\n";
		str += defaultDivisor+"\n";
		str += minNoteDivisor+"\n";
		str += minNoteLengthTicks+"\n";
		return str;		
	}

	TimingInfo(int tempoMPQ, int resolutionPPQ, float exportTempoFactor, TimeSignature meter, boolean useTripletTiming, int abcSongBPM)
			throws AbcConversionException
	{
		// Compute the export tempo and round it to a whole-number BPM
		double exportTempoMPQ = roundTempoMPQ((double) tempoMPQ / exportTempoFactor);

		// Now adjust the tempoMPQ by however much we just rounded the export tempo
		tempoMPQ = (int) Math.round(exportTempoMPQ * exportTempoFactor);

		this.tempoMPQ = tempoMPQ;
		this.resolutionPPQ = resolutionPPQ;
		this.exportTempoFactor = exportTempoFactor;
		this.meter = meter;

		final long SHORTEST_NOTE_TICKS = (long) Math.ceil((getShortestNoteMicros(abcSongBPM) * resolutionPPQ) / exportTempoMPQ);
		final long LONGEST_NOTE_TICKS = (long) Math.floor((LONGEST_NOTE_MICROS * resolutionPPQ) / exportTempoMPQ);

		final int exportTempoBPM = (int) Math.round(MidiUtils.convertTempo(exportTempoMPQ));

		if (exportTempoBPM > MAX_TEMPO_BPM || exportTempoBPM < MIN_TEMPO_BPM)
		{
			throw new AbcConversionException("Tempo " + exportTempoBPM + " is out of range. Must be between "
					+ MIN_TEMPO_BPM + " and " + MAX_TEMPO_BPM + ".");
		}

		// From http://abcnotation.com/abc2mtex/abc.txt:
		// The default note length can be calculated by computing the meter as
		// a decimal; if it is less than 0.75 the default is a sixteenth note,
		// otherwise it is an eighth note. For example, 2/4 = 0.5, so the
		// default note length is a sixteenth note, while 4/4 = 1.0 or
		// 6/8 = 0.75, so the default is an eighth note.
		this.defaultDivisor = ((meter.numerator / (double) meter.denominator < 0.75) ? 16 : 8) * 4 / meter.denominator;

		// Calculate min note length
		{
			int minNoteDivisor = defaultDivisor;
			if (useTripletTiming)
				minNoteDivisor *= 3;
			long minNoteTicks = (4 * resolutionPPQ) / (minNoteDivisor);

			while (minNoteTicks < SHORTEST_NOTE_TICKS && minNoteDivisor % 2 == 0)
			{
				minNoteTicks *= 2;
				minNoteDivisor /= 2;
			}

			assert minNoteDivisor > 0;

			while (minNoteTicks >= SHORTEST_NOTE_TICKS * 2 && minNoteTicks % 2 == 0)
			{
				minNoteTicks /= 2;
				minNoteDivisor *= 2;
			}

			if (meter.denominator > minNoteDivisor)
			{
				throw new AbcConversionException("The denominator of the meter must be no greater than "
						+ minNoteDivisor + " at this tempo");
			}

			this.minNoteLengthTicks = minNoteTicks;
			this.minNoteDivisor = minNoteDivisor;
			this.maxNoteLengthTicks = minNoteTicks * (LONGEST_NOTE_TICKS / minNoteTicks);
		}
	}
	
	public static long getShortestNoteMicros(int bpm) {
		if (bpm == 30 || bpm == 60 || bpm == 90 || bpm == 120) {
			return 60001L;
		}
		return 60000L;
	}

	/**
	 * Rounds the given MPQ tempo so it corresponds to a whole-number of beats per minute.
	 */
	public static double roundTempoMPQ(double tempoMPQ)
	{
		return MidiUtils.convertTempo(Math.round(MidiUtils.convertTempo(tempoMPQ)));
	}

	public int getTempoMPQ()
	{
		return tempoMPQ;
	}

	public int getTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(tempoMPQ));
	}

	public int getResolutionPPQ()
	{
		return resolutionPPQ;
	}

	public float getExportTempoFactor()
	{
		return exportTempoFactor;
	}

	public int getExportTempoMPQ()
	{
		return (int) Math.round(tempoMPQ / exportTempoFactor);
	}

	public int getExportTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo((double) tempoMPQ / exportTempoFactor));
	}

	public TimeSignature getMeter()
	{
		return meter;
	}

	public int getDefaultDivisor()
	{
		return defaultDivisor;
	}

	public int getMinNoteDivisor()
	{
		return minNoteDivisor;
	}

	public long getMinNoteLengthTicks()
	{
		return minNoteLengthTicks;
	}

	public long getMaxNoteLengthTicks()
	{
		return maxNoteLengthTicks;
	}

	public long getBarLengthTicks()
	{
		// for some songs this gives wrong result, but for most it works:
		return minNoteDivisor * minNoteLengthTicks * meter.numerator / meter.denominator;
	}
}
