package com.eclewlow.sequential;

import java.util.Arrays;

public class Prophet6SysexPatchValidator extends AbstractSysexPatchValidator{

	private static final int PROPHET_6_SYSEX_LENGTH = 1178;
	private static final int SYSEX_BYTE_OFFSET_PATCH_BANK = 4;
	private static final int SYSEX_BYTE_OFFSET_PATCH_PROG = 5;


	public boolean validateSysex(byte[] bytes) {
		boolean result = true;

		if (bytes.length % PROPHET_6_SYSEX_LENGTH != 0)
			return false;
		for (int i = 0; i < bytes.length / PROPHET_6_SYSEX_LENGTH; i++) {
			byte[] slice = Arrays.copyOfRange(bytes, i * PROPHET_6_SYSEX_LENGTH, (i + 1) * PROPHET_6_SYSEX_LENGTH);
			result = result && validateP6Program(slice);
		}
		return result;
	}

	public boolean validateP6Program(byte[] bytes) {
		if (bytes.length != PROPHET_6_SYSEX_LENGTH)
			return false;

		if (bytes[0] != (byte) 0xf0)
			return false;
		if (bytes[1] != (byte) 0x01)
			return false;
		if (bytes[2] != (byte) 0x2d)
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
