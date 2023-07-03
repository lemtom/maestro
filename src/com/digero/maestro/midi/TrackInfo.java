package com.digero.maestro.midi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiInstrument;
import com.digero.common.midi.Note;
import com.digero.common.midi.TimeSignature;
import com.digero.maestro.abc.TimingInfo;

public class TrackInfo implements MidiConstants
{
	private SequenceInfo sequenceInfo;

	private int trackNumber;
	private String name;
	private TimeSignature timeSignature = null;
	private KeySignature keySignature = null;
	private Set<Integer> instruments;
	private Set<String> instrumentExtensions;
	private List<NoteEvent> noteEvents;
	private SortedSet<Integer> notesInUse;
	private boolean isDrumTrack;
	private boolean isXGDrumTrack;
	private boolean isGSDrumTrack;
	private boolean isGM2DrumTrack;
	private final int minVelocity;
	private final int maxVelocity;
	
	@SuppressWarnings("unchecked")//
	TrackInfo(SequenceInfo parent, Track track, int trackNumber, SequenceDataCache sequenceCache, boolean isXGDrumTrack, boolean isGSDrumTrack, boolean wasType0, boolean isDrumsTrack, boolean isGM2DrumTrack, TreeMap<Integer, Integer> portMap)
			throws InvalidMidiDataException
	{
		this.sequenceInfo = parent;
		this.trackNumber = trackNumber;
		
		this.isXGDrumTrack = isXGDrumTrack;
		this.isGSDrumTrack = isGSDrumTrack;
		this.isGM2DrumTrack = isGM2DrumTrack;
		
		if (isXGDrumTrack || isGSDrumTrack || isDrumsTrack || isGM2DrumTrack) {
			isDrumTrack = true;
			
			// No need? Separated drum tracks already have their name. Type 0 channel tracks can keep their 'Track x', or?
			if (wasType0) {
				if (isXGDrumTrack) {
					name = "XG Drums";
				} else if (isGSDrumTrack) {
					name = "GS Drums";
				} else if (isGM2DrumTrack) {
					name = "GM2 Drums";
				} else {
					name = "Drums";
				}
			}
		}		
		
		instruments = new HashSet<>();
		instrumentExtensions = new HashSet<>();
		noteEvents = new ArrayList<>();
		notesInUse = new TreeSet<>();
		List<NoteEvent>[] notesOn = new List[16];
		int notesNotTurnedOff = 0;

		int minVelocity = Integer.MAX_VALUE;
		int maxVelocity = Integer.MIN_VALUE;
		
		
		int[] pitchBend = new int[16];
		for (int j = 0, sz = track.size(); j < sz; j++)
		{
			MidiEvent evt = track.get(j);
			MidiMessage msg = evt.getMessage();
			
			if (msg instanceof ShortMessage)
			{
				ShortMessage m = (ShortMessage) msg;
				int cmd = m.getCommand();
				int c = m.getChannel();
				
				
				/*if (isXGDrumTrack || isGSDrumTrack) {
					//
				} else if (noteEvents.isEmpty() && cmd == ShortMessage.NOTE_ON)
					isDrumTrack = (c == DRUM_CHANNEL);
				else if (isDrumTrack != (c == DRUM_CHANNEL) && cmd == ShortMessage.NOTE_ON)
					System.err.println("Track "+trackNumber+" contains both notes and drums.."+(name!=null?name:""));
				*/
				if (notesOn[c] == null)
					notesOn[c] = new ArrayList<>();

				long tick = evt.getTick();
				if (cmd == ShortMessage.NOTE_ON || cmd == ShortMessage.NOTE_OFF)
				{
					int noteId = m.getData1() + (isDrumTrack ? 0 : pitchBend[c]);
					int velocity = m.getData2() * sequenceCache.getVolume(c, tick) / DEFAULT_CHANNEL_VOLUME;
					if (velocity > 127)
						velocity = 127;
					
					/*if (trackNumber == 2) {
						System.err.println();
						System.err.println("Tick: "+evt.getTick());
						System.err.println(cmd==ShortMessage.NOTE_ON?"NOTE ON":(cmd==ShortMessage.NOTE_OFF?"NOTE OFF":cmd));
						System.err.println("Channel: "+c);
						System.err.println("Velocity: "+velocity);
						System.err.println("Pitch: "+noteId);
					}*/
					
					// If this Note ON was preceded by a similar Note ON without a Note OFF, lets turn it off
					// If its a Note OFF or Note ON with zero velocity, lets do same.
					Iterator<NoteEvent> iter = notesOn[c].iterator();
					while (iter.hasNext())
					{
						NoteEvent ne = iter.next();
						if (ne.note.id == noteId)
						{
							iter.remove();
							ne.setEndTick(tick);
							break;
						}
					}
					
					if (cmd == ShortMessage.NOTE_ON && velocity > 0)
					{
						Note note = Note.fromId(noteId);
						if (note == null)
						{
							continue; // Note was probably bent out of range. Not great, but not a reason to fail.
						}

						NoteEvent ne = new NoteEvent(note, velocity, tick, tick, sequenceCache);

						Iterator<NoteEvent> onIter = notesOn[c].iterator();
						while (onIter.hasNext())
						{
							NoteEvent on = onIter.next();
							if (on.note.id == ne.note.id)
							{
								onIter.remove();
								noteEvents.remove(on);
								notesNotTurnedOff++;
								break;
							}
						}

						if (velocity > maxVelocity)
							maxVelocity = velocity;
						if (velocity < minVelocity)
							minVelocity = velocity;

						if (!isDrumTrack)
						{
							instruments.add(sequenceCache.getInstrument(portMap.get(trackNumber), c, tick));
							instrumentExtensions.add(sequenceCache.getInstrumentExt(c, tick, isDrumTrack));
						} else if (isXGDrumTrack || isGSDrumTrack || isGM2DrumTrack) {
							String ins = sequenceCache.getInstrumentExt(c, tick, isDrumTrack);
							if (ins != null) {
								instrumentExtensions.add(ins);
							} else {
								instrumentExtensions.add(isXGDrumTrack?"XG Drum Kit":(isGM2DrumTrack?"GM2 Drum Kit":"GS Drum Kit"));
							}
						} else {
							String ins = sequenceCache.getInstrumentExt(c, tick, isDrumTrack);
							if (ins != null) {
								instrumentExtensions.add(ins);
							} else {
								instrumentExtensions.add("Standard Drum Kit");
							}
						}
						noteEvents.add(ne);
						notesInUse.add(ne.note.id);
						notesOn[c].add(ne);
					}
				}
				else if (cmd == ShortMessage.PITCH_BEND && !isDrumTrack)
				{
					double pct = 2 * (((m.getData1() | (m.getData2() << 7)) / (double) (1 << 14)) - 0.5);
					int bend = (int) Math.round(pct * sequenceCache.getPitchBendRange(m.getChannel(), tick));

					if (bend != pitchBend[c])
					{
						List<NoteEvent> bentNotes = new ArrayList<>();
						for (NoteEvent ne : notesOn[c])
						{
							ne.setEndTick(tick);
							long bendTick = tick;
							if (ne.getLengthMicros() < TimingInfo.getShortestNoteMicros(125))
							{
								// If the note is too short, just skip it. The new (bent) note will 
								// replace it, so start the bent note at the same time this one started.
								noteEvents.remove(ne);
								bendTick = ne.getStartTick();
							}

							Note bn = Note.fromId(ne.note.id + bend - pitchBend[c]);
							// If bn is null, the note was bent out of the 0-127 range. 
							// Not much we can do except skip it.
							if (bn != null)
							{
								NoteEvent bne = new NoteEvent(bn, ne.velocity, bendTick, bendTick, sequenceCache);
								noteEvents.add(bne);
								bentNotes.add(bne);
							}
						}
						notesOn[c] = bentNotes;
						pitchBend[c] = bend;
					}
				}
			}
			else if (msg instanceof MetaMessage)
			{
				MetaMessage m = (MetaMessage) msg;
				int type = m.getType();

				if (type == META_TRACK_NAME && name == null)
				{
					byte[] data = m.getData();// Text that starts with any of these indicate charset: "@LATIN", "@JP", "@UTF-16LE", or "@UTF-16BE"
					String tmp = new String(data, 0, data.length, StandardCharsets.US_ASCII).trim();//"UTF-8"
					if (tmp.length() > 0 && !tmp.equalsIgnoreCase("untitled")
							&& !tmp.equalsIgnoreCase("WinJammer Demo")) {
						//System.out.println("Starts with @ "+data[0]+" "+(data[0] & 0xFF));

						/*String pattern = "\u000B";// Vertical tab in unicode
						Pattern r = Pattern.compile(pattern);
						Matcher match = r.matcher(tmp);
						tmp = match.replaceAll(" ");*/

						name = tmp;
					}
				}
				else if (type == META_KEY_SIGNATURE && keySignature == null)
				{
					keySignature = new KeySignature(m);
				}
				else if (type == META_TIME_SIGNATURE && timeSignature == null)
				{
					timeSignature = new TimeSignature(m);
				}
			}
		}

		// Turn off notes that are on at the end of the song.  This shouldn't happen...
		int ctNotesOn = 0;
		for (List<NoteEvent> notesOnChannel : notesOn)
		{
			if (notesOnChannel != null)
				ctNotesOn += notesOnChannel.size();
		}
		if (ctNotesOn > 0)
		{
			System.err.println((ctNotesOn + notesNotTurnedOff) + " note(s) not turned off at the end of the track.");

			for (List<NoteEvent> notesOnChannel : notesOn)
			{
				if (notesOnChannel != null)
					noteEvents.removeAll(notesOnChannel);
			}
		}

		if (minVelocity == Integer.MAX_VALUE)
			minVelocity = 0;
		if (maxVelocity == Integer.MIN_VALUE)
			maxVelocity = MidiConstants.MAX_VOLUME;

		this.minVelocity = minVelocity;
		this.maxVelocity = maxVelocity;

		noteEvents = Collections.unmodifiableList(noteEvents);
		notesInUse = Collections.unmodifiableSortedSet(notesInUse);
		instruments = Collections.unmodifiableSet(instruments);
	}

