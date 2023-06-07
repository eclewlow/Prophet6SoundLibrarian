package com.eclewlow.sequential;

import java.util.Arrays;

public class OB6SysexPatchValidator extends AbstractSysexPatchValidator{

	static final int SYSEX_LENGTH = 1178;
	private static final int SYSEX_BYTE_OFFSET_PATCH_BANK = 4;
	private static final int SYSEX_BYTE_OFFSET_PATCH_PROG = 5;


	public boolean validateSysex(byte[] bytes) {
		boolean result = true;

		if (bytes.length % SYSEX_LENGTH != 0)
			return false;
		for (int i = 0; i < bytes.length / SYSEX_LENGTH; i++) {
			byte[] slice = Arrays.copyOfRange(bytes, i * SYSEX_LENGTH, (i + 1) * SYSEX_LENGTH);
			result = result && validateProgram(slice);
		}
		return result;
	}

	public boolean validateProgram(byte[] bytes) {
		if (bytes.length != SYSEX_LENGTH)
			return false;

		if (bytes[0] != (byte) 0xf0)
			return false;
		if (bytes[1] != (byte) 0x01)
			return false;
		if (bytes[2] != (byte) OB6SysexPatch.SYNTHESIZER_ID)
			return false;
		if (bytes[3] != (byte) 0x02)
			return false;

		byte bankNo = (byte) ((byte) bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] & 0x0F);
		byte progNo = (byte) ((byte) bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] & 0x7F);

		if (!(bankNo >= 0 && bankNo <= 9))
			return false;
		if (!(progNo >= 0 && progNo <= 99))
			return false;
		if (bytes[bytes.length - 1] != (byte) 0xF7)
			return false;
		return true;
	}
}
