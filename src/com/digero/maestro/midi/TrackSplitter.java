package com.digero.maestro.midi;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

import com.digero.common.midi.ExtensionMidiInstrument;
import com.digero.common.midi.MidiConstants;
import com.digero.common.midi.MidiFactory;
import com.digero.common.midi.MidiInstrument;

public class TrackSplitter {
	public static String standard = "GM";
	public static boolean hasPorts = false;
	private static boolean[] rolandDrumChannels = new boolean[16];//Which of the channels GS designates as drums
	private static boolean[] yamahaDrumChannels = new boolean[16];//Which of the channels XG designates as drums
	private static ArrayList<TreeMap<Long, Boolean>> yamahaDrumSwitches = null;//Which channel/tick XG switches to drums outside of designated drum channels
	private static ArrayList<TreeMap<Long, Boolean>> mmaDrumSwitches = null;//Which channel/tick GM2 switches to drums outside of designated drum channels
	private TreeMap<Integer, Integer> portMap = new TreeMap<>();
	private SequenceDataCache sequenceCache = null;
	private boolean isGM = true;
	
	public Sequence split (Sequence sequence, String fileName) throws InvalidMidiDataException {
		if (fileName.endsWith(".abc") || fileName.endsWith(".ABC") || fileName.endsWith(".txt") || fileName.endsWith(".TXT") || fileName.endsWith(".Abc") || fileName.endsWith(".Txt")) {
			// We do not expand ABC songs
			return sequence;
		}
		int resolution = sequence.getResolution();
		float divisionType = sequence.getDivisionType();
		Sequence expandedSequence = new Sequence(divisionType, resolution);

		determineStandard(sequence);
		isGM = standard.equals("GM");
		
		sequenceCache = new SequenceDataCache(sequence, standard, rolandDrumChannels, yamahaDrumSwitches, yamahaDrumChannels, mmaDrumSwitches, portMap);
		hasPorts = sequenceCache.hasPorts;
		
		Track[] oldTracks = sequence.getTracks();
		Track newMetaTrack = expandedSequence.createTrack();
		newMetaTrack.add(MidiFactory.createTrackNameEvent("META"));
		for (int j = 0; j < oldTracks.length; j++) {
			Track oldTrack = oldTracks[j];
			
			String oldTrackName = "";
			for (int i = 0; i < oldTrack.size(); i++) {
				String instr = "";
				MidiEvent evt = oldTrack.get(i);
				long tick = evt.getTick();
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
					}
				}
			}
			if (oldTrackName.equals("")) {
				oldTrackName = "Track "+j;
			}
			
