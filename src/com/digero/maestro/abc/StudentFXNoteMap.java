package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.midi.MidiConstants;
import com.digero.common.util.ParseException;
import com.digero.common.util.Version;
import com.digero.maestro.util.SaveUtil;
import com.digero.maestro.util.XmlUtil;

public class StudentFXNoteMap extends DrumNoteMap
{
	public static final String FILE_SUFFIX = "studentfxmap.txt";
	protected static final byte DISABLED_NOTE_ID = (byte) LotroStudentFXInfo.DISABLED.note.id;
	private static final String MAP_PREFS_KEY = "StudentFXNoteMap.map";

	private byte[] map = null;
	private List<ChangeListener> listeners = null;

	@Override public String getXmlName () {
		return "fxMap";
	}
	
	public boolean isModified()
	{
		return map != null;
	}

	public byte get(int midiNoteId)
	{
		if (midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE)
		{
			throw new IllegalArgumentException();
		}
		return get((byte) midiNoteId);
	}

	public byte get(byte midiNoteId)
	{
		// Map hasn't been initialized yet, use defaults
		if (map == null)
			return getDefaultMapping(midiNoteId);

		return map[midiNoteId];
	}

	public void set(int midiNoteId, int value)
	{
		if ((midiNoteId < Byte.MIN_VALUE || midiNoteId > Byte.MAX_VALUE)
				|| (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE))
		{
			throw new IllegalArgumentException();
		}
		set((byte) midiNoteId, (byte) value);
	}

	public void set(byte midiNoteId, byte value)
	{
		if (get(midiNoteId) != value)
		{
			ensureMap();
			map[midiNoteId] = value;
			fireChangeEvent();
		}
	}

	protected byte getDefaultMapping(byte noteId)
	{
		return DISABLED_NOTE_ID;
	}


	private void ensureMap()
	{
		if (map == null)
			map = getFailsafeDefault();
	}
	public void addChangeListener(ChangeListener listener)
	{
		if (listeners == null)
			listeners = new ArrayList<ChangeListener>(2);

		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener)
	{
		if (listeners != null)
			listeners.remove(listener);
	}

	private void fireChangeEvent()
	{
		if (listeners != null)
		{
			ChangeEvent e = new ChangeEvent(this);
			for (ChangeListener l : listeners)
			{
				l.stateChanged(e);
			}
		}
	}

	@Override public void discard()
	{
		listeners = null;
	}

	@Override public boolean equals(Object obj)
	{
		if (obj == null || obj.getClass() != this.getClass())
			return false;

		return Arrays.equals(map, ((StudentFXNoteMap) obj).map);
	}

	@Override public int hashCode()
	{
		return Arrays.hashCode(map);
	}

	public void save(Preferences prefs)
	{
		ensureMap();
		prefs.putByteArray(MAP_PREFS_KEY, map);
	}

	public void load(Preferences prefs)
	{
		setLoadedByteArray(prefs.getByteArray(MAP_PREFS_KEY, null));
	}

	private void setLoadedByteArray(byte[] bytes)
	{
		if (bytes != null && bytes.length == MidiConstants.NOTE_COUNT)
		{
			map = bytes;
			byte[] failsafe = null;
			for (int i = 0; i < map.length; i++)
			{
				if (map[i] != DISABLED_NOTE_ID && !LotroInstrument.STUDENT_FX_FIDDLE.isPlayable(map[i]))
				{
					if (failsafe == null)
					{
						failsafe = getFailsafeDefault();
					}
					map[i] = failsafe[i];
				}
			}
		}
	}





	public void saveToXml(Element ele)
	{
		if (map == null) {
			return;
		}

		for (int midiId = 0; midiId < MidiConstants.NOTE_COUNT; midiId++)
		{
			int lotroId = get(midiId);
			if (lotroId == DISABLED_NOTE_ID)
				continue;

			Element noteEle = ele.getOwnerDocument().createElement("note");
			ele.appendChild(noteEle);
			noteEle.setAttribute("id", String.valueOf(midiId));
			noteEle.setAttribute("lotroId", String.valueOf(lotroId));
		}
	}


