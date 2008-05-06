/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.event.*;

import javax.swing.*;

import org.apache.commons.logging.*;

public abstract class ActionListCommand extends AbstractAction implements MaskConstants {
	private static Log log = LogFactory.getLog(ActionListCommand.class);

	private static final long serialVersionUID = 1L;
	protected MillingActionList actionsList;
	protected ProgressStatusBar statusBar = null;
	protected String name;
	
	public ActionListCommand(String name, MillingActionList actionsList, ProgressStatusBar statusBar) {
		super(name);
		this.name = name;
		this.actionsList = actionsList;
		this.statusBar = statusBar;
		actionsList.addCommand(this);
	}
	
	public void actionPerformed(ActionEvent evt) {

		actionsList.enableCommands(false);
		try {
			showDialog();
			actionsList.saveUndoBuffer();
			
			Thread t = new Thread(name){
				public void run() {
					try {
						statusBar.setMessage("RUNNING: "+ name);
						statusBar.startProgress(0);
						execute();
					} catch (Exception e) {
						log.error("While processing thread:"+getName(),e);
					}
					if (statusBar.isRunning()) {
						statusBar.setMessage("COMPLETED "+ name);
					}
					statusBar.incrementProgress(0,0);
					statusBar.stopProgress();
					actionsList.enableCommands(true);
					actionsList.drawAll();
				}
			};
			t.start();
		} catch (RuntimeException e) {
			log.error(e);
			statusBar.setMessage("ERROR executing command! - see log");
			actionsList.enableCommands(true);
			actionsList.drawAll();
		}
		
	}

	protected void showDialog() {
		return;  // defualt is not dialog
	}

	protected abstract void execute();
}
