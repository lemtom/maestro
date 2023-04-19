package com.digero.common;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import kuusisto.*;
import kuusisto.tinysound.Sound;
import kuusisto.tinysound.TinySound;

import javax.sound.sampled.*;

import com.digero.maestro.abc.AbcPart;
import com.digero.maestro.midi.NoteEvent;

public class Preview3 implements LineListener, Runnable {
	
	static FloatControl gainControl38;
	int fadeTime = 200;
	static Preview3 instance = new Preview3();

    boolean isPlaybackCompleted;
    
    @Override
    public void update(LineEvent event) {
        if (LineEvent.Type.START == event.getType()) {
        } else if (LineEvent.Type.STOP == event.getType()) {
            isPlaybackCompleted = true;
        }
    }
    
    private Sound[] clips = null;
        
    public static void play (AbcPart part) {
    	instance.playAbcPart(part);
    }

	private void playAbcPart(AbcPart part) {
		if (notes == null) return;
		TinySound.init();
		setupClips();
		setupNotes(part);
		startPlayBack();
	}
	
	int min = 36;
	int max = 72;
	
	private void setupClips() {
		
		int numberOfSamples = max-min+1;
		
		clips = new Sound[numberOfSamples];
		
		for (int noteNumber = min; noteNumber <= max; noteNumber++) {
			String audioFilePath = "D:/Users/Nikolai/Documents/Dev/git/maestro/audio/LotroInstruments/lute_of_ages_"+noteNumber+".wav";
			
				Sound coin = TinySound.loadSound(new File(audioFilePath));
		    	clips[noteNumber-min] = coin;
	    	
		}
	}
	
	List<NoteEvent> notes = null;
	
	public static void setNotes(List<NoteEvent> noteEvents) {
		System.out.println("setNotes "+noteEvents.size());
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
						Sound myClip = clips[ne.note.id - min];
						myClip.stop();
						myClip.play();
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