	public static StudentFXNoteMap loadFromXml(Element ele, Version fileVersion) throws ParseException
	{
		try
		{
			boolean isPassthrough = SaveUtil.parseValue(ele, "@isPassthrough", false);
			StudentFXNoteMap retVal = isPassthrough ? new PassThroughFXNoteMap() : new StudentFXNoteMap();
			retVal.loadFromXmlInternal(ele, fileVersion);
			return retVal;
		}
		catch (XPathExpressionException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void loadFromXmlInternal(Element ele, Version fileVersion) throws ParseException, XPathExpressionException
	{
		if (map == null)
			map = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(map, DISABLED_NOTE_ID);

		for (Element noteEle : XmlUtil.selectElements(ele, "note"))
		{
			int midiId = SaveUtil.parseValue(noteEle, "@id", DISABLED_NOTE_ID);
			byte lotroId = SaveUtil.parseValue(noteEle, "@lotroId", DISABLED_NOTE_ID);
			if (midiId >= 0 && midiId < map.length && LotroInstrument.STUDENT_FX_FIDDLE.isPlayable(lotroId)) {
				map[midiId] = lotroId;
			}
		}
	}

	/**
	 * This can be used as a backup in the event that loading the drum map from a file fails.
	 */
	public byte[] getFailsafeDefault()
	{
		byte[] failsafe = new byte[MidiConstants.NOTE_COUNT];

		Arrays.fill(failsafe, DISABLED_NOTE_ID);

		/*failsafe[26] = 49;
		failsafe[27] = 72;
		failsafe[28] = 70;
		// failsafe[29] = DISABLED_NOTE_ID;
		// failsafe[30] = DISABLED_NOTE_ID;
		failsafe[31] = 51;
		failsafe[32] = 50;
		failsafe[33] = 39;
		// failsafe[34] = DISABLED_NOTE_ID;
		failsafe[35] = 49;*/
		failsafe[36] = 36;
		failsafe[37] = 37;
		failsafe[38] = 38;/*
		failsafe[39] = 53;
		failsafe[40] = 54;
		failsafe[41] = 49;
		failsafe[42] = 37;
		failsafe[43] = 69;
		failsafe[44] = 59;
		failsafe[45] = 47;
		failsafe[46] = 60;
		failsafe[47] = 63;
		failsafe[48] = 43;
		failsafe[49] = 57;
		failsafe[50] = 45;
		failsafe[51] = 55;
		failsafe[52] = 57;
		failsafe[53] = 43;
		failsafe[54] = 46;
		failsafe[55] = 57;
		failsafe[56] = 45;
		failsafe[57] = 57;
		failsafe[58] = 53;
		failsafe[59] = 60;
		failsafe[60] = 38;
		failsafe[61] = 69;
		failsafe[62] = 39;
		failsafe[63] = 70;
		failsafe[64] = 48;
		failsafe[65] = 65;
		failsafe[66] = 64;
		failsafe[67] = 43;
		failsafe[68] = 47;
		failsafe[69] = 37;
		failsafe[70] = 42;
		// failsafe[71] = DISABLED_NOTE_ID;
		// failsafe[72] = DISABLED_NOTE_ID;
		failsafe[73] = 64;
		failsafe[74] = 62;
		failsafe[75] = 43;
		failsafe[76] = 51;
		failsafe[77] = 67;
		failsafe[78] = 65;
		failsafe[79] = 64;
		failsafe[80] = 43;
		failsafe[81] = 43;
		failsafe[82] = 42;
		failsafe[83] = 44;
		// failsafe[84] = DISABLED_NOTE_ID;
		failsafe[85] = 72;
		failsafe[86] = 48;
		failsafe[87] = 58;*/

		return failsafe;
	}
}
