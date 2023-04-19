package com.digero.common;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.*;

import com.digero.maestro.abc.AbcPart;

public class Preview implements LineListener, Runnable {
	
	static AudioInputStream audioStream36 = null;
	static AudioInputStream audioStream38 = null;
	static Clip audioClip36 = null;
	static Clip audioClip38 = null;
	static FloatControl gainControl36;
	static FloatControl gainControl38;
	int fadeTime = 200;
	static Preview instance = new Preview();

    boolean isPlaybackCompleted;
    
    @Override
    public void update(LineEvent event) {
        if (LineEvent.Type.START == event.getType()) {
            System.out.println("Playback started.");
        } else if (LineEvent.Type.STOP == event.getType()) {
            isPlaybackCompleted = true;
            System.out.println("Playback completed.");
        }
    }
    
    public static void control() {
    	String audioFilePath36 = "D:/Users/Nikolai/Documents/Dev/git/maestro/audio/LotroInstruments/bagpipe_36.wav";
    	String audioFilePath38 = "D:/Users/Nikolai/Documents/Dev/git/maestro/audio/LotroInstruments/bagpipe_38.wav";
    	
    	try {
    		InputStream inputStream = new BufferedInputStream(new FileInputStream(audioFilePath36));
	    	audioStream36 = AudioSystem.getAudioInputStream(inputStream);
	    	
	    	inputStream = new BufferedInputStream(new FileInputStream(audioFilePath38));
	    	audioStream38 = AudioSystem.getAudioInputStream(inputStream);
	    	
	    	//AudioFormat audioFormat = audioStream.getFormat();
	    	//DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
	    	//audioClip = (Clip) AudioSystem.getLine(info);
	    	
	    	/*
	    	int bufferSize = 1024;
	    	AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
	    	    AudioSystem.NOT_SPECIFIED, 16, 2, 4, AudioSystem.NOT_SPECIFIED, true);
	    	DataLine.Info info = new DataLine.Info(Clip.class, format, bufferSize);
	    	Clip clip = (Clip) AudioSystem.getLine(info)
	    	*/
	    	
	    	audioClip36 = AudioSystem.getClip();
	    	audioClip36.addLineListener(instance);
	    	audioClip38 = AudioSystem.getClip();
	    	audioClip38.addLineListener(instance);
	    		    	
	    	//audioClip.start();
	    	
	    	//audioClip.close();
	    	//audioStream.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (UnsupportedAudioFileException e) {
    		e.printStackTrace();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
    	
    	startFade();
    }
    
    
    static boolean fading = false;
    static float targetDB = -80.0f;
    float fadePerStep = 0.05f;
    
    
    public static void startFade() {
    	//System.out.println("Master Gain = "+gainControl.getValue());
        Thread t = new Thread(instance);
        t.start();
        //value = (value<=0.0)? 0.0001 : ((value>1.0)? 1.0 : value);
        //targetDB = (float)(Math.log(value)/Math.log(10.0)*20.0);
    }
    
    public void run()
    {	
    	try {
	    	audioClip36.open(audioStream36);
	    	gainControl36 = (FloatControl) audioClip36.getControl(FloatControl.Type.MASTER_GAIN);
	    	audioClip36.loop(Clip.LOOP_CONTINUOUSLY);
	    	fade(audioClip36, 3000000, gainControl36);
	        audioClip36.stop();
	        
	        
	    	audioClip38.open(audioStream38);
	    	gainControl38 = (FloatControl) audioClip38.getControl(FloatControl.Type.MASTER_GAIN);
	    	audioClip38.loop(Clip.LOOP_CONTINUOUSLY);
	    	fade(audioClip38, 2000000, gainControl38);
	        audioClip38.stop();
	        audioClip38.close();
	        
	        audioClip36.setFramePosition(0);
	    	audioClip36.loop(Clip.LOOP_CONTINUOUSLY);
	    	fade(audioClip36, 2000000, gainControl36);
	        audioClip36.stop();
	        audioClip36.close();
    	} catch (IOException e) {
    		e.printStackTrace();
    	} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
    }
    
    public void fade(Clip audioClip, long startFade, FloatControl gainControl) {
    	while (audioClip.getMicrosecondPosition() < startFade) {
    		try {Thread.sleep(10);} catch (Exception e) {}
    	}
        fading = true;
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
        fading = false;
    }
    
    public static void err(Exception e, Clip clip, InputStream stream) {
    	e.printStackTrace();
    	if (clip != null) clip.close();
    	if (stream != null) {
			try {
				stream.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
    	}
    }
    
    public static void play (AbcPart part) {
    	
    }
}