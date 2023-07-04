package com.digero.maestro.abc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.prefs.Preferences;

import com.digero.common.abc.LotroInstrument;

public class PartAutoNumberer {
	public static class Settings {
		private Map<LotroInstrument, Integer> firstNumber = new HashMap<>();
		private boolean incrementByTen;

		private Settings(Preferences prefs) {
			incrementByTen = prefs.getBoolean("incrementByTen", true);
			int x10 = incrementByTen ? 1 : 10;

			if (!prefs.getBoolean("newCowbellDefaults", false)) {
				prefs.putBoolean("newCowbellDefaults", true);
				prefs.remove(prefsKey(LotroInstrument.BASIC_COWBELL));
				prefs.remove(prefsKey(LotroInstrument.MOOR_COWBELL));
			}

			init(prefs, LotroInstrument.LUTE_OF_AGES, prefs.getInt("Lute", 1 * x10)); // Lute was renamed to Lute of
																						// Ages
			init(prefs, LotroInstrument.BASIC_LUTE, LotroInstrument.LUTE_OF_AGES);
			init(prefs, LotroInstrument.BASIC_HARP, 2 * x10);
			init(prefs, LotroInstrument.MISTY_MOUNTAIN_HARP, LotroInstrument.BASIC_HARP);
			init(prefs, LotroInstrument.BASIC_THEORBO, 3 * x10);
			init(prefs, LotroInstrument.BASIC_FLUTE, 4 * x10);
			init(prefs, LotroInstrument.BASIC_CLARINET, 5 * x10);
			init(prefs, LotroInstrument.BASIC_HORN, 6 * x10);
			init(prefs, LotroInstrument.BASIC_BAGPIPE, 7 * x10);
			init(prefs, LotroInstrument.BASIC_PIBGORN, LotroInstrument.BASIC_BAGPIPE);
			init(prefs, LotroInstrument.BASIC_BASSOON, LotroInstrument.BASIC_BAGPIPE);
			init(prefs, LotroInstrument.LONELY_MOUNTAIN_BASSOON, LotroInstrument.BASIC_BAGPIPE);
			init(prefs, LotroInstrument.BRUSQUE_BASSOON, LotroInstrument.BASIC_BAGPIPE);
			init(prefs, LotroInstrument.BASIC_DRUM, 8 * x10);
			init(prefs, LotroInstrument.BASIC_COWBELL, LotroInstrument.BASIC_DRUM);
			init(prefs, LotroInstrument.MOOR_COWBELL, LotroInstrument.BASIC_DRUM);
			init(prefs, LotroInstrument.BASIC_FIDDLE, 9 * x10);
			init(prefs, LotroInstrument.BARDIC_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.STUDENT_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.STUDENT_FX_FIDDLE, LotroInstrument.STUDENT_FIDDLE);
			init(prefs, LotroInstrument.LONELY_MOUNTAIN_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.SPRIGHTLY_FIDDLE, LotroInstrument.BASIC_FIDDLE);
			init(prefs, LotroInstrument.TRAVELLERS_TRUSTY_FIDDLE, LotroInstrument.BASIC_FIDDLE);

			assert (firstNumber.size() == LotroInstrument.values().length);
		}

		/**
		 * @return the original name of the instrument before it was renamed, which can
		 *         be used a stable prefs key even if the instrument is renamed.
		 */
		public String prefsKey(LotroInstrument instrument) {
			return switch (instrument) {
			case LUTE_OF_AGES -> "Lute of Ages";
			case BASIC_LUTE -> "Basic Lute";
			case BASIC_HARP -> "Harp";
			case MISTY_MOUNTAIN_HARP -> "Misty Mountain Harp";
			case BARDIC_FIDDLE -> "Bardic Fiddle";
			case BASIC_FIDDLE -> "Basic Fiddle";
			case LONELY_MOUNTAIN_FIDDLE -> "Lonely Mountain Fiddle";
			case SPRIGHTLY_FIDDLE -> "Sprightly Fiddle";
			case STUDENT_FIDDLE -> "Student's Fiddle";
			case STUDENT_FX_FIDDLE -> "Student's FX Fiddle";
			case TRAVELLERS_TRUSTY_FIDDLE -> "Traveller's Trusty Fiddle";
			case BASIC_THEORBO -> "Theorbo";
			case BASIC_FLUTE -> "Flute";
			case BASIC_CLARINET -> "Clarinet";
			case BASIC_HORN -> "Horn";
			case BASIC_BASSOON -> "Basic Bassoon";
			case BRUSQUE_BASSOON -> "Brusque Bassoon";
			case LONELY_MOUNTAIN_BASSOON -> "Lonely Mountain Bassoon";
			case BASIC_BAGPIPE -> "Bagpipe";
			case BASIC_PIBGORN -> "Pibgorn";
			case BASIC_DRUM -> "Drums";
			case BASIC_COWBELL -> "Cowbell";
			case MOOR_COWBELL -> "Moor Cowbell";
			};
		}

