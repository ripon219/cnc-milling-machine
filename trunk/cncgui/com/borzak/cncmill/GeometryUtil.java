package com.borzak.cncmill;

import java.awt.*;

import com.vividsolutions.jts.geom.*;

public class GeometryUtil {
	
	public static Rectangle calculateBounds(Geometry g) {
		Coordinate[] envelope = g.getEnvelope().getCoordinates();
		if (envelope.length == 0) return null;
		
		int minx = (int) envelope[0].x;
		int miny = (int) envelope[0].y;
		
		int maxx = (int) envelope[2].x;
		int maxy = (int) envelope[2].y;
		
		int width = maxx-minx;
		int height = maxy-miny;
		
		Rectangle bounds = new Rectangle(minx,miny,width,height);
		return bounds;
	}

}
