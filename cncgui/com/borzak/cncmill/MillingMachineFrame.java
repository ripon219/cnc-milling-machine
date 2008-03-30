/*
 * Created on Dec 20, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import org.apache.commons.logging.*;


public class MillingMachineFrame extends JFrame implements Runnable {
	private static Log log = LogFactory.getLog(MillingMachineFrame.class);
	
	MillingMachine mill = new MillingMachine();
	MillingMachineFrame frame = this;
	private JTextField xGotoTF;
	private JTextField yGotoTF;
	private JTextField zGotoTF;
	private JLabel xCursorPos;
	private JLabel yCursorPos;
	private JLabel xMillPos;
	private JLabel yMillPos;
	private JLabel zMillPos;
	private SurfacePanel surface;
	private JTextField xSpeedTF;
	private JTextField ySpeedTF;
	private JTextField zSpeedTF;
	private JLabel yMaxLimit;
	private JLabel yMinLimit;
	private VerticalLabel xMinLimit;
	private VerticalLabel xMaxLimit;
	private JButton startStopButton;
	private Action startStopAction;
	private JCheckBox drillCB;
	private JCheckBox vacuumCB;
	private JCheckBox xHoldCB;
	private JCheckBox yHoldCB;
	private JCheckBox zHoldCB;
	private JTextField inchZTF;
	private JTextField inchYTF;
	private JTextField inchXTF;
	private JButton yPlusInchButton;
	private JButton yPlusOneButton;
	private JButton yMinusOneButton;
	private JButton yMinusInchButton;
	private JButton xPlusInchButton;
	private JButton xPlusOneButton;
	private JButton xMinusOneButton;
	private JButton xMinusInchButton;
	private JButton zPlusInchButton;
	private JButton zPlusOneButton;
	private JButton zMinusOneButton;
	private JButton zMinusInchButton;
	private JButton gotoButton;
	private File defaultDirectory = new File(".");
	private MillingActionList actionsList2 = new MillingActionList();
	private boolean running = false;

	private boolean forceStop = false;

	protected boolean stepMode = false;
	private JCheckBoxMenuItem runStep;

	private Action selectAll;

	private Action deleteAction;

	private Action fattenAction;

	private Action isolateAction;

	private Action simplifyAction;

	private Action traceAction;

	private Action fileExitAction;

	private Action loadAction;

	private JPopupMenu popup;

	private double scale = 1.0;

	private Action zoomIn;

	private Action zoomOut;

	private JCheckBoxMenuItem syncDisplay;

	private JRadioButtonMenuItem cbDrilledHole;

	private JRadioButtonMenuItem cbMilledLine;

	private JRadioButtonMenuItem cbTracePad;

	private JRadioButtonMenuItem cbTraceSegment;

	private JRadioButtonMenuItem cbTracePolygon;

	private ActionListener cbAction;

	private ButtonGroup selectGroup = new ButtonGroup();

	private ProgressStatusBar statusBar;

	private JCheckBoxMenuItem showManual;

	private JPanel controlsPanel;

	private Action resetMilling;

	private AbstractAction undoAction;

	private Action optimizeAction;

	private ToolSelectionDialog selectDialog;

	private AbstractAction rasterizeAction;
	
	
	private class MoveOffsetAction implements ActionListener {
		
		private int x=0;
		private int y=0;
		private int z=0;

		public MoveOffsetAction(int x, int y, int z) {
			this.x=x;
			this.y=y;
			this.z=z;
		}
		
		public void actionPerformed(ActionEvent evt) {
			try {
				int xOffset = x;
				int yOffset = y;
				int zOffset = z;
				if (xOffset == 9999) {
					xOffset = mill.getXInch();
				}
				if (xOffset == -9999) {
					xOffset = -mill.getXInch();
				}
				if (yOffset == 9999) {
					yOffset = mill.getYInch();
				}
				if (yOffset == -9999) {
					yOffset = -mill.getYInch();
				}
				if (zOffset == 9999) {
					zOffset = mill.getZInch();
				}
				if (zOffset == -9999) {
					zOffset = -mill.getZInch();
				}
				mill.moveOffset(xOffset,yOffset,zOffset);
			} catch (MillingException e) {
				log.error(e);
				mill.readStatus();
				stopMilling();
				showExceptionDialog(e);
			}
			refreshFromMill();
		}
	}

	private class MillPropertySetterAction implements ActionListener {
		
		private JTextComponent component;
		private Method setter;
		private PropertyEditor editor;
		
		public MillPropertySetterAction(JTextComponent component, String property) {
			this.component = component;
			
			try {
				BeanInfo millInfo = Introspector.getBeanInfo(MillingMachine.class);
				PropertyDescriptor[] props = millInfo.getPropertyDescriptors();
				
				for (int i = 0; i < props.length; i++) {
					PropertyDescriptor prop = props[i];
					if (prop.getName().equals(property)) {
						// get the setter and the property editor
						setter = prop.getWriteMethod();
						editor = PropertyEditorManager.findEditor(prop.getPropertyType());
					}
					
				}
			} catch (Exception e) {
				log.error("in MillPropertySetterAction.MillPropertySetterAction",e);
			}
			
		}
		public void actionPerformed(ActionEvent evt) {
			try {
				
				editor.setAsText(component.getText());
				setter.invoke(mill, new Object[] {editor.getValue()});
			} catch (Exception e) {
				log.error("in MillPropertySetterAction.actionPerformed", e);
				showExceptionDialog(e);
			} finally {
				refreshFromMill();
			}
		}
	}



	public static void main(String[] args) {
		MillingMachineFrame frame = new MillingMachineFrame();
		if ( (args.length > 0) && args[0].equals("-s")) {
			frame.mill.SetSimulating(true);
//			frame.mill.setSimulateDelay(1000);
		}

		frame.initialDisplay();
	}

	/**
	 * Loads the mill status and shows the initial display.
	 */
	private void initialDisplay() {
		// Load the status from the machine
		mill.readStatus();
		refreshFromMill();
		
		surface.centerView(new Point(0,0));
		
		setVisible(true);
	}

	public void showExceptionDialog(Exception e) {
		JOptionPane.showMessageDialog(this,e,"Milling Exception",JOptionPane.WARNING_MESSAGE);
	}

	public MillingMachineFrame() {
		super();

		setTitle("Milling Machine Controller");

		Container content = getContentPane();
		content.setLayout(new BorderLayout());

		JPanel millPanel = new JPanel();
		millPanel.setLayout(new BorderLayout());
		content.add(millPanel, BorderLayout.CENTER);

		yMaxLimit = new JLabel("Y Axis Max Limit");
		yMaxLimit.setHorizontalAlignment(JLabel.CENTER);
		yMaxLimit.setVerticalAlignment(JLabel.CENTER);
		yMaxLimit.setBackground(Color.GREEN);
		yMaxLimit.setOpaque(true);
		millPanel.add(yMaxLimit, BorderLayout.NORTH);

		yMinLimit = new JLabel("Y Axis Min Limit");
		yMinLimit.setHorizontalAlignment(JLabel.CENTER);
		yMinLimit.setVerticalAlignment(JLabel.CENTER);
		yMinLimit.setBackground(Color.GREEN);
		yMinLimit.setOpaque(true);
		millPanel.add(yMinLimit, BorderLayout.SOUTH);

		xMinLimit = new VerticalLabel("X Axis Min Limit");
		xMinLimit.setHorizontalAlignment(JLabel.CENTER);
		xMinLimit.setVerticalAlignment(JLabel.CENTER);
		xMinLimit.setBackground(Color.GREEN);
		xMinLimit.setOpaque(true);
		millPanel.add(xMinLimit, BorderLayout.WEST);

		xMaxLimit = new VerticalLabel("X Axis Max Limit");
		xMaxLimit.setHorizontalAlignment(JLabel.CENTER);
		xMaxLimit.setVerticalAlignment(JLabel.CENTER);
		xMaxLimit.setBackground(Color.GREEN);
		xMaxLimit.setOpaque(true);
		millPanel.add(xMaxLimit, BorderLayout.EAST);

		surface = new SurfacePanel();

		actionsList2.setSurface(surface);
		surface.setActions(actionsList2);

		millPanel.add(surface.getScrollPane(), BorderLayout.CENTER);

		inchXTF = new JTextField("000");
		inchXTF
				.addActionListener(new MillPropertySetterAction(inchXTF,
						"XInch"));

		inchYTF = new JTextField("000");
		inchYTF
				.addActionListener(new MillPropertySetterAction(inchYTF,
						"YInch"));

		inchZTF = new JTextField("000");
		inchZTF
				.addActionListener(new MillPropertySetterAction(inchZTF,
						"ZInch"));

		yPlusInchButton = new JButton("^^");
		yPlusInchButton.addActionListener(new MoveOffsetAction(0, 9999, 0));
		yPlusOneButton = new JButton("^");
		yPlusOneButton.addActionListener(new MoveOffsetAction(0, 1, 0));
		yMinusOneButton = new JButton("v");
		yMinusOneButton.addActionListener(new MoveOffsetAction(0, -1, 0));
		yMinusInchButton = new JButton("vv");
		yMinusInchButton.addActionListener(new MoveOffsetAction(0, -9999, 0));

		xPlusInchButton = new JButton(">>");
		xPlusInchButton.addActionListener(new MoveOffsetAction(9999, 0, 0));
		xPlusOneButton = new JButton(">");
		xPlusOneButton.addActionListener(new MoveOffsetAction(1, 0, 0));
		xMinusOneButton = new JButton("<");
		xMinusOneButton.addActionListener(new MoveOffsetAction(-1, 0, 0));
		xMinusInchButton = new JButton("<<");
		xMinusInchButton.addActionListener(new MoveOffsetAction(-9999, 0, 0));

		zPlusInchButton = new JButton("^^");
		zPlusInchButton.addActionListener(new MoveOffsetAction(0, 0, -9999));
		zPlusOneButton = new JButton("^");
		zPlusOneButton.addActionListener(new MoveOffsetAction(0, 0, -1));
		zMinusOneButton = new JButton("v");
		zMinusOneButton.addActionListener(new MoveOffsetAction(0, 0, 1));
		zMinusInchButton = new JButton("vv");
		zMinusInchButton.addActionListener(new MoveOffsetAction(0, 0, 9999));

		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(0, 7));

		controls.add(new JPanel());
		controls.add(inchYTF);
		controls.add(yPlusInchButton);
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(inchZTF);
		controls.add(zPlusInchButton);

		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(yPlusOneButton);
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(zPlusOneButton);

		controls.add(xMinusInchButton);
		controls.add(xMinusOneButton);
		controls.add(new JPanel());
		controls.add(xPlusOneButton);
		controls.add(xPlusInchButton);
		controls.add(new JPanel());
		controls.add(new JPanel());

		controls.add(inchXTF);
		controls.add(new JPanel());
		controls.add(yMinusOneButton);
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(zMinusOneButton);

		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(yMinusInchButton);
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(new JPanel());
		controls.add(zMinusInchButton);

		controlsPanel = new JPanel();
		controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		controlsPanel.add(controls);

		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		controlsPanel.add(buttonBox);

		startStopAction = new AbstractAction("Start/Stop"){
			public void actionPerformed(ActionEvent evt) {
				startStopAction();
			}
		};
		
		startStopButton = new JButton("Start");
		startStopButton.setBackground(Color.GREEN);
		startStopButton.addActionListener(startStopAction);
		buttonBox.add(startStopButton);

		drillCB = new JCheckBox("Drill");
		drillCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mill.setDrillRelay(drillCB.isSelected());
			}
		});
		buttonBox.add(drillCB);

		vacuumCB = new JCheckBox("Vacuum");
		vacuumCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mill.setVacuumRelay(vacuumCB.isSelected());
			}
		});
		buttonBox.add(vacuumCB);

		JPanel positionPanel = new JPanel();
		positionPanel.setLayout(new GridLayout(0, 4));

		// Headings
		positionPanel.add(new JLabel("Axis:"));
		positionPanel.add(new JLabel("X"));
		positionPanel.add(new JLabel("Y"));
		positionPanel.add(new JLabel("Z"));

		positionPanel.add(new JLabel("Calibrate:"));
		JButton calibrateX = new JButton("Zero");
		calibrateX.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane
						.showConfirmDialog(null, "Clear X Axis to Zero?") == JOptionPane.YES_OPTION) {
					mill.calibrateX();
					refreshFromMill();
				}
			}
		});
		JButton calibrateY = new JButton("Zero");
		calibrateY.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane
						.showConfirmDialog(null, "Clear Y Axis to Zero?") == JOptionPane.YES_OPTION) {
					mill.calibrateY();
					refreshFromMill();
				}
			}
		});
		JButton calibrateZ = new JButton("Zero");
		calibrateZ.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (JOptionPane
						.showConfirmDialog(null, "Clear Z Axis to Zero?") == JOptionPane.YES_OPTION) {
					mill.calibrateZ();
					refreshFromMill();
				}
			}
		});
		positionPanel.add(calibrateX);
		positionPanel.add(calibrateY);
		positionPanel.add(calibrateZ);

		xCursorPos = new JLabel("0");
		xCursorPos.setAlignmentY(JLabel.RIGHT_ALIGNMENT);
		yCursorPos = new JLabel("0");
		yCursorPos.setAlignmentY(JLabel.RIGHT_ALIGNMENT);

		positionPanel.add(new JLabel("Mouse:"));
		positionPanel.add(xCursorPos);
		positionPanel.add(yCursorPos);
		positionPanel.add(new JLabel());

		xMillPos = new JLabel("0");
		xMillPos.setAlignmentY(JLabel.RIGHT_ALIGNMENT);
		yMillPos = new JLabel("0");
		yMillPos.setAlignmentY(JLabel.RIGHT_ALIGNMENT);
		zMillPos = new JLabel("0");
		zMillPos.setAlignmentY(JLabel.RIGHT_ALIGNMENT);

		positionPanel.add(new JLabel("Mill:"));
		positionPanel.add(xMillPos);
		positionPanel.add(yMillPos);
		positionPanel.add(zMillPos);

		xGotoTF = new JTextField("-00000");
		yGotoTF = new JTextField("-00000");
		zGotoTF = new JTextField("-00000");

		gotoButton = new JButton("Goto:");
		gotoButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				try {
					int x = Integer.parseInt(xGotoTF.getText());
					int y = Integer.parseInt(yGotoTF.getText());
					int z = Integer.parseInt(zGotoTF.getText());
					mill.moveTo(x, y, z);
				} catch (MillingException e) {
					log.error("Exception while moving", e);
					showExceptionDialog(e);
					mill.readStatus();
				} catch (NumberFormatException e) {
					log.error("in gotoButton.actionPerformed", e);
				} finally {
					refreshFromMill();
				}
			}

		});
		positionPanel.add(gotoButton);

		positionPanel.add(xGotoTF);
		positionPanel.add(yGotoTF);
		positionPanel.add(zGotoTF);

		xSpeedTF = new JTextField("-00000");
		xSpeedTF.addActionListener(new MillPropertySetterAction(xSpeedTF,
				"XDelay"));
		ySpeedTF = new JTextField("-00000");
		ySpeedTF.addActionListener(new MillPropertySetterAction(ySpeedTF,
				"YDelay"));
		zSpeedTF = new JTextField("-00000");
		zSpeedTF.addActionListener(new MillPropertySetterAction(zSpeedTF,
				"ZDelay"));

		positionPanel.add(new JLabel("Speed:"));
		positionPanel.add(xSpeedTF);
		positionPanel.add(ySpeedTF);
		positionPanel.add(zSpeedTF);

		xHoldCB = new JCheckBox("Hold X");
		xHoldCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mill.setXHold(xHoldCB.isSelected());
			}
		});
		yHoldCB = new JCheckBox("Hold Y");
		yHoldCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mill.setYHold(yHoldCB.isSelected());
			}
		});
		zHoldCB = new JCheckBox("Hold Z");
		zHoldCB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				mill.setZHold(zHoldCB.isSelected());
			}
		});

		positionPanel.add(new JLabel("Hold:"));
		positionPanel.add(xHoldCB);
		positionPanel.add(yHoldCB);
		positionPanel.add(zHoldCB);

		controlsPanel.add(positionPanel);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(controlsPanel, BorderLayout.NORTH);
		
		statusBar = new ProgressStatusBar();
		actionsList2.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				if (evt.getPropertyName().equals("totalCount")) {
					statusBar.setTotalCount(actionsList2.getTotalCount());
					statusBar.setTotalDistance(actionsList2.getTotalDistance());
					statusBar.setTotalTime(actionsList2.getTotalTime(mill));
					return;
				}
				if (evt.getPropertyName().equals("selectionCount")) {
					statusBar.setSelectCount(actionsList2.getSelectedCount());
					
					if (actionsList2.getSelectedCount() == 1) {
						// Show the actions selected if there is only one
						List l = actionsList2.getSelectedList(0);
						statusBar.setMessage(""+l.get(0));
					} else {
						statusBar.setMessage("");
					}
					return;
				}
				if (evt.getPropertyName().equals("selectRectangle")) {
					statusBar.setSelectCount(actionsList2.getSelectedCount());
					
					Rectangle r = (Rectangle) evt.getNewValue();
					double dx = r.width / 240.0D;
					double dy = r.height / 240.0D;
					double diag  = Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2));
					Format f = new DecimalFormat("#0.0000");
					statusBar.setMessage("dx="+f.format(Double.valueOf(dx))+", dy="+f.format(Double.valueOf(dy))+", diagonal="+f.format(Double.valueOf(diag)));
					
					return;
				}
		
			}
		});
		
		
		
		
		bottomPanel.add(statusBar, BorderLayout.SOUTH);
		
		content.add(bottomPanel, BorderLayout.SOUTH);

		surface.addMouseMotionListener(new MouseMotionAdapter() {

			public void mouseMoved(MouseEvent evt) {
				// Echo the mouse position into the text fields
				Point point = surface
						.coordinatesFromScreenPoint(evt.getPoint());
				xCursorPos.setText("" + point.x);
				yCursorPos.setText("" + point.y);
				
				statusBar.setPosition(point.x,point.y);
			}

		});

		surface.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent evt) {
				if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 1) {
					Point point = surface.coordinatesFromScreenPoint(evt
							.getPoint());
					xGotoTF.setText("" + point.x);
					yGotoTF.setText("" + point.y);
					zGotoTF.setText(zMillPos.getText());
				}
				if (evt.getButton() == MouseEvent.BUTTON3) {
					popup.show(evt.getComponent(), evt.getX(), evt.getY());
				}
				if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() >= 2) {
					// Double click Button 1 centers surface at mouse location
					surface.centerView(surface.coordinatesFromScreenPoint(evt.getPoint()));
				}
			}

		});
		
		
		// Create actions and menu items

		loadAction = new AbstractAction("Load") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent arg0) {
				loadFile();
			}
		};
		fileExitAction = new AbstractAction("Exit") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);
			}
		};

		runStep = new JCheckBoxMenuItem("Step");
		runStep.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent evt) {
				stepMode = runStep.isSelected();
				if (stepMode) {
					startStopButton.setBackground(Color.YELLOW);
					startStopButton.setText("Step");
				} else {
					startStopButton.setBackground(Color.GREEN);
					startStopButton.setText("Start");
				}
			}

		});
		
		undoAction = new AbstractAction("Undo") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				actionsList2.undo();
				undoAction.setEnabled(actionsList2.isUndoable());
			}

		};
		
		optimizeAction = new AbstractAction("Optimize Mill Paths") {
					private static final long serialVersionUID = 1L;
		
					public void actionPerformed(ActionEvent evt) {
						Thread t = new Thread(){
							public void run() {
								actionsList2.optimizeMillPaths(statusBar);
								undoAction.setEnabled(actionsList2.isUndoable());
							}
						};
						t.start();
					};
					
		
				};
		traceAction = new AbstractAction("Calculate Traces") {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				Thread t = new Thread(){
					public void run() {
						actionsList2.calculateTraces(statusBar);
						undoAction.setEnabled(actionsList2.isUndoable());
					}
				};
				t.start();
			}

		};
		simplifyAction = new AbstractAction("Simplify Traces") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				String distance = JOptionPane.showInputDialog(null,
						"Optimize Distance", "1.0");
				double ddistance = Double.parseDouble(distance);
				actionsList2.simplifyTraces(ddistance);
				undoAction.setEnabled(actionsList2.isUndoable());
			}

		};
		isolateAction = new AbstractAction("Isolate Traces") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				String distance = JOptionPane.showInputDialog(null,
						"Tool Size (inches)", "0.010");
				double ddistance = Double.parseDouble(distance);
				int idistance = (int) (ddistance * 240.0D);
				actionsList2.isolateTraces(idistance);
				undoAction.setEnabled(actionsList2.isUndoable());
			}
		};
		rasterizeAction = new AbstractAction("Rasterize Interior") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				final Tool tool = showToolSelection("Select Milling Tool");

				Thread t = new Thread(){
					public void run() {
						actionsList2.rasterizeInterior(statusBar, tool,2);
						undoAction.setEnabled(actionsList2.isUndoable());
					}
				};
				t.start();
				
				
			}
		};
		fattenAction = new AbstractAction("Fatten Traces") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {

				String distance = JOptionPane.showInputDialog(null,
						"Increase Trace Size by (inches)", "" + 1.0D / 240.0D);
				double ddistance = Double.parseDouble(distance);
				ddistance = ddistance * 240.0D;
				actionsList2.fattenTraces(ddistance);
				undoAction.setEnabled(actionsList2.isUndoable());
			}
		};
		deleteAction = new AbstractAction("Delete Selected") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				if (JOptionPane.showConfirmDialog(null,
						"Delete "+actionsList2.getSelectedCount()+" actions?") == JOptionPane.YES_OPTION) {
					actionsList2.deleteSelected();
					undoAction.setEnabled(actionsList2.isUndoable());
				}

			}
		};
		resetMilling = new AbstractAction("Reset Milling") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent evt) {
				if (JOptionPane.showConfirmDialog(null,
						"Reset all completed actions?") == JOptionPane.YES_OPTION) {
					actionsList2.resetProcessing();
				}

			}
		};
		selectAll = new AbstractAction("Select all") {

			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent arg0) {
				actionsList2.selectAll();
			}
		};
		
		zoomIn = new AbstractAction("Zoom In") {
		
			private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent arg0) {
						scale*=2;
						surface.setScale(scale);
					}
				};

		zoomOut = new AbstractAction("Zoom Out") {
					
			private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent arg0) {
						scale/=2;
						surface.setScale(scale);
					}
				};
				
		Action unZoom = new AbstractAction("Zoom to 1:1") {
					
			private static final long serialVersionUID = 1L;

					public void actionPerformed(ActionEvent arg0) {
						scale = 1.0D;
						surface.setScale(scale);
					}
				};
				
				