		private void init(Preferences prefs, LotroInstrument instrument, int defaultValue) {
			firstNumber.put(instrument, prefs.getInt(prefsKey(instrument), defaultValue));
		}

		private void init(Preferences prefs, LotroInstrument instruments, LotroInstrument copyDefaultFrom) {
			init(prefs, instruments, firstNumber.get(copyDefaultFrom));
		}

		private void save(Preferences prefs) {
			for (Entry<LotroInstrument, Integer> entry : firstNumber.entrySet()) {
				prefs.putInt(prefsKey(entry.getKey()), entry.getValue());
			}
			prefs.putBoolean("incrementByTen", incrementByTen);
		}

		public Settings(Settings source) {
			copyFrom(source);
		}

		public void copyFrom(Settings source) {
			firstNumber = new HashMap<>(source.firstNumber);
			incrementByTen = source.incrementByTen;
		}

		public int getIncrement() {
			return incrementByTen ? 10 : 1;
		}

		public boolean isIncrementByTen() {
			return incrementByTen;
		}

		public void setIncrementByTen(boolean incrementByTen) {
			this.incrementByTen = incrementByTen;
		}

		public void setFirstNumber(LotroInstrument instrument, int number) {
			firstNumber.put(instrument, number);
		}

		public int getFirstNumber(LotroInstrument instrument) {
			return firstNumber.get(instrument);
		}
	}

	private Settings settings;
	private Preferences prefsNode;
	private List<? extends NumberedAbcPart> parts = null;

	public PartAutoNumberer(Preferences prefsNode) {
		this.prefsNode = prefsNode;
		this.settings = new Settings(prefsNode);
	}

	public Settings getSettingsCopy() {
		return new Settings(settings);
	}

	public boolean isIncrementByTen() {
		return settings.isIncrementByTen();
	}

	public int getIncrement() {
		return settings.getIncrement();
	}

	public int getFirstNumber(LotroInstrument instrument) {
		return settings.getFirstNumber(instrument);
	}

	public void setSettings(Settings settings) {
		this.settings.copyFrom(settings);
		this.settings.save(prefsNode);
	}

	public void setParts(List<? extends NumberedAbcPart> parts) {
		this.parts = parts;
	}

	public void renumberAllParts() {

		if (parts == null)
			return;

		Set<Integer> numbersInUse = new HashSet<>(parts.size());

		List<? extends NumberedAbcPart> partsCopy = new ArrayList<NumberedAbcPart>(parts);// This is to prevent a
																							// reordering of parts while
																							// iterating through it.

		for (NumberedAbcPart part : partsCopy) {
			int partNumber = getFirstNumber(part.getInstrument());
			while (numbersInUse.contains(partNumber)) {
				partNumber += getIncrement();
			}
			numbersInUse.add(partNumber);
			part.setPartNumber(partNumber);
		}

	}

	public void onPartAdded(NumberedAbcPart partAdded) {

		if (parts == null)
			return;

		int newPartNumber = settings.getFirstNumber(partAdded.getInstrument());

		boolean conflict;
		do {
			conflict = false;
			for (NumberedAbcPart part : parts) {
				if (part != partAdded && part.getPartNumber() == newPartNumber) {
					newPartNumber += getIncrement();
					conflict = true;
				}
			}
		} while (conflict);

		partAdded.setPartNumber(newPartNumber);
	}

