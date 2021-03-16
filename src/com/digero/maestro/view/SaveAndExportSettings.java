package com.digero.maestro.view;

import java.util.prefs.Preferences;

public class SaveAndExportSettings
{
	public boolean promptSaveNewSong = true;
	public boolean showExportFileChooser = false;
	public boolean skipSilenceAtStart = true;
	public boolean showPruned = false;
	private final Preferences prefs;

	public SaveAndExportSettings(Preferences prefs)
	{
		this.prefs = prefs;
		promptSaveNewSong = prefs.getBoolean("promptSaveNewSong", promptSaveNewSong);
		showExportFileChooser = prefs.getBoolean("showExportFileChooser", showExportFileChooser);
		skipSilenceAtStart = prefs.getBoolean("skipSilenceAtStart", skipSilenceAtStart);
		showPruned = prefs.getBoolean("showPruned", showPruned);
	}

	public SaveAndExportSettings(SaveAndExportSettings that)
	{
		this.prefs = that.prefs;
		copyFrom(that);
	}

	public void copyFrom(SaveAndExportSettings that)
	{
		promptSaveNewSong = that.promptSaveNewSong;
		showExportFileChooser = that.showExportFileChooser;
		skipSilenceAtStart = that.skipSilenceAtStart;
		showPruned = that.showPruned;
	}

	public void saveToPrefs()
	{
		prefs.putBoolean("promptSaveNewSong", promptSaveNewSong);
		prefs.putBoolean("showExportFileChooser", showExportFileChooser);
		prefs.putBoolean("skipSilenceAtStart", skipSilenceAtStart);
		prefs.putBoolean("showPruned", showPruned);
	}

	public SaveAndExportSettings getCopy()
	{
		return new SaveAndExportSettings(this);
	}
}