// View - Center Origin
		Action centerOrigin = new AbstractAction("Center Origin"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				surface.centerView(new Point(0,0));
			}
		};
				
				
				
// View - Show All
		Action showAll = new AbstractAction("Show All"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				
				Rectangle rect = actionsList2.getBounds();
				Rectangle viewSize = surface.getScrollPane().getViewport().getViewRect();
				
				double wscale = viewSize.getWidth() / rect.getWidth();
				double hscale = viewSize.getHeight() / rect.getHeight();
				
				scale = wscale < hscale ? wscale : hscale;
				
				surface.setScale(scale);
				surface.setViewPosition(surface.drawPointFromCoordinates(rect.getLocation()));
			}
		};

// View - Center Mill Position
		Action centerMill = new AbstractAction("Center Mill Position"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				surface.centerView(new Point(mill.getLocation().getX(),mill.getLocation().getY()));
			}
		};
		
// Edit - Move option (offset or use mouse)
		Action moveAction = new AbstractAction("Move"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				//TODO replace this with functional code
				JOptionPane.showMessageDialog(null, "Not Implemented");
			}
		};
		moveAction.setEnabled(false);
		
// Edit - Mirror (left-right)		
		Action mirrorAction = new AbstractAction("Mirror X Axis"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				actionsList2.mirrorActions();
				undoAction.setEnabled(actionsList2.isUndoable());
			}
		};
		moveAction.setEnabled(false);
		