	public void onPartDeleted(NumberedAbcPart partDeleted) {
		// System.out.println(partDeleted.getPartNumber()+" deleted");
		if (parts == null)
			return;

		int deletedNumber = partDeleted.getPartNumber();
		int deletedFirstNumber = getFirstNumber(partDeleted.getInstrument());// System.out.println(deletedFirstNumber+"
																				// is the first from the deleted");
		if (!isAutoAssigned(partDeleted, -1, deletedFirstNumber)) {
			// System.out.println(partDeleted.getInstrument().toString()+" deleted and did
			// not fit");
			return;
		}

		for (NumberedAbcPart part : parts) {
			int partNumber = part.getPartNumber();
			int partFirstNumber = getFirstNumber(part.getInstrument());// System.out.println(partFirstNumber+" is the
																		// first");
			boolean autoTest = isAutoAssigned(part, deletedNumber, deletedFirstNumber);
			if (part != partDeleted && partNumber > deletedNumber && partNumber > partFirstNumber
					&& partFirstNumber == deletedFirstNumber && autoTest) {
				part.setPartNumber(partNumber - getIncrement());// System.out.println(partNumber+" decremented");
				if (part.getPartNumber() == deletedNumber)
					deletedNumber = partNumber;// the deleted spot was filled out, the one that filled it out is now
												// considered deleted
			} // else System.out.println(autoTest+" "+partNumber+" isAutoAssigned(part)");
		}
	}

	private boolean isAutoAssigned(NumberedAbcPart testPart, int deletedNumber, int deletedFirstNumber) {
		// Return true if this part fit into the auto numbering scheme.
		// If it does not or a part with lower part number has a different firstNumber,
		// but seemingly fit into this parts numbering scheme
		// it will also return false.
		int testNumber = testPart.getPartNumber();
		int testFirstNumber = getFirstNumber(testPart.getInstrument());
		if (testNumber == testFirstNumber)
			return true;
		if (getIncrement() == 10 && Math.abs(testNumber) % 10 != testFirstNumber) {
			return false;
		}
		if (testNumber < testFirstNumber)
			return false;
		boolean cohesive = true;
		int checkNumber = testFirstNumber;
		if (testFirstNumber != deletedFirstNumber) {
			deletedNumber = -1;// should not be considered
		}
		outer: while (cohesive && checkNumber < testNumber) {
			if (checkNumber == deletedNumber) {
				// System.out.println(checkNumber+" checks out (deleted)");
				checkNumber += getIncrement();
				continue outer;
			}
			for (NumberedAbcPart part : parts) {
				int partNumber = part.getPartNumber();

				if (checkNumber == partNumber) {
					if (testFirstNumber != getFirstNumber(part.getInstrument())) {
						// System.out.println(" testFirstNumber !=
						// getFirstNumber(part.getInstrument())");
						return false;
					}
					// System.out.println(checkNumber+" checks out");
					checkNumber += getIncrement();
					continue outer;
				}
			}
			cohesive = false;
			// System.out.println(testNumber+" not cohesive");
		}
		return cohesive;
	}

	public void setPartNumber(NumberedAbcPart partToChange, int newPartNumber) {

		if (parts == null)
			return;

		for (NumberedAbcPart part : parts) {
			if (part != partToChange && part.getPartNumber() == newPartNumber) {
				part.setPartNumber(partToChange.getPartNumber());
				break;
			}
		}
		partToChange.setPartNumber(newPartNumber);
	}

	public void setInstrument(NumberedAbcPart partToChange, LotroInstrument newInstrument) {

		if (newInstrument != partToChange.getInstrument()) {
			if (getFirstNumber(partToChange.getInstrument()) == getFirstNumber(newInstrument)) {
				// Lets keep the part number, since it has the same first number
				partToChange.setInstrument(newInstrument);
			} else {
				onPartDeleted(partToChange);
				partToChange.setInstrument(newInstrument);
				onPartAdded(partToChange);
			}
		}
	}

	public LotroInstrument[] getSortedInstrumentList() {
		LotroInstrument[] instruments = LotroInstrument.values();
		Arrays.sort(instruments, (a, b) -> {
			int diff = getFirstNumber(a) - getFirstNumber(b);
			if (diff != 0)
				return diff;

			return a.toString().compareTo(b.toString());
		});
		return instruments;
	}
}
