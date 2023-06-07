package com.eclewlow.sequential;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Prophet5SysexPatch extends AbstractSysexPatch {
	public byte[] bytes;
	public byte[] packedMIDIData;
	public byte[] inputData;

	static final int SYNTHESIZER_ID = 0x32;
	private static final int SYSEX_BYTE_OFFSET_PATCH_BANK = 4;
	private static final int SYSEX_BYTE_OFFSET_PATCH_PROG = 5;
	private static final int SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA = 6;
	private static final int PROPHET_6_EDIT_BUFFER_LENGTH = 157;
	private static final int SYSEX_EDIT_BUFFER_BYTE_OFFSET_PACKED_MIDI_DATA = 4;
	private static final int SYSEX_PACKED_MIDI_DATA_LENGTH = 152;
	private static final int SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET = 65;
	private static final int SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH = 20;
	public static final byte[] INIT_PATCH_BYTES = new byte[] { (byte) 0xf0, (byte) 0x1, (byte) 0x32, (byte) 0x2,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x19, (byte) 0x19, (byte) 0x18, (byte) 0x1, (byte) 0x0,
			(byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x3f, (byte) 0x41, (byte) 0x0, (byte) 0x0,
			(byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x7f, (byte) 0x7f, (byte) 0x0, (byte) 0x29, (byte) 0x1,
			(byte) 0x2, (byte) 0x0, (byte) 0x0, (byte) 0x50, (byte) 0x12, (byte) 0x0, (byte) 0x1, (byte) 0x0,
			(byte) 0x0, (byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x40, (byte) 0x1, (byte) 0x0, (byte) 0x4e,
			(byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x15, (byte) 0x14, (byte) 0x57, (byte) 0x51, (byte) 0x1d,
			(byte) 0x72, (byte) 0x0, (byte) 0x5e, (byte) 0x54, (byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x7f, (byte) 0x0, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
			(byte) 0x7f, (byte) 0x0, (byte) 0x7f, (byte) 0x7f, (byte) 0x49, (byte) 0x74, (byte) 0x27, (byte) 0x73,
			(byte) 0x20, (byte) 0x0, (byte) 0x61, (byte) 0x20, (byte) 0x50, (byte) 0x72, (byte) 0x6f, (byte) 0x70,
			(byte) 0x68, (byte) 0x0, (byte) 0x65, (byte) 0x74, (byte) 0x20, (byte) 0x35, (byte) 0x20, (byte) 0x20,
			(byte) 0x20, (byte) 0x0, (byte) 0x20, (byte) 0x0, (byte) 0x6, (byte) 0x0, (byte) 0x7f, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x3c, (byte) 0x0, (byte) 0x1, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0,
			(byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xf7 };

	public Prophet5SysexPatch(byte[] bytes) {
		this.bytes = bytes.clone();
		this.packedMIDIData = Arrays.copyOfRange(bytes, SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA,
				SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA + SYSEX_PACKED_MIDI_DATA_LENGTH);
		this.inputData = MidiDataFormat.unpackMIDIData(this.packedMIDIData);
	}

	public Prophet5SysexPatch() {
		this(INIT_PATCH_BYTES);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		Prophet5SysexPatch patch = (Prophet5SysexPatch) new Prophet5SysexPatch(this.bytes);
		return patch;
	}

	public byte[] getPatchAuditionBytes() {
		byte[] auditionData = new byte[PROPHET_6_EDIT_BUFFER_LENGTH];
		auditionData[0] = (byte) 0xf0;
		auditionData[1] = (byte) 0x01;
		auditionData[2] = (byte) SYNTHESIZER_ID;
		auditionData[3] = (byte) 0x03;
		System.arraycopy(this.packedMIDIData, 0, auditionData, SYSEX_EDIT_BUFFER_BYTE_OFFSET_PACKED_MIDI_DATA,
				SYSEX_PACKED_MIDI_DATA_LENGTH);
		auditionData[PROPHET_6_EDIT_BUFFER_LENGTH - 1] = (byte) 0xf7;
		return auditionData;
	}

	public void setPatchName(String s) {
		assert s.length() <= SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH;

		while (s.length() < SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH) {
			s = s + " ";
		}

		byte[] nameBytes = s.getBytes(StandardCharsets.UTF_8);

		System.arraycopy(nameBytes, 0, this.inputData, SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET,
				SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH);

		this.packedMIDIData = MidiDataFormat.packMidiData(this.inputData);

		System.arraycopy(this.packedMIDIData, 0, this.bytes, SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA,
				SYSEX_PACKED_MIDI_DATA_LENGTH);
	}

	public String getPatchName() {
		byte[] patchNameBytes = Arrays.copyOfRange(inputData, SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET,
				SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET + SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH);

		String s = new String(patchNameBytes, StandardCharsets.UTF_8);
		return s;
	}

	public int getPatchBank() {
		return (int) bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] & 0x0F;
	}

	public int getPatchProg() {
		return (int) bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] & 0x7F;
	}

	public String toString() {
		return getBankProgPretty() + " " + getPatchName().replaceAll("\\s+$", "");
	}

	public void setPatchBank(int bankNo) {
		assert bankNo >= 0 && bankNo <= 9;
		bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] = (byte) ((byte) bankNo & 0x0F);
	}

	public void setPatchProg(int progNo) {
		assert progNo >= 0 && progNo <= 39;
		bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] = (byte) ((byte) progNo & 0x7F);
	}

	public String getBankProgPretty() {
//		return getPatchBank() + " " + getPatchProg();
		return "" + (getPatchBank() + 1) + "" + (getPatchProg() / 8 + 1) + "" + (getPatchProg() % 8 + 1);
	}

	public byte[] getBytes() {
		return this.bytes.clone();
	}

}
