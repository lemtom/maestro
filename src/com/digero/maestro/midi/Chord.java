/* Copyright (c) 2010 Ben Howell
 * This software is licensed under the MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING 
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */

package com.digero.maestro.midi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.digero.common.abc.AbcConstants;
import com.digero.common.abc.Dynamics;
import com.digero.common.midi.ITempoCache;
import com.digero.common.midi.Note;

public class Chord implements AbcConstants
{
	private ITempoCache tempoCache;
	private long startTick;
	private long endTick;
	private boolean hasTooManyNotes = false;
	private List<NoteEvent> notes = new ArrayList<NoteEvent>();
	private int highest = 0;
	private int lowest = 200; 
	private boolean pruned = false;

	public Chord(NoteEvent firstNote)
	{
		tempoCache = firstNote.getTempoCache();
		startTick = firstNote.getStartTick();
		endTick = firstNote.getEndTick();
		notes.add(firstNote);
		highest = firstNote.note.id;
		lowest = firstNote.note.id;
	}

	public long getStartTick()
	{
		return startTick;
	}

	public long getEndTick()
	{
		return endTick;
	}

	public long getStartMicros()
	{
		return tempoCache.tickToMicros(startTick);
	}

	public long getEndMicros()
	{
		return tempoCache.tickToMicros(endTick);
	}

	public int size()
	{
		return notes.size();
	}

	public boolean hasTooManyNotes()
	{
		return hasTooManyNotes;
	}

	public NoteEvent get(int i)
	{
		return notes.get(i);
	}

	public boolean add(NoteEvent ne, boolean force)
	{
		while (force && size() >= MAX_CHORD_NOTES)
		{
			remove(size() - 1);
			hasTooManyNotes = true;
		}
		return add(ne);
	}

	public boolean add(NoteEvent ne)
	{
		if (ne.getLengthTicks() == 0)
		{
			hasTooManyNotes = true;
			return false;
		}

		if (size() >= MAX_CHORD_NOTES)
		{
			return false;
		}

		notes.add(ne);
		if (ne.getEndTick() < endTick)
		{
			endTick = ne.getEndTick();
		}
		return true;
	}

	public NoteEvent remove(int i)
	{
		if (size() <= 1)
			return null;

		NoteEvent ne = notes.remove(i);
		if (ne.getEndTick() == endTick)
			recalcEndTick();

		return ne;
	}

	public boolean remove(NoteEvent ne)
	{
		return notes.remove(ne);
	}

	public Dynamics calcDynamics()
	{
		int velocity = Integer.MIN_VALUE;
		for (NoteEvent ne : notes)
		{
			if (ne.note != Note.REST && ne.tiesFrom == null && ne.velocity > velocity)
				velocity = ne.velocity;
		}

		if (velocity == Integer.MIN_VALUE)
			return null;

		return Dynamics.fromMidiVelocity(velocity);
	}

	public void recalcEndTick()
	{
		if (!notes.isEmpty())
		{
			endTick = notes.get(0).getEndTick();
			for (int k = 1; k < notes.size(); k++)
			{
				if (notes.get(k).getEndTick() < endTick)
				{
					endTick = notes.get(k).getEndTick();
				}
			}
		}
	}

	public void sort()
	{
		Collections.sort(notes);
	}

	public List<NoteEvent> prune() {
		// Determine which notes to prune to remain with a max of 6
		List<NoteEvent> deadNotes = new ArrayList<NoteEvent>();
		if (size() > MAX_CHORD_NOTES) {
			// Tied?      Keep these                       tiesFrom
			// Velocity?  Keep loudest!                    velocity
			// Pitch?     Keep highest, Keep lowest        note.id
			// Duration?  All things equal, keep lonbgest  getLengthTicks()
			List<NoteEvent> newNotes = new ArrayList<NoteEvent>();
			
			Comparator<NoteEvent> keepMe = new Comparator<NoteEvent>() {

				@Override
				public int compare(NoteEvent n1, NoteEvent n2) {
					
					List<Integer> removeFirst = new ArrayList<Integer>();
					
					removeFirst.add(highest-32);
					removeFirst.add(highest-24);
					removeFirst.add(highest-12);
					removeFirst.add(lowest+32);
					removeFirst.add(lowest+24);
					removeFirst.add(lowest+12);					
					
					// Keep tied notes, there can max be 6 of them anyway
					if (n1.tiesFrom != null) {
						return 1;
					}
					if (n2.tiesFrom != null) {
						return -1;
					}
					// return the note if its the highest in the chord
					if (highest == n1.note.id && n1.note.id != n2.note.id) {
						return 1;
					}
					if (highest == n2.note.id && n1.note.id != n2.note.id) {
						return -1;
					}
					// return the note if its the lowest in the chord
					if (lowest == n1.note.id && n1.note.id != n2.note.id) {
						return 1;
					}					
					if (lowest == n2.note.id && n1.note.id != n2.note.id) {
						return -1;
					}
					if (n1.velocity != n2.velocity) {
						// The notes differ in volume, return the loudest
						return n1.velocity - n2.velocity;
					}
					if (n1.note.id == n2.note.id) {
						// The notes have same pitch and same volume. Return the longest.
						return (int) (n1.getFullLengthTicks() - n2.getFullLengthTicks());
					}
										
					int index1 = removeFirst.indexOf(n1.note.id);
					int index2 = removeFirst.indexOf(n2.note.id);
					
					if (index1 != index2) {
						// Discard notes first that has octave spacing from highest or lowest notes
						return index2 - index1;
					}
										
					if (Math.abs(n1.note.id - n2.note.id) == 12 || Math.abs(n1.note.id - n2.note.id) == 24 || Math.abs(n1.note.id - n2.note.id) == 32) {
						// If has octave spacing, keep the highest.
						return n1.note.id - n2.note.id;
					}
					
					// discard the center-most note
					return Math.abs(n1.note.id - (lowest + (highest-lowest)/2))-Math.abs(n2.note.id - (lowest + (highest-lowest)/2));
					//1: n1 big -1: n2 big 0:equal
				}
			};
			notes.sort(keepMe);
			
			//System.err.print("Prune\n");
			
			for (int i = notes.size()-1; i >= 0; i--) {
				if (newNotes.size() < 6) {
					newNotes.add(notes.get(i));
					//System.err.print(" keep  " + notes.get(i).printout()+"\n");
				} else {
					deadNotes.add(notes.get(i));
					//System.err.print(" prune " + notes.get(i).printout()+"\n");
				}
			}
			notes = newNotes;
			recalcEndTick();
			pruned = true;
		}
		return deadNotes;
	}

	public boolean addAlways(NoteEvent ne) {
		if (ne.getLengthTicks() == 0)
		{
			hasTooManyNotes = true;
			return false;
		}
		if (pruned) {
			return false;
		}
		notes.add(ne);
		if (ne.note.id > highest) {
			highest = ne.note.id;
		}
		if (ne.note.id < lowest) {
			lowest = ne.note.id;
		}
		if (ne.getEndTick() < endTick)
		{
			endTick = ne.getEndTick();
		}
		return true;		
	}
}