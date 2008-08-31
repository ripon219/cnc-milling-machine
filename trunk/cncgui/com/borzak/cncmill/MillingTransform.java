/*
 * Created on Jul 6, 2008
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import com.vividsolutions.jts.geom.*;

public interface MillingTransform {

	public abstract MillLocation transform(int xpos, int ypos);

	public abstract Coordinate transform(Coordinate coordinate);

}