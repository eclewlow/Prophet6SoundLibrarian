package com.eclewlow.sequential;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import java.awt.print.*;
import java.awt.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.event.FocusEvent.Cause;
import java.awt.font.LineMetrics;

public class SoundLibrarian {

	public static final String APP_VERSION = "1.3.1";

	public SoundLibrarianMainFrame mainFrame;
	public SoundLibrarianMergeFrame mergeFrame;

	public class SoundLibrarianMainFrame extends JFrame {
		private static final long serialVersionUID = 1L;
		JButton openButton;
		JButton printButton;
		JFrame frame;
		NameField nameField;
		JLabel connectionStatusLabel;
		JLabel deviceLabel;

		JButton sendButton;
		JButton receiveButton;
		JButton sendAllButton;
		JButton receiveAllButton;
		JButton auditionSendButton;

		DragDropList ddl;
		NameFieldFocusListener nffl = new NameFieldFocusListener();

		public SoundLibrarianMainFrame(String arg0) {
			super(arg0);
		}

		class MyListDropHandler extends TransferHandler {
			private static final long serialVersionUID = 1L;
			DragDropList list;

			public MyListDropHandler(DragDropList list) {
				this.list = list;
			}

			protected Transferable createTransferable(JComponent c) {
				StringSelection transferable = new StringSelection(Integer.toString(list.getSelectedRow()));

				return transferable;
			}

			public int getSourceActions(JComponent c) {
				return COPY_OR_MOVE;
			}

			public boolean canImport(TransferHandler.TransferSupport support) {
				UIManager.put("Table.dropLineColor", Color.CYAN);
				UIManager.put("Table.dropLineShortColor", Color.CYAN);

				if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)
						&& !support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					return false;
				}

				JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();

				if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
					UIManager.put("Table.dropLineColor", new Color(0xffffff, true));
					UIManager.put("Table.dropLineShortColor", new Color(0xffffff, true));
				}

