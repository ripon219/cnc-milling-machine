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

public class ToolChangeDialog extends JDialog {
	JButton okBtn;
	JButton cancelBtn;
	Object waitObj = null;
	int result = RESULT_UNKNOWN;
	public static final int RESULT_UNKNOWN = 0;
	public static final int RESULT_CANCEL = 1;
	public static final int RESULT_CONTINUE = 2;
	public static final int RESULT_CLOSED = 3;

	public ToolChangeDialog(Container parent, String desc, Object waitObj) throws HeadlessException {
		super();
		setTitle("Tool Change");
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
		getContentPane().add("North",new JLabel("Tool Change: "+desc));

		JPanel btnPanel = new JPanel();
		btnPanel.add(okBtn);
		btnPanel.add(cancelBtn);
		getContentPane().add("South",btnPanel);

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
	
	
	
	
	
	

}