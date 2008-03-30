package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import org.apache.commons.logging.*;

/**
 * Displays the file load options dialog.
 * 
 * @author Vincent Greene
 */
public class FileLoadDialog extends JDialog {
	private static Log log = LogFactory.getLog(FileLoadDialog.class);

	
	public static int ACTION_CANCEL = 0;
	public static int ACTION_LOAD = 1;
	public int exitAction = 0;
	
	JFrame parent = null;
	
	JobProperties properties = new JobProperties();	
	
	private JButton loadButton;
	private JButton cancelButton;

	private JTextField tfX;
	private JTextField tfY;
	private JRadioButton rbReplace;
	private JRadioButton rbAppend;
	private JCheckBox cbDisplay;
	private JCheckBox cbSkipShort;
	private JComboBox selFormat;
	private JTextField tfFileName;
	private JButton btnBrowse;
	private JTextField tfDepth;
	private JTextField tfDiameter;
	private JCheckBox cbOverrideDepth;
	private JCheckBox cbOverrideTool;
	
	static File lastFile = new File(".");
	
	

	public FileLoadDialog(JFrame arg0) throws HeadlessException {
		super(arg0);
		this.parent = arg0;
		
		Container content = getContentPane();
		
		// Create the components
		
		tfFileName = new JTextField();
		tfFileName.setColumns(30);
		try {
			tfFileName.setText(lastFile.getCanonicalPath());
		} catch (IOException e1) {
			log.error("in FileLoadDialog.FileLoadDialog", e1);
		}
		
		btnBrowse = new JButton("Browse...");
		btnBrowse.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent arg0) {
				Frame tempFrame = new Frame();
				FileDialog chooser = new FileDialog(parent);
				chooser.setDirectory(tfFileName.getText());
				chooser.setMode(FileDialog.LOAD);
				chooser.setModal(true);
//				chooser.show();
				chooser.setVisible(true);
				
				tempFrame = null;
					try {
						tfFileName.setText(chooser.getDirectory()+chooser.getFile());
						lastFile = new File(chooser.getDirectory()+chooser.getFile());
						if (tfFileName.getText().endsWith(".rml")) {
							selFormat.setSelectedIndex(JobProperties.RML_FORMAT);
						}
						if (tfFileName.getText().endsWith(".grb")) {
							selFormat.setSelectedIndex(JobProperties.RS274X_FORMAT);
						}
						if (tfFileName.getText().endsWith(".drl")) {
							selFormat.setSelectedIndex(JobProperties.NC_FORMAT);
						}
					} catch (Exception e) {
						log.error("in FileLoadDialog.FileLoadDialog", e);
					} 
				}
		
		});
		
		tfX = new JTextField("0");
		tfX.setColumns(6);
		tfY = new JTextField("0");
		tfY.setColumns(6);
		
		rbReplace = new JRadioButton("Replace");
		rbAppend = new JRadioButton("Append");
		
		ButtonGroup replaceGroup = new ButtonGroup();
		replaceGroup.add(rbReplace);
		replaceGroup.add(rbAppend);
		rbReplace.setSelected(true);
		
		
		cbDisplay = new JCheckBox("Display Only?");
		cbSkipShort = new JCheckBox("Skip Short Segments?");
		selFormat = new JComboBox();

		
		String[] formats = properties.getFileFormats();
		
		for (int i = 0; i < formats.length; i++) {
			String s = formats[i];
			selFormat.addItem(s);
		}
		
		tfDepth = new JTextField("40");
		tfDepth.setColumns(6);
		tfDiameter = new JTextField("4");
		tfDiameter.setColumns(6);
		cbOverrideDepth = new JCheckBox("Override File?");
		cbOverrideTool = new JCheckBox("Override File?");
		
		
		loadButton = new JButton("Load");
		cancelButton = new JButton("Cancel");
		ActionListener loadListener = new ActionListener() {
			
			public void actionPerformed(ActionEvent evt) {
				dispose();
				if (evt.getSource() == cancelButton) {
					exitAction = ACTION_CANCEL;
				} else if (evt.getSource() == loadButton) {
					exitAction = ACTION_LOAD;
				}
			}
		
		};
		
		loadButton.addActionListener(loadListener);
		cancelButton.addActionListener(loadListener);

		
		// Layout the components
		
		content.setLayout(new GridLayout(0,1));
		
		JPanel panel1 = new JPanel();
		panel1.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel1);
		panel1.add(new JLabel("Filename:"));
		panel1.add(tfFileName);
		panel1.add(btnBrowse);

		JPanel panel2 = new JPanel();
		panel2.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel2);
		panel2.add(new JLabel("Origin Location:"));
		panel2.add(new JLabel("X:"));
		panel2.add(tfX);
		panel2.add(new JLabel("Y:"));
		panel2.add(tfY);
		
		JPanel panel3 = new JPanel();
		panel3.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel3);
		panel3.add(rbReplace);
		panel3.add(rbAppend);

		content.add(cbDisplay);
		content.add(cbSkipShort);

		JPanel panel4 = new JPanel();
		panel4.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel4);
		panel4.add(new JLabel("File Type:"));
		panel4.add(selFormat);
		
		JPanel panel5 = new JPanel();
		panel5.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel5);
		panel5.add(new JLabel("Depth:"));
		panel5.add(tfDepth);
		panel5.add(cbOverrideDepth);

		JPanel panel6 = new JPanel();
		panel6.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel6);
		panel6.add(new JLabel("Tool Diameter:"));
		panel6.add(tfDiameter);
		panel6.add(cbOverrideTool);

		JPanel panel7 = new JPanel();
		panel7.setLayout(new FlowLayout(FlowLayout.LEFT));
		content.add(panel7);
		panel7.add(loadButton);
		panel7.add(cancelButton);
		
		getProperties(); // sets the components initial values
		setModal(true);
		pack();
	}



	public JobProperties getProperties() {

		tfX.setText(""+properties.getX());
		tfY.setText(""+properties.getY());
		if (properties.isReplace()) {
			rbReplace.setSelected(true);
		} else {
			rbAppend.setSelected(true);
		}
		
		cbDisplay.setSelected(properties.isDisplayOnly());
		cbSkipShort.setSelected(properties.isSkipShort());
		selFormat.setSelectedIndex(properties.getFileFormat());
		tfFileName.setText(properties.getFilename());
		tfDepth.setText(""+properties.getDepth());
		tfDiameter.setText(""+properties.getDiameter());
		cbOverrideDepth.setSelected(properties.isDepthOverride());
		cbOverrideTool.setSelected(properties.isToolOverride());
		
		return properties;
	}



	public void setProperties(JobProperties properties) {
		this.properties = properties;

		try {
			properties.setX(Integer.parseInt(tfX.getText()));
		} catch (NumberFormatException e) {
			log.error("X is not valid", e);
		}
		

		try {
			properties.setY(Integer.parseInt(tfY.getText()));
		} catch (NumberFormatException e) {
			log.error("Y is not valid", e);
		}
		
		properties.setReplace(rbReplace.isSelected());
		properties.setDisplayOnly(cbDisplay.isSelected());
		properties.setSkipShort(cbSkipShort.isSelected());
		properties.setFileFormat(selFormat.getSelectedIndex());
		properties.setFilename(tfFileName.getText());
		
		try {
			properties.setDepth(Integer.parseInt(tfDepth.getText()));
		} catch (NumberFormatException e) {
			log.error("depth is not valid", e);
		}

		try {
			properties.setDiameter(Integer.parseInt(tfDiameter.getText()));
		} catch (NumberFormatException e) {
			log.error("Diameter is not valid", e);
		}

		properties.setDepthOverride(cbOverrideDepth.isSelected());
		properties.setToolOverride(cbOverrideTool.isSelected());
	}



	public void dispose() {
		super.dispose();
		setProperties(properties);
	}



	public int getExitAction() {
		return exitAction;
	}

}
