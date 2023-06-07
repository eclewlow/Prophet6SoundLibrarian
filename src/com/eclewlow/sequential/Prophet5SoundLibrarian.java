package com.eclewlow.sequential;

public class Prophet5SoundLibrarian {
	public static void main(String[] args) {
		new SoundLibrarian(Prophet5SysexPatch.class, 200, Prophet5SysexPatchValidator.SYSEX_LENGTH);
	}
}
