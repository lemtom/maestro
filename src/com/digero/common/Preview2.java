package com.digero.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.sound.sampled.*;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.midi.NoteEvent;

public class Preview2 implements LineListener, Runnable {
	
	static FloatControl gainControl38;
	int fadeTime = 200;
	static Preview2 instance = new Preview2();

    boolean isPlaybackCompleted;
    
    @Override
    public void update(LineEvent event) {
        if (LineEvent.Type.START == event.getType()) {
            //System.out.println("Playback started.");
        } else if (LineEvent.Type.STOP == event.getType()) {
            isPlaybackCompleted = true;
            //System.out.println("Playback completed.");
        }
    }
    
    private Clip[] clips = null;
        
    public static void play (AbcPart part) {
    	//javax.sound.sampled.Mixer.Info[] infos = AudioSystem.getMixerInfo();
    	//for (javax.sound.sampled.Mixer.Info i : infos) System.out.println(i.getName());
    	instance.playAbcPart(part);
    }

	private void playAbcPart(AbcPart part) {
		if (notes == null) return;
		setupClips();
		setupNotes(part);
		startPlayBack();
	}
	
	int min = 36;
	int max = 72;
	
	private void setupClips() {
		
		int numberOfSamples = max-min+1;
		
		clips = new Clip[numberOfSamples];
		
		for (int noteNumber = min; noteNumber <= max; noteNumber++) {
			String audioFilePath = "D:/Users/Nikolai/Documents/Dev/git/maestro/audio/LotroInstruments/lute_of_ages_"+noteNumber+".wav";
			try {
	    		InputStream inputStream = new BufferedInputStream(new FileInputStream(audioFilePath));
	    		AudioInputStream audioStream = AudioSystem.getAudioInputStream(inputStream);
	    		AudioFormat audioFormat = audioStream.getFormat();
		    	DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, 2048);
		    	Clip audioClip = (Clip) AudioSystem.getLine(info);
		    	//Clip audioClip = AudioSystem.getClip();
		    	audioClip.open(audioStream);
		    	clips[noteNumber-min] = audioClip;
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	} catch (UnsupportedAudioFileException e) {
	    		e.printStackTrace();
			} catch (LineUnavailableException e) {
				e.printStackTrace();
			}
		}
	}
	
	List<NoteEvent> notes = null;
	
	public static void setNotes(List<NoteEvent> noteEvents) {
		System.out.println("setNotes2 "+noteEvents.size());
		instance.notes = noteEvents;
	}
	
	private void setupNotes(AbcPart part) {
		
	}
	
	private void startPlayBack() {
        Thread t = new Thread(this);
        //t.setPriority(8);
        t.start();
    }
	
    float targetDB = -80.0f;
    float fadePerStep = 0.05f;
   
    public void run()
    {	
		int notesSize = notes.size();
		int currNote = 0;
		long milliTime = 0;
		long endMilliTime = 0;
		for (NoteEvent n : notes) {
			long end = n.getEndMicros()/1000;
			if (end > endMilliTime) {
				endMilliTime = end;
			}
		}
		long startTime = System.currentTimeMillis();
		long delay = 10;
		int last = -1;
		while(milliTime < endMilliTime) {
			boolean tooBig = false;
			while (!tooBig && currNote < notesSize) {
				NoteEvent ne = notes.get(currNote);
				long startMicros = ne.getStartMicros();
				if (startMicros <= milliTime*1000) {
					if (ne.note.id - min >= 0 && ne.note.id - min < clips.length && ne.tiesFrom == null) {
						Clip myClip = clips[ne.note.id - min];
						myClip.stop();
						//myClip.flush();
						myClip.setFramePosition(0);						
						myClip.start();
						if (last + 1 != currNote) System.out.println("weird");
						last = currNote;
					} else {
						System.out.println("weirder");
					}
					currNote++;
				} else {
					tooBig = true;
				}
			}
			try {Thread.sleep(delay);} catch (Exception e) {}
			//delay = System.currentTimeMillis() - startTime - milliTime;
			//if (delay > 12) System.out.println(delay);
			milliTime = System.currentTimeMillis() - startTime;
			delay = 20;//Math.max(1, Math.min(10, 20 - delay));
		}
		System.out.println("Preview thread ended.");
    }
    
    public void fade(Clip audioClip, long startFade, FloatControl gainControl) {
    	while (audioClip.getMicrosecondPosition() < startFade) {
    		try {Thread.sleep(10);} catch (Exception e) {}
    	}
        float curr = 1f;
        float currDB = 0f;
        
        while (curr > 0.01f) {
        	curr -= fadePerStep;
        	currDB = targetDB;
        	if (curr > 0.01f) {
        		currDB = (float)Math.log10(curr)*20.0f;
        	}
        	System.out.println("Volume "+curr+" = "+currDB+" dB");
            gainControl.setValue(curr);
            try {Thread.sleep(10);} catch (Exception e) {}
        }
    }
}