package com.eclewlow.sequential.prophet6;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.event.FocusEvent.Cause;

public class Prophet6SoundLibrarian {

	public Prophet6SoundLibrarianMainFrame mainFrame;
	public Prophet6SoundLibrarianMergeFrame mergeFrame;

	public class Prophet6SoundLibrarianMainFrame extends JFrame {
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

		JProgressBar progressBar;
		DragDropList ddl;
		NameFieldFocusListener nffl = new NameFieldFocusListener();

		public Prophet6SoundLibrarianMainFrame(String arg0) {
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
				if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
					return false;
				}
				JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();

				if (dl.getRow() == -1) {
					return false;
				} else {
					return true;
				}
			}

			public boolean importData(TransferHandler.TransferSupport support) {
				if (!canImport(support)) {
					return false;
				}

				try {
					JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
					int dropTargetIndex = dl.getRow();

					DragDropList dpl = (DragDropList) support.getComponent();
					Prophet6SysexTableItemModel dlm = (Prophet6SysexTableItemModel) dpl.getModel();

					int[] selectedRows = dpl.getSelectedRows();

					List<Prophet6SysexPatch> patches = dlm.getPatches();

					List<Prophet6SysexPatch> selectedObjects = new ArrayList<Prophet6SysexPatch>();
					List<Prophet6SysexPatch> selectedObjectClones = new ArrayList<Prophet6SysexPatch>();

					for (int i = 0; i < selectedRows.length; i++) {
						Prophet6SysexPatch patch = patches.get(selectedRows[i]);
						selectedObjects.add(patch);
						selectedObjectClones.add((Prophet6SysexPatch) patch.clone());
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
			Prophet6SysexTableItemModel model;

			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component returnComp = super.prepareRenderer(renderer, row, column);
				Color alternateColor = new Color(252, 242, 206);
				Color whiteColor = Color.WHITE;
				if (!returnComp.getBackground().equals(getSelectionBackground())) {
					Color bg = (row % 2 == 0 ? alternateColor : whiteColor);
					returnComp.setBackground(bg);
					bg = null;
				}

				return returnComp;
			};

			public DragDropList() {
				super(new Prophet6SysexTableItemModel());
				model = (Prophet6SysexTableItemModel) getModel();

				setDropMode(DropMode.INSERT_ROWS);
				setFillsViewportHeight(true);
				getTableHeader().setReorderingAllowed(false);

				setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

				UIManager.put("Table.dropLineColor", Color.cyan);
				UIManager.put("Table.dropLineShortColor", Color.cyan);

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

						Prophet6SysexTableItemModel dlm = (Prophet6SysexTableItemModel) getModel();
						List<Prophet6SysexPatch> patches = dlm.getPatches();

						int n = JOptionPane.showConfirmDialog(mainFrame,
								"The selected program(s) will be initialized, continue?", "Initialize Program",
								JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
						if (n == JOptionPane.OK_OPTION) {
							for (int i = 0; i < selectedRows.length; i++) {
								Prophet6SysexPatch initPatch = new Prophet6SysexPatch(
										Prophet6SysexPatch.INIT_PATCH_BYTES);

								patches.set(selectedRows[i], initPatch);

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

						Prophet6SoundLibrarianMenuBar menuBar = (Prophet6SoundLibrarianMenuBar) getJMenuBar();

						if (!e.getValueIsAdjusting()) {
							int[] selectedRows = getSelectedRows();
							if (selectedRows.length > 1) {
								Prophet6SysexPatch patch = model.getProphet6SysexPatchAt(selectedRows[0]);
								nameField.setCurrentIndex(selectedRows[0]);
								nameField.setText(patch.getPatchName().replaceAll("\\s+$", ""));
								nameField.setEnabled(true);
								menuBar.menuItemLoadProgram.setEnabled(false);
								menuBar.menuItemSaveProgram.setEnabled(true);
								if (Prophet6Sysex.getInstance().isConnected() && !progressBar.isEnabled()) {
									sendButton.setEnabled(true);
									receiveButton.setEnabled(true);
									auditionSendButton.setEnabled(true);
								}
							} else if (selectedRows.length == 1) {
								Prophet6SysexPatch patch = model.getProphet6SysexPatchAt(selectedRows[0]);
								nameField.setCurrentIndex(selectedRows[0]);
								nameField.setText(patch.getPatchName().replaceAll("\\s+$", ""));
								nameField.setEnabled(true);
								menuBar.menuItemLoadProgram.setEnabled(true);
								menuBar.menuItemSaveProgram.setEnabled(true);
								if (Prophet6Sysex.getInstance().isConnected() && !progressBar.isEnabled()) {
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

						Prophet6SysexTableItemModel dlm = (Prophet6SysexTableItemModel) getModel();
						List<Prophet6SysexPatch> patches = dlm.getPatches();
						int selectedRow = getSelectedRow();

						Prophet6SysexPatchSelection selection = new Prophet6SysexPatchSelection(
								patches.get(selectedRow));
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
								&& content.isDataFlavorSupported(Prophet6SysexPatchSelection.dmselFlavor);
						if (hasTransferable) {
							try {
								Prophet6SysexPatch result = (Prophet6SysexPatch) content
										.getTransferData(Prophet6SysexPatchSelection.dmselFlavor);

								Prophet6SysexPatch clone = (Prophet6SysexPatch) result.clone();
								Prophet6SysexTableItemModel dlm = (Prophet6SysexTableItemModel) getModel();
								List<Prophet6SysexPatch> patches = dlm.getPatches();
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
						Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
						List<Prophet6SysexPatch> l = model.getPatches();
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

						Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
						List<Prophet6SysexPatch> l = model.getPatches();

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

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();
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

		public JPanel createProgressArea() {

			JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));

			progressBar = new JProgressBar(0, PROPHET_6_USER_BANK_COUNT);
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
			progressBar.setVisible(true);
			progressBar.setString("");
			progressBar.setEnabled(false);
			progressBar.setPreferredSize(new Dimension(300, 29));

			panel.add(progressBar);

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
			} else if (Prophet6Sysex.getInstance().isConnected() && mainFrame.ddl.getSelectedRowCount() > 0) {
				sendButton.setEnabled(enabled);
				receiveButton.setEnabled(enabled);
				auditionSendButton.setEnabled(enabled);
			}
		}

		public void progressStart(int max) {
			setTransferAreaEnabled(false);
			progressBar.setValue(0);
			progressBar.setMaximum(max);
			progressBar.setVisible(true);
			progressBar.setString("Loading...");
			progressBar.setEnabled(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		}

		public void progressFinish() {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			progressBar.setValue(PROPHET_6_USER_BANK_COUNT);
			progressBar.setString("Done!");

			new java.util.Timer().schedule(new java.util.TimerTask() {
				@Override
				public void run() {
					progressBar.setEnabled(false);
					progressBar.setString("");
					setTransferAreaEnabled(true);
				}
			}, 2000);
		}

		public JPanel createTransferArea() {

			JPanel panel = new JPanel(new GridBagLayout());

			panel.setBorder(new EmptyBorder(5, 0, 0, 0));

			GridBagConstraints c = new GridBagConstraints();

			JPanel sequentialPanel = new JPanel();

			ImageIcon sequentialIcon = new ImageIcon(getClass().getResource("prophet6-small-black.png"));

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

			Prophet6Sysex.getInstance().addObserver(new Observer() {

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

			sendButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();

					Runnable runner = new Runnable() {
						public void run() {

							try {
								if (selectedRows.length == 0)
									throw new Exception("No rows selected");

								Prophet6Sysex p6sysex = Prophet6Sysex.getInstance();

								progressStart(selectedRows.length);

								for (int i = 0; i < selectedRows.length; i++) {

									progressBar.setValue(i + 1);
									progressBar.setString("Sending..." + (i + 1) + " / " + selectedRows.length);

									p6sysex.send(l.get(selectedRows[i]).bytes.clone());
									Thread.sleep(SYSEX_SEND_DELAY_TIME);
								}

							} catch (Exception ex) {
								ex.printStackTrace();
							} finally {
								ddl.clearSelection();

								for (int i = 0; i < selectedRows.length; i++)
									ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);

								progressFinish();
							}
						}
					};
					Thread t = new Thread(runner, "Code Executer");
					t.start();

				}
			});

			receiveButton = new JButton("RECEIVE");
			receiveButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			receiveButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));

			receiveButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();
					try {
						if (selectedRows.length == 0)
							throw new Exception("No rows selected");

						Prophet6Sysex p6sysex = Prophet6Sysex.getInstance();

						progressStart(selectedRows.length);

						synchronized (p6sysex) {

							for (int i = 0; i < selectedRows.length; i++) {
								int bankNo = l.get(selectedRows[i]).getPatchBank();
								int progNo = l.get(selectedRows[i]).getPatchProg();

								p6sysex.dumpRequest(bankNo, progNo);

								progressBar.setValue(i + 1);
								progressBar.setString("Receiving..." + (i + 1) + " / " + selectedRows.length);
								p6sysex.wait();

								Prophet6SysexPatch patch = new Prophet6SysexPatch(p6sysex.getReadBytes());

								l.set(selectedRows[i], patch);
							}

							model.fireTableDataChanged();

						}

					} catch (Exception ex) {
						ex.printStackTrace();
					} finally {
						ddl.clearSelection();

						for (int i = 0; i < selectedRows.length; i++)
							ddl.addRowSelectionInterval(selectedRows[i], selectedRows[i]);
						progressFinish();
					}
				}
			});

			sendAllButton = new JButton("SEND ALL");
			sendAllButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			sendAllButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));

			sendAllButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();

					Runnable runner = new Runnable() {
						public void run() {
							try {
								Prophet6Sysex p6sysex = Prophet6Sysex.getInstance();

								progressStart(PROPHET_6_USER_BANK_COUNT);

								for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {

									progressBar.setString("Sending..." + (i + 1) + " / " + PROPHET_6_USER_BANK_COUNT);
									progressBar.setValue(i + 1);

									p6sysex.send(l.get(i).bytes.clone());

									Thread.sleep(SYSEX_SEND_DELAY_TIME);
								}

							} catch (Exception ex) {
								ex.printStackTrace();
							} finally {
								progressFinish();
							}
						}
					};
					Thread t = new Thread(runner, "Code Executer");
					t.start();

				}
			});

			receiveAllButton = new JButton("RECEIVE ALL");
			receiveAllButton.setFont(new Font("Verdana", Font.PLAIN, 11));
			receiveAllButton.setPreferredSize(new Dimension(BUTTON_PREFERRED_WIDTH, BUTTON_PREFERRED_HEIGHT));

			receiveAllButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					Runnable runner = new Runnable() {
						public void run() {

							try {
								Prophet6Sysex p6sysex = Prophet6Sysex.getInstance();

								synchronized (p6sysex) {
									List<Prophet6SysexPatch> newList = new ArrayList<>();

									progressStart(PROPHET_6_USER_BANK_COUNT);

									for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
										int bankNo = i / 100;
										int progNo = i % 100;
										p6sysex.dumpRequest(bankNo, progNo);

										progressBar.setValue(i + 1);
										progressBar.setString(
												"Receiving..." + (i + 1) + " / " + PROPHET_6_USER_BANK_COUNT);
										p6sysex.wait();

										Prophet6SysexPatch patch = new Prophet6SysexPatch(p6sysex.getReadBytes());

										newList.add(patch);
									}
									model.setPatches(newList);
									model.fireTableDataChanged();
									ddl.addRowSelectionInterval(0, 0);

								}

							} catch (Exception ex) {
								ex.printStackTrace();
							} finally {
								progressFinish();
							}
						}
					};
					Thread t = new Thread(runner, "Code Executer");
					t.start();

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

			auditionSendButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();
					int[] selectedRows = ddl.getSelectedRows();

					Runnable runner = new Runnable() {
						public void run() {

							try {
								if (selectedRows.length == 0)
									throw new Exception("No rows selected");

								Prophet6Sysex p6sysex = Prophet6Sysex.getInstance();

								progressStart(1);

								progressBar.setValue(1);
								progressBar.setString("Sending..." + (1) + " / " + selectedRows.length);

								p6sysex.send(l.get(selectedRows[0]).getPatchAuditionBytes());
								Thread.sleep(SYSEX_SEND_DELAY_TIME);

							} catch (Exception ex) {
								ex.printStackTrace();
							} finally {
								ddl.clearSelection();

								if (selectedRows.length > 0)
									ddl.addRowSelectionInterval(selectedRows[0], selectedRows[0]);

								progressFinish();
							}
						}
					};
					Thread t = new Thread(runner, "Code Executer");
					t.start();

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

	}

	public class Prophet6SoundLibrarianMergeFrame extends JFrame {
		private static final long serialVersionUID = 1L;

		public MergeTable mergeIntoTable;
		public MergeTable mergeFromTable;

		public Prophet6SoundLibrarianMergeFrame(String arg0) {
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

					Prophet6SysexTableItemModel model;

					model = (Prophet6SysexTableItemModel) table.getModel();

					List<Prophet6SysexPatch> mergeSource = model.getPatches();

					model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();

					List<Prophet6SysexPatch> mergeSourceClone = new ArrayList<Prophet6SysexPatch>();

					try {
						for (Prophet6SysexPatch p : mergeSource) {
							mergeSourceClone.add((Prophet6SysexPatch) p.clone());
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

			Prophet6SoundLibrarianDummyMenuBar menuBar = new Prophet6SoundLibrarianDummyMenuBar();
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
			Prophet6SysexTableItemModel model;
			model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();

			List<Prophet6SysexPatch> mergeSource = model.getPatches();

			model = (Prophet6SysexTableItemModel) table.getModel();

			List<Prophet6SysexPatch> mergeSourceClone = new ArrayList<Prophet6SysexPatch>();

			try {
				for (Prophet6SysexPatch p : mergeSource) {
					mergeSourceClone.add((Prophet6SysexPatch) p.clone());
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
					transferable = new Prophet6SysexDummySelection(null);
				} else {

					int[] selectedRows = table.getSelectedRows();

					Prophet6SysexTableItemModel dlm = table.model;
					List<Prophet6SysexPatch> patches = dlm.getPatches();

					List<Prophet6SysexPatch> selectedObjectClones = new ArrayList<Prophet6SysexPatch>();

					for (int i = 0; i < selectedRows.length; i++) {
						Prophet6SysexPatch patch = patches.get(selectedRows[i]);

						try {
							selectedObjectClones.add((Prophet6SysexPatch) patch.clone());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					transferable = new Prophet6SysexPatchMultiSelection(selectedObjectClones);
				}
				return transferable;
			}

			public int getSourceActions(JComponent c) {
				return COPY_OR_MOVE;
			}

			public boolean canImport(TransferHandler.TransferSupport support) {

				if (!support.isDataFlavorSupported(Prophet6SysexPatchMultiSelection.multiPatchFlavor)) {
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
				if (!t.isDataFlavorSupported(Prophet6SysexPatchMultiSelection.multiPatchFlavor)) {
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

				Prophet6SoundLibrarianMergeDragStateManager.getInstance().setDragging(false);

				try {
					List<Prophet6SysexPatch> patches = (List<Prophet6SysexPatch>) support.getTransferable()
							.getTransferData(Prophet6SysexPatchMultiSelection.multiPatchFlavor);

					JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
					int dropTargetIndex = dl.getRow();

					MergeTable table = (MergeTable) support.getComponent();
					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) table.getModel();

					List<Prophet6SysexPatch> mergeDestination = model.getPatches();

					int i = dropTargetIndex;
					for (Prophet6SysexPatch p : patches) {
						if (i >= mergeDestination.size() || i < 0)
							break;
						mergeDestination.set(i++, (Prophet6SysexPatch) p.clone());
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
			Prophet6SysexTableItemModel model;
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

				if (Prophet6SoundLibrarianMergeDragStateManager.getInstance().isDragging()) {
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
				super(new Prophet6SysexTableItemModel());
				model = (Prophet6SysexTableItemModel) getModel();

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
						Prophet6SoundLibrarianMergeDragStateManager.getInstance().setDragging(false);
					}

					@Override
					public void mouseDragged(MouseEvent e) {
						if (mode == MergeTable.MERGE_TABLE_MODE_SOURCE)
							Prophet6SoundLibrarianMergeDragStateManager.getInstance().setDragging(true);
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

	public class Prophet6SoundLibrarianMenuBar extends JMenuBar {
		private static final long serialVersionUID = 1L;
		JMenuItem menuItemNewLibrary;
		JMenuItem menuItemLoadLibrary;
		JMenuItem menuItemSaveLibrary;
		JMenuItem menuItemMergeLibrary;
		JMenuItem menuItemLoadProgram;
		JMenuItem menuItemSaveProgram;
		JMenuItem menuItemMergeSysex;

		public Prophet6SoundLibrarianMenuBar() {
			JMenu menu = new JMenu("File");

			menuItemNewLibrary = new JMenuItem("New Library");
			menuItemNewLibrary.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					List<Prophet6SysexPatch> newList = new ArrayList<>();

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();

					for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
						Prophet6SysexPatch patch = new Prophet6SysexPatch(Prophet6SysexPatch.INIT_PATCH_BYTES);

						newList.add(patch);
					}
					model.setPatches(newList);
					model.fireTableDataChanged();
					mainFrame.ddl.addRowSelectionInterval(0, 0);

				}
			});
			menuItemNewLibrary.setAccelerator(KeyStroke.getKeyStroke("meta N"));

			menu.add(menuItemNewLibrary);

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

					fc.addChoosableFileFilter(p6libraryFilter);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File For Import");

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {

							FileInputStream fis = new FileInputStream(file);

							byte[] buf = new byte[PROPHET_6_SYSEX_LENGTH * PROPHET_6_USER_BANK_COUNT];
							fis.read(buf, 0, buf.length);

							fis.close();

							Prophet6SoundLibrarianFileValidator.validateP6Library(buf);

							List<Prophet6SysexPatch> newList = new ArrayList<>();

							Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();

							for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
								byte[] patchBytes = Arrays.copyOfRange(buf, i * PROPHET_6_SYSEX_LENGTH,
										(i + 1) * PROPHET_6_SYSEX_LENGTH);
								Prophet6SysexPatch patch = new Prophet6SysexPatch(patchBytes);

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

			menu.add(menuItemLoadLibrary);

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

					fc.addChoosableFileFilter(p6libraryFilter);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Choose a destination");

					int returnVal = fc.showSaveDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {
							if (!file.getAbsolutePath().endsWith(".p6lib")) {
								file = new File(file + ".p6lib");
							}
							FileOutputStream fos = new FileOutputStream(file);
							Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();
							List<Prophet6SysexPatch> l = model.getPatches();

							for (Prophet6SysexPatch patch : l) {
								fos.write(patch.bytes);
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
			menu.add(menuItemSaveLibrary);

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

					fc.addChoosableFileFilter(p6libraryFilter);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File To Merge");

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File file = fc.getSelectedFile();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {

							FileInputStream fis = new FileInputStream(file);

							byte[] buf = new byte[PROPHET_6_SYSEX_LENGTH * PROPHET_6_USER_BANK_COUNT];
							fis.read(buf, 0, buf.length);

							fis.close();

							Prophet6SoundLibrarianFileValidator.validateP6Library(buf);

							List<Prophet6SysexPatch> newList = new ArrayList<>();

							for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
								byte[] patchBytes = Arrays.copyOfRange(buf, i * PROPHET_6_SYSEX_LENGTH,
										(i + 1) * PROPHET_6_SYSEX_LENGTH);
								Prophet6SysexPatch patch = new Prophet6SysexPatch(patchBytes);

								newList.add(patch);
							}

							Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mergeFrame.mergeFromTable
									.getModel();
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
			menu.add(menuItemMergeLibrary);

			menu.addSeparator();

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

					fc.addChoosableFileFilter(p6programFilter);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Select File(s) For Import");
					fc.setMultiSelectionEnabled(true);

					int returnVal = fc.showOpenDialog(mainFrame);

					if (returnVal == JFileChooser.APPROVE_OPTION) {
						File files[] = fc.getSelectedFiles();
						String absPath = fc.getCurrentDirectory().getAbsolutePath();
						prefs.put(PREFS_MOST_RECENT_DIRECTORY, absPath);

						try {
							Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();
							List<Prophet6SysexPatch> l = model.getPatches();
							int selectedRow = mainFrame.ddl.getSelectedRow();

							int i = selectedRow;

							for (File f : files) {

								if (i >= model.getRowCount() || i < 0)
									break;

								FileInputStream fis = new FileInputStream(f);

								byte[] buf = new byte[PROPHET_6_SYSEX_LENGTH];
								fis.read(buf, 0, PROPHET_6_SYSEX_LENGTH);

								Prophet6SoundLibrarianFileValidator.validateP6Program(buf);

								Prophet6SysexPatch patch = new Prophet6SysexPatch(buf);

								l.set(i++, patch);

								fis.close();

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

			menu.add(menuItemLoadProgram);

			menuItemSaveProgram = new JMenuItem("Save Program...");
			menuItemSaveProgram.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					Preferences prefs = Preferences.userNodeForPackage(getClass());

					String mostRecentDirectory = prefs.get(PREFS_MOST_RECENT_DIRECTORY, null);

					Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mainFrame.ddl.getModel();
					List<Prophet6SysexPatch> l = model.getPatches();

					int selectedRow = mainFrame.ddl.getSelectedRow();
					int[] selectedRows = mainFrame.ddl.getSelectedRows();

					final JFileChooser fc;

					if (mostRecentDirectory == null)
						fc = new JFileChooser();
					else
						fc = new JFileChooser(new File(mostRecentDirectory));

					fc.addChoosableFileFilter(p6programFilter);
					fc.setAcceptAllFileFilterUsed(false);
					fc.setDialogTitle("Choose a destination");

					if (selectedRows.length > 1) {

					} else {
						fc.setSelectedFile(new File(l.get(selectedRow).toString() + ".p6program"));
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

										File oneFile = new File(file, l.get(selectedRows[i]).toString() + ".p6program");
										FileOutputStream fos = new FileOutputStream(oneFile);
										fos.write(l.get(selectedRows[i]).bytes);
										fos.close();
									}
								} else {
									throw new Exception("Error creating directory");
								}
							} else {
								if (!file.getAbsolutePath().endsWith(".p6program")) {
									file = new File(file + ".p6program");
								}

								FileOutputStream fos = new FileOutputStream(file);
								fos.write(l.get(selectedRow).bytes);
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
			menu.add(menuItemSaveProgram);

			menu.addSeparator();

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

					fc.addChoosableFileFilter(p6sysexFilter);
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

							Prophet6SoundLibrarianFileValidator.validateP6SysEx(buf);

							List<Prophet6SysexPatch> newList = new ArrayList<>();

							for (int i = 0; i < buf.length / PROPHET_6_SYSEX_LENGTH; i++) {
								byte[] patchBytes = Arrays.copyOfRange(buf, i * PROPHET_6_SYSEX_LENGTH,
										(i + 1) * PROPHET_6_SYSEX_LENGTH);
								Prophet6SysexPatch patch = new Prophet6SysexPatch(patchBytes);

								newList.add(patch);
							}

							Prophet6SysexTableItemModel model = (Prophet6SysexTableItemModel) mergeFrame.mergeFromTable
									.getModel();
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
			menu.add(menuItemMergeSysex);

			menuItemLoadProgram.setEnabled(false);
			menuItemSaveProgram.setEnabled(false);

			add(menu);

		}
	}

	public class Prophet6SoundLibrarianDummyMenuBar extends JMenuBar {
		private static final long serialVersionUID = 1L;
		JMenuItem menuItemNewLibrary;
		JMenuItem menuItemLoadLibrary;
		JMenuItem menuItemSaveLibrary;
		JMenuItem menuItemMergeLibrary;
		JMenuItem menuItemLoadProgram;
		JMenuItem menuItemSaveProgram;
		JMenuItem menuItemMergeSysex;

		public Prophet6SoundLibrarianDummyMenuBar() {
			JMenu menu = new JMenu("File");

			menuItemNewLibrary = new JMenuItem("New Library");
			menuItemNewLibrary.setEnabled(false);
			menuItemNewLibrary.setAccelerator(KeyStroke.getKeyStroke("meta N"));

			menu.add(menuItemNewLibrary);

			menuItemLoadLibrary = new JMenuItem("Load Library...");
			menuItemLoadLibrary.setEnabled(false);
			menuItemLoadLibrary.setAccelerator(KeyStroke.getKeyStroke("meta O"));

			menu.add(menuItemLoadLibrary);

			menuItemSaveLibrary = new JMenuItem("Save Library...");
			menuItemSaveLibrary.setEnabled(false);
			menuItemSaveLibrary.setAccelerator(KeyStroke.getKeyStroke("meta S"));
			menu.add(menuItemSaveLibrary);

			menuItemMergeLibrary = new JMenuItem("Merge Library...");
			menuItemMergeLibrary.setEnabled(false);
			menu.add(menuItemMergeLibrary);

			menu.addSeparator();

			menuItemLoadProgram = new JMenuItem("Load Program...");
			menuItemLoadProgram.setEnabled(false);
			menuItemLoadProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt O"));

			menu.add(menuItemLoadProgram);

			menuItemSaveProgram = new JMenuItem("Save Program...");
			menuItemSaveProgram.setEnabled(false);
			menuItemSaveProgram.setAccelerator(KeyStroke.getKeyStroke("meta alt S"));
			menu.add(menuItemSaveProgram);

			menu.addSeparator();

			menuItemMergeSysex = new JMenuItem("Merge Sysex...");
			menuItemMergeSysex.setEnabled(false);
			menu.add(menuItemMergeSysex);

			add(menu);
		}
	}

	public static final byte[] SYSEX_MSG_DUMP_REQUEST = { (byte) 0xF0, 0x01, 0b00101101, 0b00000101, 0, 0,
			(byte) 0b11110111 };

	private static final String PREFS_MOST_RECENT_DIRECTORY = "directory";
	private static final int SYSEX_BYTE_OFFSET_PATCH_BANK = 4;
	private static final int SYSEX_BYTE_OFFSET_PATCH_PROG = 5;
	private static final int SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA = 6;
	private static final int PROPHET_6_SYSEX_LENGTH = 1178;
	private static final int PROPHET_6_EDIT_BUFFER_LENGTH = 1176;
	private static final int SYSEX_EDIT_BUFFER_BYTE_OFFSET_PACKED_MIDI_DATA = 4;
	private static final int SYSEX_PACKED_MIDI_DATA_LENGTH = 1171;
	private static final int SYSEX_PACKED_MIDI_LAST_PACKET_LENGTH = 3;
	private static final int SYSEX_UNPACKED_MIDI_DATA_LENGTH = 1024;
	private static final int SYSEX_UNPACKED_MIDI_LAST_PACKET_LENGTH = 2;
	private static final int SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET = 107;
	private static final int SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH = 20;
	private static final int PROPHET_6_USER_BANK_COUNT = 500;
	private static final int SYSEX_SEND_DELAY_TIME = 150;

	static FileNameExtensionFilter p6libraryFilter = new FileNameExtensionFilter("Prophet 6 Library Files (*.p6lib)",
			"p6lib");
	static FileNameExtensionFilter p6programFilter = new FileNameExtensionFilter(
			"Prophet 6 Program Files (*.p6program)", "p6program");
	static FileNameExtensionFilter p6sysexFilter = new FileNameExtensionFilter("Prophet 6 SysEx Files (*.syx)", "syx");

	public Prophet6SoundLibrarian() {
		super();
	}

	private void createAndShowGUI() {

		Prophet6SoundLibrarianMainFrame mainFrame = new Prophet6SoundLibrarianMainFrame("Prophet 6 Sound Librarian");
		Prophet6SoundLibrarianMergeFrame mergeFrame = new Prophet6SoundLibrarianMergeFrame("Merge");

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

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE;
		c.weightx = 1.0;
		c.weighty = 0.0;

		mainPanel.add(mainFrame.createProgressArea(), c);

		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 2;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE;
		c.weightx = 1.0;
		c.weighty = 1.0;

		mainPanel.add(mainFrame.createDragDropList(), c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 3;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.BASELINE;
		c.weightx = 1.0;
		c.weighty = 0.0;

		mainPanel.add(mainFrame.createEditor(), c);

		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setContentPane(mainPanel);

		mainFrame.setJMenuBar(new Prophet6SoundLibrarianMenuBar());

		mainFrame.pack();

		mainFrame.setVisible(true);

		this.mainFrame = mainFrame;
		this.mergeFrame = mergeFrame;
	}

	public static void main(String args[]) {

		Prophet6SoundLibrarian p6librarian = new Prophet6SoundLibrarian();

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				// set the name of the application menu item
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Prophet 6 Sound Librarian");

				try {
					// set the look and feel
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				p6librarian.createAndShowGUI();
				try {
					Prophet6Sysex.getInstance().rescanDevices();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("%02x", b));
		return sb.toString();
	}

	public static String byteArrayToHex2(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for (byte b : a)
			sb.append(String.format("(byte)0x%02x,", b));
		return sb.toString();
	}

	public static class Prophet6SoundLibrarianMergeDragStateManager {
		private static Prophet6SoundLibrarianMergeDragStateManager instance = null;
		private boolean isDragging = false;

		private Prophet6SoundLibrarianMergeDragStateManager() {
			super();

		}

		public static Prophet6SoundLibrarianMergeDragStateManager getInstance() {
			if (instance == null)
				instance = new Prophet6SoundLibrarianMergeDragStateManager();

			return instance;
		}

		public void setDragging(boolean dragging) {
			this.isDragging = dragging;
		}

		public boolean isDragging() {
			return this.isDragging;
		}
	}

	public static class Prophet6SoundLibrarianFileValidator {

		public static void validateP6SysEx(byte[] bytes) throws Exception {
			if (bytes.length % PROPHET_6_SYSEX_LENGTH != 0)
				throw new Exception("Invalid Prophet 6 Sysex File:  Invalid Byte Length");
			for (int i = 0; i < bytes.length / PROPHET_6_SYSEX_LENGTH; i++) {
				byte[] slice = Arrays.copyOfRange(bytes, i * PROPHET_6_SYSEX_LENGTH, (i + 1) * PROPHET_6_SYSEX_LENGTH);
				validateP6Program(slice);
			}
		}

		public static void validateP6Library(byte[] bytes) throws Exception {
			if (bytes.length != PROPHET_6_SYSEX_LENGTH * PROPHET_6_USER_BANK_COUNT)
				throw new Exception("Invalid Prophet 6 Library File:  Invalid Byte Length");
			for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
				byte[] slice = Arrays.copyOfRange(bytes, i * PROPHET_6_SYSEX_LENGTH, (i + 1) * PROPHET_6_SYSEX_LENGTH);
				validateP6Program(slice);
			}
		}

		public static void validateP6Program(byte[] bytes) throws Exception {
			if (bytes.length != PROPHET_6_SYSEX_LENGTH)
				throw new Exception("Invalid Prophet 6 Program File:  Invalid Byte Length");

			if (bytes[0] != (byte) 0xf0)
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 0 - System Exclusive");
			if (bytes[1] != (byte) 0x01)
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 1 - DSI ID");
			if (bytes[2] != (byte) 0x2d)
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 2 - Prophet-6 ID");
			if (bytes[3] != (byte) 0x02)
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 3 - Program Data");

			byte bankNo = (byte) ((byte) bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] & 0x0F);
			byte progNo = (byte) ((byte) bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] & 0x7F);

			if (!(bankNo >= 0 && bankNo <= 9))
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 4 - Bank Number");
			if (!(progNo >= 0 && progNo <= 99))
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - Byte 5 - Program Number");
			if (bytes[bytes.length - 1] != (byte) 0xF7)
				throw new Exception("Invalid Prophet 6 Program File:  Sysex - End of Exclusive (EOX)");
		}
	}

	public static class Prophet6SysexPatch implements Cloneable {
		byte[] bytes;
		byte[] packedMIDIData;
		byte[] inputData;

		public static final byte[] INIT_PATCH_BYTES = new byte[] { (byte) 0xf0, (byte) 0x01, (byte) 0x2d, (byte) 0x02,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x18, (byte) 0x18, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x00, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x01, (byte) 0x00, (byte) 0x20, (byte) 0x46, (byte) 0x00, (byte) 0x00, (byte) 0x07, (byte) 0x00,
				(byte) 0x23, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x28, (byte) 0x00, (byte) 0x00, (byte) 0x0a, (byte) 0x00,
				(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x40, (byte) 0x64, (byte) 0x40, (byte) 0x64, (byte) 0x40,
				(byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x03, (byte) 0x03, (byte) 0x00, (byte) 0x06, (byte) 0x0b,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x78, (byte) 0x00,
				(byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x42, (byte) 0x61, (byte) 0x73,
				(byte) 0x69, (byte) 0x63, (byte) 0x00, (byte) 0x20, (byte) 0x50, (byte) 0x72, (byte) 0x6f, (byte) 0x67,
				(byte) 0x72, (byte) 0x61, (byte) 0x00, (byte) 0x6d, (byte) 0x20, (byte) 0x20, (byte) 0x20, (byte) 0x20,
				(byte) 0x20, (byte) 0x20, (byte) 0x30, (byte) 0x20, (byte) 0x64, (byte) 0x3c, (byte) 0x40, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x3c, (byte) 0x02, (byte) 0x43, (byte) 0x7f, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x78, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x07, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x3c, (byte) 0x00, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c, (byte) 0x3c,
				(byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
				(byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x02,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
				(byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x00, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x00, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f, (byte) 0x7f,
				(byte) 0x7f, (byte) 0x7f, (byte) 0x03, (byte) 0x7f, (byte) 0x7f, (byte) 0xf7 };

		public Prophet6SysexPatch(byte[] bytes) {
			this.bytes = bytes.clone();
			this.packedMIDIData = parsePackedMIDIData();
			this.inputData = unpackMIDIData(this.packedMIDIData);
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			Prophet6SysexPatch patch = (Prophet6SysexPatch) super.clone();
			patch.bytes = this.bytes.clone();
			patch.packedMIDIData = this.packedMIDIData.clone();
			patch.inputData = this.inputData.clone();
			return patch;
		}

		public byte[] getPatchAuditionBytes() {
			byte[] auditionData = new byte[PROPHET_6_EDIT_BUFFER_LENGTH];
			auditionData[0] = (byte) 0xf0;
			auditionData[1] = (byte) 0x01;
			auditionData[2] = (byte) 0x2d;
			auditionData[3] = (byte) 0x03;
			System.arraycopy(this.packedMIDIData, 0, auditionData, SYSEX_EDIT_BUFFER_BYTE_OFFSET_PACKED_MIDI_DATA,
					SYSEX_PACKED_MIDI_DATA_LENGTH);
			auditionData[PROPHET_6_EDIT_BUFFER_LENGTH - 1] = (byte) 0xf7;
			return auditionData;
		}

		public void setPatchName(String s) {
			assert s.length() <= SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH;

			while (s.length() < SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH) {
				s = s + " ";
			}

			byte[] nameBytes = s.getBytes(StandardCharsets.UTF_8);

			System.arraycopy(nameBytes, 0, this.inputData, SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET,
					SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH);

			this.packedMIDIData = packMIDIData(this.inputData);

			System.arraycopy(this.packedMIDIData, 0, this.bytes, SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA,
					SYSEX_PACKED_MIDI_DATA_LENGTH);
		}

		public byte[] parsePackedMIDIData() {
			byte[] packedMIDIData = Arrays.copyOfRange(bytes, SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA,
					SYSEX_BYTE_OFFSET_PACKED_MIDI_DATA + SYSEX_PACKED_MIDI_DATA_LENGTH);
			return packedMIDIData;
		}

		public byte[] packMIDIData(byte[] data) {
			assert data.length == SYSEX_UNPACKED_MIDI_DATA_LENGTH;
			byte[] packedMIDIData = new byte[SYSEX_PACKED_MIDI_DATA_LENGTH];
			byte[] unpackedMIDIData = data;

			ByteBuffer bb = ByteBuffer.wrap(packedMIDIData);

			int i = 0;
			for (i = 0; i < SYSEX_UNPACKED_MIDI_DATA_LENGTH - SYSEX_UNPACKED_MIDI_LAST_PACKET_LENGTH;) {
				byte a = unpackedMIDIData[i++];
				byte b = unpackedMIDIData[i++];
				byte c = unpackedMIDIData[i++];
				byte d = unpackedMIDIData[i++];
				byte e = unpackedMIDIData[i++];
				byte f = unpackedMIDIData[i++];
				byte g = unpackedMIDIData[i++];

				byte a7 = (byte) ((a & 0x80) >> 7);
				byte b7 = (byte) ((b & 0x80) >> 6);
				byte c7 = (byte) ((c & 0x80) >> 5);
				byte d7 = (byte) ((d & 0x80) >> 4);
				byte e7 = (byte) ((e & 0x80) >> 3);
				byte f7 = (byte) ((f & 0x80) >> 2);
				byte g7 = (byte) ((g & 0x80) >> 1);

				byte eigthByte = (byte) (a7 | b7 | c7 | d7 | e7 | f7 | g7);

				bb.put(eigthByte);
				bb.put((byte) (a & 0x7F));
				bb.put((byte) (b & 0x7F));
				bb.put((byte) (c & 0x7F));
				bb.put((byte) (d & 0x7F));
				bb.put((byte) (e & 0x7F));
				bb.put((byte) (f & 0x7F));
				bb.put((byte) (g & 0x7F));
			}
			/* there are 3 bytes remaining */
			byte a = unpackedMIDIData[i++];
			byte b = unpackedMIDIData[i++];

			byte a7 = (byte) ((a & 0x80) >> 7);
			byte b7 = (byte) ((b & 0x80) >> 6);

			byte eigthByte = (byte) (a7 | b7);

			bb.put(eigthByte);
			bb.put((byte) (a & 0x7F));
			bb.put((byte) (b & 0x7F));

			return bb.array();
		}

		public byte[] unpackMIDIData(byte[] data) {
			byte[] packedMIDIData = data;
			byte[] unpackedMIDIData = new byte[SYSEX_UNPACKED_MIDI_DATA_LENGTH];

			ByteBuffer bb = ByteBuffer.wrap(unpackedMIDIData);

			int i = 0;
			for (i = 0; i < SYSEX_PACKED_MIDI_DATA_LENGTH - SYSEX_PACKED_MIDI_LAST_PACKET_LENGTH;) {
				byte eigthByte = packedMIDIData[i++];
				byte a = packedMIDIData[i++];
				byte b = packedMIDIData[i++];
				byte c = packedMIDIData[i++];
				byte d = packedMIDIData[i++];
				byte e = packedMIDIData[i++];
				byte f = packedMIDIData[i++];
				byte g = packedMIDIData[i++];

				byte newA = (byte) (((eigthByte & 0x01) << 7) | a);
				byte newB = (byte) (((eigthByte & 0x02) << 6) | b);
				byte newC = (byte) (((eigthByte & 0x04) << 5) | c);
				byte newD = (byte) (((eigthByte & 0x08) << 4) | d);
				byte newE = (byte) (((eigthByte & 0x10) << 3) | e);
				byte newF = (byte) (((eigthByte & 0x20) << 2) | f);
				byte newG = (byte) (((eigthByte & 0x40) << 1) | g);

				bb.put(newA);
				bb.put(newB);
				bb.put(newC);
				bb.put(newD);
				bb.put(newE);
				bb.put(newF);
				bb.put(newG);
			}
			/* there are 3 bytes remaining */
			byte eigthByte = packedMIDIData[i++];
			byte a = packedMIDIData[i++];
			byte b = packedMIDIData[i++];
			byte newA = (byte) (((eigthByte & 0x01) << 7) | a);
			byte newB = (byte) (((eigthByte & 0x02) << 6) | b);
			bb.put(newA);
			bb.put(newB);

			return bb.array();
		}

		public String getPatchName() {
			byte[] patchNameBytes = Arrays.copyOfRange(inputData, SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET,
					SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_OFFSET + SYSEX_UNPACKED_MIDI_DATA_PATCH_NAME_LENGTH);

			String s = new String(patchNameBytes, StandardCharsets.UTF_8);
			return s;
		}

		public int getPatchBank() {
			return (int) bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] & 0x0F;
		}

		public int getPatchProg() {
			return (int) bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] & 0x7F;
		}

		public String toString() {
			int bankProg = getPatchBank() * 100 + getPatchProg();
			return String.format("%03d", bankProg) + " " + getPatchName().replaceAll("\\s+$", "");
		}

		public String getBankProgPadded() {
			int bankProg = getPatchBank() * 100 + getPatchProg();
			return String.format("%03d", bankProg);
		}

		public void setPatchBank(int bankNo) {
			assert bankNo >= 0 && bankNo <= 9;
			bytes[SYSEX_BYTE_OFFSET_PATCH_BANK] = (byte) ((byte) bankNo & 0x0F);
		}

		public void setPatchProg(int progNo) {
			assert progNo >= 0 && progNo <= 99;
			bytes[SYSEX_BYTE_OFFSET_PATCH_PROG] = (byte) ((byte) progNo & 0x7F);
		}
	}

	public static class Prophet6SysexDummySelection implements Transferable {

		private static DataFlavor dmselFlavor = new DataFlavor(Object.class, "Prophet 6 Sysex Dummy data flavor");
		private Object selection;

		public Prophet6SysexDummySelection(Object selection) {
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

	public static class Prophet6SysexPatchMultiSelection implements Transferable {

		private static DataFlavor multiPatchFlavor = new DataFlavor(List.class,
				"Prophet 6 Sysex Multiple Patch data flavor");

		private List<Prophet6SysexPatch> selection;

		public Prophet6SysexPatchMultiSelection(List<Prophet6SysexPatch> selection) {
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

	public static class Prophet6SysexPatchSelection implements Transferable, ClipboardOwner {

		private static DataFlavor dmselFlavor = new DataFlavor(Prophet6SysexPatch.class,
				"Prophet 6 Sysex Patch data flavor");
		private Prophet6SysexPatch selection;

		public Prophet6SysexPatchSelection(Prophet6SysexPatch selection) {
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

	public class Prophet6SysexTableItemModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;
		private List<Prophet6SysexPatch> patches;
		public String[] headers = new String[] { "#", "Name" };

		public Prophet6SysexTableItemModel() {
			this.patches = new ArrayList<Prophet6SysexPatch>();
			for (int i = 0; i < PROPHET_6_USER_BANK_COUNT; i++) {
				this.patches.add(new Prophet6SysexPatch(Prophet6SysexPatch.INIT_PATCH_BYTES));
				updateAllPatchNumbers();
			}
		}

		public Prophet6SysexTableItemModel(List<Prophet6SysexPatch> patches) {

			this.patches = new ArrayList<Prophet6SysexPatch>(patches);

		}

		@Override
		public void fireTableDataChanged() {
			super.fireTableDataChanged();
			updateAllPatchNumbers();
		}

		public void updateAllPatchNumbers() {
			for (int i = 0; i < patches.size(); i++) {
				Prophet6SysexPatch patch = patches.get(i);
				patch.setPatchBank(i / 100);
				patch.setPatchProg(i % 100);
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

		public void addRow(Prophet6SysexPatch patch) {
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
			Prophet6SysexPatch patch = patches.get(rowIndex);
			switch (columnIndex) {
			case 0:
//				value = patch.getPatchBank() + "-" + patch.getPatchProg();
				value = patch.getBankProgPadded();
				break;
			case 1:
				value = patch.getPatchName();
				break;
			}

			return value;

		}

		public List<Prophet6SysexPatch> getPatches() {
			return patches;
		}

		public Prophet6SysexPatch getProphet6SysexPatchAt(int row) {
			return patches.get(row);
		}

		public void setPatches(List<Prophet6SysexPatch> patches) {
			this.patches = patches;
		}

	}

}
