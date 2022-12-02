package com.eclewlow.sequential;

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiDeviceTransmitter;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiException;
import uk.co.xfactorylibrarians.coremidi4j.CoreMidiNotification;

public class SysexIOManager {
	private static SysexIOManager instance = null;

	MidiDevice inDevice = null;
	MidiDevice outDevice = null;
	MidiDeviceTransmitter transmitter = null;

	private byte[] readBytes;

	public boolean isConnected() {
		return inDevice != null && outDevice != null;
	}

	public void setReadBytes(byte[] bytes) {
		this.readBytes = bytes;
	}

	public byte[] getReadBytes() {
		return this.readBytes;
	}

	public static final byte[] SYSEX_MSG_DUMP_REQUEST = { (byte) 0xF0, 0x01, 0b00101101, 0b00000101, 0, 0,
			(byte) 0b11110111 };

	private SysexIOManager() {
		super();
		try {
			for (javax.sound.midi.MidiDevice.Info device : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
				System.out.println("  " + device);
			}

			if (SysexIOManager.isCoreMidiLoaded()) {
				System.out.println("CoreMIDI4J native library is running.");
			} else {
				System.out.println("CoreMIDI4J native library is not available.");
			}

			SysexIOManager.watchForMidiChanges();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static SysexIOManager getInstance() {
		if (instance == null)
			instance = new SysexIOManager();

		return instance;
	}

	public void setInDevice(MidiDevice in) {
		inDevice = in;
	}

	public void setOutDevice(MidiDevice out) throws Exception {
		if (out == null && outDevice != null && outDevice.isOpen())
			outDevice.close();
		outDevice = out;

		if (out == null) {
			if (this.transmitter != null) {
				this.transmitter.setReceiver(null);
				this.transmitter.close();
			}
			this.transmitter = null;
			return;
		}
	}

	public void cleanupTransmitter() {
		if (transmitter != null) {
			transmitter.setReceiver(null);
			transmitter.close();
		}
		this.transmitter = null;
	}

	public void dumpRequest(int bankNo, int progNo) throws Exception {
		if (bankNo < 0 || bankNo > 9)
			throw new Exception("invalid bank number");
		if (progNo < 0 || progNo > 99)
			throw new Exception("invalid prog number");

		byte bankNumber = (byte) bankNo;
		byte progNumber = (byte) progNo;

		byte[] msg = new byte[] { (byte) 0xF0, 0x01, 0b00101101, 0b00000101, bankNumber, progNumber,
				(byte) 0b11110111 };

		try {
			if (outDevice == null)
				throw new Exception("Midi Device not instantiated");

			if (!outDevice.isOpen())
				outDevice.open();

			this.transmitter = (MidiDeviceTransmitter) outDevice.getTransmitter();

			TransmitterReceiver tr = new TransmitterReceiver();

			transmitter.setReceiver(null);
			transmitter.setReceiver(tr);

			setReadBytes(null);

			send(msg);

		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}

	class TransmitterReceiver implements Receiver {

		public TransmitterReceiver() {
		}

		@Override
		public void send(MidiMessage message, long timeStamp) {

			if (message instanceof SysexMessage) {

				byte[] msg = message.getMessage();

				synchronized (SysexIOManager.getInstance()) {
					try {
						setReadBytes(msg);

						if (transmitter != null) {
							transmitter.setReceiver(null);
							transmitter.close();
						}
						transmitter = null;

						SysexIOManager.getInstance().notify();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public void close() {
		}

	}

	public void send(byte[] msg) throws Exception {
		if (inDevice == null)
			throw new Exception("Midi Device not instantiated");

		long timeStamp = -1;
		SysexMessage sysexMsg = new SysexMessage();
		sysexMsg.setMessage(msg, msg.length);

		inDevice.open();

		MidiDeviceReceiver receiver = (MidiDeviceReceiver) inDevice.getReceiver();

		receiver.send(sysexMsg, timeStamp);

		inDevice.close();

	}

	public static boolean isCoreMidiLoaded() throws CoreMidiException {
		return CoreMidiDeviceProvider.isLibraryLoaded();
	}

	public void rescanDevices() throws Exception {

		setInDevice(null);
		setOutDevice(null);

		for (javax.sound.midi.MidiDevice.Info device : CoreMidiDeviceProvider.getMidiDeviceInfo()) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(device);
				if (device.getName().equals("CoreMIDI4J - Prophet 6")
						&& device.getVendor().equals("Dave Smith Instruments")) {
					if (md.getMaxReceivers() == -1) {
						setInDevice(md);
					}
					if (md.getMaxTransmitters() == -1) {
						setOutDevice(md);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (isConnected()) {
				notifyObservers("CONNECTED", "Prophet 6");
			} else {
				notifyObservers("DISCONNECTED", "No Device");
			}
		}
	}

	public static void watchForMidiChanges() throws CoreMidiException {
		CoreMidiDeviceProvider.addNotificationListener(new CoreMidiNotification() {
			public void midiSystemUpdated() {
				System.out.println("The MIDI environment has changed.");
				try {
					SysexIOManager.getInstance().rescanDevices();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private final List<Observer> observers = new ArrayList<>();

	private void notifyObservers(String status, String target) {
		observers.forEach(observer -> observer.update(status, target));
	}

	public void addObserver(Observer observer) {
		observers.add(observer);
	}

	public void removeObserver(Observer observer) {
		observers.remove(observer);
	}
}
