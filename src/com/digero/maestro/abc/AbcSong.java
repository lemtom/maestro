package com.digero.maestro.abc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.DefaultListModel;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.digero.common.abc.LotroInstrument;
import com.digero.common.abc.StringCleaner;
import com.digero.common.abctomidi.AbcInfo;
import com.digero.common.abctomidi.AbcToMidi;
import com.digero.common.midi.KeySignature;
import com.digero.common.midi.TimeSignature;
import com.digero.common.util.ICompileConstants;
import com.digero.common.util.IDiscardable;
import com.digero.common.util.Listener;
import com.digero.common.util.ListenerList;
import com.digero.common.util.Pair;
import com.digero.common.util.ParseException;
import com.digero.common.util.Util;
import com.digero.common.util.Version;
import com.digero.maestro.MaestroMain;
import com.digero.maestro.abc.AbcPartEvent.AbcPartProperty;
import com.digero.maestro.abc.AbcSongEvent.AbcSongProperty;
import com.digero.maestro.midi.SequenceDataCache;
import com.digero.maestro.midi.SequenceInfo;
import com.digero.maestro.midi.TrackInfo;
import com.digero.maestro.util.FileResolver;
import com.digero.maestro.util.ListModelWrapper;
import com.digero.maestro.util.SaveUtil;
import com.digero.maestro.util.XmlUtil;

public class AbcSong implements IDiscardable, AbcMetadataSource
{
	public static final String MSX_FILE_DESCRIPTION = MaestroMain.APP_NAME + " Song";
	public static final String MSX_FILE_DESCRIPTION_PLURAL = MaestroMain.APP_NAME + " Songs";
	public static final String MSX_FILE_EXTENSION_NO_DOT = "msx";
	public static final String MSX_FILE_EXTENSION = "." + MSX_FILE_EXTENSION_NO_DOT;
	public static final Version SONG_FILE_VERSION = new Version(1, 0, 100);

	private String title = "";
	private String composer = "";
	private String transcriber = "";
	private String genre = "";
	private String mood = "";
	private String note = "";// not continuously updated
	private boolean badger = false;
	private boolean allOut = false;
	private float tempoFactor = 1.0f;
	private int transpose = 0;
	private KeySignature keySignature = KeySignature.C_MAJOR;
	private TimeSignature timeSignature = TimeSignature.FOUR_FOUR;
	private boolean tripletTiming = false;
	private boolean mixTiming = true;
	private boolean priorityActive = false;
	private boolean skipSilenceAtStart = true;
	//private boolean showPruned = false;
	public TreeMap<Integer, TuneLine> tuneBars = null;
	public boolean[] tuneBarsModified = null;

	private final boolean fromAbcFile;
	private final boolean fromXmlFile;
	private SequenceInfo sequenceInfo;
	private final PartAutoNumberer partAutoNumberer;
	private final PartNameTemplate partNameTemplate;
	private QuantizedTimingInfo timingInfo;
	private AbcExporter abcExporter;
	private File sourceFile; // The MIDI or ABC file that this song was loaded from
	private File exportFile; // The ABC export file
	private File saveFile; // The XML Maestro song file

	private final ListModelWrapper<AbcPart> parts = new ListModelWrapper<>(new DefaultListModel<>());

	private final ListenerList<AbcSongEvent> listeners = new ListenerList<>();
	boolean mixDirty = true;

	public AbcSong(File file, PartAutoNumberer partAutoNumberer, PartNameTemplate partNameTemplate,
			FileResolver fileResolver) throws IOException, InvalidMidiDataException, ParseException, SAXException
	{
		sourceFile = file;

		this.partAutoNumberer = partAutoNumberer;
		this.partAutoNumberer.setParts(Collections.unmodifiableList(parts));

		this.partNameTemplate = partNameTemplate;
		this.partNameTemplate.setMetadataSource(this);

		String fileName = file.getName().toLowerCase();
		fromXmlFile = fileName.endsWith(MSX_FILE_EXTENSION);
		fromAbcFile = fileName.endsWith(".abc") || fileName.endsWith(".txt");
		
		if (fromXmlFile)
			initFromXml(file, fileResolver);
		else if (fromAbcFile)
			initFromAbc(file);
		else
			initFromMidi(file);
	}

