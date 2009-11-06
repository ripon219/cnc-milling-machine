/*
 * Created on Apr 26, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.util.*;

import org.apache.commons.logging.*;

public class MirrorCommand extends ActionListCommand {
	private static Log log = LogFactory.getLog(MirrorCommand.class);
	private MillingTransform transform = new MirrorXTransform();

	public MirrorCommand(MillingActionList actionsList,
			ProgressStatusBar statusBar) {
		super("Mirror X Axis", actionsList, statusBar);
	}

	protected void execute() {
		List list = actionsList.getSelectedList(0);
		List newActions = new LinkedList();
		
		// Now mirror x axis on all actions
		log.debug("Mirroring around X axis");
		
		
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			MillingAction a = (MillingAction) iter.next();
			newActions.add(a.getTransformedInstance(transform));
		}
		
		// Add the new traces
		actionsList.addAll(newActions);
		// discard the existing traces
		actionsList.removeAll(list);
	}

}
