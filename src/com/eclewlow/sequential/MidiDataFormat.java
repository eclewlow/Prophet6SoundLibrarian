package com.eclewlow.sequential;

import java.nio.ByteBuffer;
import java.lang.Math;

public class MidiDataFormat {
	public static byte[] packMidiData(byte[] unpackedMidiIData) {
		int headerCount = (int) Math.ceil((double) unpackedMidiIData.length / 7);
		int packedMidiDataLength = unpackedMidiIData.length + headerCount;
		byte[] packedMidiData = new byte[packedMidiDataLength];

		ByteBuffer bb = ByteBuffer.wrap(packedMidiData);

		for (int i = 0; i < headerCount; i++) {
			int frameSize = i < unpackedMidiIData.length / 7 ? 7 : unpackedMidiIData.length % 7;

			byte header = (byte) 0x00;

			for (int j = 0; j < frameSize; j++) {
				header |= (byte) ((unpackedMidiIData[j] & 0x80) >> (7 - j));
			}

			bb.put(header);

			for (int j = 0; j < frameSize; j++) {
				bb.put((byte) (unpackedMidiIData[j] & 0x7F));
			}
		}
		return bb.array();
	}

	public static byte[] unpackMIDIData(byte[] packedMidiData) {
		int headerCount = (int) Math.ceil((double) packedMidiData.length / 8);
		int unpackedMidiDataLength = packedMidiData.length - headerCount;

		byte[] unpackedMidiData = new byte[unpackedMidiDataLength];

		ByteBuffer bb = ByteBuffer.wrap(unpackedMidiData);

		for (int i = 0; i < headerCount; i++) {
			int frameSize = i < unpackedMidiDataLength / 7 ? 7 : unpackedMidiDataLength % 7;

			byte header = (byte) packedMidiData[i * 8];

			for (int j = 0; j < frameSize; j++) {
				bb.put((byte) (((header & (0x01 << j)) << (7 - j)) | packedMidiData[i * 8 + j + 1]));
			}
		}
		return bb.array();
	}
}