	@Override public void discard()
	{
		if (partAutoNumberer != null)
			partAutoNumberer.setParts(null);

		if (partNameTemplate != null)
			partNameTemplate.setMetadataSource(null);

		listeners.discard();

		for (AbcPart part : parts)
		{
			if (part != null)
				part.discard();
		}
		parts.clear();
		
		tuneBarsModified = null;
		tuneBars = null;
		
		/*if (sequenceInfo != null) {
			// Make life easier for Garbage Collector
			for (TrackInfo ti : sequenceInfo.getTrackList()) {
				for (NoteEvent ne : ti.getEvents()) {
					ne.resetAllPruned();
				}
			}
		}*/
	}

	private void initFromMidi(File file) throws IOException, InvalidMidiDataException, ParseException
	{
		sequenceInfo = SequenceInfo.fromMidi(file);
		title = sequenceInfo.getTitle();
		composer = sequenceInfo.getComposer();
		keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? sequenceInfo.getKeySignature() : KeySignature.C_MAJOR;
		timeSignature = sequenceInfo.getTimeSignature();
		note = "";
	}

	private void initFromAbc(File file) throws IOException, InvalidMidiDataException, ParseException
	{
		AbcInfo abcInfo = new AbcInfo();

		AbcToMidi.Params params = new AbcToMidi.Params(file);
		params.abcInfo = abcInfo;
		params.useLotroInstruments = false;
		//params.stereo = false;
		sequenceInfo = SequenceInfo.fromAbc(params);
		exportFile = file;

		title = sequenceInfo.getTitle();
		composer = sequenceInfo.getComposer();
		keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? abcInfo.getKeySignature() : KeySignature.C_MAJOR;
		timeSignature = abcInfo.getTimeSignature();

		int t = 0;
		for (TrackInfo trackInfo : sequenceInfo.getTrackList())
		{
			if (!trackInfo.hasEvents())
			{
				t++;
				continue;
			}

			AbcPart newPart = new AbcPart(this);

			newPart.setTitle(abcInfo.getPartName(t));
			newPart.setPartNumber(abcInfo.getPartNumber(t));
			newPart.setTrackEnabled(t, true);

			Set<Integer> midiInstruments = trackInfo.getInstruments();
			for (LotroInstrument lotroInst : LotroInstrument.values())
			{
				if (midiInstruments.contains(lotroInst.midi.id()))
				{
					newPart.setInstrument(lotroInst);
					break;
				}
			}

			int ins = Collections.binarySearch(parts, newPart, partNumberComparator);
			if (ins < 0)
				ins = -ins - 1;
			parts.add(ins, newPart);

			newPart.addAbcListener(abcPartListener);
			t++;
		}

		tripletTiming = abcInfo.hasTriplets();
		mixTiming = abcInfo.hasMixTimings();
		priorityActive = false;
		transcriber = abcInfo.getTranscriber();
		genre = abcInfo.getGenre();
		mood = abcInfo.getMood();
		note = "";
	}