	public TrackInfo(SequenceInfo parent, int trackNumber, String name, LotroInstrument instrument,
			TimeSignature timeSignature, KeySignature keySignature, List<NoteEvent> noteEvents)
	{
		this.sequenceInfo = parent;
		this.trackNumber = trackNumber;
		this.name = name;
		this.timeSignature = timeSignature;
		this.keySignature = keySignature;
		this.instruments = new HashSet<>();
		this.instruments.add(instrument.midi.id());
		this.noteEvents = noteEvents;
		this.notesInUse = new TreeSet<>();

		int minVelocity = Integer.MAX_VALUE;
		int maxVelocity = Integer.MIN_VALUE;
		for (NoteEvent ne : noteEvents)
		{
			this.notesInUse.add(ne.note.id);

			if (ne.velocity > maxVelocity)
				maxVelocity = ne.velocity;
			if (ne.velocity < minVelocity)
				minVelocity = ne.velocity;
		}
		if (minVelocity == Integer.MAX_VALUE)
			minVelocity = 0;
		if (maxVelocity == Integer.MIN_VALUE)
			maxVelocity = MidiConstants.MAX_VOLUME;

		this.minVelocity = minVelocity;
		this.maxVelocity = maxVelocity;

		this.isDrumTrack = false;

		this.noteEvents = Collections.unmodifiableList(this.noteEvents);
		this.notesInUse = Collections.unmodifiableSortedSet(this.notesInUse);
		this.instruments = Collections.unmodifiableSet(this.instruments);
	}

