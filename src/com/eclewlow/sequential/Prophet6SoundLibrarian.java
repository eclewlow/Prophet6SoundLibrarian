package com.eclewlow.sequential;

public class Prophet6SoundLibrarian {
	public static void main(String[] args) {
		new SoundLibrarian(Prophet6SysexPatch.class, 500, Prophet6SysexPatchValidator.SYSEX_LENGTH);
	}
}
