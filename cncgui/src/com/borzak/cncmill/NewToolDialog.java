/*
 * Created on Feb 3, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.*;

public class NewToolDialog extends JDialog {
	private static Log log = LogFactory.getLog(NewToolDialog.class);
	
	private JComboBox cbType;
	private JTextField tfDiameter;
	private JTextField tfDesc;

	private JTextField tfMaxDepth;

	public NewToolDialog(Dialog parent) {
		super(parent);
		setTitle("Create a new Tool");
		setModal(true);
		
		Container content = getContentPane();
		content.setLayout(new GridLayout(0,2));
		
		content.add(new JLabel("Type"));
		cbType = new JComboBox();
		cbType.addItem("Drill");
		cbType.addItem("EndMill");
		content.add(cbType);

		content.add(new JLabel("Diameter (inches)"));
		tfDiameter = new JTextField();
		content.add(tfDiameter);
		
		content.add(new JLabel("Description"));
		tfDesc = new JTextField();
		content.add(tfDesc);
		
		content.add(new JLabel("Max Depth per pass"));
		tfMaxDepth = new JTextField("0");
		content.add(tfMaxDepth);
		
		tfDesc.addFocusListener(new FocusAdapter() {
		
			public void focusGained(FocusEvent arg0) {
				super.focusGained(arg0);
				if (tfDesc.getText().length() == 0) {
					tfDesc.setText(cbType.getSelectedItem()+" "+tfDiameter.getText());
				}
			}
		
		});
		
		
		JPanel buttons = new JPanel();
		content.add(buttons);
		JButton okBtn = new JButton("Ok");
		buttons.add(okBtn);
		
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		
		pack();
	}
	
	public Tool getTool() {
		int type = 0;
		if (cbType.getSelectedItem().equals("Drill")) {
			type = Tool.DRILL;
		}
		if (cbType.getSelectedItem().equals("EndMill")) {
			type = Tool.ENDMILL;
		}
		
		double diameter = 0;
		try {
			diameter = Double.parseDouble(tfDiameter.getText());
		} catch (NumberFormatException e) {
			log.debug("diameter text field is not a number",e);
		}
		
		String desc = tfDesc.getText();
		
		int maxDepth = 0;
		try {
			maxDepth = Integer.parseInt(tfMaxDepth.getText());
		} catch (NumberFormatException e) {
			log.debug("max depth per pass is not a valid number",e);
		}
		
		Tool tool = new Tool(type, diameter, desc);
		tool.setMaxDepth(maxDepth);
		return tool;
	}

}
