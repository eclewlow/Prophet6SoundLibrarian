package com.eclewlow.sequential;

public class SysexPatchFactory {

	public static AbstractSysexPatch getClosestPatchType(byte[] data, Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return new Prophet6SysexPatch(data);
		} else {
			throw new Exception("no class match");
		}
	}
}