				if (dl.getRow() == -1) {
					return false;
				} else {
					return true;
				}
			}

			@SuppressWarnings("unchecked")
			public boolean importData(TransferHandler.TransferSupport support) {
				if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {

					DragDropList dpl = (DragDropList) support.getComponent();
					SysexTableItemModel dlm = (SysexTableItemModel) dpl.getModel();

					JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
					int dropTargetIndex = dl.getRow();

					int i = dropTargetIndex;

					try {

						List<File> files = (List<File>) support.getTransferable()
								.getTransferData(DataFlavor.javaFileListFlavor);

						List<AbstractSysexPatch> l = dlm.getPatches();

						for (File f : files) {

							if (i >= dlm.getRowCount() || i < 0)
								break;

							FileInputStream fis = new FileInputStream(f);

							byte[] buf = fis.readAllBytes();

							fis.close();

							SysexPatchValidator validator = new SysexPatchValidator();

							Class<?> c = validator.getSinglePatchClass(buf);

							if (c != SoundLibrarian.this.sysexPatchClass) {
//								throw new Exception("Invalid file data");
								continue;
							}

							AbstractSysexPatch patch = SysexPatchFactory.getClosestPatchType(buf,
									SoundLibrarian.this.sysexPatchClass);

							l.set(i++, patch);

						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					dlm.fireTableDataChanged();

					if (i > dropTargetIndex)
						ddl.addRowSelectionInterval(dropTargetIndex, i - 1);

					return true;
				}

				if (!canImport(support)) {
					return false;
				}

				try {
					JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
					int dropTargetIndex = dl.getRow();

					DragDropList dpl = (DragDropList) support.getComponent();
					SysexTableItemModel dlm = (SysexTableItemModel) dpl.getModel();

					int[] selectedRows = dpl.getSelectedRows();

					List<AbstractSysexPatch> patches = dlm.getPatches();

					List<AbstractSysexPatch> selectedObjects = new ArrayList<AbstractSysexPatch>();
					List<AbstractSysexPatch> selectedObjectClones = new ArrayList<AbstractSysexPatch>();

					for (int i = 0; i < selectedRows.length; i++) {
						AbstractSysexPatch patch = patches.get(selectedRows[i]);
						selectedObjects.add(patch);
						selectedObjectClones.add((AbstractSysexPatch) patch.clone());
					}
					patches.addAll(dropTargetIndex, selectedObjectClones);
					patches.removeAll(selectedObjects);

					dlm.fireTableDataChanged();

					int cloneIndex = patches.indexOf(selectedObjectClones.get(0));
					ddl.addRowSelectionInterval(cloneIndex, cloneIndex + selectedObjectClones.size() - 1);

				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		}

		public class DragDropList extends JTable implements ClipboardOwner {
			private static final long serialVersionUID = 1L;
			SysexTableItemModel model;

			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component returnComp = super.prepareRenderer(renderer, row, column);
				Color alternateColor = new Color(252, 242, 206);
				Color whiteColor = Color.WHITE;

				if (!returnComp.getBackground().equals(getSelectionBackground())) {
					Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
					returnComp.setBackground(bg);
					bg = null;
				}

				JComponent jcomp = (JComponent) returnComp;
				jcomp.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

				return returnComp;
			};

			public DragDropList() {
				super(new SysexTableItemModel());
				model = (SysexTableItemModel) getModel();

				setDropMode(DropMode.INSERT_ROWS);
				setFillsViewportHeight(true);
				getTableHeader().setReorderingAllowed(false);

				setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

				setDragEnabled(true);
				setTransferHandler(new MyListDropHandler(this));

				getColumnModel().getColumn(0).setPreferredWidth(60);
				getColumnModel().getColumn(0).setMinWidth(60);
				getColumnModel().getColumn(1).setPreferredWidth(600);

				getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "table-shift-tab");
				getActionMap().put("table-shift-tab", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
						manager.focusPreviousComponent();
					}
				});
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "table-tab");
				getActionMap().put("table-tab", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
						manager.focusNextComponent();
					}
				});

				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "table-row-delete");
				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "table-row-delete");
				getActionMap().put("table-row-delete", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						int[] selectedRows = getSelectedRows();
						if (selectedRows.length == 0)
							return;

						SysexTableItemModel dlm = (SysexTableItemModel) getModel();
						List<AbstractSysexPatch> patches = dlm.getPatches();

						int n = JOptionPane.showConfirmDialog(mainFrame,
								"The selected program(s) will be initialized, continue?", "Initialize Program",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
						if (n == JOptionPane.OK_OPTION) {
							for (int i = 0; i < selectedRows.length; i++) {
								try {
									AbstractSysexPatch initPatch = SysexPatchFactory.getClosestPatchType(null,
											SoundLibrarian.this.sysexPatchClass);

									patches.set(selectedRows[i], initPatch);
								} catch (Exception ex) {
									ex.printStackTrace();
								}

							}
							dlm.fireTableDataChanged();
							for (int i = 0; i < selectedRows.length; i++) {
								ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
							}
						} else if (n == JOptionPane.CANCEL_OPTION) {

						}
					}
				});

				getSelectionModel().addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {

						SoundLibrarianMenuBar menuBar = (SoundLibrarianMenuBar) getJMenuBar();

						if (!e.getValueIsAdjusting()) {
							int[] selectedRows = getSelectedRows();
							if (selectedRows.length > 1) {
								AbstractSysexPatch patch = model.getPatchAt(selectedRows[0]);
								nameField.setCurrentIndex(selectedRows[0]);
								nameField.setText(patch.getPatchName().replaceAll("\\s+$", ""));
								nameField.setEnabled(true);
								menuBar.menuItemLoadProgram.setEnabled(false);
								menuBar.menuItemSaveProgram.setEnabled(true);
								if (SysexIOManager.getInstance(SoundLibrarian.this.sysexPatchClass).isConnected()) {
									sendButton.setEnabled(true);
									receiveButton.setEnabled(true);
									auditionSendButton.setEnabled(true);
								}
							} else if (selectedRows.length == 1) {
								AbstractSysexPatch patch = model.getPatchAt(selectedRows[0]);
								nameField.setCurrentIndex(selectedRows[0]);
								nameField.setText(patch.getPatchName().replaceAll("\\s+$", ""));
								nameField.setEnabled(true);
								menuBar.menuItemLoadProgram.setEnabled(true);
								menuBar.menuItemSaveProgram.setEnabled(true);
								if (SysexIOManager.getInstance(SoundLibrarian.this.sysexPatchClass).isConnected()) {
									sendButton.setEnabled(true);
									receiveButton.setEnabled(true);
									auditionSendButton.setEnabled(true);
								}
							} else if (selectedRows.length == 0) {
								nameField.setCurrentIndex(-1);
								nameField.setText("");
								nameField.setEnabled(false);
								menuBar.menuItemLoadProgram.setEnabled(false);
								menuBar.menuItemSaveProgram.setEnabled(false);
								sendButton.setEnabled(false);
								receiveButton.setEnabled(false);
								auditionSendButton.setEnabled(false);

							}
						}
					}
				});
				getInputMap().put(KeyStroke.getKeyStroke("meta C"), "copy");
				getActionMap().put("copy", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {

						Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();

						SysexTableItemModel dlm = (SysexTableItemModel) getModel();
						List<AbstractSysexPatch> patches = dlm.getPatches();
						int selectedRow = getSelectedRow();

						SysexPatchSelection selection = new SysexPatchSelection(patches.get(selectedRow));
						c.setContents(selection, DragDropList.this);
					}
				});
				getInputMap().put(KeyStroke.getKeyStroke("meta V"), "paste");
				getActionMap().put("paste", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {

						Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
						Transferable content = clipboard.getContents(DragDropList.this);

						boolean hasTransferable = (content != null)
								&& content.isDataFlavorSupported(SysexPatchSelection.dmselFlavor);
						if (hasTransferable) {
							try {
								AbstractSysexPatch result = (AbstractSysexPatch) content
										.getTransferData(SysexPatchSelection.dmselFlavor);

								AbstractSysexPatch clone = (AbstractSysexPatch) result.clone();
								SysexTableItemModel dlm = (SysexTableItemModel) getModel();
								List<AbstractSysexPatch> patches = dlm.getPatches();
								int selectedRow = getSelectedRow();
								patches.set(selectedRow, clone);
								dlm.fireTableDataChanged();
								ddl.addRowSelectionInterval(selectedRow, selectedRow);

							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
					}
				});
			}

			@Override
			public void lostOwnership(Clipboard clipboard, Transferable contents) {
				System.out.println("DragDropList Clipboard: Lost ownership");
			}

		}

		public class NameField extends JTextField {
			private static final long serialVersionUID = 1L;
			public int currentIndex = -1;

			public void setCurrentIndex(int index) {
				this.currentIndex = index;
			}

			public int getCurrentIndex() {
				return this.currentIndex;
			}
		}

		public class NameFieldFocusListener implements FocusListener {

			public int lastCaretPosition = -1;

			@Override
			public void focusGained(FocusEvent e) {
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (e.getCause() == Cause.TRAVERSAL_FORWARD || e.getCause() == Cause.TRAVERSAL_BACKWARD) {
					if (nameField.getCurrentIndex() != -1) {
						SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
						List<AbstractSysexPatch> l = model.getPatches();
						int[] selectedRows = ddl.getSelectedRows();

						l.get(nameField.getCurrentIndex()).setPatchName(nameField.getText());
						model.fireTableDataChanged();

						for (int i = 0; i < selectedRows.length; i++) {
							ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
						}

					}
				} else if (e.getCause() == Cause.UNKNOWN) {

					if (nameField.getCurrentIndex() != -1) {
						int currentIndex = nameField.getCurrentIndex();

						int[] selectedRows = ddl.getSelectedRows();

						SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
						List<AbstractSysexPatch> l = model.getPatches();

						l.get(currentIndex).setPatchName(nameField.getText());
						model.fireTableDataChanged();

						for (int i = 0; i < selectedRows.length; i++) {
							ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
						}
					}
				}
			}

			public void setLastCaretPosition(int pos) {
				this.lastCaretPosition = pos;
			}

			public int getLastCaretPosition() {
				return this.lastCaretPosition;
			}

		}

		private JPanel createEditor() {
			JPanel panel = new JPanel(new GridBagLayout());

			GridBagConstraints c = new GridBagConstraints();

			nameField = new NameField();
			AbstractDocument ad = (AbstractDocument) nameField.getDocument();

			nameField.addFocusListener(nffl);
			nameField.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();
					int selectedRow = ddl.getSelectedRow();
					int[] selectedRows = ddl.getSelectedRows();
					l.get(selectedRow).setPatchName(((JTextField) e.getSource()).getText());
					model.fireTableDataChanged();

					for (int i = 0; i < selectedRows.length; i++) {
						ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
					}
				}
			});
			ad.setDocumentFilter(new DocumentFilter() {
				@Override
				public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
						throws BadLocationException {

					if (fb.getDocument().getLength() + string.length() > SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH) {
						return;
					}
					super.insertString(fb, offset, string, attr);
				}

				@Override
				public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text,
						AttributeSet attrs) throws BadLocationException {
					int documentLength = fb.getDocument().getLength();
					if (documentLength - length + text.length() <= SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH)
						super.replace(fb, offset, length, text, attrs);
				}
			});
			nameField.setFont(new Font("Verdana", Font.PLAIN, 11));

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 1.0;
			c.weighty = 0.0;

			panel.add(nameField, c);

			TitledBorder tb = BorderFactory.createTitledBorder("Patch Name");
			tb.setTitleFont(new Font("Verdana", Font.PLAIN, 11));
			panel.setBorder(tb);
			return panel;

		}

		public JScrollPane createDragDropList() {
			this.ddl = new DragDropList();
			return new JScrollPane(this.ddl);
		}

		public void setConnection(String connectionStatus, String device) {
			if (connectionStatus.equals("DISCONNECTED") || device.equals("No Device")) {
				connectionStatusLabel.setText("DISCONNECTED");
				connectionStatusLabel.setForeground(Color.WHITE);
				connectionStatusLabel.setBackground(Color.BLACK);

				deviceLabel.setText("No Device");
				setTransferAreaEnabled(false);
			} else {
				connectionStatusLabel.setText(connectionStatus);
				connectionStatusLabel.setForeground(Color.BLACK);
				connectionStatusLabel.setBackground(Color.WHITE);

				deviceLabel.setText(device);
				setTransferAreaEnabled(true);
			}
		}

		public void setTransferAreaEnabled(boolean enabled) {
			sendAllButton.setEnabled(enabled);
			receiveAllButton.setEnabled(enabled);
			if (!enabled) {
				sendButton.setEnabled(enabled);
				receiveButton.setEnabled(enabled);
				auditionSendButton.setEnabled(enabled);
			} else if (SysexIOManager.getInstance(SoundLibrarian.this.sysexPatchClass).isConnected()
					&& mainFrame.ddl.getSelectedRowCount() > 0) {
				sendButton.setEnabled(enabled);
				receiveButton.setEnabled(enabled);
				auditionSendButton.setEnabled(enabled);
			}
		}

		public JPanel createTransferArea() {

			JPanel panel = new JPanel(new GridBagLayout());

			panel.setBorder(new EmptyBorder(5, 0, 0, 0));

			GridBagConstraints c = new GridBagConstraints();

			JPanel sequentialPanel = new JPanel();

			ImageIcon sequentialIcon = null;

			if (SoundLibrarian.this.sysexPatchClass == Prophet6SysexPatch.class)
				sequentialIcon = new ImageIcon(getClass().getResource("prophet6-small-black.png"));
			else if (SoundLibrarian.this.sysexPatchClass == OB6SysexPatch.class)
				sequentialIcon = new ImageIcon(getClass().getResource("ob6-small-black.png"));

			JLabel sequentialLabel = new JLabel(sequentialIcon);

			sequentialPanel.add(sequentialLabel);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 3;
			c.gridwidth = 1;
			c.weightx = 0.0;
			c.weighty = 0.0;
			c.insets = new Insets(0, 10, 0, 20);
			c.anchor = GridBagConstraints.BASELINE;

			panel.add(sequentialPanel, c);

			final int CONNECTION_PREFERRED_WIDTH = 105;
			final int CONNECTION_PREFERRED_HEIGHT = 17;

			connectionStatusLabel = new JLabel("DISCONNECTED");

			Border blackline = BorderFactory.createLineBorder(Color.black);
			Border margin = new EmptyBorder(0, 6, 0, 6);
			connectionStatusLabel.setBorder(new CompoundBorder(blackline, margin));
			connectionStatusLabel
					.setPreferredSize(new Dimension(CONNECTION_PREFERRED_WIDTH, CONNECTION_PREFERRED_HEIGHT));

			connectionStatusLabel.setFont(new Font("Verdana", Font.PLAIN, 11));
			connectionStatusLabel.setVerticalAlignment(SwingConstants.BOTTOM);
			connectionStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
			connectionStatusLabel.setOpaque(true);
			connectionStatusLabel.setForeground(Color.WHITE);
			connectionStatusLabel.setBackground(Color.BLACK);

			deviceLabel = new JLabel("No Device");
			deviceLabel.setFont(new Font("Verdana", Font.PLAIN, 11));
			deviceLabel.setHorizontalAlignment(JLabel.CENTER);

			deviceLabel.setPreferredSize(new Dimension(CONNECTION_PREFERRED_WIDTH, CONNECTION_PREFERRED_HEIGHT));

			SysexIOManager.getInstance(SoundLibrarian.this.sysexPatchClass).addObserver(new Observer() {

				@Override
				public void update(String status, String target) {

					if (status.equals("DISCONNECTED") || status.equals("CONNECTED"))
						setConnection(status, target);
				}
			});

			c.fill = GridBagConstraints.NONE;
			c.gridx = 1;
			c.gridy = 1;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			panel.add(connectionStatusLabel, c);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 1;
			c.gridy = 2;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			panel.add(deviceLabel, c);

			final int BUTTON_PREFERRED_WIDTH = 99;
			final int BUTTON_PREFERRED_HEIGHT = 29;

			sendButton = new JButton("SEND");
			sendButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			sendButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			sendButton.setFocusable(false);
			sendButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();

					SoundLibrarianProgressDialog dialog = new SoundLibrarianProgressDialog(selectedRows.length);
					dialog.setRunnable(new Runnable() {

						@Override
						public void run() {

							try {
								if (selectedRows.length == 0)
									throw new Exception("No rows selected");

								SysexIOManager sysexManager = SysexIOManager
										.getInstance(SoundLibrarian.this.sysexPatchClass);

								for (int i = 0; i < selectedRows.length; i++) {

									dialog.setProgressBarValue(i + 1);
									dialog.setProgressText("Sending..." + (i + 1) + " / " + selectedRows.length);

									sysexManager.send(l.get(selectedRows[i]).getBytes());
									Thread.sleep(SYSEX_SEND_DELAY_TIME);
								}

							} catch (Exception ex) {
								ex.printStackTrace();
								if (dialog.isVisible())
									dialog.setVisible(false);
								JOptionPane.showMessageDialog(null, ex.getMessage(), "Error sending patch(es)",
										JOptionPane.ERROR_MESSAGE);
							} finally {
								ddl.clearSelection();

								for (int i = 0; i < selectedRows.length; i++)
									ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
							}
						}
					});
					dialog.showAndRun();
				}
			});

			receiveButton = new JButton("RECEIVE");
			receiveButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			receiveButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			receiveButton.setFocusable(false);
			receiveButton.addActionListener(new ActionListener() {

				@Override
				public synchronized void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();

					SoundLibrarianProgressDialog dialog = new SoundLibrarianProgressDialog(selectedRows.length);
					dialog.setRunnable(new Runnable() {

						@Override
						public void run() {

							try {
								if (selectedRows.length == 0)
									throw new Exception("No rows selected");

								SysexIOManager sysexManager = SysexIOManager
										.getInstance(SoundLibrarian.this.sysexPatchClass);

								synchronized (sysexManager) {

									for (int i = 0; i < selectedRows.length; i++) {
										int bankNo = l.get(selectedRows[i]).getPatchBank();
										int progNo = l.get(selectedRows[i]).getPatchProg();

										dialog.setProgressBarValue(i + 1);
										dialog.setProgressText("Receiving..." + (i + 1) + " / " + selectedRows.length);

										byte[] readBytes = null;

										for (int j = 0; j < MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_RETRY_COUNT; j++) {

											sysexManager.dumpRequest(bankNo, progNo);

											sysexManager.wait(MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_WAIT_MILLISECONDS);

											readBytes = sysexManager.getReadBytes();

											if (readBytes != null)
												break;

											sysexManager.cleanupTransmitter();
										}

										if (readBytes == null) {
											throw new Exception(
													"Error reading bytes.  This happens sometimes when disconnecting and reconnecting the Prophet 6.  Please try again.");
										}

										try {
											AbstractSysexPatch patch = SysexPatchFactory.getClosestPatchType(readBytes,
													SoundLibrarian.this.sysexPatchClass);
											l.set(selectedRows[i], patch);
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									}
								}

							} catch (Exception ex) {
								ex.printStackTrace();
								if (dialog.isVisible())
									dialog.setVisible(false);
								JOptionPane.showMessageDialog(null, ex.getMessage(), "Error receiving patch(es)",
										JOptionPane.ERROR_MESSAGE);
							} finally {
								model.fireTableDataChanged();

								ddl.clearSelection();

								for (int i = 0; i < selectedRows.length; i++)
									ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);

								ddl.requestFocus();
							}
						}
					});
					dialog.showAndRun();
				}
			});

			sendAllButton = new JButton("SEND ALL");
			sendAllButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			sendAllButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			sendAllButton.setFocusable(false);
			sendAllButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();

					SoundLibrarianProgressDialog dialog = new SoundLibrarianProgressDialog(SoundLibrarian.this.patchCount);
					dialog.setRunnable(new Runnable() {

						@Override
						public void run() {

							try {
								SysexIOManager sysexManager = SysexIOManager
										.getInstance(SoundLibrarian.this.sysexPatchClass);

								for (int i = 0; i < SoundLibrarian.this.patchCount; i++) {

									dialog.setProgressBarValue(i + 1);
									dialog.setProgressText("Sending..." + (i + 1) + " / " + SoundLibrarian.this.patchCount);

									sysexManager.send(l.get(i).getBytes());

									Thread.sleep(SYSEX_SEND_DELAY_TIME);
								}

							} catch (Exception ex) {
								ex.printStackTrace();
								if (dialog.isVisible())
									dialog.setVisible(false);
								JOptionPane.showMessageDialog(null, ex.getMessage(), "Error sending all patches",
										JOptionPane.ERROR_MESSAGE);

							} finally {
							}
						}
					});
					dialog.showAndRun();
				}
			});

			receiveAllButton = new JButton("RECEIVE ALL");
			receiveAllButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			receiveAllButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			receiveAllButton.setFocusable(false);
			receiveAllButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();

					SoundLibrarianProgressDialog dialog = new SoundLibrarianProgressDialog(SoundLibrarian.this.patchCount);
					dialog.setRunnable(new Runnable() {

						@Override
						public synchronized void run() {
							try {
								SysexIOManager sysexManager = SysexIOManager
										.getInstance(SoundLibrarian.this.sysexPatchClass);

								synchronized (sysexManager) {
									for (int i = 0; i < SoundLibrarian.this.patchCount; i++) {
										int bankNo = i / 100;
										int progNo = i % 100;

										dialog.setProgressBarValue(i + 1);
										dialog.setProgressText(
												"Receiving..." + (i + 1) + " / " + SoundLibrarian.this.patchCount);

										byte[] readBytes = null;

										for (int j = 0; j < MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_RETRY_COUNT; j++) {

											sysexManager.dumpRequest(bankNo, progNo);

											sysexManager.wait(MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_WAIT_MILLISECONDS);

											readBytes = sysexManager.getReadBytes();

											if (readBytes != null)
												break;

											sysexManager.cleanupTransmitter();
										}

										if (readBytes == null) {
											throw new Exception(
													"Error reading bytes.  This happens sometimes when disconnecting and reconnecting the Prophet 6.  Please try again.");
										}
										try {
											AbstractSysexPatch patch = SysexPatchFactory.getClosestPatchType(readBytes,
													SoundLibrarian.this.sysexPatchClass);
											l.set(i, patch);
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									}
								}

							} catch (Exception ex) {
								ex.printStackTrace();
								if (dialog.isVisible())
									dialog.setVisible(false);
								JOptionPane.showMessageDialog(null, ex.getMessage(), "Error receiving all patches",
										JOptionPane.ERROR_MESSAGE);
							} finally {
								model.fireTableDataChanged();

								ddl.clearSelection();
								ddl.addRowSelectionInterval(0, 0);
							}
						}
					});
					dialog.showAndRun();
				}
			});

			JPanel transferButtons = new JPanel(new GridBagLayout());

			c.insets = new Insets(0, 20, 0, 0);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE_TRAILING;
			c.weightx = 0.0;
			c.weighty = 0.0;
			transferButtons.add(sendButton, c);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 0;
			c.gridy = 1;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE_TRAILING;
			c.weightx = 0.0;
			c.weighty = 0.0;
			transferButtons.add(receiveButton, c);

			c.insets = new Insets(0, 0, 0, 20);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 1;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE_LEADING;
			c.weightx = 0.0;
			c.weighty = 0.0;
			transferButtons.add(sendAllButton, c);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 1;
			c.gridy = 1;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE_LEADING;
			c.weightx = 0.0;
			c.weighty = 0.0;
			transferButtons.add(receiveAllButton, c);

			c.insets = new Insets(0, 0, 0, 0);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 1;
			c.gridheight = 2;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;

			panel.add(transferButtons, c);

			JLabel programsLabel = new JLabel("PROGRAMS");
			programsLabel.setFont(new Font("Verdana", Font.PLAIN, 10));
			programsLabel.setVerticalAlignment(SwingConstants.TOP);
			programsLabel.setHorizontalAlignment(SwingConstants.CENTER);
			programsLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray));

			c.insets = new Insets(0, 10, 0, 10);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 2;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			panel.add(programsLabel, c);

			auditionSendButton = new JButton("SEND");
			auditionSendButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			auditionSendButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			auditionSendButton.setMinimumSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));
			auditionSendButton.setFocusable(false);
			auditionSendButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					SysexTableItemModel model = (SysexTableItemModel) ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();

					SoundLibrarianProgressDialog dialog = new SoundLibrarianProgressDialog(1);
					dialog.setRunnable(new Runnable() {

						@Override
						public void run() {
							try {
								if (selectedRows.length == 0)
									throw new Exception("No rows selected");

								SysexIOManager sysexManager = SysexIOManager
										.getInstance(SoundLibrarian.this.sysexPatchClass);

								dialog.setProgressBarValue(1);
								dialog.setProgressText("Sending..." + (1) + " / " + selectedRows.length);

								sysexManager.send(l.get(selectedRows[0]).getPatchAuditionBytes());

								Thread.sleep(SYSEX_SEND_DELAY_TIME);

							} catch (Exception ex) {
								ex.printStackTrace();
								if (dialog.isVisible())
									dialog.setVisible(false);
								JOptionPane.showMessageDialog(null, ex.getMessage(), "Error auditioning patch",
										JOptionPane.ERROR_MESSAGE);
							} finally {
								ddl.clearSelection();

								if (selectedRows.length > 0)
									ddl.addRowSelectionInterval(selectedRows[0], selectedRows[0]);
							}

						}
					});
					dialog.showAndRun();
				}
			});

			JPanel audtionButtons = new JPanel(new GridBagLayout());

			c.insets = new Insets(0, 20, 0, 20);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			audtionButtons.add(auditionSendButton, c);

			c.insets = new Insets(0, 0, 0, 0);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 3;
			c.gridy = 1;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			panel.add(audtionButtons, c);

			JLabel auditionLabel = new JLabel("AUDITION");
			auditionLabel.setFont(new Font("Verdana", Font.PLAIN, 10));
			auditionLabel.setVerticalAlignment(SwingConstants.TOP);
			auditionLabel.setHorizontalAlignment(SwingConstants.CENTER);
			auditionLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray));

			c.insets = new Insets(0, 10, 0, 10);

			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 3;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			panel.add(auditionLabel, c);

			setTransferAreaEnabled(false);

			return panel;
		}

		public class SoundLibrarianProgressDialog extends JDialog {
			private static final long serialVersionUID = 1L;
			private JLabel progressText;
			JProgressBar progressBar;

			private Runnable runnable;

			public SoundLibrarianProgressDialog(int max) {
				super(mainFrame, "Progress Dialog", true);

				this.progressBar = new JProgressBar(0, max);
				add(BorderLayout.CENTER, this.progressBar);

				this.progressText = new JLabel("Progress...");
				add(BorderLayout.NORTH, this.progressText);

				setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
				setSize(300, 75);
				setLocationRelativeTo(mainFrame);
				setUndecorated(true);

				JPanel dp = (JPanel) getContentPane();
				dp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			}

			public void setRunnable(Runnable runnable) {
				this.runnable = runnable;
			}

			public void setProgressText(String text) {
				this.progressText.setText(text);
			}

			public void setProgressBarValue(int value) {
				this.progressBar.setValue(value);
			}

			public void showAndRun() {
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						if (runnable != null)
							runnable.run();
						setVisible(false);
						;
					}
				});
				t.start();
				setVisible(true);
			}
		}
	}

	public class SoundLibrarianMergeFrame extends JFrame {
		private static final long serialVersionUID = 1L;

		public MergeTable mergeIntoTable;
		public MergeTable mergeFromTable;

		public SoundLibrarianMergeFrame(String arg0) {
			super(arg0);

			JPanel mergePanel = new JPanel();
			mergePanel.setOpaque(true);
			mergePanel.setLayout(new GridBagLayout());

			this.mergeFromTable = new MergeTable(MergeTable.MERGE_TABLE_MODE_SOURCE);
			this.mergeIntoTable = new MergeTable(MergeTable.MERGE_TABLE_MODE_DESTINATION);

			GridBagConstraints c = new GridBagConstraints();

			c.fill = GridBagConstraints.BOTH;
			c.gridx = 0;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 1.0;
			c.weighty = 1.0;
			mergePanel.add(new JScrollPane(mergeFromTable), c);

			c.fill = GridBagConstraints.BOTH;
			c.gridx = 1;
			c.gridy = 0;
			c.gridheight = 1;
			c.gridwidth = 1;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 1.0;
			c.weighty = 1.0;
			mergePanel.add(new JScrollPane(mergeIntoTable), c);

			c.fill = GridBagConstraints.NONE;
			c.gridx = 0;
			c.gridy = 1;
			c.gridheight = 1;
			c.gridwidth = 2;
			c.anchor = GridBagConstraints.BASELINE;
			c.weightx = 0.0;
			c.weighty = 0.0;
			JButton mergeButton = new JButton("Merge");
			mergeButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					MergeTable table = (MergeTable) mergeIntoTable;

					SysexTableItemModel model;

					model = (SysexTableItemModel) table.getModel();

					List<AbstractSysexPatch> mergeSource = model.getPatches();

					model = (SysexTableItemModel) mainFrame.ddl.getModel();

					List<AbstractSysexPatch> mergeSourceClone = new ArrayList<AbstractSysexPatch>();

					try {
						for (AbstractSysexPatch p : mergeSource) {
							mergeSourceClone.add((AbstractSysexPatch) p.clone());
						}
						model.setPatches(mergeSourceClone);
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					close();
				}
			});
			mergePanel.add(mergeButton, c);

			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			setContentPane(mergePanel);

			SoundLibrarianDummyMenuBar menuBar = new SoundLibrarianDummyMenuBar();
			setJMenuBar(menuBar);

			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent windowEvent) {
					close();
				}
			});
			pack();
		}

		public void open() {
			setVisible(true);

			MergeTable table = (MergeTable) mergeIntoTable;
			SysexTableItemModel model;
			model = (SysexTableItemModel) mainFrame.ddl.getModel();

			List<AbstractSysexPatch> mergeSource = model.getPatches();

			model = (SysexTableItemModel) table.getModel();

			List<AbstractSysexPatch> mergeSourceClone = new ArrayList<AbstractSysexPatch>();

			try {
				for (AbstractSysexPatch p : mergeSource) {
					mergeSourceClone.add((AbstractSysexPatch) p.clone());
				}
				model.setPatches(mergeSourceClone);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			mainFrame.setEnabled(false);
		}

		public void close() {
			setVisible(false);
			mainFrame.setEnabled(true);
		}

		class MergeTransferHandler extends TransferHandler {
			private static final long serialVersionUID = 1L;
			MergeTable table;

			public MergeTransferHandler(MergeTable table) {
				this.table = table;
			}

			protected Transferable createTransferable(JComponent c) {

				Transferable transferable;

				if (table.mode == MergeTable.MERGE_TABLE_MODE_DESTINATION) {
					transferable = new SysexDummySelection(null);
				} else {

					int[] selectedRows = table.getSelectedRows();

					SysexTableItemModel dlm = table.model;
					List<AbstractSysexPatch> patches = dlm.getPatches();

					List<AbstractSysexPatch> selectedObjectClones = new ArrayList<AbstractSysexPatch>();

					for (int i = 0; i < selectedRows.length; i++) {
						AbstractSysexPatch patch = patches.get(selectedRows[i]);

						try {
							selectedObjectClones.add((AbstractSysexPatch) patch.clone());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					transferable = new SysexPatchMultiSelection(selectedObjectClones);
				}
				return transferable;
			}

			public int getSourceActions(JComponent c) {
				return COPY_OR_MOVE;
			}

			public boolean canImport(TransferHandler.TransferSupport support) {

				if (!support.isDataFlavorSupported(SysexPatchMultiSelection.multiPatchFlavor)) {
					return false;
				}

				if (table.mode == MergeTable.MERGE_TABLE_MODE_SOURCE)
					return false;

				JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();

				if (dl.getRow() == -1) {
					return false;
				} else {
					return true;
				}
			}

			public boolean canImport(JComponent comp, Transferable t) {
				if (!t.isDataFlavorSupported(SysexPatchMultiSelection.multiPatchFlavor)) {
					return false;
				}

				if (table.mode == MergeTable.MERGE_TABLE_MODE_SOURCE)
					return false;

				return true;
			}

			@SuppressWarnings("unchecked")
			public boolean importData(TransferHandler.TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}

				SoundLibrarianMergeDragStateManager.getInstance().setDragging(false);

				try {
					List<AbstractSysexPatch> patches = (List<AbstractSysexPatch>) support.getTransferable()
							.getTransferData(SysexPatchMultiSelection.multiPatchFlavor);

					JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
					int dropTargetIndex = dl.getRow();

					MergeTable table = (MergeTable) support.getComponent();
					SysexTableItemModel model = (SysexTableItemModel) table.getModel();

					List<AbstractSysexPatch> mergeDestination = model.getPatches();

					int i = dropTargetIndex;
					for (AbstractSysexPatch p : patches) {
						if (i >= mergeDestination.size() || i < 0)
							break;
						mergeDestination.set(i++, (AbstractSysexPatch) p.clone());
					}

					model.fireTableDataChanged();

					table.addRowSelectionInterval(dropTargetIndex,
							Math.min(dropTargetIndex + patches.size() - 1, mergeDestination.size() - 1));
					table.repaint();

				} catch (Exception e) {
					e.printStackTrace();
				}

				return true;
			}
		}

		public class MergeTable extends JTable {
			private static final long serialVersionUID = 1L;
			SysexTableItemModel model;
			public static final int MERGE_TABLE_MODE_SOURCE = 0;
			public static final int MERGE_TABLE_MODE_DESTINATION = 1;
			public int mode;

			public String toString() {
				if (this.mode == MERGE_TABLE_MODE_SOURCE)
					return "MERGE_TABLE_MODE_SOURCE";
				if (this.mode == MERGE_TABLE_MODE_DESTINATION)
					return "MERGE_TABLE_MODE_DESTINATION";
				return super.toString();
			}

			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component returnComp = super.prepareRenderer(renderer, row, column);
				Color alternateColor = new Color(252, 242, 206);
				Color whiteColor = Color.WHITE;
				if (!returnComp.getBackground().equals(getSelectionBackground())) {
					Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
					returnComp.setBackground(bg);
					bg = null;
				}

				JComponent jcomp = (JComponent) returnComp;
				jcomp.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

				if (SoundLibrarianMergeDragStateManager.getInstance().isDragging()) {
					if (mode == MERGE_TABLE_MODE_DESTINATION) {
						int selectedRow = getSelectedRow();

						Color bg = (row % 2 == 0 ? new Color(0xB4C5E5) : new Color(0xC3D3F6));

						if (row >= selectedRow && row < selectedRow + mergeFromTable.getSelectedRowCount()) {
							returnComp.setBackground(bg);

						}
					}
				}

				return returnComp;
			};

			public MergeTable(int mode) {
				super(new SysexTableItemModel());
				model = (SysexTableItemModel) getModel();

				this.mode = mode;

				setDropMode(DropMode.USE_SELECTION);

				setFillsViewportHeight(true);
				getTableHeader().setReorderingAllowed(false);

				setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

				UIManager.put("Table.dropLineColor", Color.cyan);
				UIManager.put("Table.dropLineShortColor", Color.cyan);

				setDragEnabled(true);
				setTransferHandler(new MergeTransferHandler(this));

				addMouseMotionListener(new MouseMotionAdapter() {

					@Override
					public void mouseMoved(MouseEvent e) {
						SoundLibrarianMergeDragStateManager.getInstance().setDragging(false);
					}

					@Override
					public void mouseDragged(MouseEvent e) {
						if (mode == MergeTable.MERGE_TABLE_MODE_SOURCE)
							SoundLibrarianMergeDragStateManager.getInstance().setDragging(true);
					}
				});

				getColumnModel().getColumn(0).setPreferredWidth(60);
				getColumnModel().getColumn(0).setMinWidth(60);
				getColumnModel().getColumn(1).setPreferredWidth(600);

				getInputMap().put(KeyStroke.getKeyStroke("shift TAB"), "table-shift-tab");
				getActionMap().put("table-shift-tab", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
						manager.focusPreviousComponent();
					}
				});

				getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "table-tab");
				getActionMap().put("table-tab", new AbstractAction() {

					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
						manager.focusNextComponent();
					}
				});

				getSelectionModel().addListSelectionListener(new ListSelectionListener() {

					@Override
					public void valueChanged(ListSelectionEvent e) {
						repaint();
					}
				});
			}
		}
	}

	public class SoundLibrarianMenuBar extends JMenuBar {
		private static final long serialVersionUID = 1L;
		JMenuItem menuItemNewLibrary;
		JMenuItem menuItemLoadLibrary;
		JMenuItem menuItemSaveLibrary;
		JMenuItem menuItemMergeLibrary;
		JMenuItem menuItemLoadProgram;
		JMenuItem menuItemSaveProgram;
		JMenuItem menuItemMergeSysex;
		JMenuItem menuItemPrint;

		public SoundLibrarianMenuBar() {
			JMenu fileMenu = new JMenu("File");

			menuItemNewLibrary = new JMenuItem("New Library");
			menuItemNewLibrary.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					List<AbstractSysexPatch> newList = new ArrayList<>();

					SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();

					try {
						for (int i = 0; i < SysexPatchFactory
								.getUserBankCount(SoundLibrarian.this.sysexPatchClass); i++) {
							newList.add(
									SysexPatchFactory.getClosestPatchType(null, SoundLibrarian.this.sysexPatchClass));
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}

					model.setPatches(newList);
					model.fireTableDataChanged();
					mainFrame.ddl.addRowSelectionInterval(0, 0);

				}
			});
			menuItemNewLibrary.setAccelerator(KeyStroke.getKeyStroke("meta N"));

			fileMenu.add(menuItemNewLibrary);

			menuItemLoadLibrary = new JMenuItem("Load Library...");
			menuItemLoadLibrary.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getLibraryFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}
					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File For Import");

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {

							FileInputStream fis = new FileInputStream(file);

							byte[] buf = new byte[SoundLibrarian.this.patchLengthBytes * SoundLibrarian.this.patchCount];
							fis.read(buf, 0, buf.length);

							fis.close();

							SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();

							SysexPatchValidator validator = new SysexPatchValidator();

							Class<?> c = validator.getPatchClass(buf);

							if (c != SoundLibrarian.this.sysexPatchClass) {
								throw new Exception("Invalid file data");
							}

							SysexPatchIterator iterator = new SysexPatchIterator(buf);

							List<AbstractSysexPatch> newList = new ArrayList<>();

							while (iterator.hasNext()) {
								byte[] patchBytes = (byte[]) iterator.next();
								AbstractSysexPatch patch;
								patch = SysexPatchFactory.getClosestPatchType(patchBytes,
										SoundLibrarian.this.sysexPatchClass);
								newList.add(patch);
							}

							model.setPatches(newList);
							model.fireTableDataChanged();
							mainFrame.ddl.addRowSelectionInterval(0, 0);

						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, ex.getMessage(), "Error opening file",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			menuItemLoadLibrary.setAccelerator(KeyStroke.getKeyStroke("meta O"));

			fileMenu.add(menuItemLoadLibrary);

			menuItemSaveLibrary = new JMenuItem("Save Library...");
			menuItemSaveLibrary.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getLibraryFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Choose a destination");

					int returnVal = fc.showSaveDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {
							if (!file.getAbsolutePath().endsWith("." + fnef.getExtensions()[0])) {
								file = new File(file + "." + fnef.getExtensions()[0]);
							}
							FileOutputStream fos = new FileOutputStream(file);
							SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();
							List<AbstractSysexPatch> l = model.getPatches();

							for (AbstractSysexPatch patch : l) {
								fos.write(patch.getBytes());
							}
							fos.close();

						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {

						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			menuItemSaveLibrary.setAccelerator(KeyStroke.getKeyStroke("meta S"));
			fileMenu.add(menuItemSaveLibrary);

			menuItemMergeLibrary = new JMenuItem("Merge Library...");
			menuItemMergeLibrary.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getLibraryFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File To Merge");

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {

							FileInputStream fis = new FileInputStream(file);

							byte[] buf = new byte[SoundLibrarian.this.patchLengthBytes * SoundLibrarian.this.patchCount];
							fis.read(buf, 0, buf.length);

							fis.close();

							SysexPatchValidator validator = new SysexPatchValidator();

							Class<?> c = validator.getPatchClass(buf);

							if (c != SoundLibrarian.this.sysexPatchClass) {
								throw new Exception("Invalid file data");
							}

							SysexPatchIterator iterator = new SysexPatchIterator(buf);

							List<AbstractSysexPatch> newList = new ArrayList<>();

							while (iterator.hasNext()) {
								byte[] patchBytes = (byte[]) iterator.next();
								AbstractSysexPatch patch;
								patch = SysexPatchFactory.getClosestPatchType(patchBytes,
										SoundLibrarian.this.sysexPatchClass);
								newList.add(patch);
							}

							SysexTableItemModel model = (SysexTableItemModel) mergeFrame.mergeFromTable.getModel();
							model.setPatches(newList);

							mergeFrame.open();

						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, ex.getMessage(), "Error opening file",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			fileMenu.add(menuItemMergeLibrary);

			fileMenu.addSeparator();

			menuItemLoadProgram = new JMenuItem("Load Program...");
			menuItemLoadProgram.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getProgramFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File(s) For Import");
					fc.setMultiSelectionEnabled(true);

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File files[] = fc.getSelectedFiles();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {
							SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();
							List<AbstractSysexPatch> l = model.getPatches();
							int selectedRow = mainFrame.ddl.getSelectedRow();

							int i = selectedRow;

							for (File f : files) {

								if (i >= model.getRowCount() || i < 0)
									break;

								FileInputStream fis = new FileInputStream(f);

								byte[] buf = new byte[SoundLibrarian.this.patchLengthBytes];
								fis.read(buf, 0, SoundLibrarian.this.patchLengthBytes);
								fis.close();

								SysexPatchValidator validator = new SysexPatchValidator();

								Class<?> c = validator.getPatchClass(buf);

								if (c != SoundLibrarian.this.sysexPatchClass) {
									throw new Exception("Invalid file data");
								}

								AbstractSysexPatch patch = SysexPatchFactory.getClosestPatchType(buf,
										SoundLibrarian.this.sysexPatchClass);

								l.set(i++, patch);

							}

							model.fireTableDataChanged();

							mainFrame.ddl.addRowSelectionInterval(selectedRow, selectedRow);

						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, ex.getMessage(), "Error opening file",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			menuItemLoadProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt O"));

			fileMenu.add(menuItemLoadProgram);

			menuItemSaveProgram = new JMenuItem("Save Program...");
			menuItemSaveProgram.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();
					List<AbstractSysexPatch> l = model.getPatches();

					int selectedRow = mainFrame.ddl.getSelectedRow();
					int[] selectedRows = mainFrame.ddl.getSelectedRows();

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getProgramFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Choose a destination");

					if (selectedRows.length > 1) {

					} else {
						fc.setSelectedFile(new File(l.get(selectedRow).toString() + "." + fnef.getExtensions()[0]));
					}

					int returnVal = fc.showSaveDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {
							if (selectedRows.length > 1) {
								if (file.exists() || file.mkdirs()) {
									for (int i = 0; i < selectedRows.length; i++) {

										File oneFile = new File(file,
												l.get(selectedRows[i]).toString() + "." + fnef.getExtensions()[0]);
										FileOutputStream fos = new FileOutputStream(oneFile);
										fos.write(l.get(selectedRows[i]).getBytes());
										fos.close();
									}
								} else {
									throw new Exception("Error creating directory");
								}
							} else {
								if (!file.getAbsolutePath().endsWith("." + fnef.getExtensions()[0])) {
									file = new File(file + "." + fnef.getExtensions()[0]);
								}

								FileOutputStream fos = new FileOutputStream(file);
								fos.write(l.get(selectedRow).getBytes());
								fos.close();
							}

							for (int i = 0; i < selectedRows.length; i++)
								mainFrame.ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);

						} catch (Exception ex) {
							ex.printStackTrace();
						}
					} else {

						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			menuItemSaveProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt S"));
			fileMenu.add(menuItemSaveProgram);

			fileMenu.addSeparator();

			menuItemMergeSysex = new JMenuItem("Merge Sysex...");
			menuItemMergeSysex.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					FileNameExtensionFilter fnef;
					try {
						fnef = SysexPatchFactory.getSysexFileNameExtensionFilter(SoundLibrarian.this.sysexPatchClass);
					} catch (Exception ex) {
						ex.printStackTrace();
						return;
					}

					fc.addChoosableFileFilter(fnef);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File To Merge");

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {

							FileInputStream fis = new FileInputStream(file);

							byte[] buf = fis.readAllBytes();

							fis.close();

							SysexPatchValidator validator = new SysexPatchValidator();

							Class<?> c = validator.getPatchClass(buf);

							if (c != SoundLibrarian.this.sysexPatchClass) {
								throw new Exception("Invalid file data");
							}

							SysexPatchIterator iterator = new SysexPatchIterator(buf);

							List<AbstractSysexPatch> newList = new ArrayList<>();

							while (iterator.hasNext()) {
								byte[] patchBytes = (byte[]) iterator.next();
								AbstractSysexPatch patch;
								patch = SysexPatchFactory.getClosestPatchType(patchBytes,
										SoundLibrarian.this.sysexPatchClass);
								newList.add(patch);
							}

							SysexTableItemModel model = (SysexTableItemModel) mergeFrame.mergeFromTable.getModel();
							model.setPatches(newList);

							mergeFrame.open();

						} catch (Exception ex) {
							ex.printStackTrace();
							JOptionPane.showMessageDialog(null, ex.getMessage(), "Error opening file",
									JOptionPane.ERROR_MESSAGE);
						}
					} else {
						String absPath = fc.getCurrentDirectory().getAbsolutePath();

						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);
					}

				}
			});
			fileMenu.add(menuItemMergeSysex);

			menuItemPrint = new JMenuItem("Print...");
			menuItemPrint.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					PrinterJob job = PrinterJob.getPrinterJob();

					PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
					PageFormat pageFormat = job.defaultPage();

					Book book = new Book();

					book.append(new Prophet6UserBankPrinter(), pageFormat, 5);

					job.setPageable(book);

					attributes.add(DialogTypeSelection.NATIVE);

					boolean ok = job.printDialog(attributes);

					if (ok && attributes.containsKey(Destination.class)) {
						// this is a pdf save
						try {
							job.print();
						} catch (PrinterException ex) {
							ex.printStackTrace();
						}
					} else if (ok) {
						try {
							job.print();
						} catch (PrinterException ex) {
							ex.printStackTrace();
						}
					}
				}
			});
			menuItemPrint.setAccelerator(KeyStroke.getKeyStroke("meta P"));
			fileMenu.addSeparator();
			fileMenu.add(menuItemPrint);

			menuItemLoadProgram.setEnabled(false);
			menuItemSaveProgram.setEnabled(false);

			add(fileMenu);

			JMenu helpMenu = new JMenu("Help");

			JMenuItem aboutItem = new JMenuItem("About");
			aboutItem.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					SoundLibrarianAboutDialog dialog = new SoundLibrarianAboutDialog();
					dialog.setVisible(true);
				}
			});
			helpMenu.add(aboutItem);

			add(helpMenu);

		}
	}

	public class SoundLibrarianDummyMenuBar extends JMenuBar {
		private static final long serialVersionUID = 1L;
		JMenuItem menuItemNewLibrary;
		JMenuItem menuItemLoadLibrary;
		JMenuItem menuItemSaveLibrary;
		JMenuItem menuItemMergeLibrary;
		JMenuItem menuItemLoadProgram;
		JMenuItem menuItemSaveProgram;
		JMenuItem menuItemMergeSysex;

		public SoundLibrarianDummyMenuBar() {
			JMenu fileMenu = new JMenu("File");

			menuItemNewLibrary = new JMenuItem("New Library");
			menuItemNewLibrary.setEnabled(false);
			menuItemNewLibrary.setAccelerator(KeyStroke.getKeyStroke("meta N"));

			fileMenu.add(menuItemNewLibrary);

			menuItemLoadLibrary = new JMenuItem("Load Library...");
			menuItemLoadLibrary.setEnabled(false);
			menuItemLoadLibrary.setAccelerator(KeyStroke.getKeyStroke("meta O"));

			fileMenu.add(menuItemLoadLibrary);

			menuItemSaveLibrary = new JMenuItem("Save Library...");
			menuItemSaveLibrary.setEnabled(false);
			menuItemSaveLibrary.setAccelerator(KeyStroke.getKeyStroke("meta S"));
			fileMenu.add(menuItemSaveLibrary);

			menuItemMergeLibrary = new JMenuItem("Merge Library...");
			menuItemMergeLibrary.setEnabled(false);
			fileMenu.add(menuItemMergeLibrary);

			fileMenu.addSeparator();

			menuItemLoadProgram = new JMenuItem("Load Program...");
			menuItemLoadProgram.setEnabled(false);
			menuItemLoadProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt O"));

			fileMenu.add(menuItemLoadProgram);

			menuItemSaveProgram = new JMenuItem("Save Program...");
			menuItemSaveProgram.setEnabled(false);
			menuItemSaveProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt S"));
			fileMenu.add(menuItemSaveProgram);

			fileMenu.addSeparator();

			menuItemMergeSysex = new JMenuItem("Merge Sysex...");
			menuItemMergeSysex.setEnabled(false);
			fileMenu.add(menuItemMergeSysex);

			add(fileMenu);

			JMenu helpMenu = new JMenu("Help");

			JMenuItem aboutItem = new JMenuItem("About");
			aboutItem.setEnabled(false);
			helpMenu.add(aboutItem);

			add(helpMenu);

		}
	}

	public class SoundLibrarianAboutDialog extends JDialog {
		private static final long serialVersionUID = 1L;

		public SoundLibrarianAboutDialog() {

			super(mainFrame, "About Sound Librarian", true);

			try {
				String synthName = SysexPatchFactory.getSynthName(SoundLibrarian.this.sysexPatchClass);
				setTitle("About " + synthName + " Sound Librarian");
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			ImageIcon sequentialIcon = null;

			if (SoundLibrarian.this.sysexPatchClass == Prophet6SysexPatch.class)
				sequentialIcon = new ImageIcon(getClass().getResource("prophet6-small-black.png"));
			else if (SoundLibrarian.this.sysexPatchClass == OB6SysexPatch.class)
				sequentialIcon = new ImageIcon(getClass().getResource("ob6-small-black.png"));

			JLabel sequentialLabel = new JLabel(sequentialIcon);

			add(BorderLayout.CENTER, sequentialLabel);

			JPanel southPanel = new JPanel(new BorderLayout());

			try {
				String synthName = SysexPatchFactory.getSynthName(SoundLibrarian.this.sysexPatchClass);
				southPanel.add(BorderLayout.EAST, new JLabel(synthName + " Sound Librarian Version: " + APP_VERSION));
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			southPanel.add(BorderLayout.WEST, new JLabel("Eclipse Public License - v 2.0"));

			add(BorderLayout.SOUTH, southPanel);

			setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			setLocationRelativeTo(mainFrame);

			JPanel dp = (JPanel) getContentPane();
			dp.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			setSize(600, 200);
			setResizable(false);
		}
	}

	public class Prophet6UserBankPrinter implements Printable {

		public void drawString(Graphics2D g2d, String s, int x, int y) {
			g2d.drawString(s, x, y + getStringHeight(g2d, s));
		}

		public int getStringHeight(Graphics2D g2d, String s) {
			LineMetrics lm = g2d.getFont().getLineMetrics(s, g2d.getFontRenderContext());

			return (int) lm.getAscent();
		}

		public int print(Graphics g, PageFormat pf, int page) throws PrinterException {

			if (page > 4) {
				return NO_SUCH_PAGE;
			}

			try {
				Graphics2D g2d = (Graphics2D) g;

				InputStream is = getClass().getResourceAsStream("Avenir Book.ttf");
				Font avenirBookFont = Font.createFont(Font.TRUETYPE_FONT, is);

				is = getClass().getResourceAsStream("Avenir Next Condensed Demi Bold.ttf");
				Font avenirDemiBoldFont = Font.createFont(Font.TRUETYPE_FONT, is);

				SysexTableItemModel model = (SysexTableItemModel) mainFrame.ddl.getModel();
				List<AbstractSysexPatch> l = model.getPatches();

				if (page == 0) {

					g2d.translate(1.0 * 72.0f, 0.75 * 72.0f);

					int y = 0;

					String presets = "USER BANKS";

					if (SoundLibrarian.this.sysexPatchClass == Prophet6SysexPatch.class)
						presets = "SEQUENTIAL PROPHET-6 USER BANKS";
					else if (SoundLibrarian.this.sysexPatchClass == OB6SysexPatch.class)
						presets = "SEQUENTIAL OB-6 USER BANKS";
					else if (SoundLibrarian.this.sysexPatchClass == Prophet5SysexPatch.class)
						presets = "SEQUENTIAL PROPHET-5 USER BANKS";

					g2d.setColor(new Color(0x231f20));

					g2d.setFont(avenirDemiBoldFont.deriveFont(Font.PLAIN, 18f));
					drawString(g2d, presets, 0, y);
					y += getStringHeight(g2d, presets);

					y = (int) (2.0f * 72.0f - 0.75 * 72.0f);

					g2d.setColor(Color.BLACK);
					g2d.setFont(avenirDemiBoldFont.deriveFont(Font.PLAIN, 12f));

					LineMetrics lm = g2d.getFont().getLineMetrics("000", g2d.getFontRenderContext());

					drawString(g2d, "BANK " + page, 0, y);
					y += 14f;

					drawString(g2d, String.format("%03d", page * 100) + "-" + String.format("%03d", page * 100 + 99), 0,
							y);

					y += 14f;
					y += 14f;

					g2d.setFont(avenirBookFont.deriveFont(Font.PLAIN, 9f));

					int yOffset = y;

					lm = g2d.getFont().getLineMetrics("000", g2d.getFontRenderContext());

					for (AbstractSysexPatch patch : l) {
						if (patch.getPatchBank() > 0)
							break;
						drawString(g2d, patch.getBankProgPretty(),
								Math.floorDiv(patch.getPatchProg(), 50) * (int) (3.375 * 72.0f),
								yOffset + (int) ((patch.getPatchProg() % 50) * (lm.getAscent() + 1 + 1.0 / 16)));
						drawString(g2d, patch.getPatchName(),
								Math.floorDiv(patch.getPatchProg(), 50) * (int) (3.375 * 72.0f) + (int) (0.5 * 72),
								yOffset + (int) ((patch.getPatchProg() % 50) * (lm.getAscent() + 1 + 1.0 / 16)));
					}
				} else {
					g2d.translate(1.0 * 72.0f, 1.0 * 72.0f);

					int y = 0;

					g2d.setColor(Color.BLACK);

					g2d.setFont(avenirDemiBoldFont.deriveFont(Font.PLAIN, 12f));

					LineMetrics lm = g2d.getFont().getLineMetrics("000", g2d.getFontRenderContext());

					drawString(g2d, "BANK " + page, 0, 0);
					y += 14f;

					drawString(g2d, String.format("%03d", page * 100) + "-" + String.format("%03d", page * 100 + 99), 0,
							y);

					y += 14f;
					y += 14f;

					g2d.setFont(avenirBookFont.deriveFont(Font.PLAIN, 9f));

					int yOffset = y;

					lm = g2d.getFont().getLineMetrics("000", g2d.getFontRenderContext());

					for (int i = 0; i < 100; i++) {
						AbstractSysexPatch patch = l.get(page * 100 + i);
						drawString(g2d, patch.getBankProgPretty(),
								Math.floorDiv(patch.getPatchProg(), 50) * (int) (3.375 * 72.0f),
								yOffset + (int) ((patch.getPatchProg() % 50) * (lm.getAscent() + 1 + 1.0 / 16)));
						drawString(g2d, patch.getPatchName(),
								Math.floorDiv(patch.getPatchProg(), 50) * (int) (3.375 * 72.0f) + (int) (0.5 * 72),
								yOffset + (int) ((patch.getPatchProg() % 50) * (lm.getAscent() + 1 + 1.0 / 16)));
					}
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}

			return PAGE_EXISTS;
		}
	}

	private static final String PREFS_MOST_RECENT_DIRECTORY = "directory";
	private static final int SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH = 20;
	private static final int SYSEX_SEND_DELAY_TIME = 150;
	private static final int MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_RETRY_COUNT = 5;
	private static final int MIDI_SYSEX_SET_TRANSMITTER_RECEIVER_WAIT_MILLISECONDS = 1000;

	private void createAndShowGUI() {

		String frameTitle = "Sound Librarian";

		try {
			String synthName = SysexPatchFactory.getSynthName(SoundLibrarian.this.sysexPatchClass);
			frameTitle = synthName + " " + frameTitle;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		SoundLibrarianMainFrame mainFrame = new SoundLibrarianMainFrame(frameTitle);
		SoundLibrarianMergeFrame mergeFrame = new SoundLibrarianMergeFrame("Merge");

		JPanel mainPanel = new JPanel();
		mainPanel.setOpaque(true);
		mainPanel.setLayout(new GridBagLayout());
		mainPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE_LEADING;
		c.weightx = 0.0;
		c.weighty = 0.0;

		mainPanel.add(mainFrame.createTransferArea(), c);

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE;
		c.weightx = 1.0;
		c.weighty = 1.0;

		mainPanel.add(mainFrame.createDragDropList(), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE;
		c.weightx = 1.0;
		c.weighty = 0.0;

		mainPanel.add(mainFrame.createEditor(), c);

		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setContentPane(mainPanel);

		mainFrame.setJMenuBar(new SoundLibrarianMenuBar());

		mainFrame.pack();

		mainFrame.setVisible(true);

		this.mainFrame = mainFrame;
		this.mergeFrame = mergeFrame;
	}

	public Class<?> sysexPatchClass;
	public int patchCount;
	public int patchLengthBytes;

	public SoundLibrarian(Class<?> sysexPatchClass, int patchCount, int patchLengthBytes) {
		super();

		this.sysexPatchClass = sysexPatchClass;
		this.patchCount = patchCount;
		this.patchLengthBytes = patchLengthBytes;

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				// set the name of the application menu item
				try {
					String synthName = SysexPatchFactory.getSynthName(SoundLibrarian.this.sysexPatchClass);
					System.setProperty("com.apple.mrj.application.apple.menu.about.name",
							synthName + " Sound Librarian");
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				try {
					// set the look and feel
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					UIManager.put("Table.dropLineColor", Color.cyan);
					UIManager.put("Table.dropLineShortColor", Color.cyan);
					UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder(1, 1, 1, 1));

				} catch (Exception e) {
					e.printStackTrace();
				}

				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				createAndShowGUI();
				try {
					SysexIOManager.getInstance(sysexPatchClass).rescanDevices();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public static class SoundLibrarianMergeDragStateManager {
		private static SoundLibrarianMergeDragStateManager instance = null;
		private boolean isDragging = false;

		private SoundLibrarianMergeDragStateManager() {
			super();

		}

		public static SoundLibrarianMergeDragStateManager getInstance() {
			if (instance == null)
				instance = new SoundLibrarianMergeDragStateManager();

			return instance;
		}

		public void setDragging(boolean dragging) {
			this.isDragging = dragging;
		}

		public boolean isDragging() {
			return this.isDragging;
		}
	}

	public static class SysexDummySelection implements Transferable {

		private static DataFlavor dmselFlavor = new DataFlavor(Object.class, "Prophet 6 Sysex Dummy data flavor");
		private Object selection;

		public SysexDummySelection(Object selection) {
			this.selection = selection;
		}

		// Transferable implementation

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] ret = { dmselFlavor };
			return ret;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return dmselFlavor.equals(flavor);
		}

		@Override
		public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (isDataFlavorSupported(flavor)) {
				return this.selection;
			} else {
				throw new UnsupportedFlavorException(dmselFlavor);
			}
		}
	}

	public static class SysexPatchMultiSelection implements Transferable {

		private static DataFlavor multiPatchFlavor = new DataFlavor(List.class, "Sysex Multiple Patch data flavor");

		private List<AbstractSysexPatch> selection;

		public SysexPatchMultiSelection(List<AbstractSysexPatch> selection) {
			this.selection = selection;
		}

		// Transferable implementation

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] ret = { multiPatchFlavor };
			return ret;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return Arrays.asList(getTransferDataFlavors()).contains(flavor);
		}

		@Override
		public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (isDataFlavorSupported(flavor)) {
				if (flavor.equals(multiPatchFlavor))
					return this.selection;
				else
					return null;
			} else {
				throw new UnsupportedFlavorException(flavor);
			}
		}
	}

	public static class SysexPatchSelection implements Transferable, ClipboardOwner {

		private static DataFlavor dmselFlavor = new DataFlavor(AbstractSysexPatch.class, "Sysex Patch data flavor");
		private AbstractSysexPatch selection;

		public SysexPatchSelection(AbstractSysexPatch selection) {
			this.selection = selection;
		}

		// Transferable implementation

		@Override
		public DataFlavor[] getTransferDataFlavors() {
			DataFlavor[] ret = { dmselFlavor };
			return ret;
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return dmselFlavor.equals(flavor);
		}

		@Override
		public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
			if (isDataFlavorSupported(flavor)) {
				return this.selection;
			} else {
				throw new UnsupportedFlavorException(dmselFlavor);
			}
		}

		// ClipboardOwner implementation

		@Override
		public void lostOwnership(Clipboard clipboard, Transferable transferable) {
			System.out.println("MyObjectSelection: Lost ownership");
		}

	}

	public class SysexTableItemModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private List<AbstractSysexPatch> patches;
		public String[] headers = new String[] { "#", "Name" };

		public SysexTableItemModel() {
			try {
				this.patches = new ArrayList<AbstractSysexPatch>();
				for (int i = 0; i < SysexPatchFactory.getUserBankCount(SoundLibrarian.this.sysexPatchClass); i++) {
					this.patches.add(SysexPatchFactory.getClosestPatchType(null, SoundLibrarian.this.sysexPatchClass));
				}
				updateAllPatchNumbers();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public SysexTableItemModel(List<AbstractSysexPatch> patches) {

			this.patches = new ArrayList<AbstractSysexPatch>(patches);

		}

		@Override
		public void fireTableDataChanged() {
			super.fireTableDataChanged();
			updateAllPatchNumbers();
		}

		public void updateAllPatchNumbers() {
			if (SoundLibrarian.this.sysexPatchClass == Prophet5SysexPatch.class) {
				for (int i = 0; i < patches.size(); i++) {
					AbstractSysexPatch patch = patches.get(i);
					patch.setPatchBank(i / 40);
					patch.setPatchProg(i % 40);
					
					// maybe use later
//					int group = i % 8 + 1;
//					int bank = (i % 40) / 8 + 1;
//					int program = (i % 200) / 40 + 1;
				}				
			} else {
			for (int i = 0; i < patches.size(); i++) {
				AbstractSysexPatch patch = patches.get(i);
				patch.setPatchBank(i / 100);
				patch.setPatchProg(i % 100);
			}
			}
		}

		public void debugPrintTable() {
			for (int i = 0; i < patches.size(); i++)
				System.out.println(patches.get(i).getPatchName());
		}

		public void clearTable() {
			patches.clear();
			fireTableDataChanged();
		}

		public void addRow(AbstractSysexPatch patch) {
			patches.add(patch);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return patches.size();
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int col) {
			return headers[col];
		}

		@Override
		public boolean isCellEditable(int row, int col) {
			return false;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {

			Object value = "??";
			AbstractSysexPatch patch = patches.get(rowIndex);
			switch (columnIndex) {
			case 0:
//				value = patch.getPatchBank() + "-" + patch.getPatchProg();
				value = patch.getBankProgPretty();
				break;
			case 1:
				value = patch.getPatchName();
				break;
			}

			return value;

		}

		public List<AbstractSysexPatch> getPatches() {
			return patches;
		}

		public AbstractSysexPatch getPatchAt(int row) {
			return patches.get(row);
		}

		public void setPatches(List<AbstractSysexPatch> patches) {
			this.patches = patches;
		}

	}

}
