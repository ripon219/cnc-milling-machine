/*
 * Created on Jul 6, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import com.vividsolutions.jts.geom.*;

public class MirrorXTransform implements MillingTransform {

	public MirrorXTransform() {
		super();
	}

	/**
	 * Transforms to mirror across the Y axis
	 * @see com.borzak.cncmill.MillingTransform#transform(int, int)
	 */
	public MillLocation transform(int xpos, int ypos) {
		return new MillLocation(-xpos,ypos,0);
	}

	/**
	 * Transforms to mirror across the Y axis
	 * @see com.borzak.cncmill.MillingTransform#transform(int, int)
	 */
	public Coordinate transform(Coordinate in) {
		return new Coordinate(-in.x,in.y,in.z);
	}
}