	private void initFromXml(File file, FileResolver fileResolver) throws SAXException, IOException, ParseException
	{
		try
		{
			saveFile = file;
			Document doc = XmlUtil.openDocument(sourceFile);
			Element songEle = XmlUtil.selectSingleElement(doc, "song");
			if (songEle == null)
			{
				throw new ParseException("Does not appear to be a valid Maestro file. Missing <song> root element.",
						sourceFile.getName());
			}
			Version fileVersion = SaveUtil.parseValue(songEle, "@fileVersion", SONG_FILE_VERSION);

			sourceFile = SaveUtil.parseValue(songEle, "sourceFile", (File) null);
			if (sourceFile == null)
			{
				throw SaveUtil.missingValueException(songEle, "<sourceFile>");
			}
			File origSourceFile = sourceFile;

			exportFile = SaveUtil.parseValue(songEle, "exportFile", exportFile);

			sequenceInfo = null;
			while (sequenceInfo == null)
			{
				String name = sourceFile.getName().toLowerCase();
				boolean isAbc = name.endsWith(".abc") || name.endsWith(".txt");

				try
				{
					if (isAbc)
					{
						AbcInfo abcInfo = new AbcInfo();

						AbcToMidi.Params params = new AbcToMidi.Params(sourceFile);
						params.abcInfo = abcInfo;
						params.useLotroInstruments = false;
						//params.stereo = false;
						sequenceInfo = SequenceInfo.fromAbc(params);

						tripletTiming = abcInfo.hasTriplets();
						mixTiming = abcInfo.hasMixTimings();
						priorityActive = false;
						transcriber = abcInfo.getTranscriber();
					}
					else
					{
						sequenceInfo = SequenceInfo.fromMidi(sourceFile);
					}

					title = sequenceInfo.getTitle();
					composer = sequenceInfo.getComposer();
					keySignature = (ICompileConstants.SHOW_KEY_FIELD) ? sequenceInfo.getKeySignature()
							: KeySignature.C_MAJOR;
					timeSignature = sequenceInfo.getTimeSignature();
				}
				catch (FileNotFoundException e)
				{
					String msg = "Could not find the file used to create this song:\n" + sourceFile;
					sourceFile = fileResolver.locateFile(sourceFile, msg);
				}
				catch (InvalidMidiDataException | IOException | ParseException e)
				{
					String msg = "Could not load the file used to create this song:\n" + sourceFile + "\n\n"
							+ e.getMessage();
					sourceFile = fileResolver.resolveFile(sourceFile, msg);
				}

				if (sourceFile == null)
					throw new ParseException("Failed to load file", name);
			}
			
			if (!sourceFile.equals(origSourceFile)) {
				MaestroMain.setMIDIFileResolved();
			}

			title = SaveUtil.parseValue(songEle, "title", sequenceInfo.getTitle());
			composer = SaveUtil.parseValue(songEle, "composer", sequenceInfo.getComposer());
			transcriber = SaveUtil.parseValue(songEle, "transcriber", transcriber);
			genre = SaveUtil.parseValue(songEle, "genre", genre);
			mood = SaveUtil.parseValue(songEle, "mood", mood);
			note = SaveUtil.parseValue(songEle, "note", "");

			tempoFactor = SaveUtil.parseValue(songEle, "exportSettings/@tempoFactor", tempoFactor);
			transpose = SaveUtil.parseValue(songEle, "exportSettings/@transpose", transpose);
			if (ICompileConstants.SHOW_KEY_FIELD)
				keySignature = SaveUtil.parseValue(songEle, "exportSettings/@keySignature", keySignature);
			timeSignature = SaveUtil.parseValue(songEle, "exportSettings/@timeSignature", timeSignature);
			tripletTiming = SaveUtil.parseValue(songEle, "exportSettings/@tripletTiming", tripletTiming);
			mixTiming = SaveUtil.parseValue(songEle, "exportSettings/@mixTiming", false);// default false as old projects did not have that available. This means for old project with source abc that was exported with mix timings, the project will decide and it will be false.
			priorityActive = SaveUtil.parseValue(songEle, "exportSettings/@combinePriorities", false);

			int lastEnd = 0;
			for (Element tuneEle : XmlUtil.selectElements(songEle, "tuneSection")) {
				TuneLine tl = new TuneLine();
				tl.startBar = SaveUtil.parseValue(tuneEle, "startBar", 0);
				tl.endBar = SaveUtil.parseValue(tuneEle, "endBar", 0);
				tl.seminoteStep = SaveUtil.parseValue(tuneEle, "seminoteStep", 0);
				tl.tempo = SaveUtil.parseValue(tuneEle, "tempoChange", 0);
				tl.dialogLine = SaveUtil.parseValue(tuneEle, "dialogLine", -1);
				if (tl.startBar > 0 && tl.endBar >= tl.startBar) {
					if (tuneBars == null) {
						tuneBars = new TreeMap<>();
					}
					if (tl.endBar > lastEnd) {
						lastEnd = tl.endBar;
					}
					tuneBars.put(tl.startBar, tl);
				}
			}
			boolean[] booleanArray = new boolean[lastEnd+1];
			if (tuneBars != null) {
				for(int i = 0; i<lastEnd+1; i++) {
					Entry<Integer, TuneLine> entry = tuneBars.floorEntry(i+1);
					booleanArray[i] = entry != null && entry.getValue().startBar <= i+1 && entry.getValue().endBar >= i+1;
				}
				tuneBarsModified = booleanArray;
			}
			
			for (Element ele : XmlUtil.selectElements(songEle, "part"))
			{
				AbcPart part = AbcPart.loadFromXml(this, ele, fileVersion);
				int ins = Collections.binarySearch(parts, part, partNumberComparator);
				if (ins < 0)
					ins = -ins - 1;
				parts.add(ins, part);
				part.addAbcListener(abcPartListener);
			}
		}
		catch (XPathExpressionException e)
		{
			e.printStackTrace();
			throw new ParseException("XPath error: " + e.getMessage(), null);
		}
	}
	
