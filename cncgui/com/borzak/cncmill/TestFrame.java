/*
 * Created on Dec 24, 2007
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.borzak.cncmill;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import org.omg.PortableServer.POAManagerPackage.*;

import sun.java2d.loops.*;

public class TestFrame extends JFrame {
	JPanel drawPanel;

	public static void main(String[] args) {
		new TestFrame();
	}

	public TestFrame() {
		super();
		Container content = getContentPane();

		drawPanel = new JPanel(){
		
			protected void paintComponent(Graphics g) {
				// TODO Auto-generated method stub
				super.paintComponent(g);
				Dimension size = getSize();
				g.setColor(Color.BLACK);
				Point endPoint = new Point((int) size.getWidth() / 2, (int) size
						.getHeight() / 2);
				g.drawOval(endPoint.x - 255, endPoint.y - 255, 255 * 2, 255 * 2);
				g.drawOval(endPoint.x-2,endPoint.y-2,4,4); // Center point
				g.drawLine(endPoint.x-128,endPoint.y-128,endPoint.x+128,endPoint.y+128);
				g.drawLine(endPoint.x+128,endPoint.y-128,endPoint.x-128,endPoint.y+128);
				
				g.drawRect(endPoint.x-255,endPoint.y-255,255*2,255*2);

				
			}
		
		};

		content.add(drawPanel);
		setSize(600, 600);

		drawPanel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent evt) {
				Dimension size = drawPanel.getSize();
				Point startPoint = evt.getPoint();
				Point endPoint = new Point((int) size.getWidth() / 2,
						(int) size.getHeight() / 2);

				Graphics g = drawPanel.getGraphics();

				if (evt.getButton() == MouseEvent.BUTTON1) {
					// Draw a line from center of screen to here
					g.drawLine(evt.getX(), evt.getY(), endPoint.x, endPoint.y);
				} else if (evt.getButton() == MouseEvent.BUTTON3) {
					/* dumbLineMethod(startPoint, endPoint, g); */
					fastLineMethod(startPoint, endPoint, g);
				}
			}

		});

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent evt) {
				super.windowClosing(evt);
				System.exit(0);
			}

		});

		show();

		Dimension size = drawPanel.getSize();
		Point endPoint = new Point((int) size.getWidth() / 2, (int) size
				.getHeight() / 2);
		Graphics g = drawPanel.getGraphics();
		g.drawOval(endPoint.x - 255, endPoint.y - 255, 255 * 2, 255 * 2);

	}


	void fastLineMethod(Point endPoint, Point startPoint, Graphics g) {
/*
REM Bresenham algorithm for a line in an arbitrary octant in pseudo Basic
dx = xend-xstart
dy = yend-ystart

REM Initialisations
adx = ABS(dx): ady = ABS(dy) ' Absolute values of distances
sdx = SGN(dx): sdy = SGN(dy) ' Signum of distances

IF adx > ady THEN
  ' x is fast direction
  pdx = sdx: pdy = 0   ' pd. is parallel step
  ddx = sdx: ddy = sdy ' dd. is diagonal step
  ef  = ady: es  = adx ' error steps fast, slow
             ELSE
  ' y is fast direction
  pdx = 0  : pdy = sdy ' pd. is parallel step
  ddx = sdx: ddy = sdy ' dd. is diagonal step
  ef  = adx: es  = ady ' error steps fast, slow
  END IF

x = xstart
y = ystart
SETPIXEL x,y
error = es/2 

REM Pixel loop: always a step in fast direction, every now and then also one in slow direction
FOR i=1 TO es          ' es also is the count of pixels to be drawn
   REM update error term
   error = error - ef
   IF error < 0 THEN
      error = error + es ' make error term positive (>=0) again
      REM step in both slow and fast direction
      x = x + ddx: y = y + ddy ' Diagonal step
                ELSE
      REM step in fast direction
      x = x + pdx: y = y + pdy ' Parallel step
      END IF
   SETPIXEL x,y
   NEXT 
 */
		
		
		//startPoint is always the center
		
		g.setColor(Color.RED);
		int dx = (endPoint.x - startPoint.x);
		int dy = (endPoint.y - startPoint.y);
		
		int xStepsToGo =  Math.abs(dx);
		int yStepsToGo =  Math.abs(dy);
		int xDirection =  (dx > 0 ? 1 : -1);
		int yDirection =  (dy > 0 ? 1 : -1);
		
		int pdx,pdy,ddx,ddy,ef,es;
		ddx = xDirection;
		ddy = yDirection; // dd is a diagonal step
		
		if (xStepsToGo > yStepsToGo) {
			// x is the fast direction
			pdx = xDirection;
			pdy = 0;  // pd is parallel step 
			ef = yStepsToGo; es = xStepsToGo; // fast error step is y, slow is x
		} else {
			// y is the fast direction
			pdx = 0;
			pdy = yDirection;  // pd is parallel step 
			ef = xStepsToGo; es = yStepsToGo; // fast error step is x, slow is y
		}
		
		int xOffset = 0;
		int yOffset = 0;
		g.drawLine(startPoint.x,startPoint.y,startPoint.x,startPoint.y);
		
		
		int error = (byte) (es << 1);
		
		for (int i = 1; i < es; i++) {
			// step in the fast direction
			error -= ef; //decrease the error term
			if (error < 0) {
				error += es; // adjust for a slow step
				// diagonal step 
				xOffset += ddx; // step x
				yOffset += ddy; // step y
			} else {
				xOffset += pdx;  // step fast (parallel)
				yOffset += pdy; 
			}
			if (error > 255) {
				g.setColor(Color.BLUE);
			}
			g.drawLine(startPoint.x+xOffset,startPoint.y+yOffset,startPoint.x+xOffset,startPoint.y+yOffset);
		}
		
	}
	
	
	
	void smartLineMethod(Point startPoint, Point endPoint, Graphics g) {
		{
			
			int x=startPoint.x;
			int y=startPoint.y;
			int xStepsToGo = endPoint.x-startPoint.x;
			int y2 = endPoint.y-startPoint.y;

			int yDirection, xDirection;
			int dx, dy, incE, incNE, d;
			dx = xStepsToGo;
			dy = y2;
			// Adjust y-increment for negatively sloped lines
			if (dy < 0) {
				yDirection = -1;
				dy = -dy;
			} else {
				yDirection = 1;
			}

			// Adjust x-increment for negatively sloped lines
			if (dx < 0) {
				xDirection = -1;
				dx = -dx;
				xStepsToGo = -xStepsToGo;
			} else {
				xDirection = 1;
			}
			
			
			// Bresenham constants
			incE = 2 * dy;
			incNE = 2 * dy - 2 * dx;
			d = 2 * dy - dx;
			// Blit
			while (xStepsToGo > 0) {
				xStepsToGo--;
				if (d <= 0) {
					// step in the x direction only
					x+=xDirection;
					d += incE;
					g.drawLine(x, y, x, y);
				} else {
					// step in the x and y direction
					d += incNE;
					x+=xDirection;
					y += yDirection;
					g.drawLine(x, y, x, y);
				}
			}
		}
	}

	/**
	 * @param startPoint
	 * @param endPoint
	 * @param g
	 */
	private void dumbLineMethod(Point startPoint, Point endPoint, Graphics g) {
		// duplicate logic in the controller to draw a line

		int xLocation = startPoint.x;
		int yLocation = startPoint.y;
		int xStepsToGo = endPoint.x - startPoint.x;
		int yStepsToGo = endPoint.y - startPoint.y;
		boolean xDirection = true; // assume positive
		boolean yDirection = true; // assume positive

		if (xStepsToGo < 0) {
			xDirection = false;
			xStepsToGo = 0 - xStepsToGo;
		}
		if (yStepsToGo < 0) {
			yDirection = false;
			yStepsToGo = 0 - yStepsToGo;
		}

		while ((xStepsToGo > 0) || (yStepsToGo > 0)) {
			g.drawLine(xLocation, yLocation, xLocation, yLocation); // one point
																	// only
			if (xStepsToGo > 0) {
				xStepsToGo--;
				if (xDirection) {
					xLocation++;
				} else {
					xLocation--;
				}
			}
			if (yStepsToGo > 0) {
				yStepsToGo--;
				if (yDirection) {
					yLocation++;
				} else {
					yLocation--;
				}
			}
		}
	}

}
