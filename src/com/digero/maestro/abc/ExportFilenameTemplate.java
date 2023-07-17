package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.digero.common.util.Pair;
import com.digero.common.util.Util;
import com.digero.maestro.view.SettingsDialog.MockMetadataSource;

public class ExportFilenameTemplate
{
	public static final String spaceReplaceChars[] = {" ", "_", "-"};
	public static final String spaceReplaceLabels[] = {"Don't Replace", "_ (Underscore)", "- (Dash)"};
	
	public static class Settings
	{
		private boolean exportFilenamePatternEnabled;
		private String exportFilenamePattern;
		private String whitespaceReplaceText;

		private Settings(Preferences prefs)
		{
			exportFilenamePatternEnabled = prefs.getBoolean("exportFilenamePatternEnabled", false);
			exportFilenamePattern = prefs.get("exportFilenamePattern", "$PartCount - $SongTitle");
			whitespaceReplaceText = prefs.get("whitespaceReplaceText", " ");
		}

		public Settings(Settings source)
		{
			copyFrom(source);
		}

		private void save(Preferences prefs)
		{
			prefs.putBoolean("exportFilenamePatternEnabled", exportFilenamePatternEnabled);
			prefs.put("exportFilenamePattern", exportFilenamePattern);
			prefs.put("whitespaceReplaceText", whitespaceReplaceText);
		}

		private void copyFrom(Settings source)
		{
			this.exportFilenamePatternEnabled = source.exportFilenamePatternEnabled;
			this.exportFilenamePattern = source.exportFilenamePattern;
			this.whitespaceReplaceText = source.whitespaceReplaceText;
		}
		
		public boolean isExportFilenamePatternEnabled()
		{
			return exportFilenamePatternEnabled;
		}
		
		public void setExportFilenamePatternEnabled(boolean exportFilenamePatternEnabled)
		{
			this.exportFilenamePatternEnabled = exportFilenamePatternEnabled;
		}

		public String getExportFilenamePattern()
		{
			return exportFilenamePattern;
		}

		public void setExportFilenamePattern(String exportFilenamePattern)
		{
			this.exportFilenamePattern = exportFilenamePattern;
		}

		public String getWhitespaceReplaceText()
		{
			return whitespaceReplaceText;
		}

		public void setWhitespaceReplaceText(String whitespaceReplaceText)
		{
			this.whitespaceReplaceText = whitespaceReplaceText;
		}
	}

	public static abstract class Variable
	{
		private String description;

		private Variable(String description)
		{
			this.description = description;
		}

		public abstract String getValue();

		public String getDescription()
		{
			return description;
		}

		@Override public String toString()
		{
			return getValue();
		}
	}

	private Settings settings;
	private Preferences prefsNode;

	private AbcMetadataSource metadata = null;

	private SortedMap<String, Variable> variables;

	public ExportFilenameTemplate(Preferences prefsNode)
	{
		this.prefsNode = prefsNode;
		this.settings = new Settings(prefsNode);

		Comparator<String> caseInsensitiveStringComparator = String::compareToIgnoreCase;

		variables = new TreeMap<>(caseInsensitiveStringComparator);

		variables.put("$SongTitle", new Variable("The title of the song, as entered in the \"T:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getSongTitle().trim();
			}
		});
		variables.put("$SongLength", new Variable("The playing time of the song in mm_ss format")
		{
			@Override public String getValue()
			{
				return Util.formatDuration(getMetadataSource().getSongLengthMicros(), 0, '-');
			}
		});
		variables.put("$SongComposer", new Variable("The song composer's name, as entered in the \"C:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getComposer().trim();
			}
		});
		variables.put("$SongTranscriber", new Variable("Your name, as entered in the \"Z:\" field")
		{
			@Override public String getValue()
			{
				return getMetadataSource().getTranscriber().trim();
			}
		});
		variables.put("$PartCount", new Variable("Number of parts in the ABC file")
		{
			@Override public String getValue()
			{
				return String.format("%02d", getMetadataSource().getPartCount());
			}
		});
	}

	public Settings getSettingsCopy()
	{
		return new Settings(settings);
	}

	public void setSettings(Settings settings)
	{
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public AbcMetadataSource getMetadataSource()
	{
		if (metadata == null)
			metadata = new MockMetadataSource(null);

		return metadata;
	}

	public void setMetadataSource(AbcMetadataSource metadata)
	{
		this.metadata = metadata;
	}

	public SortedMap<String, Variable> getVariables()
	{
		return Collections.unmodifiableSortedMap(variables);
	}
	
	public boolean isEnabled()
	{
		return settings.isExportFilenamePatternEnabled();
	}

	public String formatName()
	{
		return formatName(settings);
	}

	public String formatName(ExportFilenameTemplate.Settings settings)
	{
		String name = settings.getExportFilenamePattern();

		// Find all variables starting with $
		Pattern regex = Pattern.compile("\\$[A-Za-z]+");
		Matcher matcher = regex.matcher(name);

		ArrayList<Pair<Integer, Integer>> matches = new ArrayList<>();
		while (matcher.find())
		{
			matches.add(new Pair<>(matcher.start(), matcher.end()));
		}

		ListIterator<Pair<Integer, Integer>> reverseIter = matches.listIterator(matches.size());
		while (reverseIter.hasPrevious())
		{
			Pair<Integer, Integer> match = reverseIter.previous();
			Variable var = variables.get(name.substring(match.first, match.second));
			if (var != null)
			{
				String value = var.getValue();
				value = value.replaceAll("\\s+", settings.getWhitespaceReplaceText());
				name = name.substring(0, match.first) + value + name.substring(match.second);
			}
		}
		
		name += ".abc";
		
		return name;
	}
}