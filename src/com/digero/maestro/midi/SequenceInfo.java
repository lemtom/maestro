package com.digero.maestro.midi;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.maestro.abc.AbcConversionException;
import com.digero.maestro.abc.AbcExporter;
import com.digero.maestro.abc.AbcExporter.ExportTrackInfo;
import com.digero.maestro.abc.AbcMetadataSource;
import com.sun.media.sound.MidiUtils;

/**
 * Container for a MIDI sequence. If necessary, converts type 0 MIDI files to type 1.
 */
public class SequenceInfo implements MidiConstants
{
	private final Sequence sequence;
	private final SequenceDataCache sequenceCache;
	private final String fileName;
	private String title;
	private String composer;
	public static String standard = "GM";
	private static boolean[] rolandDrumChannels = new boolean[16];
	private static ArrayList<TreeMap<Long, Boolean>> yamahaDrumChannels = new ArrayList<TreeMap<Long, Boolean>>();
	private int primaryTempoMPQ;
	private final List<TrackInfo> trackInfoList;

	public static SequenceInfo fromAbc(AbcToMidi.Params params) throws InvalidMidiDataException, ParseException
	{
		if (params.abcInfo == null)
			params.abcInfo = new AbcInfo();
		SequenceInfo sequenceInfo = new SequenceInfo(params.filesData.get(0).file.getName(), AbcToMidi.convert(params));
		sequenceInfo.title = params.abcInfo.getTitle();
		sequenceInfo.composer = params.abcInfo.getComposer();
		sequenceInfo.primaryTempoMPQ = (int) Math.round(MidiUtils.convertTempo(params.abcInfo.getPrimaryTempoBPM()));
		return sequenceInfo;
	}

	public static SequenceInfo fromMidi(File midiFile) throws InvalidMidiDataException, IOException, ParseException
	{
		return new SequenceInfo(midiFile.getName(), MidiSystem.getSequence(midiFile));
	}

	public static SequenceInfo fromAbcParts(AbcExporter abcExporter, boolean useLotroInstruments)
			throws InvalidMidiDataException, AbcConversionException
	{
		return new SequenceInfo(abcExporter, useLotroInstruments);
	}

	private SequenceInfo(String fileName, Sequence sequence) throws InvalidMidiDataException, ParseException
	{
		this.fileName = fileName;
		this.sequence = sequence;
		
		

		determineStandard(sequence, fileName);
		
		// Since the drum track separation is only applicable to type 1 midi sequences, 
		// do it before we convert this sequence to type 1, to avoid doing unnecessary work
		// Aifel: changed order so that XG drums in middle of a track from a type 0 gets seperated out
		boolean wasType0 = convertToType1(sequence);
		separateDrumTracks(sequence);
		fixupTrackLength(sequence);

		Track[] tracks = sequence.getTracks();
		if (tracks.length == 0)
		{
			throw new InvalidMidiDataException("The MIDI file doesn't have any tracks");
		}

		sequenceCache = new SequenceDataCache(sequence, standard, rolandDrumChannels, yamahaDrumChannels);
		primaryTempoMPQ = sequenceCache.getPrimaryTempoMPQ();

		List<TrackInfo> trackInfoList = new ArrayList<TrackInfo>(tracks.length);
		for (int i = 0; i < tracks.length; i++)
		{
			trackInfoList.add(new TrackInfo(this, tracks[i], i, sequenceCache, sequenceCache.isXGDrumsTrack(i), sequenceCache.isGSDrumsTrack(i), wasType0, sequenceCache.isDrumsTrack(i)));
		}

		composer = "";
		if (trackInfoList.get(0).hasName())
		{
			title = trackInfoList.get(0).getName();
		}
		else
		{
			title = fileName;
			int dot = title.lastIndexOf('.');
			if (dot > 0)
				title = title.substring(0, dot);
			title = title.replace('_', ' ');
		}

		this.trackInfoList = Collections.unmodifiableList(trackInfoList);
	}

	private SequenceInfo(AbcExporter abcExporter, boolean useLotroInstruments) throws InvalidMidiDataException,
			AbcConversionException
	{
		AbcMetadataSource metadata = abcExporter.getMetadataSource();
		this.fileName = metadata.getSongTitle() + ".abc";
		this.composer = metadata.getComposer();
		this.title = metadata.getSongTitle();

		Pair<List<ExportTrackInfo>, Sequence> result = abcExporter.exportToPreview(useLotroInstruments);

		sequence = result.second;
		sequenceCache = new SequenceDataCache(sequence, standard, null, null);
		primaryTempoMPQ = sequenceCache.getPrimaryTempoMPQ();

		List<TrackInfo> trackInfoList = new ArrayList<TrackInfo>(result.first.size());
		for (ExportTrackInfo i : result.first)
		{
			trackInfoList.add(new TrackInfo(this, i.trackNumber, i.part.getTitle(), i.part.getInstrument(), abcExporter
					.getTimingInfo().getMeter(), abcExporter.getKeySignature(), i.noteEvents));
		}

		this.trackInfoList = Collections.unmodifiableList(trackInfoList);
	}

