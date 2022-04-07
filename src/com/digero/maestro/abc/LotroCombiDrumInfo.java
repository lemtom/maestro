package com.digero.maestro.abc;

import java.util.HashMap;
import java.util.Map;

import com.digero.common.midi.Note;
import com.digero.maestro.midi.NoteEvent;

public class LotroCombiDrumInfo {
	
	public static Note maxCombi = Note.Ds5;
	public static Map<Note, Note> firstNotes = new HashMap<Note, Note>();
	public static Map<Note, Note> secondNotes = new HashMap<Note, Note>();
	public static int combiNoteCount = 0;
		
	static {
		// When adding here, also add to LotroDrumInfo
		
		// Added Rock Kick Drum (Jersiel)
		firstNotes.put(Note.Cs5, Note.As3);
		secondNotes.put(Note.Cs5, Note.D3);
		
		// Added Rock Snare Drum (Jersiel)
		firstNotes.put(Note.D5, Note.E3);
		secondNotes.put(Note.D5, Note.C5);
		
		// Added Rock Crash Cymbal (Jersiel)
		firstNotes.put(Note.Ds5, Note.A3);
		secondNotes.put(Note.Ds5, Note.Cs3);
		
		// Added Pop Kick Drum (Aifel)
		//firstNotes.put(Note.E5, Note.C3);
		//secondNotes.put(Note.E5, Note.Gs3);
						
		combiNoteCount = firstNotes.size();
			
		// TODO:
		//
		//   Solo is silent, but can live with that
	}
	
	public static NoteEvent getId1 (NoteEvent ne, Note extraNote) {
		Note n1 = firstNotes.get(extraNote);
		NoteEvent newNote = new NoteEvent(n1, ne.velocity, ne.getStartTick(), ne.getEndTick(), ne.getTempoCache());
		newNote.alreadyMapped = true;
		return newNote;
	}
	
	public static NoteEvent getId2 (NoteEvent ne, Note extraNote) {
		Note n2 = secondNotes.get(extraNote);
		NoteEvent newNote = new NoteEvent(n2, ne.velocity, ne.getStartTick(), ne.getEndTick(), ne.getTempoCache());
		newNote.alreadyMapped = true;
		return newNote;
	}
}
