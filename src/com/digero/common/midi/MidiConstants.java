package com.digero.common.midi;

public interface MidiConstants
{
	int META_TEXT = 0x01;
	int META_COPYRIGHT = 0x02;
	int META_TRACK_NAME = 0x03;
	int META_INSTRUMENT = 0x04;
	int META_PROGRAM_NAME = 0x08;
	int META_END_OF_TRACK = 0x2F;
	int META_TEMPO = 0x51;
	int META_TIME_SIGNATURE = 0x58;
	int META_KEY_SIGNATURE = 0x59;
	int META_PORT_CHANGE = 0x21;

	byte CHANNEL_VOLUME_CONTROLLER_COARSE = 0x07;
	byte CHANNEL_VOLUME_CONTROLLER_FINE = 0x27;
	byte CHANNEL_EXPRESSION_CONTROLLER = 0x0B;
	int ALL_CONTROLLERS_OFF = 0x79;
	int REGISTERED_PARAMETER_NUMBER_MSB = 0x65;
	int REGISTERED_PARAMETER_NUMBER_LSB = 0x64;
	int DATA_ENTRY_COARSE = 0x06;
	int DATA_ENTRY_FINE = 0x26;
	int BANK_SELECT_MSB = 0x00;//cc data 1
	int BANK_SELECT_LSB = 0x20;//cc data 1
	int REGISTERED_PARAM_PITCH_BEND_RANGE = 0x0000;
	int REGISTERED_PARAM_NONE = 0x3FFF;

	int DRUM_CHANNEL = 9;
	int CHANNEL_COUNT = 16;
	int PORT_COUNT = 16;
	int LOWEST_NOTE_ID = 0;
	int HIGHEST_NOTE_ID = 127;
	int NOTE_COUNT = HIGHEST_NOTE_ID - LOWEST_NOTE_ID + 1;
	int MAX_VOLUME = 127;
	int PAN_CENTER = 64;

	byte PAN_CONTROL = 0x0A;
	byte REVERB_CONTROL = 0x5B;
	byte TREMOLO_CONTROL = 0x5C;
	byte CHORUS_CONTROL = 0x5D;
	byte DETUNE_CONTROL = 0x5E;
	byte PHASER_CONTROL = 0x5F;
	byte SYSEX_UNIVERSAL_REALTIME = 0x7F;
	byte SYSEX_UNIVERSAL_NON_REALTIME = 0x7E;

	int DEFAULT_TEMPO_BPM = 120;
	int DEFAULT_TEMPO_MPQ = 500000;
	int DEFAULT_INSTRUMENT = 0;
	int DEFAULT_CHANNEL_VOLUME = 100;
	int DEFAULT_PITCH_BEND_RANGE_SEMITONES = 2;
	int DEFAULT_PITCH_BEND_RANGE_CENTS = 0;
}
