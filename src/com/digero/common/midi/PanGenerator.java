package com.digero.common.midi;

import java.util.Arrays;
import java.util.regex.Pattern;

import com.digero.common.abc.LotroInstrument;

public class PanGenerator
{
	public static final int CENTER = 64;

	private int[] count;

	public PanGenerator()
	{
		count = new int[LotroInstrument.values().length];
	}

	public void reset()
	{
		Arrays.fill(count, 0);
	}

	static final Pattern leftRegex = Pattern.compile("\\b(left|links|gauche)\\b");
	static final Pattern rightRegex = Pattern.compile("\\b(right|rechts|droite)\\b");
	static final Pattern centerRegex = Pattern.compile("\\b(middle|center|zentrum|mitte|centre)\\b");

	public int get(LotroInstrument instrument, String partTitle)
	{
		int pan = get(instrument);

		String titleLower = partTitle.toLowerCase();
		if (leftRegex.matcher(titleLower).find())
			pan = CENTER - 50;//Math.abs(pan - CENTER);
		else if (rightRegex.matcher(titleLower).find())
			pan = CENTER + 50;//Math.abs(pan - CENTER);
		else if (centerRegex.matcher(titleLower).find())
			pan = CENTER;

		return pan;
	}
	
	public int get(LotroInstrument instrument, String partTitle, int panModifier)
	{
		int pan = get(instrument);
		
		if (panModifier != 100) {
			pan = pan - CENTER;
			pan = (int) (pan * (float) panModifier * 0.01f);
			pan = pan + CENTER;
		}

		String titleLower = partTitle.toLowerCase();
		if (leftRegex.matcher(titleLower).find())
			pan = CENTER - (int) (50 * (float) panModifier * 0.01f);//Math.abs(pan - CENTER);
		else if (rightRegex.matcher(titleLower).find())
			pan = CENTER + (int) (50 * (float) panModifier * 0.01f);//Math.abs(pan - CENTER);
		else if (centerRegex.matcher(titleLower).find())
			pan = CENTER;

		return pan;
	}

	public int get(LotroInstrument instrument)
	{
		switch (instrument)
		{
			case LUTE_OF_AGES:
			case TRAVELLERS_TRUSTY_FIDDLE:
			case BASIC_LUTE:
				instrument = LotroInstrument.LUTE_OF_AGES;
				break;
			case BASIC_HARP:
			case SPRIGHTLY_FIDDLE:
			case MISTY_MOUNTAIN_HARP:
				instrument = LotroInstrument.BASIC_HARP;
				break;
			case BASIC_COWBELL:
			case MOOR_COWBELL:
				instrument = LotroInstrument.BASIC_COWBELL;
				break;
			case BASIC_FIDDLE:
			case STUDENT_FIDDLE:
			case STUDENT_FX_FIDDLE:
			case LONELY_MOUNTAIN_FIDDLE:
			case BARDIC_FIDDLE:
				instrument = LotroInstrument.BASIC_FIDDLE;
				break;
			case BASIC_BASSOON:
			case LONELY_MOUNTAIN_BASSOON:
			case BRUSQUE_BASSOON:
				instrument = LotroInstrument.BASIC_BASSOON;
				break;
			case BASIC_BAGPIPE:
			case BASIC_CLARINET:
			case BASIC_DRUM:
			case BASIC_FLUTE:
			case BASIC_HORN:
			case BASIC_PIBGORN:
			case BASIC_THEORBO:
				break;
		}

		int sign;
		int c = count[instrument.ordinal()]++;

		sign = switch (c % 3) {
			case 0 -> 1;
			case 1 -> -1;
			default -> 0;
		};

		return switch (instrument) {
			case BARDIC_FIDDLE, BASIC_FIDDLE, LONELY_MOUNTAIN_FIDDLE, STUDENT_FIDDLE -> CENTER + sign * -50;
			case STUDENT_FX_FIDDLE -> CENTER + sign * -50;
			case BASIC_HARP, MISTY_MOUNTAIN_HARP, SPRIGHTLY_FIDDLE -> CENTER + sign * -45;
			case BASIC_FLUTE -> CENTER + sign * -40;
			case BASIC_BAGPIPE -> CENTER + sign * -30;
			case BASIC_THEORBO -> CENTER + sign * -25;
			case BASIC_COWBELL, MOOR_COWBELL -> CENTER + sign * -15;
			case BASIC_DRUM -> CENTER + sign * 15;
			case BASIC_PIBGORN -> CENTER + sign * 20;
			case BASIC_HORN -> CENTER + sign * 25;
			case BASIC_LUTE, LUTE_OF_AGES, TRAVELLERS_TRUSTY_FIDDLE -> CENTER + sign * 35;
			case BASIC_CLARINET -> CENTER + sign * 45;
			case BASIC_BASSOON, LONELY_MOUNTAIN_BASSOON, BRUSQUE_BASSOON -> CENTER + sign * 50;
		};

	}
}
