package com.digero.maestro.midi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.MidiInstrument;

/**
 *  Takes a midi input and expands each instrument to its own track.
 *  Works with GM2, XG, GS, GM and GM+ 
 *  
 */
public class TrackSplitter {
	private SequenceDataCache sequenceCache = null;
	private boolean isGM = true;
	
	public Sequence split (Sequence sequence, SequenceDataCache sequenceCache, String standard, boolean[] rolandDrumChannels, ArrayList<TreeMap<Long, Boolean>> yamahaDrumSwitches, boolean[] yamahaDrumChannels, ArrayList<TreeMap<Long, Boolean>> mmaDrumSwitches, TreeMap<Integer, Integer> portMap) throws InvalidMidiDataException {

		this.sequenceCache = sequenceCache;
		
		int resolution = sequence.getResolution();
		float divisionType = sequence.getDivisionType();
		Sequence expandedSequence = new Sequence(divisionType, resolution);

		isGM = standard.equals("GM");
		boolean hasPorts = sequenceCache.hasPorts;
		
		Track[] oldTracks = sequence.getTracks();
		Track newMetaTrack = expandedSequence.createTrack();
		newMetaTrack.add(MidiFactory.createTrackNameEvent("META"));
		long lastEOTTick = 0L;
		for (int j = 0; j < oldTracks.length; j++) {
			Track oldTrack = oldTracks[j];
			
			// Find the old name and end of track for the track we want to expand
			String oldTrackName = "";
			MidiEvent oldEndOfTrack = null;
			for (int i = 0; i < oldTrack.size(); i++) {
				MidiEvent evt = oldTrack.get(i);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof MetaMessage) {
					MetaMessage meta = (MetaMessage) msg;
					int type = meta.getType();
					if (type == MidiConstants.META_TRACK_NAME) {
						byte[] data = meta.getData();// Text that starts with any of these indicate charset: "@LATIN", "@JP", "@UTF-16LE", or "@UTF-16BE"
						String tmp = new String(data, 0, data.length, StandardCharsets.US_ASCII).trim();
						if (tmp.length() > 0) {
							oldTrackName = tmp;
							break;
						}
					} else if (type == MidiConstants.META_END_OF_TRACK) {
						oldEndOfTrack = evt;
						if (evt.getTick() > lastEOTTick) {
							lastEOTTick = evt.getTick(); 
						}
					}
				}
			}
			if (oldTrackName.equals("")) {
				oldTrackName = "Track "+j;
			}
			
			// This hash map contains a map from instrument name into new track.
			// This instrument name is prepended with the channel if its a GM+ format midi.
			HashMap<String, Track> newTracks = new HashMap<>();
			
			// Making a list of which notes are playing, the note will map into an instrument, so that the Midi OFF event gets put on same track as its midi ON event.
			List<HashMap<Integer, String>> notesOn = new ArrayList<HashMap<Integer, String>>();
			for (int i = 0; i < 16; i++) {
				notesOn.add(new HashMap<>());
			}
			
			// GM+ stuff
			int port = portMap.get(j);
			List<MidiEvent> portPrograms = new ArrayList<>();// Program changes within specific port, they will later be put into 'firstTrackUsingPorts'
			Track firstTrackUsingPorts = null;// The first of the new expanded tracks, this will come to contain GM+ port program changes.
			
			// Iterate over all midi events in old track
			int trackCounter = 1;
			evtIter:for (int i = 0; i < oldTrack.size(); i++) {
				 
				String instr = "";
				MidiEvent evt = oldTrack.get(i);
				long tick = evt.getTick();
				MidiMessage msg = evt.getMessage();
				if (msg instanceof ShortMessage) {
					ShortMessage shortMsg = (ShortMessage) msg;
					int cmd = shortMsg.getCommand();
					int channel = shortMsg.getChannel();
					
					
					if (cmd == ShortMessage.NOTE_OFF || cmd == ShortMessage.NOTE_ON) {
						boolean on = cmd == ShortMessage.NOTE_ON;
						
						int note = shortMsg.getData1();
						int velocity = shortMsg.getData2();
												
						if (on && velocity > 0) {
							// This is a genuine midi ON event
							instr = fetchInstrName(tick, channel, port, j);
							if (instr != null && !"".equals(instr)) {
								notesOn.get(channel).put(note, instr);
							}
						} else if (!on) {
							// This is a genuine midi OFF event
							instr = notesOn.get(channel).remove(note);
						} else {
							// This is a midi ON event that might act as a midi OFF
							instr = notesOn.get(channel).remove(note);
							if (instr == null) {
								// Its not preceded by a midi ON, so we treat is as a midi ON although, its silent.
								// TODO: Consider to remove it, cause Maestro will assign +pppp+ to it,
								// and it will become audible which is probably not what the midi maker intended.
								instr = fetchInstrName(tick, channel, port, j);
								if (instr != null && !"".equals(instr)) {
									notesOn.get(channel).put(note, instr);
								}
							}
						}
						//if (instr == null) System.out.println("instr==null "+on);
						//if ("".equals(instr)) System.out.println("instr=='' "+on);
					} else if (hasPorts && cmd == ShortMessage.PROGRAM_CHANGE) {
						portPrograms.add(evt);
						continue evtIter;
					}
					
					// Lets put the midi event in its new track. If we determined its tied to an instrument,
					// then we place it in one of the new tracks, which each represent an instrument.
					// If not associated with an instrument, then it is put in track 0, where we keep all the meta, sysex, bank changes and normal program changes..
					if (instr != null && !"".equals(instr)) {
						String trackID = hasPorts?(channel +instr):instr;// If its not a Cakewalk midi then we lumps all of same instr together, regardless of channel.
						Track newTrack = newTracks.get(trackID);
						if (newTrack == null) {
							newTrack = expandedSequence.createTrack();
							if (firstTrackUsingPorts == null) firstTrackUsingPorts = newTrack;
							newTrack.add(MidiFactory.createTrackNameEvent(oldTrackName+" : "+trackCounter));
							newTrack.add(oldEndOfTrack);
							if (hasPorts) {
								// We put the GM+ port change in every one of the new tracks if the old had it.
								MidiEvent evtPort = MidiFactory.createPortEvent(port);
								if (evtPort != null) {
									newTrack.add(evtPort);
								} else {
									System.out.println("Failed to create GM+ port event when expanding midi");
									return null;
								}
							}
							trackCounter += 1;
							newTracks.put(trackID, newTrack);
						}
						newTrack.add(evt);
					} else {
						newMetaTrack.add(evt);
					}
				} else {
					newMetaTrack.add(evt);
				}
			}
			if (firstTrackUsingPorts != null) {
				// We add all program changes for this GM+ port to the first expanded track that uses this port.
				for (MidiEvent event : portPrograms) {
					firstTrackUsingPorts.add(event);
				}
			} else {
				// This should not be needed, something went wrong if this is executed.
				for (MidiEvent event : portPrograms) {
					newMetaTrack.add(event);
				}
			}
		}
		
		if (lastEOTTick > 0L) {
			newMetaTrack.add(MidiFactory.createEndOfTrackEvent(lastEOTTick));
		}		
		return expandedSequence;
	}

	private String fetchInstrName(long tick, int channel, int port, int track) {
		if (isGM) {
			if (channel == MidiConstants.DRUM_CHANNEL) return "Standard Drum Kit";
			int instrumentNumber = sequenceCache.getInstrument(port, channel, tick);
			String in = MidiInstrument.fromId(instrumentNumber).toString();
			return in;
		} else {
			String in = sequenceCache.getInstrumentExt(channel, tick, sequenceCache.isXGDrumsTrack(track) || sequenceCache.isGSDrumsTrack(track) || sequenceCache.isDrumsTrack(track) || sequenceCache.isGM2DrumsTrack(track));
			if (in == null && (sequenceCache.isXGDrumsTrack(track) || sequenceCache.isGSDrumsTrack(track) || sequenceCache.isGM2DrumsTrack(track))) {
				in = sequenceCache.isXGDrumsTrack(track)?"XG Drum Kit":(sequenceCache.isGM2DrumsTrack(track)?"GM2 Drum Kit":"GS Drum Kit");
			}
			return in;
		}
	}
}