package com.digero.common.abc;

import java.io.UnsupportedEncodingException;
import java.text.Normalizer;

public class StringCleaner {

	public static boolean cleanABC = true;

	public static String cleanForFileName(String before) {
		//System.out.println("Orig file: "+before);
		String after = replaceUmlaut(before);
	    after = convertToBasicAscii(after);
	    after = after.replace('.', ' ');// lotro do not like when there is more than one dot
	    //System.out.println("New file : "+after);
	    return after.trim();
	}
	
	public static String cleanForABC(String before) {
		if (before == null) return "";
		if (cleanABC) {
			//System.out.println("Orig: "+before);
			String after = replaceUmlaut(before);
		    after = convertToBasicAscii(after);
		    //System.out.println("New : "+after);
		    return after.trim();
		}
		return before.trim();
	}
	
	private static String replaceUmlaut(String before) {
		before = before.replaceAll("ß", "ss");
		before = before.replaceAll("æ", "ae");
		before = before.replaceAll("Æ", "AE");
		before = before.replaceAll("ø", "oe");
		before = before.replaceAll("Ø", "OE");
		before = before.replaceAll("å", "aa");
		before = before.replaceAll("Å", "AA");
		before = before.replaceAll("ä", "ae");
		before = before.replaceAll("Ä", "AE");
		before = before.replaceAll("ö", "oe");
		before = before.replaceAll("Ö", "OE");
		before = before.replaceAll("—", "-");
	    return before;
	}
	
	private static String convertToBasicAscii(String before) {
	    String s1 = Normalizer.normalize(before, Normalizer.Form.NFKD);
	    String regex = "[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+";
	    //String regex = "[\\P{InBasicLatin}]+";
	    String after = new String(s1);
	    try {
	    	after = new String(s1.replaceAll(regex, "").getBytes("ascii"), "ascii");
		} catch (UnsupportedEncodingException e) {
			//e.printStackTrace();
		}
	    after = after.replace('?', ' ');
	    return after;
	}
}