			List<Integer> newTrackIndices = new ArrayList<Integer>();// List of all tracks that this old track is being expanded into
			HashMap<String, Track> newTracks = new HashMap<String, Track>();// ch:instr -> new track
			Track firstTrackUsingPorts = null;
			HashMap<Integer, String>[] notesOn = new HashMap[16];
			for (int i = 0; i < 16; i++) {
				notesOn[i] = new HashMap<Integer, String>();
			}
			int port = portMap.get(j);
			List<MidiEvent> portPrograms = new ArrayList();
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
							instr = fetchInstrName(tick, channel, port, j);
							if (instr != null && !"".equals(instr)) {
								notesOn[channel].put(note, instr);
							}
						} else if (!on) {
							instr = notesOn[channel].remove(note);
						} else {
							instr = notesOn[channel].remove(note);
							if (instr == null) {
								instr = fetchInstrName(tick, channel, port, j);
								if (instr != null && !"".equals(instr)) {
									notesOn[channel].put(note, instr);
								}
							}
						}
						//if (instr == null) System.out.println("instr==null "+on);
						//if ("".equals(instr)) System.out.println("instr=='' "+on);
					} else if (hasPorts && cmd == ShortMessage.PROGRAM_CHANGE) {
						portPrograms.add(evt);
						continue evtIter;
					}
					if (instr != null && !"".equals(instr)) {
						Track newTrack = newTracks.get(Integer.toString(channel)+instr);
						if (newTrack == null) {
							newTrack = expandedSequence.createTrack();
							if (firstTrackUsingPorts == null) firstTrackUsingPorts = newTrack;
							newTrack.add(MidiFactory.createTrackNameEvent(oldTrackName+" : "+trackCounter));
							if (hasPorts) {
								MidiEvent evtPort = MidiFactory.createPortEvent(port);
								if (evtPort != null) {
									newTrack.add(evtPort);
								} else {
									System.out.println("Failed to create GM+ port event when expanding midi");
									return null;
								}
							}
							trackCounter += 1;
							newTracks.put(Integer.toString(channel)+instr, newTrack);
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
				for (MidiEvent event : portPrograms) {
					firstTrackUsingPorts.add(event);// We add all program changes for this port to the first expanded track that uses this port.
				}
			} else {
				for (MidiEvent event : portPrograms) {
					newMetaTrack.add(event);
				}
			}
		}
		
		return expandedSequence;
	}

	private String fetchInstrName(long tick, int channel, int port, int track) {
		if (isGM) {
			int instrumentNumber = sequenceCache.getInstrument(port, channel, tick);
			String in = MidiInstrument.fromId(instrumentNumber).toString();
			if (in == null) {
				in = "Standard Drum Kit";
			}
			return in;
		} else {
			String in = sequenceCache.getInstrumentExt(channel, tick, sequenceCache.isXGDrumsTrack(track) || sequenceCache.isGSDrumsTrack(track) || sequenceCache.isDrumsTrack(track) || sequenceCache.isGM2DrumsTrack(track));
			if (in == null && (sequenceCache.isXGDrumsTrack(track) || sequenceCache.isGSDrumsTrack(track) || sequenceCache.isGM2DrumsTrack(track))) {
				in = sequenceCache.isXGDrumsTrack(track)?"XG Drum Kit":(sequenceCache.isGM2DrumsTrack(track)?"GM2 Drum Kit":"GS Drum Kit");
			}
			return in;
		}
	}

	public int getType(Sequence song)
	{
		if (song.getTracks().length == 1) {
			return 0;
		}
		return 1;
	}
	
	private void determineStandard (Sequence seq) {
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
		
		// sysex XG switch channel to/from drums:
		// F0,43,dv,md,08,ch,07,xx,F7 (dv = device ID, md = model id, ch = channel, xx = drum mode)
		
		// sysex XG drum part protect mode:
		// F0 43 dv md 00 00 07 pp F7 (dv = device ID, md = model id, pp = 0 is off, 1 is on)
		// If ON then only MSB 126/127 on chan #10. (unless by sysex bank change). XG Reset is counted as protect ON.
		// Ignoring this sysex as I have tested 130,000 midi files and none of them had this, so its super rare.
		
		// sysex XG bank change:
		// F0 43 dv md 08 nn 01 bb F7 (dv = device ID, md = model id, bb = MSB, nn = 0=non-chan#10 7F=chan#10) [However the real nn is just channel number]
		
		// sysex XG bank change:
		// F0 43 dv md 08 nn 02 bb F7 (dv = device ID, md = model id, bb = LSB, nn = default 0)
		
		// sysex XG program change:
		// F0 43 dv md 08 nn 03 pp F7 (dv = device ID, md = model id, pp = patch, nn = default 0)

		
		standard = "GM";
		
		for (int i = 0; i<16; i++) {
			rolandDrumChannels[i] = false;
		}		
		rolandDrumChannels[MidiConstants.DRUM_CHANNEL] = true;
		
		for (int i = 0; i<16; i++) {
			yamahaDrumChannels[i] = false;
		}		
		yamahaDrumChannels[MidiConstants.DRUM_CHANNEL] = true;
		
		Track[] tracks = seq.getTracks();
		long lastResetTick = -10000;
		TreeMap<Long, PatchEntry> bankAndPatchTrack = new TreeMap<>();//Maps cannot have duplicate entries, so using a PatchEntry class to store.
		
		//System.err.println("\nDetermineStandard:");
		
		/*
		 * 
		 * Iterate and find all Resets and assignments to rhythm channels.
		 * 
		 * 
		 */
		for (Track track : tracks) {
			for (int j = 0; j < track.size(); j++) {
				MidiEvent evt = track.get(j);
				MidiMessage msg = evt.getMessage();
				if (msg instanceof SysexMessage) {
					SysexMessage sysex = (SysexMessage) msg;
					byte[] message = sysex.getMessage();

					/*
					StringBuilder sb = new StringBuilder();
				    for (byte b : message) {
				        sb.append(String.format("%02X ", b));
				    }				    				    
				    System.err.println("SYSEX on track "+i+": "+sb.toString());
				    */

					// the "& 0xFF" is to convert to unsigned int from signed byte. 				    
					if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
							&& (message[4] & 0xFF) == 0x00 && (message[5] & 0xFF) == 0x00 && (message[6] & 0xFF) == 0x7E
							&& (message[7] & 0xFF) == 0x00 && (message[8] & 0xFF) == 0xF7) {
						if (standard != "GM" && standard != "XG") {
							System.err.println("Splitter: MIDI XG Reset in a " + standard + " file. This is unusual!");
						}
						if (evt.getTick() > lastResetTick) {
							lastResetTick = evt.getTick();
							standard = "XG";
						} else if (standard == "GS" && evt.getTick() == lastResetTick) {
							System.err.println("They are at same tick. Statistically bigger chance its a GS, so not switching to XG.");
						} else if (standard == "GM2" && evt.getTick() == lastResetTick) {
							System.err.println("They are at same tick. Statistically bigger chance its a XG, so switching to that.");
							lastResetTick = evt.getTick();
							standard = "XG";
						}
						ExtensionMidiInstrument.getInstance();
						//System.err.println("Yamaha XG Reset, tick "+evt.getTick());
					} else if (message.length == 11 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x41 && (message[3] & 0xFF) == 0x42
							&& (message[4] & 0xFF) == 0x12
							&& (message[5] & 0xFF) == 0x40 && (message[6] & 0xFF) == 0x00 && (message[7] & 0xFF) == 0x7F
							&& (message[8] & 0xFF) == 0x00 && (message[10] & 0xFF) == 0xF7) {
						if (standard != "GM" && standard != "GS") {
							System.err.println("Splitter: MIDI GS Reset in a " + standard + " file. This is unusual!");
						}
						if (evt.getTick() >= lastResetTick) {
							lastResetTick = evt.getTick();
							standard = "GS";
						}
						ExtensionMidiInstrument.getInstance();
						//System.err.println("Roland GS Reset, tick "+evt.getTick());
					} else if (message.length == 6 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x7E && (message[3] & 0xFF) == 0x09
							&& (message[4] & 0xFF) == 0x03 && (message[5] & 0xFF) == 0xF7) {
						if (standard != "GM" && standard != "GM2") {
							System.err.println("Splitter: MIDI GM2 Reset in a " + standard + " file. This is unusual!");
						}
						if (evt.getTick() > lastResetTick) {
							lastResetTick = evt.getTick();
							standard = "GM2";
						} else if (evt.getTick() == lastResetTick && standard != "GM") {
							System.err.println("They are at same tick. Statistically bigger chance its not a GM2, so not switching standard.");
						}
						ExtensionMidiInstrument.getInstance();
						//System.err.println("MIDI GM2 Reset, tick "+evt.getTick());
					} else if (message.length == 11 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x41 && (message[3] & 0xFF) == 0x42
							&& (message[4] & 0xFF) == 0x12 && (message[5] & 0xFF) == 0x40 && (message[7] & 0xFF) == 0x15
							&& (message[10] & 0xFF) == 0xF7) {
						boolean toDrums = message[8] == 1 || message[8] == 2;
						int channel = -1;
						if (message[6] == 16) {
							channel = MidiConstants.DRUM_CHANNEL;
						} else if (message[6] > 25 && message[6] < 32) {
							channel = message[6] - 16;
						} else if (message[6] > 16 && message[6] < 26) {
							channel = message[6] - 17;
						}
						if (channel != -1 && channel < 16) {
							if (toDrums) {
								//System.err.println("Roland GS sets channel "+(channel+1)+" to drums.");
							} else {
								//System.err.println("Roland GS unsets channel "+(channel+1)+" to drums.");
							}
							rolandDrumChannels[channel] = toDrums;
						}
					} else if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43 && (message[4] & 0xFF) == 0x08
							&& (message[6] & 0xFF) == 0x07 && (message[8] & 0xFF) == 0xF7) {
						String type = "Normal";
						if (message[5] < 16) {
							// From Tyros 1 data doc: part10=0x02, other parts=0x00. Korg EX-20 say this is channel. TODO: Drum Setup Reset sysex.
							// Sure looks like Korg has it correct, at least for pre Tyros XG standard.
							if (message[7] == 0) {
								type = "Normal";
								yamahaDrumChannels[message[5]] = false;
							} else if (message[7] == 1) {
								type = "Drums";
								yamahaDrumChannels[message[5]] = true;
							} else if (message[7] > 1 && message[7] <= 5) {
								type = "Drums Setup " + (message[7] - 1);
								yamahaDrumChannels[message[5]] = true;
							} else {
								type = "Invalid setup: " + message[7];
							}
							//System.err.println("Yamaha XG setting channel #"+message[5]+" to "+type);
						}
					} else if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
							&& (message[4] & 0xFF) == 0x00 && (message[5] & 0xFF) == 0x00 && (message[6] & 0xFF) == 0x07
							&& (message[8] & 0xFF) == 0xF7) {

						System.err.println("Splitter: Yamaha XG Drum Part Protect mode " + (message[7] == 0 ? "OFF" : "ON"));
					} else if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
							&& (message[4] & 0xFF) == 0x08 && (message[8] & 0xFF) == 0xF7) {
						// XG bank/patch change
						PatchEntry entry = null;
						entry = bankAndPatchTrack.get(evt.getTick());
						if (entry == null) {
							entry = new PatchEntry();
							entry.sysex.add(evt);
							bankAndPatchTrack.put(evt.getTick(), entry);
						} else {
							entry.sysex.add(evt);
						}
					}
				} else if (msg instanceof ShortMessage) {
					ShortMessage m = (ShortMessage) msg;
					int cmd = m.getCommand();

					if (cmd == ShortMessage.PROGRAM_CHANGE) {
						PatchEntry entry = null;
						entry = bankAndPatchTrack.get(evt.getTick());
						if (entry == null) {
							entry = new PatchEntry();
							entry.patch.add(evt);
							bankAndPatchTrack.put(evt.getTick(), entry);
						} else {
							entry.patch.add(evt);
						}
					} else if (cmd == ShortMessage.CONTROL_CHANGE) {
						PatchEntry entry = null;
						entry = bankAndPatchTrack.get(evt.getTick());
						if (entry == null) {
							entry = new PatchEntry();
							entry.bank.add(evt);
							bankAndPatchTrack.put(evt.getTick(), entry);
						} else {
							entry.bank.add(evt);
						}
					}
				}
			}
		}
		yamahaDrumSwitches = new ArrayList<>();
		for (int i = 0; i<16; i++) {
			yamahaDrumSwitches.add(new TreeMap<>());
		}
		mmaDrumSwitches = new ArrayList<>();
		for (int i = 0; i<16; i++) {
			mmaDrumSwitches.add(new TreeMap<>());
		}
		Integer[] yamahaBankAndPatchChanges = new Integer[16];
		Integer[] mmaBankAndPatchChanges = new Integer[16];
		for (int i = 0; i<16; i++) {
			if (yamahaDrumChannels[i]) {
				yamahaBankAndPatchChanges[i] = 2;
			} else {
				yamahaBankAndPatchChanges[i] = 0;
			}
			if (i == MidiConstants.DRUM_CHANNEL) {
				mmaBankAndPatchChanges[i] = 2;
			} else {
				mmaBankAndPatchChanges[i] = 0;
			}
		}
		
		/*
		 * Iterate again, but this time in order of ticks no matter which track the events come from.
		 * This time we find where there is changes from rhythm to chromatic voices and the other way around.
		 * We need that for determining how to seperate drum tracks and which tracks to mark as drum tracks.
		 * 
		 */
		
		for (PatchEntry entry : bankAndPatchTrack.values())
		{
			List<MidiEvent> masterList = new ArrayList<>();
			
			// The order here is important, patch must be last, since not all MIDI files adhere to standard of certain time separation between these events:
			// Not sure if sysex bank/patch change have higher priority than Control Change events. But giving it lowest priority for now.
			masterList.addAll(entry.sysex);
			masterList.addAll(entry.bank);
			masterList.addAll(entry.patch);
			
			for (MidiEvent evt : masterList) {
				MidiMessage msg = evt.getMessage();
				if (msg instanceof SysexMessage) {
					SysexMessage sysex = (SysexMessage) msg;
					byte[] message = sysex.getMessage();
					// we already know that this sysex is a XG bank/patch change, so no need for if statement.
				   	String bank = message[6]==1?"MSB":(message[6]==2?"LSB":(message[6]==3?"Patch":""));
			    	if (bank != "" && message[5] < 16 && message[5] > -1 && message[7] < 128 && message[7] > -1) {
			    		//System.err.println(fileName+": Yamaha XG Sysex "+bank+" set to "+message[7]+" for channel "+message[5]);
			    		int ch = message[5];
			    		if (bank == "MSB") {
				    		if (message[7] == 126 || message[7] == 127) {// 64 is chromatic effects, so not testing for that.
				    			yamahaBankAndPatchChanges[ch] = 1;
				    		} else {
								yamahaBankAndPatchChanges[ch] = 0;
							}
			    		} else if (bank == "Patch") {
			    			if (yamahaBankAndPatchChanges[ch] > 0) {
								yamahaBankAndPatchChanges[ch] = 2;
								yamahaDrumSwitches.get(ch).put(evt.getTick(), true);
								//System.err.println(" XG drums in channel "+(ch+1));
							} else if (yamahaBankAndPatchChanges[ch] == 0) {
								yamahaDrumSwitches.get(ch).put(evt.getTick(), false);
								//System.err.println(" channel "+(ch+1)+" changed voice in track "+i);
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
							yamahaDrumSwitches.get(ch).put(evt.getTick(), true);
							//if (ch == 9) System.err.println("XG channel "+ch+" changed to drum kit "+m.getData1()+" at tick "+evt.getTick());
						} else if (yamahaBankAndPatchChanges[ch] == 0) {
							yamahaDrumSwitches.get(ch).put(evt.getTick(), false);
							//if (ch == 9) System.err.println("XG channel "+ch+" changed to voice "+m.getData1()+" at tick "+evt.getTick());
						}
						if (mmaBankAndPatchChanges[ch] > 0) {
							mmaBankAndPatchChanges[ch] = 2;
							mmaDrumSwitches.get(ch).put(evt.getTick(), true);
							//System.err.println(" GM2 channel "+ch+" changed kit at tick "+evt.getTick());
						} else if (mmaBankAndPatchChanges[ch] == 0) {
							mmaDrumSwitches.get(ch).put(evt.getTick(), false);
							//System.err.println(" GM2 channel "+ch+" changed voice at tick "+evt.getTick());
						}
					}
					else if (cmd == ShortMessage.CONTROL_CHANGE)
					{
						switch (m.getData1()) {
							case MidiConstants.BANK_SELECT_MSB:
								if (m.getData2() == 127 || m.getData2() == 126) {// 64 is chromatic effects, so not testing for that.
									yamahaBankAndPatchChanges[ch] = 1;
								} else {
									yamahaBankAndPatchChanges[ch] = 0;
									//if (ch == 9) System.err.println(" channel "+ch+" changed to voice in track "+i+" to MSB "+m.getData2()+" at tick "+evt.getTick());
								}
								if (m.getData2() == 120) {
									mmaBankAndPatchChanges[ch] = 1;
								} else {
									mmaBankAndPatchChanges[ch] = 0;
								}
								//System.err.println("Channel "+ch+" bank select MSB "+m.getData2()+" at tick "+evt.getTick());
								break;
							case MidiConstants.BANK_SELECT_LSB:
								//System.err.println("Bank select LSB "+m.getData2());
								break;
						}
					}
				}
			}
		}
		for (int i = 0; i<16; i++) {
			yamahaDrumSwitches.get(i).put(-1l, yamahaDrumChannels[i]);
			if (i == MidiConstants.DRUM_CHANNEL) {
				mmaDrumSwitches.get(i).put(-1l, true);
			} else if (i != MidiConstants.DRUM_CHANNEL) {
				mmaDrumSwitches.get(i).put(-1l, false);
			}
		}		
	}
	
	private class PatchEntry {
		public List<MidiEvent> bank = new ArrayList<>();
		public List<MidiEvent> patch = new ArrayList<>();
		public List<MidiEvent> sysex = new ArrayList<>();
	}
}