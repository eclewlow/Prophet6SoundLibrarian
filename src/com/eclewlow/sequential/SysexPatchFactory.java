package com.eclewlow.sequential;

import javax.swing.filechooser.FileNameExtensionFilter;

public class SysexPatchFactory {

	public static AbstractSysexPatch getClosestPatchType(byte[] data, Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			if (data == null)
				return new Prophet6SysexPatch();
			else
				return new Prophet6SysexPatch(data);
		} else if (c == OB6SysexPatch.class) {
			if (data == null)
				return new OB6SysexPatch();
			else
				return new OB6SysexPatch(data);
		} else if (c == Prophet5SysexPatch.class) {
			if (data == null)
				return new Prophet5SysexPatch();
			else
				return new Prophet5SysexPatch(data);
		} else {
			throw new Exception("no class match");
		}
	}

	public static int getUserBankCount(Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return 500;
		} else if (c == OB6SysexPatch.class) {
			return 500;
		} else if (c == Prophet5SysexPatch.class) {
			return 200;
		} else {
			throw new Exception("no class match");
		}
	}

	public static String getSynthName(Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return "Prophet 6";
		} else if (c == OB6SysexPatch.class) {
			return "OB-6";
		} else if (c == Prophet5SysexPatch.class) {
			return "Prophet 5";
		} else {
			throw new Exception("no class match");
		}
	}

	public static FileNameExtensionFilter getProgramFileNameExtensionFilter(Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 6 Program Files (*.p6program)", "p6program");
		} else if (c == OB6SysexPatch.class) {
			return new FileNameExtensionFilter("OB-6 Program Files (*.ob6program)", "ob6program");
		} else if (c == Prophet5SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 5 Program Files (*.p5program)", "p5program");
		} else {
			throw new Exception("no class match");
		}
	}

	public static FileNameExtensionFilter getLibraryFileNameExtensionFilter(Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 6 Library Files (*.p6lib)", "p6lib");
		} else if (c == OB6SysexPatch.class) {
			return new FileNameExtensionFilter("OB-6 Library Files (*.ob6lib)", "ob6lib");
		} else if (c == Prophet5SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 5 Library Files (*.p5lib)", "p5lib");
		} else {
			throw new Exception("no class match");
		}
	}

	public static FileNameExtensionFilter getSysexFileNameExtensionFilter(Class<?> c) throws Exception {
		if (c == Prophet6SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 6 SysEx Files (*.syx)", "syx");
		} else if (c == OB6SysexPatch.class) {
			return new FileNameExtensionFilter("OB-6 SysEx Files (*.syx)", "syx");
		} else if (c == Prophet5SysexPatch.class) {
			return new FileNameExtensionFilter("Prophet 5 SysEx Files (*.syx)", "syx");
		} else {
			throw new Exception("no class match");
		}
	}

}