	public String getNote() {
		// Only call this just after loading a msx file.
		return note;
	}
	
	public void setNote(String note) {
		// Only call this just before saving a msx file.
		this.note = note;
	}

	public Document saveToXml()
	{
		Document doc = XmlUtil.createDocument();
		doc.setXmlVersion("1.1");// This will allow project files with numerical chars to later be loaded fine. Like "&#11;".
		Element songEle = (Element) doc.appendChild(doc.createElement("song"));
		songEle.setAttribute("fileVersion", String.valueOf(SONG_FILE_VERSION));

		SaveUtil.appendChildTextElement(songEle, "sourceFile", String.valueOf(sourceFile));
		if (exportFile != null)
			SaveUtil.appendChildTextElement(songEle, "exportFile", String.valueOf(exportFile));

		SaveUtil.appendChildTextElement(songEle, "title", title);
		SaveUtil.appendChildTextElement(songEle, "composer", composer);
		SaveUtil.appendChildTextElement(songEle, "transcriber", transcriber);
		if (genre.length() > 0)	SaveUtil.appendChildTextElement(songEle, "genre", genre);
		if (mood.length() > 0)	SaveUtil.appendChildTextElement(songEle, "mood", mood);
		if (note.length() > 0)	SaveUtil.appendChildTextElement(songEle, "note", note);

		{
			Element exportSettingsEle = doc.createElement("exportSettings");

			if (tempoFactor != 1.0f)
				exportSettingsEle.setAttribute("tempoFactor", String.valueOf(tempoFactor));

			if (transpose != 0)
				exportSettingsEle.setAttribute("transpose", String.valueOf(transpose));

			if (ICompileConstants.SHOW_KEY_FIELD)
			{
				if (!keySignature.equals(sequenceInfo.getKeySignature()))
					exportSettingsEle.setAttribute("keySignature", String.valueOf(keySignature));
			}

			if (!timeSignature.equals(sequenceInfo.getTimeSignature()))
				exportSettingsEle.setAttribute("timeSignature", String.valueOf(timeSignature));

			if (tripletTiming)
				exportSettingsEle.setAttribute("tripletTiming", String.valueOf(tripletTiming));
			
			exportSettingsEle.setAttribute("mixTiming", String.valueOf(mixTiming));
			if (mixTiming)
				exportSettingsEle.setAttribute("combinePriorities", String.valueOf(priorityActive));

			if (exportSettingsEle.getAttributes().getLength() > 0 || exportSettingsEle.getChildNodes().getLength() > 0)
				songEle.appendChild(exportSettingsEle);
		}

		if (tuneBars != null && tuneBarsModified != null) {
			for (TuneLine tuneLine : tuneBars.values()) {
				Element tuneEle = (Element) songEle.appendChild(doc.createElement("tuneSection"));
				SaveUtil.appendChildTextElement(tuneEle, "startBar", String.valueOf(tuneLine.startBar));
				SaveUtil.appendChildTextElement(tuneEle, "endBar", String.valueOf(tuneLine.endBar));
				SaveUtil.appendChildTextElement(tuneEle, "seminoteStep", String.valueOf(tuneLine.seminoteStep));
				SaveUtil.appendChildTextElement(tuneEle, "tempoChange", String.valueOf(tuneLine.tempo));
				SaveUtil.appendChildTextElement(tuneEle, "dialogLine", String.valueOf(tuneLine.dialogLine));
			}
		}
		
		for (AbcPart part : parts)
		{
			part.saveToXml((Element) songEle.appendChild(doc.createElement("part")));
		}

		return doc;
	}

