/*
 * Created on Jan 13, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import org.apache.commons.logging.*;

public class ToolSelectionDialog extends JDialog {
	private static Log log = LogFactory.getLog(ToolSelectionDialog.class);
	JButton okBtn;
	JButton cancelBtn;
	int result = RESULT_UNKNOWN;
	public static final int RESULT_UNKNOWN = 0;
	public static final int RESULT_CANCEL = 1;
	public static final int RESULT_CONTINUE = 2;
	public static final int RESULT_CLOSED = 3;
	private JComboBox select;
	private NewToolDialog newTool;
	private JLabel lbDesc = null;
	private JPanel extraPanel = new JPanel();

	public ToolSelectionDialog(Frame parent) throws HeadlessException {
		super(parent, "Tool Selection");
		setModal(true);
		
		okBtn = new JButton("Ok");
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = RESULT_CONTINUE;
				dispose();
			}
		});
		
		cancelBtn = new JButton("Cancel");
		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				result = RESULT_CANCEL;
				dispose();
			}
		});
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add("North",lbDesc = new JLabel());

		JPanel centerTop = new JPanel();
		
		centerTop.setLayout(new GridLayout(0,2));
		
		centerTop.add(new JLabel("Select Tool:"));
		
		select = new JComboBox();
		centerTop.add(select);
		
		select.addItem("<New Tool>");
		
		loadTools();
		
		JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.add("North",centerTop);
		center.add("Center",extraPanel);

		getContentPane().add("Center",center);
		
		JPanel btnPanel = new JPanel();
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		getContentPane().add("South",btnPanel);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		
		int x = (parent.getWidth()-getWidth())/2;
		int y = (parent.getHeight()-getHeight())/2;
		setLocation(x,y);
	}

	private void loadTools() {
		FileInputStream fis = null;
		XMLDecoder ois = null;
		try {
			// Read the tools from a file
			fis = new FileInputStream("tools.xml");
			ois = new XMLDecoder(fis);
			Object o = null;
			do {
				o = ois.readObject();
				if (o instanceof Tool) {
					Tool newTool = (Tool) o;
					select.addItem(newTool);
				}
			} while (o != null);
		} catch (IOException e) {
			log.error("While loading tools", e);
		} catch (Exception e) {
			log.error("Failed to load tools:",e);
		} finally {
			if (ois != null) {
				ois.close();
			}
			
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				log.error("Error closing fis", e);
			}
		}
	}

	public int getResult() {
		return result;
	}
	
	public Tool getSelectedTool() {
		Object selected = select.getSelectedItem();
		if (selected.equals("<New Tool>")) {
			// Create a new tool here
			selected = showNewToolDialog();
			
			if (selected != null) {
				select.addItem(selected);
				
				// Save the current tools after each add
				saveTools();
			}
			
		}
		if (selected instanceof Tool) {
			Tool selectedTool = (Tool) selected;
			return selectedTool;
		}
		return null;
	}

	private void saveTools() {
		
		FileOutputStream fos = null;
		XMLEncoder oos = null;
		
		try {
			fos = new FileOutputStream("tools.xml",false);
			oos = new XMLEncoder(fos);
			for (int i = 1; i < select.getItemCount(); i++) {
				Object o = select.getItemAt(i);
				oos.writeObject(o);
			}
		} catch (IOException e) {
			log.error("in ToolSelectionDialog.saveTools", e);
		} finally {
			if (oos != null) {
				oos.close();
			}
			
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				log.error("Error closing fos", e);
			}
		}
		
	}

	private Object showNewToolDialog() {
		Object selected;
		if (newTool == null) {
			newTool = new NewToolDialog(this);
		}
		newTool.setVisible(true);
		selected = newTool.getTool();
		return selected;
	}

	public void setDescription(String desc) {
		lbDesc.setText("Select Tool for: "+desc);
		pack();
	}

	public JPanel getExtraPanel() {
		return extraPanel;
	}
	
	
	
	
	

}