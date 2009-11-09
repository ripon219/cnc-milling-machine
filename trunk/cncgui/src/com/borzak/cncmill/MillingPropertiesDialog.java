/*
 * Created on Apr 28, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.*;


public class MillingPropertiesDialog extends JDialog {
	private static Log log = LogFactory.getLog(MillingPropertiesDialog.class);
	
	MillingProperties properties = null;
	
	JTextField tfPortname = new JTextField("COM1");
	JTextField tfZSafe = new JTextField("-10");
	JCheckBox cbShowVerticesCheckbox = new JCheckBox("Show Vertices?");
	JCheckBox cbCoolDown = new JCheckBox("Cool Down?");
	JTextField tfMaxCutTimeSecs = new JTextField("300");
	JTextField tfCoolDownSecs = new JTextField("300");

	private JButton btnApply;

	private JButton btnSave;

	private JButton btnCancel;

	public MillingPropertiesDialog(MillingProperties props) {
		super();
		
		setLayout(new BorderLayout());

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout(0,2));
		
		centerPanel.add(new JLabel("Communications Port"));
		centerPanel.add(tfPortname);

		centerPanel.add(new JLabel("Z-Axis Safe Height"));
		centerPanel.add(tfZSafe);
		
		centerPanel.add(new JLabel("Show Vertices?"));
		centerPanel.add(cbShowVerticesCheckbox);
		
		centerPanel.add(new JLabel("Enable Cool-Down?"));
		centerPanel.add(cbCoolDown);
		

		centerPanel.add(new JLabel("Max Mill Time before Cool Down (secs)"));
		centerPanel.add(tfMaxCutTimeSecs);
		
		centerPanel.add(new JLabel("Time to Cool Down (secs)"));
		centerPanel.add(tfCoolDownSecs);
	
		add("Center", centerPanel);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		buttonPanel.add(btnApply = new JButton("Apply"));
		buttonPanel.add(btnSave = new JButton("Save"));
		buttonPanel.add(btnCancel = new JButton("Cancel"));
		
		add("South",buttonPanel);
		
		btnApply.addActionListener(new ActionListener() {
		
			public void actionPerformed(ActionEvent arg0) {
				apply();
			}
		
		});
		btnSave.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				save();
			}
		
		});
		btnCancel.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				cancel();
			}
		
		});
		
		setProperties(props);
		
		pack();
	}
	

	public void apply() {
		loadFromFields();
		setVisible(false);
	}

	public void save() {
		loadFromFields();
		properties.save();
		setVisible(false);
	}

	public void cancel() {
		setVisible(false);
	}


	public MillingProperties getProperties() {
		loadFromFields();
		return properties;
	}


	public void setProperties(MillingProperties properties) {
		this.properties = properties;
		loadToFields();
	}


	private void loadToFields() {
		try {
			tfPortname.setText(properties.getPortname());
			cbCoolDown.setSelected(properties.isCoolDown());
			tfCoolDownSecs.setText(""+properties.getCoolDownSecs());
			tfMaxCutTimeSecs.setText(""+properties.getMaxCutTimeSecs());
			cbShowVerticesCheckbox.setSelected(properties.isShowVertices());
			tfZSafe.setText(""+properties.getZsafe());
		} catch (RuntimeException e) {
			log.error("in MillingPropertiesDialog.loadToFields", e);
		}
	}


	private void loadFromFields() {
		try {
			properties.setPortname(tfPortname.getText());
			properties.setCoolDown(cbCoolDown.isSelected());
			properties.setCoolDownSecs(Integer.parseInt(tfCoolDownSecs.getText()));
			properties.setMaxCutTimeSecs(Integer.parseInt(tfMaxCutTimeSecs.getText()));
			properties.setShowVertices(cbShowVerticesCheckbox.isSelected());
			properties.setZsafe(Integer.parseInt(tfZSafe.getText()));
		} catch (NumberFormatException e) {
			log.error("in MillingPropertiesDialog.loadFromFields", e);
		}
	}
}