	public void exportAbc(File exportFile) throws FileNotFoundException, IOException, AbcConversionException
	{
		boolean delayEnabled = false;
		for (AbcPart part : parts) {
			if (part.delay != 0) {
				delayEnabled = true;
			}
		}
		try (FileOutputStream out = new FileOutputStream(exportFile))
		{
			getAbcExporter().exportToAbc(out, delayEnabled);
		}
	}

	public AbcPart createNewPart()
	{
		AbcPart newPart = new AbcPart(this);
		newPart.addAbcListener(abcPartListener);
		partAutoNumberer.onPartAdded(newPart);

		int idx = Collections.binarySearch(parts, newPart, partNumberComparator);
		if (idx < 0)
			idx = (-idx - 1);
		parts.add(idx, newPart);
		
		mixDirty = true;
		fireChangeEvent(AbcSongProperty.PART_ADDED, newPart);
		return newPart;
	}

	public void deletePart(AbcPart part)
	{
		if (part == null || !parts.contains(part))
			return;
		mixDirty = true;
		fireChangeEvent(AbcSongProperty.BEFORE_PART_REMOVED, part);
		parts.remove(part);
		partAutoNumberer.onPartDeleted(part);
		part.discard();
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		title = Util.emptyIfNull(title);
		if (!this.title.equals(title))
		{
			this.title = title;
			fireChangeEvent(AbcSongProperty.TITLE);
		}
	}
	
	@Override public String getGenre()
	{
		return genre;
	}

	public void setGenre(String genre)
	{
		genre = Util.emptyIfNull(genre);
		if (!this.genre.equals(genre))
		{
			this.genre = genre;
			fireChangeEvent(AbcSongProperty.GENRE);
		}
	}
	
	@Override public String getMood()
	{
		return mood;
	}

	public void setMood(String mood)
	{
		mood = Util.emptyIfNull(mood);
		if (!this.genre.equals(mood))
		{
			this.mood = mood;
			fireChangeEvent(AbcSongProperty.MOOD);
		}
	}

	@Override public String getComposer()
	{
		return composer;
	}

	public void setComposer(String composer)
	{
		composer = Util.emptyIfNull(composer);
		if (!this.composer.equals(composer))
		{
			this.composer = composer;
			fireChangeEvent(AbcSongProperty.COMPOSER);
		}
	}
	
	@Override public String getAllParts()
	{
		if (!allOut) {
			return null;
		}
		String str = "N: TS  ";
		StringBuilder str2 = new StringBuilder();
		ListModelWrapper<AbcPart> prts = getParts();
		int count = 0;
		for (AbcPart prt : prts) {
			if (prt.getEnabledTrackCount() > 0) {
				count += 1;
				str2.append("  ").append(prt.getPartNumber());
			}
		}
		if (count == 0) return null;
		str += count+", ";
		return str+str2;
	}

	@Override public String getTranscriber()
	{
		return transcriber;
	}

	public void setTranscriber(String transcriber)
	{
		transcriber = Util.emptyIfNull(transcriber);
		if (!this.transcriber.equals(transcriber))
		{
			this.transcriber = transcriber;
			fireChangeEvent(AbcSongProperty.TRANSCRIBER);
		}
	}

	public float getTempoFactor()
	{
		return tempoFactor;
	}

	public void setTempoFactor(float tempoFactor)
	{
		if (this.tempoFactor != tempoFactor)
		{
			this.tempoFactor = tempoFactor;
			fireChangeEvent(AbcSongProperty.TEMPO_FACTOR);
		}
	}

	public int getTempoBPM()
	{
		return Math.round(tempoFactor * sequenceInfo.getPrimaryTempoBPM());
	}

	public void setTempoBPM(int tempoBPM)
	{
		setTempoFactor((float) tempoBPM / sequenceInfo.getPrimaryTempoBPM());
	}

