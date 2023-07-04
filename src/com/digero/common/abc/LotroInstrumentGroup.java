package com.digero.common.abc;

public enum LotroInstrumentGroup {
	PLUCKED_STRINGS("Plucked Strings"), //
	BOWED_STRINGS("Bowed Strings"), //
	WOODWINDS("Woodwinds"), //
	PERCUSSION("Percussion"); //

	private final String label;

	private LotroInstrumentGroup(String label) {
		this.label = label;
	}

	@Override
	public String toString() {
		return label;
	}

	public static LotroInstrumentGroup groupOf(LotroInstrument instrument) {
		return switch (instrument) {
		default -> LotroInstrumentGroup.PLUCKED_STRINGS;
		case LUTE_OF_AGES, BASIC_LUTE, BASIC_HARP, MISTY_MOUNTAIN_HARP, BASIC_THEORBO, TRAVELLERS_TRUSTY_FIDDLE ->
			LotroInstrumentGroup.PLUCKED_STRINGS;
		case BARDIC_FIDDLE, BASIC_FIDDLE, LONELY_MOUNTAIN_FIDDLE, SPRIGHTLY_FIDDLE, STUDENT_FIDDLE ->
			LotroInstrumentGroup.BOWED_STRINGS;
		case BASIC_FLUTE, BASIC_CLARINET, BASIC_HORN, BASIC_BASSOON, BRUSQUE_BASSOON, LONELY_MOUNTAIN_BASSOON,
				BASIC_BAGPIPE, BASIC_PIBGORN ->
			LotroInstrumentGroup.WOODWINDS;
		case BASIC_DRUM, BASIC_COWBELL, MOOR_COWBELL, STUDENT_FX_FIDDLE -> LotroInstrumentGroup.PERCUSSION;
		};
	}
}