	public SequenceInfo getSequenceInfo()
	{
		return sequenceInfo;
	}

	public int getTrackNumber()
	{
		return trackNumber;
	}

	public boolean hasName()
	{
		return name != null;
	}

	public String getName()
	{
		if (name == null)
			return "Track " + trackNumber;
		return name;
	}

	public KeySignature getKeySignature()
	{
		return keySignature;
	}

	public TimeSignature getTimeSignature()
	{
		return timeSignature;
	}

	@Override public String toString()
	{
		return getName();
	}

	public boolean isDrumTrack()
	{
		return isDrumTrack;
	}

	/** Gets an unmodifiable list of the note events in this track. */
	public List<NoteEvent> getEvents()
	{
		return noteEvents;
	}

	public boolean hasEvents()
	{
		return !noteEvents.isEmpty();
	}

	public SortedSet<Integer> getNotesInUse()
	{
		return notesInUse;
	}

	public int getEventCount()
	{
		return noteEvents.size();
	}

	public String getEventCountString()
	{
		if (getEventCount() == 1)
		{
			return "1 note";
		}
		return getEventCount() + " notes";
	}

	public String getInstrumentNames()
	{
		if (isDrumTrack) {
						
			StringBuilder names = new StringBuilder();
			boolean first = true;
			
			for (String i : instrumentExtensions)
			{
				if (i == null) break;
				if (!first)
					names.append(", ");
				else
					first = false;

				names.append(i);
			}
			if (first || (names.length() == 0)) return isXGDrumTrack?"XG Drum Kit":(isGM2DrumTrack?"GM2 Drum Kit":(isGSDrumTrack?"GS Drum Kit":"Standard Drum Kit"));
			
			return names.toString();
		}
		
		if (instruments.size() == 0)
		{
			if (hasEvents())
				return MidiInstrument.PIANO.name;
			else
				return "<None>";
		}

		StringBuilder names = new StringBuilder();
		boolean first = true;
		
		if (!isGM()) {// Due to Maestro only supporting port assignments for GM, we make sure to use the GM instr. names for GM. 
			for (String i : instrumentExtensions)
			{
				if (i == null) break;
				if (!first)
					names.append(", ");
				else
					first = false;
	
				names.append(i);
			}
		}
		if (names.length() == 0) {
			first = true;		
			for (int i : instruments)
			{
				if (!first)
					names.append(", ");
				else
					first = false;
	
				names.append(MidiInstrument.fromId(i).name);
			}
		}

		return names.toString();
	}
	
	private boolean isGM() {
		return sequenceInfo.getDataCache().isGM();
	}

	public int getInstrumentCount()
	{
		return instruments.size();
	}

	public Set<Integer> getInstruments()
	{
		return instruments;
	}

	public int getMinVelocity()
	{
		return minVelocity;
	}

	public int getMaxVelocity()
	{
		return maxVelocity;
	}
}