	public int getTranspose()
	{
		return transpose;
	}

	public void setTranspose(int transpose)
	{
		if (this.transpose != transpose)
		{
			this.transpose = transpose;
			fireChangeEvent(AbcSongProperty.TRANSPOSE);
		}
	}

	public KeySignature getKeySignature()
	{
		if (ICompileConstants.SHOW_KEY_FIELD)
			return keySignature;
		else
			return KeySignature.C_MAJOR;
	}

	public void setKeySignature(KeySignature keySignature)
	{
		if (!ICompileConstants.SHOW_KEY_FIELD)
			keySignature = KeySignature.C_MAJOR;

		if (!this.keySignature.equals(keySignature))
		{
			this.keySignature = keySignature;
			fireChangeEvent(AbcSongProperty.KEY_SIGNATURE);
		}
	}

	public TimeSignature getTimeSignature()
	{
		return timeSignature;
	}

	public void setTimeSignature(TimeSignature timeSignature)
	{
		if (!this.timeSignature.equals(timeSignature))
		{
			this.timeSignature = timeSignature;
			fireChangeEvent(AbcSongProperty.TIME_SIGNATURE);
		}
	}

	public boolean isTripletTiming()
	{
		return tripletTiming;
	}
	
	public boolean isMixTiming()
	{
		return mixTiming;
	}
	
	public void setMixTiming(boolean mixTiming)
	{
		if (this.mixTiming != mixTiming)
		{
			this.mixTiming = mixTiming;
			fireChangeEvent(AbcSongProperty.MIX_TIMING);
		}
	}

	public void setTripletTiming(boolean tripletTiming)
	{
		if (this.tripletTiming != tripletTiming)
		{
			this.tripletTiming = tripletTiming;
			fireChangeEvent(AbcSongProperty.TRIPLET_TIMING);
		}
	}

	public boolean isSkipSilenceAtStart()
	{
		return skipSilenceAtStart;
	}

	public void setSkipSilenceAtStart(boolean skipSilenceAtStart)
	{
		if (this.skipSilenceAtStart != skipSilenceAtStart)
		{
			this.skipSilenceAtStart = skipSilenceAtStart;
			fireChangeEvent(AbcSongProperty.SKIP_SILENCE_AT_START);
		}
	}
	
	/*public void setShowPruned(boolean showPruned)
	{
		if (this.showPruned != showPruned)
		{
			this.showPruned = showPruned;
			//fireChangeEvent(AbcSongProperty.SHOW_PRUNED);
		}
	}
	
	public boolean isShowPruned()
	{
		return showPruned;
	}*/
	
	public void setBadger (boolean badger) {
		this.badger = badger;
	}
	
	public void setAllOut (boolean allOut) {
		this.allOut = allOut;
	}

	public SequenceInfo getSequenceInfo()
	{
		return sequenceInfo;
	}

	public boolean isFromAbcFile()
	{
		return fromAbcFile;
	}

	public boolean isFromXmlFile()
	{
		return fromXmlFile;
	}

	@Override public String getPartName(AbcPartMetadataSource abcPart)
	{
		return partNameTemplate.formatName(abcPart);
	}

	public File getSourceFile()
	{
		return sourceFile;
	}

	public File getSaveFile()
	{
		return saveFile;
	}

	public void setSaveFile(File saveFile)
	{
		this.saveFile = saveFile;
	}

	@Override public File getExportFile()
	{
		return exportFile;
	}

	public void setExportFile(File exportFile)
	{
		if (this.exportFile == null && exportFile == null)
			return;
		if (this.exportFile != null && this.exportFile.equals(exportFile))
			return;

		this.exportFile = exportFile;
		fireChangeEvent(AbcSongProperty.EXPORT_FILE);
	}

	@Override public String getSongTitle()
	{
		return getTitle();
	}