	public String getFileName()
	{
		return fileName;
	}

	public Sequence getSequence()
	{
		return sequence;
	}

	public String getTitle()
	{
		return title;
	}

	public String getComposer()
	{
		return composer;
	}

	public int getTrackCount()
	{
		return trackInfoList.size();
	}

	public TrackInfo getTrackInfo(int track)
	{
		return trackInfoList.get(track);
	}

	public List<TrackInfo> getTrackList()
	{
		return trackInfoList;
	}

	public int getPrimaryTempoMPQ()
	{
		return primaryTempoMPQ;
	}

	public int getPrimaryTempoBPM()
	{
		return (int) Math.round(MidiUtils.convertTempo(getPrimaryTempoMPQ()));
	}

	public boolean hasTempoChanges()
	{
		return sequenceCache.getTempoEvents().size() > 1;
	}

	public KeySignature getKeySignature()
	{
		for (TrackInfo track : trackInfoList)
		{
			if (track.getKeySignature() != null)
				return track.getKeySignature();
		}
		return KeySignature.C_MAJOR;
	}

	public TimeSignature getTimeSignature()
	{
		for (TrackInfo track : trackInfoList)
		{
			if (track.getTimeSignature() != null)
				return track.getTimeSignature();
		}
		return TimeSignature.FOUR_FOUR;
	}

	public SequenceDataCache getDataCache()
	{
		return sequenceCache;
	}

	@Override public String toString()
	{
		return getTitle();
	}

	public long calcFirstNoteTick()
	{
		long firstNoteTick = Long.MAX_VALUE;
		for (Track t : sequence.getTracks())
		{
			for (int j = 0; j < t.size(); j++)
			{
				MidiEvent evt = t.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON)
					{
						if (evt.getTick() < firstNoteTick)
						{
							firstNoteTick = evt.getTick();
						}
						break;
					}
				}
			}
		}
		if (firstNoteTick == Long.MAX_VALUE)
			return 0;
		return firstNoteTick;
	}

	public long calcLastNoteTick()
	{
		long lastNoteTick = 0;
		for (Track t : sequence.getTracks())
		{
			for (int j = t.size() - 1; j >= 0; j--)
			{
				MidiEvent evt = t.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_OFF)
					{
						if (evt.getTick() > lastNoteTick)
						{
							lastNoteTick = evt.getTick();
						}
						break;
					}
				}
			}
		}

		return lastNoteTick;
	}

	@SuppressWarnings("unchecked")//
	public static void fixupTrackLength(Sequence song)
	{
//		System.out.println("Before: " + Util.formatDuration(song.getMicrosecondLength()));
//		TempoCache tempoCache = new TempoCache(song);
		Track[] tracks = song.getTracks();
		List<MidiEvent>[] suspectEvents = new List[tracks.length];
		long endTick = 0;

		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			for (int j = track.size() - 1; j >= 0; --j)
			{
				MidiEvent evt = track.get(j);
				if (MidiUtils.isMetaEndOfTrack(evt.getMessage()))
				{
					if (suspectEvents[i] == null)
						suspectEvents[i] = new ArrayList<MidiEvent>();
					suspectEvents[i].add(evt);
				}
				else if (evt.getTick() > endTick)
				{
					// Seems like some songs have extra meta messages way past the end
					if (evt.getMessage() instanceof MetaMessage)
					{
						if (suspectEvents[i] == null)
							suspectEvents[i] = new ArrayList<MidiEvent>();
						suspectEvents[i].add(0, evt);
					}
					else
					{
						endTick = evt.getTick();
						break;
					}
				}
			}
		}

		for (int i = 0; i < tracks.length; i++)
		{
			for (MidiEvent evt : suspectEvents[i])
			{
				if (evt.getTick() > endTick)
				{
					tracks[i].remove(evt);
//					System.out.println("Moving event from "
//							+ Util.formatDuration(MidiUtils.tick2microsecond(song, evt.getTick(), tempoCache)) + " to "
//							+ Util.formatDuration(MidiUtils.tick2microsecond(song, endTick, tempoCache)));
					evt.setTick(endTick);
					tracks[i].add(evt);
				}
			}
		}

