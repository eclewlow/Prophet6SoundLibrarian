package com.eclewlow.sequential;

public class SysexPatchValidator extends AbstractSysexPatchValidator {

	public boolean validateSysex(byte[] bytes) {
		return false;
	};

	public Class<?> getPatchClass(byte[] bytes) throws Exception {
		Prophet6SysexPatchValidator p6validator = new Prophet6SysexPatchValidator();
		if (p6validator.validateSysex(bytes)) {
			return Prophet6SysexPatch.class;
		}

		throw new Exception("Class not found");
	}
}