	@Override public long getSongLengthMicros()
	{
		if (parts.size() == 0 || sequenceInfo == null)
			return 0;

		try
		{
			AbcExporter exporter = getAbcExporter();
			Pair<Long, Long> startEndTick = exporter
					.getSongStartEndTick(false /* lengthenToBar */, true /* accountForSustain */);
			QuantizedTimingInfo qtm = exporter.getTimingInfo();

			return (long)((qtm.tickToMicros(startEndTick.second) - qtm.tickToMicros(startEndTick.first))/(double)timingInfo.getExportTempoFactor());
		}
		catch (AbcConversionException e)
		{
			return 0;
		}
	}

	public ListModelWrapper<AbcPart> getParts()
	{
		return parts;
	}

	public void addSongListener(Listener<AbcSongEvent> l)
	{
		listeners.add(l);
	}

	public void removeSongListener(Listener<AbcSongEvent> l)
	{
		listeners.remove(l);
	}

	private void fireChangeEvent(AbcSongProperty property)
	{
		fireChangeEvent(property, null);
	}

	private void fireChangeEvent(AbcSongProperty property, AbcPart part)
	{
		if (listeners.size() > 0)
			listeners.fire(new AbcSongEvent(this, property, part));
	}

	public QuantizedTimingInfo getAbcTimingInfo() throws AbcConversionException
	{
		if (timingInfo == null //
				|| timingInfo.getExportTempoFactor() != getTempoFactor() //
				|| timingInfo.getMeter() != getTimeSignature() //
				|| timingInfo.isTripletTiming() != isTripletTiming() //
				|| timingInfo.isMixTiming() != isMixTiming() //
				|| mixDirty)
		{
			mixDirty = false;
			timingInfo = new QuantizedTimingInfo(sequenceInfo, getTempoFactor(), getTimeSignature(), isTripletTiming(), getTempoBPM(), this, isMixTiming());
		}

		return timingInfo;
	}

	public AbcExporter getAbcExporter() throws AbcConversionException
	{
		QuantizedTimingInfo qtm = getAbcTimingInfo();
		KeySignature key = getKeySignature();

		if (abcExporter == null)
		{
			abcExporter = new AbcExporter(parts, qtm, key, this);
		}
		else
		{
			if (abcExporter.getTimingInfo() != qtm)
				abcExporter.setTimingInfo(qtm);

			if (abcExporter.getKeySignature() != key)
				abcExporter.setKeySignature(key);

			if (abcExporter.isSkipSilenceAtStart() != skipSilenceAtStart)
				abcExporter.setSkipSilenceAtStart(skipSilenceAtStart);
			
			//if (abcExporter.isShowPruned() != showPruned)
			//	abcExporter.setShowPruned(showPruned);
		}

		return abcExporter;
	}

	private Comparator<AbcPart> partNumberComparator = new Comparator<AbcPart>()
	{
		@Override public int compare(AbcPart p1, AbcPart p2)
		{
			int base1 = partAutoNumberer.getFirstNumber(p1.getInstrument());
			int base2 = partAutoNumberer.getFirstNumber(p2.getInstrument());

			if (base1 != base2)
				return base1 - base2;
			return p1.getPartNumber() - p2.getPartNumber();
		}
	};

	private Listener<AbcPartEvent> abcPartListener = e -> {
		if (e.getProperty() == AbcPartProperty.PART_NUMBER)
		{
			parts.sort(partNumberComparator);
			fireChangeEvent(AbcSongProperty.PART_LIST_ORDER, e.getSource());
		}
	};

	@Override
	public String getBadgerTitle() {
		if (!badger) return null;
		return "N: Title: "+StringCleaner.cleanForABC(getComposer())+" - "+StringCleaner.cleanForABC(getSongTitle());
	}
	