//		System.out.println("Real song duration: "
//				+ Util.formatDuration(MidiUtils.tick2microsecond(song, endTick, tempoCache)));
//		System.out.println("After: " + Util.formatDuration(song.getMicrosecondLength()));
	}

	/**
	 * Separates the MIDI file to have one track per channel (Type 1).
	 */
	public static boolean convertToType1(Sequence song)
	{
		if (song.getTracks().length == 1)
		{
			Track track0 = song.getTracks()[0];
			Track[] tracks = new Track[CHANNEL_COUNT];

			int trackNumber = 1;
			int i = 0;
			while (i < track0.size())
			{
				MidiEvent evt = track0.get(i);
				if (evt.getMessage() instanceof ShortMessage)
				{
					int chan = ((ShortMessage) evt.getMessage()).getChannel();
					if (tracks[chan] == null)
					{
						tracks[chan] = song.createTrack();
						boolean GS = chan != 9 && standard == "GS";
						String trackName = ((rolandDrumChannels[chan] == true && standard == "GS") || (chan == 9 && standard != "GS")) ? (GS?"GS Drums":"Drums") : ("Track " + trackNumber);
						if (standard == "XG" && yamahaDrumChannels != null && yamahaDrumChannels.get(chan).floorEntry(evt.getTick()) != null && yamahaDrumChannels.get(chan).floorEntry(evt.getTick()).getValue() == true) {
							trackName = "XG Drums";
						}
						trackNumber++;
						tracks[chan].add(MidiFactory.createTrackNameEvent(trackName));
					}
					tracks[chan].add(evt);
					if (track0.remove(evt))
						continue;
				}
				i++;
			}
			return true;
		}
		return false;
	}

	private void determineStandard (Sequence seq, String fileName) {
		// sysex GM reset:  F0 7E dv 09 01 F7  (dv = device ID)
		// sysex GM2 reset: F0 7E dv 09 03 F7  (dv = device ID)
		// sysex Yamaha XG: F0 43 dv md 00 00 7E 00 F7 (dv = device ID, md = model id)
		// sysex Roland GS: F0 41 dv 42 12 40 00 7F 00 sm F7 (dv = device ID, sm = checksum)
		
		// sysex GS switch channel to/from drums:
		// [ F0 41 dv 42 12 40 1x 15 mm sm F7 ]
		//        x : 1 - 9 => 0 - 8 channel / 0 => 9 channel / A - F => 10 - 15 channel
		//        mm : 0 => normal part / 1,2 => set to drum track
		//        sm: checksum
		//        dv: device ID
		
		standard = "GM";
		
		for (int i = 0; i<16; i++) {
			rolandDrumChannels[i] = false;
		}		
		rolandDrumChannels[9] = true;
		
		yamahaDrumChannels = new ArrayList<TreeMap<Long, Boolean>>();
		for (int i = 0; i<16; i++) {
			yamahaDrumChannels.add(new TreeMap<Long, Boolean>());
		}
		
		Track[] tracks = seq.getTracks();
		Integer[] yamahaBankAndPatchChanges = new Integer[16];
		
		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			for (int k = 0; k<16; k++) {
				yamahaBankAndPatchChanges[k] = 0;
			}
			for (int j = 0; j < track.size(); j++)
			{
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof SysexMessage) {
					SysexMessage sysex = (SysexMessage) msg;
					byte message[] = sysex.getMessage();

					StringBuilder sb = new StringBuilder();
				    for (byte b : message) {
				        sb.append(String.format("%02X ", b));
				    }
				    
				    if (27 == sb.length() && sb.substring(0, 5).equals("F0 43") && sb.substring(12, 26).equals("00 00 7E 00 F7")) {
				    	standard = "XG";
				    	//System.err.println("Yamaha XG Reset, track "+i);
				    } else if (33 == sb.length() && sb.substring(0, 5).equals("F0 41") && sb.substring(9, 26).equals("42 12 40 00 7F 00") && sb.substring(30, 32).equals("F7")) {
				    	standard = "GS";
				    	//System.err.println("Roland GS Reset, track "+i);
				    } else if (sb.length() == 18 && sb.toString().startsWith("F0 7E") && sb.toString().endsWith("09 03 F7 ") && standard != "GS" && standard != "XG") {
				    	standard = "GM2";
				    	//System.err.println("MIDI GM2 Reset, track "+i);
				    } else if (17 <= sb.length() && sb.substring(0, 5).equals("F0 41") && sb.substring(9, 17).equals("42 12 40")) {
				    	if (message.length == 11 && message[7] == 21) {
				    		boolean toDrums = message[8] == 1 || message[8] == 2;
				    		int channel = -1;
				    		if (message[6] == 16) {
				    			channel = 9;
				    		} else if (message[6] > 25 && message[6] < 32) {
				    			channel = message[6]-16;
				    		} else if (message[6] > 16 && message[6] < 26) {
				    			channel = message[6]-17;
				    		}
				    		if (channel != -1) {
				    			if (toDrums) {
				    				//System.err.println("Roland GS sets channel "+(channel+1)+" to drums.");
				    			} else {
				    				//System.err.println("Roland GS unsets channel "+(channel+1)+" to drums.");
				    			}
				    			rolandDrumChannels[channel] = toDrums;
				    		}
				    	}
				    }
				} else if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();
					int ch = m.getChannel();
					
					if (cmd == ShortMessage.PROGRAM_CHANGE)
					{
						if (yamahaBankAndPatchChanges[ch] > 0) {
							yamahaBankAndPatchChanges[ch] = 2;
							yamahaDrumChannels.get(ch).put(evt.getTick(), true);
							//System.err.println(" XG drums in channel "+(ch+1));
						} else if (yamahaBankAndPatchChanges[ch] == 0) {
							yamahaDrumChannels.get(ch).put(evt.getTick(), false);
							//System.err.println(" channel "+(ch+1)+" changed voice in track "+i);
						}
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE)
					{
						switch (m.getData1()) {
							case BANK_SELECT_MSB:
								if (m.getData2() == 127 || m.getData2() == 126 || m.getData2() == 64) {
									yamahaBankAndPatchChanges[ch] = 1;
								} else {
									yamahaBankAndPatchChanges[ch] = 0;
								}
								//System.err.println("Bank select MSB "+m.getData2());
								break;
							case BANK_SELECT_LSB:
								//System.err.println("Bank select LSB "+m.getData2());
								break;
						}
					}
				}
			}
		}
		if (fileName.endsWith(".abc") || fileName.endsWith(".ABC") || fileName.endsWith(".txt") || fileName.endsWith(".TXT") || fileName.endsWith(".Abc") || fileName.endsWith(".Txt")) {
			standard = "ABC";
		}
	}
	
	/**
	 * Ensures that there are no tracks with both drums and notes.
	 */
	public static void separateDrumTracks(Sequence song)
	{
		Track[] tracks = song.getTracks();
		
		// This doesn't work on Type 0 MIDI files
		if (tracks.length <= 1) {
			//return;
		}		

		for (int i = 0; i < tracks.length; i++)
		{
			Track track = tracks[i];
			
			int GS = 0;
			int XG = 0;
			int drums = 0;
			int notes = 0;
			
			for (int j = 0; j < track.size(); j++)
			{
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage)
				{
					ShortMessage m = (ShortMessage) msg;
					if (m.getCommand() == ShortMessage.NOTE_ON)
					{
						if (m.getChannel() == DRUM_CHANNEL && rolandDrumChannels[DRUM_CHANNEL] == true)
						{
							drums = 1;
						}
						else if (standard == "GS" && rolandDrumChannels[m.getChannel()] == true)
						{
							GS = 1;
						}
						else if (standard == "XG" && yamahaDrumChannels != null && yamahaDrumChannels.get(m.getChannel()).floorEntry(evt.getTick()) != null && yamahaDrumChannels.get(m.getChannel()).floorEntry(evt.getTick()).getValue() == true)
						{
							XG = 1;
						}
						else
						{
							notes = 1;
						}
					}
				}
			}
			if (GS + XG + notes + drums > 1) {
				Track drumTrack = null;
				Track brandDrumTrack = null;
				if (drums == 1 && notes == 1) {
					drumTrack = song.createTrack();
					drumTrack.add(MidiFactory.createTrackNameEvent("Drums"));
				}
				if (XG == 1 || GS == 1) {
					brandDrumTrack = song.createTrack();
					if (XG == 1) {
						brandDrumTrack.add(MidiFactory.createTrackNameEvent("XG Drums"));
					} else {
						brandDrumTrack.add(MidiFactory.createTrackNameEvent("GS Drums"));
					}
				}
				// Mixed track: copy only the events on the drum channel
				for (int j = 0; j < track.size(); j++)
				{
					MidiEvent evt = track.get(j);
					MidiMessage msg = evt.getMessage();
					if (msg instanceof ShortMessage) {
						ShortMessage smsg = (ShortMessage) msg;
						if (drumTrack != null && rolandDrumChannels[smsg.getChannel()] && smsg.getChannel() == DRUM_CHANNEL)
						{
							drumTrack.add(evt);
							if (track.remove(evt))
								j--;
						} else if (brandDrumTrack != null && XG == 1 && yamahaDrumChannels != null && yamahaDrumChannels.get(smsg.getChannel()).floorEntry(evt.getTick()) != null && yamahaDrumChannels.get(smsg.getChannel()).floorEntry(evt.getTick()).getValue() == true) {
							brandDrumTrack.add(evt);
							if (track.remove(evt))
								j--;
						} else if (brandDrumTrack != null && GS == 1 && rolandDrumChannels[smsg.getChannel()] && smsg.getChannel() != DRUM_CHANNEL)
						{
							brandDrumTrack.add(evt);
							if (track.remove(evt))
								j--;
						}
					}
				}
			}
		}		
	}
}
