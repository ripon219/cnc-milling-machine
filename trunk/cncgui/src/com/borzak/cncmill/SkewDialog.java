/*
 * Created on Jan 13, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.*;

public class SkewDialog extends JDialog {
	private static Log log = LogFactory.getLog(SkewDialog.class);
	JButton okBtn;
	JButton cancelBtn;
	Object waitObj = null;
	int result = RESULT_UNKNOWN;
	public static final int RESULT_UNKNOWN = 0;
	public static final int RESULT_CANCEL = 1;
	public static final int RESULT_CONTINUE = 2;
	public static final int RESULT_CLOSED = 3;
	private JTextField tfX;
	private JTextField tfY;
	private JTextField tfAdjX;
	private JTextField tfAdjY;	

	public SkewDialog(Container parent, Object waitObj) throws HeadlessException {
		super((JFrame) null, true);
		setTitle("Skew Parameters");
		this.waitObj = waitObj;
		
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
		getContentPane().add("North",new JLabel("Skew Parameters"));

		JPanel btnPanel = new JPanel();
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		getContentPane().add("South",btnPanel);
		
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout(0,2));
		centerPanel.add(new JLabel("Origin"));
		JPanel xyPanel = new JPanel();
		xyPanel.setLayout(new FlowLayout());
		xyPanel.add(tfX = new JTextField("0000"));
		xyPanel.add(tfY = new JTextField("0000"));
		centerPanel.add(xyPanel);
		centerPanel.add(new JLabel("X Adjustment Factor"));
		centerPanel.add(tfAdjX = new JTextField("0.030"));
		centerPanel.add(new JLabel("Y Adjustment Factor"));
		centerPanel.add(tfAdjY = new JTextField("000.00"));
		getContentPane().add("Center",centerPanel);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		
		if (parent != null) { 
			int x = (parent.getWidth()-getWidth())/2;
			int y = (parent.getHeight()-getHeight())/2;
			setLocation(x,y);
		}
	}

	public void dispose() {
		super.dispose();
		if (result == RESULT_UNKNOWN) {
			result = RESULT_CLOSED;
		}
		synchronized (waitObj) {
			waitObj.notifyAll();
		}
	}

	public int getResult() {
		return result;
	}

	public double getXFactor() {
		return java.lang.Double.parseDouble(tfAdjX.getText());
	}

	public double getYFactor() {
		return java.lang.Double.parseDouble(tfAdjY.getText());
	}

	public MillLocation getOrigin() {
		int x = Integer.parseInt(tfX.getText());
		int y = Integer.parseInt(tfY.getText());
		return new MillLocation(x,y,0);
	}

}