	public void assignNumbersToSimilarPartTypes () {
		//System.out.println();
		//System.out.println("=== assignNumbersToSimilarPartTypes ===");
		for (LotroInstrument instr : LotroInstrument.values()) {
			List<AbcPart> instrParts = new ArrayList<>();
			for (AbcPart part : parts) {
				if(part.getInstrument().equals(instr)) {
					instrParts.add(part);
				}
			}
			//int partIndex = 0;
			if (instrParts.size() > 1) {// This is not super tight, as one or more of them might not have assigned any track. TODO.
				int index = 1;
				for (AbcPart part : instrParts) {
					if (part.getEnabledTrackCount() == 0) {
						//System.out.println(partIndex + ": " + part.getInstrument() + " (old title is " + part.getTitle() +")  = " + 0 + " (not assigned)");partIndex++;
						part.setTypeNumber(0);
					} else {
						//System.out.println(partIndex + ": " + part.getInstrument() + " (old title is " + part.getTitle() +")  = " + index);partIndex++;
						if (part.setTypeNumber(index)) {
							index++;
						} else {
							//System.out.println("    failure, -1 was set");
						}
					}
				}
			} else if (instrParts.size() == 1) {
				//System.out.println(partIndex + ": " + instrParts.get(0).getInstrument() + " = " + 0);
				instrParts.get(0).setTypeNumber(0);
			}
		}
		//System.out.println();
	}
	
	public boolean isPriorityActive()
	{
		return priorityActive;
	}
	
	public void setPriorityActive(boolean priorityActive)
	{
		if (this.priorityActive != priorityActive)
		{
			mixDirty = true;
			this.priorityActive = priorityActive;
			fireChangeEvent(AbcSongProperty.MIX_TIMING_COMBINE_PRIORITIES);
		}
	}

	public void tuneEdited() {
		mixDirty = true; // Tempo might have changed, in which case the mixTimings need to be recomputed
		fireChangeEvent(AbcSongProperty.TUNE_EDIT);
	}
	
	public int getTuneTranspose(long tickStart) {
		int tuneTrans = 0;
		SequenceInfo se = getSequenceInfo();
		TreeMap<Integer, TuneLine> tree = tuneBars;
		if (se != null && tree != null) {
			SequenceDataCache data = se.getDataCache();
			long barLengthTicks = data.getBarLengthTicks();

			long startTick = barLengthTicks;
			long endTick = data.getSongLengthTicks();

			int bar = -1;
			int curBar = 1;
			for (long barTick = startTick; barTick <= endTick+barLengthTicks; barTick += barLengthTicks) {
				if (tickStart < barTick) {
					bar = curBar;
					break;
				}
				curBar += 1;
			}
			if (bar != -1) {
				Entry<Integer, TuneLine> entry = tree.floorEntry(bar);
				if (entry != null) {
					if (bar <= entry.getValue().endBar) {
						tuneTrans = entry.getValue().seminoteStep;
					}
				}
			}
		}		
		return tuneTrans;
	}
	
	public TreeMap<Long, Integer> getTuneTempoChanges() {
		SequenceInfo se = getSequenceInfo();
		TreeMap<Integer, TuneLine> tree = tuneBars;
		TreeMap<Long, Integer> treeChanges = new TreeMap<>();
		if (se != null && tree != null) {
			SequenceDataCache data = se.getDataCache();
			Collection<TuneLine> lines = tree.values();
			long barLength = data.getBarLengthTicks();
			for (TuneLine line : lines) {
				if (line.tempo != 0) {
					long tickStart = data.getBarToTick(line.startBar);
					long tickEnd = data.getBarToTick(line.endBar)+barLength;
					treeChanges.put(tickStart, line.tempo);
					treeChanges.put(tickEnd, 0);
				}
			}
		}
		return treeChanges;
	}

	/*public boolean isKept(long tickStart) {
		SequenceInfo se = getSequenceInfo();
		TreeMap<Integer, TuneLine> tree = tuneBars;
		if (se != null && tree != null) {
			SequenceDataCache data = se.getDataCache();
			long barLengthTicks = data.getBarLengthTicks();

			long startTick = barLengthTicks;
			long endTick = data.getSongLengthTicks();

			int bar = -1;
			int curBar = 1;
			for (long barTick = startTick; barTick <= endTick+barLengthTicks; barTick += barLengthTicks) {
				if (tickStart < barTick) {
					bar = curBar;
					break;
				}
				curBar += 1;
			}
			if (bar != -1) {
				Entry<Integer, TuneLine> entry = tree.floorEntry(bar);
				if (entry != null) {
					if (bar <= entry.getValue().endBar) {
						return !entry.getValue().remove;
					}
				}
			}
		}
		
		return true;
	}*/
}
