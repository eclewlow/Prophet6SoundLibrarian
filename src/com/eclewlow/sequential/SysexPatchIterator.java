package com.eclewlow.sequential;

import java.util.Arrays;
import java.util.Iterator;

public class SysexPatchIterator implements Iterator<byte[]> {

	byte[] data;
	int cursor;

	public SysexPatchIterator(byte[] data) {
		this.data = data.clone();
		this.cursor = 0;
	}

	public int getNextIndexOf(byte target) {
		int offset = this.cursor;
		while (offset < this.data.length) {
			if (this.data[offset] == (byte) target)
				return offset;
			offset++;
		}
		return this.data.length;
	}

	@Override
	public boolean hasNext() {
		int nextStartByte = getNextIndexOf((byte) 0xf0);
		int nextEndByte = getNextIndexOf((byte) 0xf7);

		if (nextStartByte < this.data.length && nextEndByte < this.data.length) {
			return true;
		}
		return false;
	}

	@Override
	public byte[] next() {
		if (hasNext()) {
			int nextStartByte = getNextIndexOf((byte) 0xf0);
			int nextEndByte = getNextIndexOf((byte) 0xf7);

			byte[] result = Arrays.copyOfRange(this.data, nextStartByte, nextEndByte + 1);

			this.cursor = nextEndByte + 1;

			return result;
		}
		else
			return null;
	}

}
