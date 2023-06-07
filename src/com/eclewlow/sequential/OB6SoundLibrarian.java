package com.eclewlow.sequential;

public class OB6SoundLibrarian {
	public static void main(String[] args) {
		new SoundLibrarian(OB6SysexPatch.class, 500, OB6SysexPatchValidator.SYSEX_LENGTH);
	}
}
