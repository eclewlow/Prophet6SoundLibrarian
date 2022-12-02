package com.eclewlow.sequential;

public abstract class AbstractSysexPatch implements Cloneable {
	public abstract Object clone() throws CloneNotSupportedException;

	public abstract byte[] getPatchAuditionBytes();

	public abstract void setPatchName(String s);

	public abstract String getPatchName();

	public abstract int getPatchBank();

	public abstract int getPatchProg();

	public abstract String toString();

	public abstract String getBankProgPretty();

	public abstract void setPatchBank(int bankNo);

	public abstract void setPatchProg(int progNo);
	
	public abstract byte[] getBytes();
}
