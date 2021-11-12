/*package com.digero.common.midi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.midi.*;


public class sortReset {
	// Sorts MIDI files in specified folder. They will be copied into subfolders named after which resets they contain and which type they are. GM MIDIs is not copied.
	
	private static int count = 0;

	public static void main(String[] args) {
		if (args.length != 1) return;
		String sourceFolder = args[0];

		try {
			Path sf = Paths.get(sourceFolder);
			Path sfdotdot = sf.getParent();
			if (sfdotdot == null) {
				System.out.println("Folder argument cannot be harddisk root. As the sorted folders will appear in parent folder of specified folder.");
				return;
			}
			Files.find(sf, 999, (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().matches("(?i).*\\.(mid|midi|kar)$")).forEach(t -> {
				try {
					sort(t, sf);
				} catch (InvalidMidiDataException | IOException e) {
					//e.printStackTrace();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("\nCopied "+count+" MIDI files.");
	}
	
	private static void sort (Path path, Path sf) throws InvalidMidiDataException, IOException {
		File file = path.toFile();
		String folder = null;
		MidiFileFormat format = MidiSystem.getMidiFileFormat(file);
		Sequence seq = MidiSystem.getSequence(file);
		
		Track t = toSingleTrack(seq);
		if (t == null) {
			return;
		}
		folder = determineStandard(t,file.getName());
		if (folder.isEmpty()) {
			return;
		}
		folder += format.getType();
		String fullPath = file.getAbsolutePath();
		Path sfdotdot = sf.getParent();
		String newFullPath = sfdotdot.toString();
		newFullPath += "\\"+folder+"\\"+file.getName();
		if (newFullPath.toLowerCase().endsWith(".midi") || newFullPath.toLowerCase().endsWith(".kar")) {
			//Make sure all extensions is .mid or .MID for convenience.
			newFullPath = newFullPath+".mid";
		}
		System.out.println("\nCopy:");
		System.out.println(fullPath);
		System.out.println(newFullPath);
		
		Path newPath = Paths.get(newFullPath);
		Files.createDirectories(newPath.getParent());
		Files.copy(path, newPath);
		
		count++;
	}
	
	private static Track toSingleTrack (Sequence seq) {
		Track[] tracks = seq.getTracks();
		if (tracks.length == 0) {
			return null;
		}
		for (int i = 1; i < tracks.length; i++)
		{
			Track track = tracks[i];
			for (int j = 0; j < track.size(); j++)
			{
				MidiEvent evt = track.get(j);
				tracks[0].add(evt);
			}
		}
		return tracks[0];
	}
	
	private static String determineStandard (Track track, String fileName) {
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
		// //F0,43,dv,md,08,ch,07,xx,F7 (dv = device ID, md = model id, ch = channel, xx = drum mode)
		
		
		
		long lastResetTick = -1;
		long GS = -1;
		long XG = -1;
		long GM2 = -1;
		
		for (int j = 0; j < track.size(); j++)
		{
			MidiEvent evt = track.get(j);
			MidiMessage msg = evt.getMessage();
			if (msg instanceof SysexMessage) {
				SysexMessage sysex = (SysexMessage) msg;
				byte message[] = sysex.getMessage();

				
				// the "& 0xFF" is to convert to unsigned int from signed byte. 				    
			    if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
			    						&& (message[4] & 0xFF) == 0x00 && (message[5] & 0xFF) == 0x00 && (message[6] & 0xFF) == 0x7E
			    						&& (message[7] & 0xFF) == 0x00 && (message[8] & 0xFF) == 0xF7) {
			    	if (evt.getTick() >= lastResetTick) {
			    		lastResetTick = evt.getTick();
			    	}
		    		XG = evt.getTick();
			    } else if (message.length == 11 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x41 && (message[3] & 0xFF) == 0x42
			    								&& (message[4] & 0xFF) == 0x12
			    								&& (message[5] & 0xFF) == 0x40 && (message[6] & 0xFF) == 0x00 && (message[7] & 0xFF) == 0x7F
			    								&& (message[8] & 0xFF) == 0x00 && (message[10] & 0xFF) == 0xF7) {
			    	if (evt.getTick() >= lastResetTick) {
			    		lastResetTick = evt.getTick();
			    	}
			    	GS = evt.getTick();
			    } else if (message.length == 6 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x7E && (message[3] & 0xFF) == 0x09
			    							   && (message[4] & 0xFF) == 0x03 && (message[5] & 0xFF) == 0xF7) {
			    	if (evt.getTick() >= lastResetTick) {
			    		lastResetTick = evt.getTick();
			    	}
			    	GM2 = evt.getTick();
			    }// else if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
				//		&& (message[4] & 0xFF) == 0x00 && (message[5] & 0xFF) == 0x00 && (message[6] & 0xFF) == 0x07
				//		&& (message[8] & 0xFF) == 0xF7) {
			    //	
			    //	System.err.println(fileName+": Yamaha XG Drum Part Protect mode "+(message[7]==0?"OFF":"ON"));
			    //} else if (message.length == 9 && (message[0] & 0xFF) == 0xF0 && (message[1] & 0xFF) == 0x43
				//		&& ((message[4] & 0xFF) == 0x08 || (message[4] & 0xFF) == 0x10) && (message[8] & 0xFF) == 0xF7) {
			    //	String bank = message[6]==1?"MSB":(message[6]==2?"LSB":(message[6]==3?"Patch":""));
			    //	if (bank != "") {
			    //		System.err.println(fileName+": Yamaha XG Sysex "+bank+" set to "+message[7]+" for channel "+message[5]+". ("+message[4]+")");
			    //	}
			    //
			}
		}
		
		String standard = "";
		
		if (lastResetTick != -1) {
			if (GS == lastResetTick) {
				standard = "GS."+standard;
				GS = -1;
			}
			if (XG == lastResetTick) {
				standard = "XG."+standard;
				XG = -1;
			}
			if (GM2 == lastResetTick) {
				standard = "GM2."+standard;
				GM2 = -1;
			}
			
			if (GM2 == -1) {
				if (GS != -1 && GS > XG) {
					standard = "GS."+standard;
					GS = -1;
				}
				if (XG != -1 && XG > GS) {
					standard = "XG."+standard;
					XG = -1;
				}
			}
			if (XG == -1) {
				if (GS != -1 && GS > GM2) {
					standard = "GS."+standard;
					GS = -1;
				}
				if (GM2 != -1 && GM2 > GS) {
					standard = "GM2."+standard;
					GM2 = -1;
				}
			}
			if (GS == -1) {
				if (GM2 != -1 && GM2 > XG) {
					standard = "GM2."+standard;
					GM2 = -1;
				}
				if (XG != -1 && XG > GM2) {
					standard = "XG."+standard;
					XG = -1;
				}
			}
		}		
		return standard;
	}
}*/