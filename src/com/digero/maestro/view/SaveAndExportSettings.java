package com.digero.maestro.view;

import java.util.prefs.Preferences;

public class SaveAndExportSettings
{
	public boolean promptSaveNewSong = true;
	public boolean showExportFileChooser = false;
	public boolean skipSilenceAtStart = true;
	public boolean showPruned = false;
	public int stereoPan = 100;
	private final Preferences prefs;
	public boolean showMaxPolyphony = false;
	public boolean showBadger = false;
	public boolean allBadger = false;

	public SaveAndExportSettings(Preferences prefs)
	{
		this.prefs = prefs;
		promptSaveNewSong = prefs.getBoolean("promptSaveNewSong", promptSaveNewSong);
		showExportFileChooser = prefs.getBoolean("showExportFileChooser", showExportFileChooser);
		skipSilenceAtStart = prefs.getBoolean("skipSilenceAtStart", skipSilenceAtStart);
		showPruned = prefs.getBoolean("showPruned", showPruned);
		stereoPan = prefs.getInt("stereoPan", stereoPan);
		showMaxPolyphony = prefs.getBoolean("showMaxPolyphony", showMaxPolyphony);
		showBadger = prefs.getBoolean("showBadger", showBadger);
		allBadger = prefs.getBoolean("allBadger", allBadger);
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
		stereoPan = that.stereoPan;
		showMaxPolyphony = that.showMaxPolyphony;
		showBadger = that.showBadger;
		allBadger = that.allBadger;
	}

	public void saveToPrefs()
	{
		prefs.putBoolean("promptSaveNewSong", promptSaveNewSong);
		prefs.putBoolean("showExportFileChooser", showExportFileChooser);
		prefs.putBoolean("skipSilenceAtStart", skipSilenceAtStart);
		prefs.putBoolean("showPruned", showPruned);
		prefs.putInt("stereoPan", stereoPan);
		prefs.putBoolean("showMaxPolyphony", showMaxPolyphony);
		prefs.putBoolean("showBadger", showBadger);
		prefs.putBoolean("allBadger", allBadger);
	}

	public SaveAndExportSettings getCopy()
	{
		return new SaveAndExportSettings(this);
	}
}