// Edit - Properties
		Action propertiesAction = new AbstractAction("Properties"){

			private static final long serialVersionUID = 1L;
			
			public void actionPerformed(ActionEvent arg0) {
				//TODO replace this with functional code
				JOptionPane.showMessageDialog(null, "Not Implemented");
			}
		};
		propertiesAction.setEnabled(false);
		
				
		// Create the main menu bar
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		// File Menu
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		fileMenu.add(new JMenuItem(loadAction));
		fileMenu.add(new JMenuItem(fileExitAction));

		// Edit Menu
		JMenu editMenu = new JMenu("Edit");
		menuBar.add(editMenu);
		editMenu.add(new JMenuItem(undoAction));
		editMenu.add(new JMenuItem(traceAction));
		editMenu.add(new JMenuItem(simplifyAction));
		editMenu.add(new JMenuItem(fattenAction));
		editMenu.add(new JMenuItem(isolateAction));
		editMenu.add(new JMenuItem(optimizeAction));
		editMenu.add(new JMenuItem(rasterizeAction));
		editMenu.add(new JMenuItem(deleteAction));
		editMenu.add(new JMenuItem(mirrorAction));
		editMenu.add(new JMenuItem(moveAction));
		editMenu.add(new JMenuItem(propertiesAction));
		
		JMenu selectMenu = new JMenu("Select");
		menuBar.add(selectMenu);
		selectMenu.add(new JMenuItem(selectAll));
		selectMenu.addSeparator();
		
		// limit to submenu
		JMenu limitTo = new JMenu("Limit To:");
		selectMenu.add(limitTo);

		JRadioButtonMenuItem any;
		limitTo.add(any = createSelectMenuItem("Any Type"));
		any.setSelected(true);
		limitTo.add(cbDrilledHole = createSelectMenuItem("Drilled Hole"));
		limitTo.add(cbMilledLine = createSelectMenuItem("Milled Line"));
		limitTo.add(cbTracePad = createSelectMenuItem("Trace Pad"));
		limitTo.add(cbTraceSegment = createSelectMenuItem("Trace Segment"));
		limitTo.add(cbTracePolygon = createSelectMenuItem("Trace Polygon"));
		
		JMenu viewMenu = new JMenu("View");
		menuBar.add(viewMenu);
		viewMenu.add(new JMenuItem(unZoom));
		viewMenu.add(new JMenuItem(zoomIn));
		viewMenu.add(new JMenuItem(zoomOut));
		viewMenu.add(new JMenuItem(showAll));
		viewMenu.addSeparator();
		viewMenu.add(new JMenuItem(centerOrigin));
		viewMenu.add(new JMenuItem(centerMill));
		viewMenu.addSeparator();
		viewMenu.add(syncDisplay = new JCheckBoxMenuItem("Sync Display"));
		viewMenu.add(showManual = new JCheckBoxMenuItem("Show Manual Controls"));
		showManual.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent evt) {
				controlsPanel.setVisible(showManual.isSelected());
				controlsPanel.invalidate();
				frame.validate();
			}
		});
		showManual.setSelected(true); // initially selected
				
		// Run Menu
		JMenu runMenu = new JMenu("Run");
		menuBar.add(runMenu);
		runMenu.add(startStopAction);
		runMenu.add(runStep);
		runMenu.add(new JMenuItem(resetMilling));
		
		
		
		// Create a popup menu for the Surface area
		
		popup = new JPopupMenu("Actions");
		popup.add(new JMenuItem(selectAll));
		popup.addSeparator();
		popup.add(new JMenuItem(traceAction));
		popup.add(new JMenuItem(simplifyAction));
		popup.add(new JMenuItem(fattenAction));
		popup.add(new JMenuItem(isolateAction));
		popup.add(new JMenuItem(deleteAction));
		popup.add(new JMenuItem(mirrorAction));
		popup.add(new JMenuItem(moveAction));
		popup.add(new JMenuItem(propertiesAction));
		popup.addSeparator();
		popup.add(new JMenuItem(unZoom));
		popup.add(new JMenuItem(zoomIn));
		popup.add(new JMenuItem(zoomOut));
		popup.add(new JMenuItem(showAll));
		
		surface.add(popup);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent evt) {
				super.windowClosing(evt);
				System.exit(0);
			}

		});
		
		
		pack();

		// Register an interest in the mill movements
		mill.addPropertyChangeListener(new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent evt) {
				// just refresh the display for every movement
				if (syncDisplay.isSelected()) {
					refreshFromMill();
					Point millLoc = new Point(mill.getXLocation(), mill
							.getYLocation());
					if (surface.isVisible(millLoc)) {
						surface.centerView(millLoc);
					}
				}
			}

		});

	}

	private JRadioButtonMenuItem createSelectMenuItem(String text) {
		if (cbAction == null) {
			cbAction = new ActionListener() {
						// This handles setting the action mask by menu selections
						public void actionPerformed(ActionEvent evt) {
							int mask = 0;
							if (cbDrilledHole.isSelected()) mask |= 4;
							if (cbMilledLine.isSelected()) mask |= 8;
							if (cbTracePad.isSelected()) mask |= 128;
							if (cbTraceSegment.isSelected()) mask |= 256;
							if (cbTracePolygon.isSelected()) mask |= 512;
							actionsList2.setSelectionMask(mask);
						}
					};
		}
		JRadioButtonMenuItem cbItem = new JRadioButtonMenuItem(text);
		selectGroup.add(cbItem);
		cbItem.addActionListener(cbAction);
		return cbItem;
	}

	protected void startStopAction() {
		try {
			if (running && !stepMode) {
				// stop the current processing
				stopMilling();
			} else {
				// start processing
				if (actionsList2.isEmpty()) {
					showExceptionDialog(new Exception("no Actions to process"));
					stopMilling();
					return;
				}

				if (!stepMode && !actionsList2.isAtStartOfList()) {
					// Already processing - continue or restart?
					String[] options = {"Restart","Continue","Cancel"};
					int response = JOptionPane.showOptionDialog(this,"Restart or Continue?",
							"Restart Milling?"
							,JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[2]);
					switch (response) {
					case 0:
						actionsList2.resetProcessing();
						break;

					case 1:
						// Continue
						break;

					default:
						throw new RuntimeException("Action Cancelled by user");
					}
				}

				if (!stepMode) { // run it on another thread...
					mill.setDrillRelay(true); // Run the drill for the whole time
					new Thread(this).start();
				} else { // unless stepping
					if (actionsList2.isAtEndOfList()) {
						actionsList2.resetProcessing(); // restart at beginning for testing
					}
					new Thread(this).start();
				}
				
			}
		} catch (Exception e) {
			log.error("in MillingMachineFrame.startStopAction", e);
			showExceptionDialog(e);
		}
	}

	/**
	 * Run method that processes the current action file until it is done or stopped by user
	 */
	public void run() {
		// Basic process is get the next action and do it in a cycle until done or told to stop
		try {
			
			forceStop = false;
			running = true;
			
			if (!stepMode) {
				startStopButton.setBackground(Color.RED);
				startStopButton.setText("Stop");
			} else {
				startStopButton.setEnabled(false);
			}
			
			long runTimer = System.currentTimeMillis() + (5*60*1000);
			//TODO cooldown should be an option, as should the cut and cool down timeouts
			
			while (!actionsList2.isAtEndOfList()) {

				statusBar.setMessage("Processing actions...");
				
				if (forceStop) {
					throw new RuntimeException("Stopped by user");
				}
				
				
				if (System.currentTimeMillis() > runTimer) {
					// Take a break 
					// first save the state of the vacuum and drill motors
					boolean vacstate = mill.isVacuumRelay();
					boolean drillstate = mill.isDrillRelay();
					mill.moveOffset(0,0,-10); // lift the z-axis up 10 steps
					mill.setDrillRelay(false); // turn the drill off
					mill.setVacuumRelay(false); // turn the vacuum off
					statusBar.setMessage("Motor Cool down break");
					
					try {
						Thread.sleep(5*60*1000); // sleep for 5 minutes
					} catch (InterruptedException e) {
						log.error("Motor cool down delay interrupted", e);
					} 
					
					statusBar.setMessage("Processing actions...");
					// return everything back to where it was
					mill.setVacuumRelay(vacstate);
					mill.setDrillRelay(drillstate);
					mill.moveOffset(0,0,10); // put the z-axis back where it was
					
					// set the next cool down interval
					runTimer = System.currentTimeMillis() + (5*60*1000);
				}
				
				actionsList2.processNextAction(mill);
				if (stepMode) break;
			}
		} catch (RuntimeException e) {
			log.error("in MillingMachineFrame.startStopAction", e);
			statusBar.setMessage("Processing interrupted!");
			mill.readStatus();
			mill.setDrillRelay(false);
			mill.setVacuumRelay(false);
			refreshFromMill();
			showExceptionDialog(e);
		} finally {
			actionsList2.drawAll();
			mill.setDrillRelay(false);
			mill.setVacuumRelay(false);
			running = false;
			refreshFromMill();
			if (mill.getZLocation() >= -10) {
				mill.moveTo(-9999,-9999,-10); // raise the bit (unless it is already up)
			}
			
			startStopButton.setEnabled(true);
			if (!stepMode || actionsList2.isAtEndOfList()) {
				startStopButton.setBackground(Color.GREEN);
				startStopButton.setText("Start");
			}
			
			if (actionsList2.isAtEndOfList()) {
				// at the end - tell them
				statusBar.setMessage("Milling is complete!");
				JOptionPane.showMessageDialog(this,"Milling is Complete!","Milling is Complete!",JOptionPane.INFORMATION_MESSAGE);
			}
			
		}
	}
	
	public void stopMilling() {
		forceStop = true;
	}
	

	protected void loadFile() {
		// Prompts for a file to load and reads it into the execution buffer.
		FileLoadDialog dialog = new FileLoadDialog(this);
		JobProperties props = dialog.getProperties();
		try {
			props.setFilename(defaultDirectory.getCanonicalPath());
			dialog.getProperties(); // updates the display from the current properties
		} catch (IOException e1) {
			log.error("Cannot set default directory", e1);
		}
		dialog.setVisible(true);

		if (dialog.getExitAction() == FileLoadDialog.ACTION_CANCEL) {
			return;
		}

		File selected = new File(props.getFilename());
		File temp = selected.getParentFile();
		if (temp != null) {
			defaultDirectory = temp;
		}
		
		// read the file
		log.debug("Loading file "+selected.getAbsolutePath());
		
		actionsList2.saveUndoBuffer();
		// Load the file instructions into an array
		if (props.isReplace()) {
			actionsList2.clear();
		}
		
		try {
			
			int count = 0;
			
			switch (props.getFileFormat()) {

			case JobProperties.RML_FORMAT:
				count = loadRMLFile(selected, props);
				break;
				
			case JobProperties.NC_FORMAT:
				count = loadNCFile(selected, props);
				break;
				
			case JobProperties.RS274X_FORMAT:
				count = loadGerberFile(selected, props);
				break;
				
			default:
				count = loadCustomFile(selected, props);
				break;
			}
			
			log.info("Loaded "+count+" actions");
			actionsList2.drawAll();
			startStopButton.setEnabled(true);
			startStopButton.setVisible(true);
		} catch (Exception e) {
			log.error("in MillingMachineFrame.loadFile", e);
			showExceptionDialog(e);
		}
		
	}
	

	private int loadCustomFile(File selected, JobProperties props) throws IOException {
	FileReader fr = null;
	BufferedReader br = null;
	int depth = props.getDepth();
	int diameter = props.getDiameter();
	int count = 0;
	MillingAction action = null;

	try {
		fr = new FileReader(selected);
		br = new BufferedReader(fr);
		
		String line = null;
		Pattern drillcsv1 = Pattern.compile("([-]?[0-9]+),([-]?[0-9]+)");               //xxx,yyy
		Pattern drillcsv2 = Pattern.compile("D([-]?[0-9]+),([-]?[0-9]+),([-]?[0-9]+)"); //Dxxx,yyy,ddd
		Pattern movecsv = Pattern.compile("M([-]?[0-9]+),([-]?[0-9]+)");                //Mxxx,yyy
		Pattern routecsv = Pattern.compile("R([-]?[0-9]+),([-]?[0-9]+),([-]?[0-9]+)");  //Rxxx,yyy,ddd
		
		int lastx=-9999;
		int lasty=-9999;
		
		while ((line=br.readLine()) != null) {
			Matcher m = drillcsv1.matcher(line);
			Matcher m2 = drillcsv2.matcher(line);
			if (m.matches() || m2.matches()) {
				int xpos = 0;
				int ypos = 0;
				if (m.matches()) {
					xpos = Integer.parseInt(m.group(1));
					ypos =Integer.parseInt(m.group(2));
				}
				if (m2.matches()) {
					xpos = Integer.parseInt(m2.group(1));
					ypos =Integer.parseInt(m2.group(2));
					depth = Integer.parseInt(m2.group(3));
				}
				lastx = xpos;
				lasty = ypos;
				actionsList2.add(action = new DrilledHole(xpos, ypos, depth, diameter));
				action.setDisplayOnly(props.isDisplayOnly());
				count++;
				log.info("Loaded a drilled hole at "+xpos+","+ypos+" depth = "+depth);
				continue;
			}
			m = movecsv.matcher(line);
			if (m.matches()) {
				int xpos = 0;
				int ypos = 0;
				xpos = Integer.parseInt(m.group(1));
				ypos =Integer.parseInt(m.group(2));
				if ( (xpos == lastx) && (ypos == lasty) ) {
					log.info("Skipping duplicate move instruction to current position");
				} else {
					actionsList2.add(action = new MilledMovement(xpos, ypos));
					action.setDisplayOnly(props.isDisplayOnly());
					lastx = xpos;
					lasty = ypos;
					log.info("Loaded a rapid movement to "+xpos+","+ypos);
				}
				continue;
			}
			m = routecsv.matcher(line);
			if (m.matches()) {
				int xpos = 0;
				int ypos = 0;
				xpos = Integer.parseInt(m.group(1));
				ypos =Integer.parseInt(m.group(2));
				depth = Integer.parseInt(m.group(3));

				// skip short segments less than tool radius
				if ( props.isSkipShort() && (Math.abs(lastx-xpos) < diameter/2) && (Math.abs(lasty-ypos) < diameter/2)) {
					log.info("Skipping short segment");
					continue;
				}
				
				if (lastx != -9999) {
					actionsList2.add(action = new MilledLine(lastx, lasty, xpos, ypos, depth, diameter));
					action.setDisplayOnly(props.isDisplayOnly());
					log.info("Loaded a routed line from "+lastx+","+lasty+ " to "+xpos+","+ypos+" at depth "+depth);
					count++;
				}
				lastx = xpos;
				lasty = ypos;
				continue;
			}
			
			log.info("bypassing: "+line);
		}
		return count;
	} catch (IOException e) {
		throw e;
	} finally {
		try {
			if (br != null) {
				br.close();
			}
		} catch (IOException e) {
			log.error("Error closing br", e);
		}
		try {
			if (fr != null) {
				fr.close();
			}
		} catch (IOException e) {
			log.error("Error closing fr", e);
		}
		
	}
}	
	
	
	private int loadGerberFile(File selected, JobProperties props) throws IOException {
		// This one loads a RS274X Gerber file without messing with traces
		FileReader fr = null;
		BufferedReader br = null;
		int depth = props.getDepth();
		int diameter = props.getDiameter();
		int count = 0;
		double factor = 240.0D; // 240 steps per inch
		HashMap tools = new HashMap();
		HashMap toolDiameter = new HashMap();
		MillingAction action = null;
		int xoffset = props.getX();
		int yoffset = props.getY();
		
		// Format Specifications
		boolean omitleadingzeros = false;
		boolean omittrailingzeros = false;
		boolean absolute = true;
		
		int xbefore = 2;
		int xafter = 4;
		int ybefore = 2;
		int yafter = 4;
		
		
		tools.put("dft","Default Tool");
		toolDiameter.put("dft",new int[] {diameter});
		String newTool = "dft";
		
		try {
			StringBuffer block = new StringBuffer();
			fr = new FileReader(selected);
			br = new BufferedReader(fr);
			
			String line = null;

			int lastx=-9999;
			int lasty=-9999;
			
			while ((line=br.readLine()) != null) {
				
				int end = line.indexOf('*');
				if (end == -1) {
					// no block end means just append and look for the next
					block.append(line);
					continue;
				} else {
					// if the block is ended, process that block and start the next
					block.append(line.substring(0,end));
					StringBuffer nextBlock = null;
					if ((end < line.length()-1) && (line.charAt(end+1) == '%')) {
						end++;
					}
					if (end < line.length()-1) {
						nextBlock = new StringBuffer(line.substring(end+1));
					} else {
						nextBlock = new StringBuffer();
					}
					
					// Process the current block 
					
					Pattern patterns[] = new Pattern[] {
							Pattern.compile("\\s*G04(.*)"), // 0 = G04 Comment
							Pattern.compile("\\s*G54(D[0-9]+)"), // 1 = G54 Tool Change
							Pattern.compile("\\s*%FS(L|T)(A|I)X([0-9])([0-9])Y([0-9])([0-9])"), // 2 = Format Specification
							Pattern.compile("\\s*%MO(IN|MM)"), // 3 = Mode (Inch or Millimeter)
							Pattern.compile("\\s*%LN(.*)"), // 4 = Layer Name
							Pattern.compile("\\s*%AD(D[0-9]+)R,([.0-9]+)X([.0-9]+)"), // 5 = Define Rectangular Tool
							Pattern.compile("\\s*%AD(D[0-9]+)C,([.0-9]+)"), // 6 = Define Circular Tool
							Pattern.compile("\\s*G90"), // 7 = Absolute Mode
							Pattern.compile("\\s*G91"), // 8 = Relative Mode
							Pattern.compile("\\s*%LP(D|C)"), // 9 = Layer Polarity (Dark or Clear)
							Pattern.compile("\\s*G01X(-?[0-9]+)Y(-?[0-9]+)(D[0-9]+)"), // 10 = Linear Interpolation
							Pattern.compile("\\s*G70"), // 11 = Inch Mode
							Pattern.compile("\\s*G71"), // 12 = Millimeter Mode
							
							
							Pattern.compile(".*") // LAST (DEFAULT) = Matches anything (unrecognized)
					};
					

					
					while (block.length() > 0) {
						for (int i = 0; i < patterns.length; i++) {
							Matcher m = patterns[i].matcher(block);
							if (m.matches()) {
								
								switch (i) {
								case 0:
									//G04 comment
									log.info("Comment:"+m.group(1));
									break;
									
								case 1:
									// G54D11
									newTool = m.group(1);
									String desc = (String) tools.get(newTool);
									if (desc == null) {
										log.info("No definition loaded for tool "+newTool+" Ignoring tool change!");
									} else {
										int dsteps = ((int[]) toolDiameter.get(newTool))[0];
										diameter = dsteps;
										log.info("Load: "+desc+" Steps: "+dsteps);
										
										if (!props.isToolOverride()) {
											// if the override is not set, use the diameter set.
											diameter = dsteps;
										}
									}
									break;
	
								case 2:
									// %FS(L|T)(A|I)X([0-9])([0-9])Y([0-9])([0-9]) - Format Specfication
									omitleadingzeros = m.group(1).equals("L");
									omittrailingzeros = m.group(1).equals("T");
									absolute = m.group(2).equals("A");
									
									xbefore = Integer.parseInt(m.group(3));
									xafter = Integer.parseInt(m.group(4));
									ybefore = Integer.parseInt(m.group(5));
									yafter = Integer.parseInt(m.group(6));
									log.info("Format: Leading="+omitleadingzeros+
											" Trailing="+omittrailingzeros+
											" Absolute="+absolute+
											" X:"+xbefore+"."+xafter+
											" Y:"+ybefore+"."+yafter);
									break;
									
								case 3:
									// Mode Inch or Millimeter
									if (m.group(1).equals("IN")) {
										factor = 240.0d;
										log.info("Inch Mode");
									} else {
										factor = 9.4488D; // steps per millimeter
										log.info("Millimeter Mode");
									}
									break;
									
								case 4:
									// Layer Name "\\s*%LN(.*)"
									// For now, just ignoring this
									break; 
									
								case 5:  // Apeture definitions - Rectangular
									// pattern = %AD(D[0-9]+)R,([.0-9]+)X([.0-9]+)
									String toolName = m.group(1);
									double xdbl = Double.parseDouble(m.group(2));
									double ydbl =Double.parseDouble(m.group(3));
									
									// calculate the location/ adjust for factor
									int xlen = (int) (xdbl * factor);
									int ylen = (int) (ydbl * factor);

									tools.put(toolName,toolName+":"+m.group());
									toolDiameter.put(toolName,new int[] {xlen,ylen});
									
									log.info("Defined Rectangular Tool "+toolName+" as "+xlen+"x"+ylen+" steps");
									break;
									
								case 6:  // Apeture definitions - Circular
									// pattern = %AD(D[0-9]+)C,([.0-9]+)
									toolName = m.group(1);
									xdbl =Double.parseDouble(m.group(2));
									
									// calculate the location/ adjust for factor
									int dsteps = (int) (xdbl * factor);

									tools.put(toolName,toolName+":"+m.group());
									toolDiameter.put(toolName,new int[] {dsteps});
									
									log.info("Defined Circular Tool "+toolName+" as diameter "+dsteps);
									break;
									
								case 7:  // G90 = Absolute Mode
									absolute = true;
									break;
									
								case 8:  // G91 = Relative Mode 
									absolute = false;
									log.info("Warning: Relative Motion is not implmented!");
									break;

									
								case 9:  // %LP Layer Polarity 
									if (!(m.group(1).equals("D"))) {
										log.info("WARNING: Clear layer polarity is not supported");
									}
									break;

									
								case 10:  // G01X000000Y000000D01 - Linear Interpolation
									// "G01X([0-9]+)Y([0-9]+)(D[0-9]+)"
									int xpos = Integer.parseInt(m.group(1));
									int ypos =Integer.parseInt(m.group(2));
									
									// calculate the location/ adjust for factor
									xdbl = ((double) xpos) / 10000.0D;
									ydbl = ((double) ypos) / 10000.0D;
									xpos = (int) (xdbl * factor);
									ypos = (int) (ydbl * factor);
									xpos += xoffset;
									ypos += yoffset;
									
									if (m.group(3).equals("D03")) {
										// Draw a pad
										//TODO this should be replaced with a polygon creator
										actionsList2.add(action = new TracePad(xpos,ypos,diameter,diameter)); //TODO - Rectangle
										lastx = xpos;
										lasty = ypos;
										count++;
										log.info("Loaded a Pad at "+xpos+","+ypos+" with diameter "+diameter);
									} else if (m.group(3).equals("D01")) {
										// Draw the line 
										
										if ( (xpos == lastx) && (ypos == lasty) ) {
											log.info("Skipping duplicate route instruction to current position");
										} else {
											if (lastx != -9999) {
												action = new TraceSegment(lastx, lasty, xpos, ypos, diameter);
												actionsList2.add(action); 
												log.info("Loaded a Trace from "+lastx+","+lasty+ " to "+xpos+","+ypos+" width ="+diameter);
												count++;
											}
											lastx = xpos;
											lasty = ypos;
										}
										
									} else if (m.group(3).equals("D02")) {
										lastx = xpos;
										lasty = ypos;
									}
									break;
									
								case 11: // Inch Mode
									factor = 240.0d;
									log.info("Inch Mode");
									break;
									
								case 12: // Millimeter mode
									factor = 9.4488D; // steps per millimeter
									log.info("Millimeter Mode");
									break;
									
									
								default:
									if (block.toString().trim().length() == 0) break; // empty = no message
									log.info("Got No idea how to do: "+block);
									break;
								}
								
								block.delete(0,m.end());
							}
							
						}
					}
					
					// Now start on the next block
					block = nextBlock;
					continue;
					
				}
			}
			return count;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				log.error("Error closing br", e);
			}
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
				log.error("Error closing fr", e);
			}
			
		}
	}	
	
	

	private int loadNCFile(File selected, JobProperties props) throws IOException {
		FileReader fr = null;
		BufferedReader br = null;
		int depth = props.getDepth();
		int diameter = props.getDiameter();
		int count = 0;
		double factor = 240.0D; // 240 steps per inch
		HashMap tools = new HashMap();
		boolean drillMode = true; // assume we are drilling
		int xstart = mill.getXLocation();
		int ystart = mill.getYLocation();
		MillingAction action = null;

		Tool newTool = new Tool(Tool.DRILL,0.0256);

		/*
;Holesize 1 =   28.0 PLATED MILS
M48
INCH
T01C0.028
%
G05
G90
T01
X017000Y003500
X017000Y002500
M30


2.4 format 01.7000  00.3500  / 00.2500 (diff = .100)
*/		
		try {
			fr = new FileReader(selected);
			br = new BufferedReader(fr);
			
			String line = null;

			Pattern tooldef = Pattern.compile("T([0-9]+)C([.0-9]+)"); // Tool Definition
			Pattern toolsel = Pattern.compile("T([0-9]+)"); // Tool Selection
			Pattern drillloc = Pattern.compile("X([-]?[0-9]+)Y([-]?[0-9]+)");               //XnnnnnYnnnnn
			
			int lastx=-9999;
			int lasty=-9999;
			
			while ((line=br.readLine()) != null) {
				
				if (line.equals("M48")) {
					// Inch Mode
					factor = 240.0d;
					continue;
				}

				if (line.equals("M71")) {
					// Metric Mode .254 
					factor = 9.4488D; // steps per millimeter
					continue;
				}
				
				if (line.equals("M00")) {
					// End Program 
					continue;
				}
				
				if (line.equals("M30")) {
					// End Program and rewind
					actionsList2.add(action = new MilledMovement(xstart, ystart));
					action.setDisplayOnly(props.isDisplayOnly());
					lastx = xstart;
					lasty = ystart;
					log.info("Loaded a REWIND movement to "+xstart+","+ystart);
					continue;
				}
				
				if (line.equals("G05")) {
					// Drilling Mode
					drillMode = true;
				}
				
				if (line.equals("G90")) {
					// Absolute positioning 
					continue;
				}
				
				Matcher m = tooldef.matcher(line);
				if (m.matches()) {
					// T01C0.028
					Tool tool = showToolSelection("Tool "+m.group(1)+" Diameter = "+m.group(2));
					tools.put(m.group(1),tool);
					continue;
				}
				
				m = toolsel.matcher(line);
				if (m.matches()) {
					// Tnn - Tool selection - Change to a new tool
					// This actually just stores it to load before next drill
					newTool = (Tool) tools.get(m.group(1));
					continue;
				}
				
				
				m = drillloc.matcher(line);
				if (m.matches()) {
					int xpos = Integer.parseInt(m.group(1));
					int ypos =Integer.parseInt(m.group(2));
					
					// calculate the location/ adjust for factor
					double xdbl = ((double) xpos) / 10000.0D;
					double ydbl = ((double) ypos) / 10000.0D;
					xpos = (int) (xdbl * factor);
					ypos = (int) (ydbl * factor);
					
					lastx = xpos;
					lasty = ypos;
					actionsList2.add(action = new DrilledHole(xpos, ypos, depth, newTool));
					action.setDisplayOnly(props.isDisplayOnly());
					count++;
					log.info("Loaded a drilled hole at "+xpos+","+ypos+" depth = "+depth+" with diameter "+diameter);
					continue;
				}
				log.info("bypassing: "+line);
			}
			return count;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				log.error("Error closing br", e);
			}
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
				log.error("Error closing fr", e);
			}
			
		}
	}	
	
	
	
	public Tool showToolSelection(String desc) {

		Tool selection = null;
		
		if (selectDialog == null) {
			selectDialog = new ToolSelectionDialog(this);
		}
		
		while (selection == null) {
			selectDialog.setDescription(desc);
			selectDialog.setVisible(true);
			selection = selectDialog.getSelectedTool();
		}
		
		return selection;
	}

	private int loadRMLFile(File selected, JobProperties props) throws IOException {
		
		int xoffset = props.getX() /* -1200 for big board */;
		int yoffset = props.getY() /* -900 for big board */;
		int depth = props.getDepth();
		int diameter = props.getDiameter();
		MillingAction action = null;
		
		FileReader fr = null;
		BufferedReader br = null;
		int count = 0;

		try {
			fr = new FileReader(selected);
			br = new BufferedReader(fr);
			
			String line = null;
			Pattern movecsv = Pattern.compile("PU(-?[0-9]+),(-?[0-9]+)");
			Pattern routecsv = Pattern.compile("PD(-?[0-9]+),(-?[0-9]+)");
			
			int lastx=-9999;
			int lasty=-9999;
			
			while ((line=br.readLine()) != null) {
				String[] commands = line.split(";");
				for (int i = 0; i < commands.length; i++) {
					String command = commands[i];
					Matcher m = movecsv.matcher(command);
					if (m.matches()) {
						int xpos = 0;
						int ypos = 0;
						xpos = Integer.parseInt(m.group(1));
						ypos =Integer.parseInt(m.group(2));
						xpos += xoffset;
						ypos += yoffset;
						lastx = xpos;
						lasty = ypos;
						continue;
					}
					m = routecsv.matcher(command);
					if (m.matches()) {
						int xpos = 0;
						int ypos = 0;
						xpos = Integer.parseInt(m.group(1));
						ypos =Integer.parseInt(m.group(2));
						xpos += xoffset;
						ypos += yoffset;
						
						// skip short segments less than tool radius
						if ( props.isSkipShort() && (Math.abs(lastx-xpos) < diameter/2) && (Math.abs(lasty-ypos) < diameter/2)) {
							log.info("Skipping short segment");
							continue;
						}
						
						if ( (xpos == lastx) && (ypos == lasty) ) {
							log.info("Skipping duplicate route instruction to current position");
						} else {
							if (lastx != -9999) {
								action = new MilledLine(lastx, lasty, xpos, ypos, depth, diameter);
								action.setDisplayOnly(props.isDisplayOnly());
								actionsList2.add(action); 
								log.info("Loaded a routed line from "+lastx+","+lasty+ " to "+xpos+","+ypos+" at depth "+depth);
								count++;
							}
							lastx = xpos;
							lasty = ypos;
						}
						continue;
					}
					
					log.info("bypassing: "+command);
				}
			}
			return count;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				log.error("Error closing br", e);
			}
			try {
				if (fr != null) {
					fr.close();
				}
			} catch (IOException e) {
				log.error("Error closing fr", e);
			}
			
		}
	}	
	
	
	/**
	 * 
	 */
	private void refreshFromMill() {
		xMillPos.setText(""+mill.getXLocation());
		yMillPos.setText(""+mill.getYLocation());
		zMillPos.setText(""+mill.getZLocation());
		surface.setMillPosition(mill.getXLocation(),mill.getYLocation());
		surface.repaint(); 
		xSpeedTF.setText(""+mill.getXDelay());
		ySpeedTF.setText(""+mill.getYDelay());
		zSpeedTF.setText(""+mill.getZDelay());
		inchXTF.setText(""+mill.getXInch());
		inchYTF.setText(""+mill.getYInch());
		inchZTF.setText(""+mill.getZInch());
		yMaxLimit.setBackground(mill.isYLimitMax() ? Color.RED : Color.GREEN);
		yMinLimit.setBackground(mill.isYLimitMin() ? Color.RED : Color.GREEN);
		xMaxLimit.setBackground(mill.isXLimitMax() ? Color.RED : Color.GREEN);
		xMinLimit.setBackground(mill.isXLimitMin() ? Color.RED : Color.GREEN);
		drillCB.setSelected(mill.isDrillRelay());
		vacuumCB.setSelected(mill.isVacuumRelay());
		xHoldCB.setSelected(mill.isXHold());
		yHoldCB.setSelected(mill.isYHold());
		zHoldCB.setSelected(mill.isZHold());
		xGotoTF.setText(""+mill.getXLocation());
		yGotoTF.setText(""+mill.getYLocation());
		zGotoTF.setText(""+mill.getZLocation());
		
		zPlusInchButton.setBackground(mill.isZLimitMax() ? Color.RED : Color.GREEN);
		zMinusInchButton.setBackground(mill.isZLimitMin() ? Color.RED : Color.GREEN);
	}

	public boolean isStepMode() {
		return stepMode;
	}

	public void setStepMode(boolean stepMode) {
		this.stepMode = stepMode;
	}



}
