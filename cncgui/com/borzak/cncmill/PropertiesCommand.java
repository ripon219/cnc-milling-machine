/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import org.apache.commons.logging.*;

public class PropertiesCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(PropertiesCommand.class);

	public PropertiesCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Show Properties", actionsList, statusBar);
	}

	protected void execute() {
//TODO Expand this to allow editing of properties using a table
		
		List list = actionsList.getSelectedList(0);
		
		// Now mirror x axis on all actions
		log.debug("Dumping Properties to log");
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingAction a = (MillingAction) iter.next();
			log.info(a);
		}
		
	}

}
