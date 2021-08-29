package com.github.skjolber.packing.impl.points;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;

import com.github.skjolber.packing.impl.ExtremePoint;
import com.github.skjolber.packing.impl.ExtremePoints;


public class DrawOnImage
{
	public static void show(ExtremePoints p) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				init(p);
			}
		});
	}
	
	public static void main(String[] args) {
		ExtremePoints extremePoints = new ExtremePoints(1000, 1000);
		
		ExtremePoint extremePoint = extremePoints.getValues().get(0);
		extremePoints.add(extremePoint, 50, 100);

		System.out.println("One: " + extremePoints);
		
		ExtremePoint extremePoint2 = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoint2, 50, 50);
		
		/*
		System.out.println("Two: " + extremePoints);
		
		ExtremePoint extremePoint3 = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoint3, 50, 50);

		System.out.println("Three: " + extremePoints);

		ExtremePoint extremePoint4 = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		extremePoints.add(extremePoint4, 50, 25);

		System.out.println("Four: " + extremePoints);
*/
		ExtremePoint extremePoint5 = extremePoints.getValues().get(1);
		extremePoints.add(extremePoint5, 75, 75);

		ExtremePoint extremePoint4 = extremePoints.getValues().get(extremePoints.getValues().size() - 2);
		extremePoints.add(extremePoint4, 10, 10);

		ExtremePoint extremePoint6 = extremePoints.getValues().get(extremePoints.getValues().size() - 1);
		//extremePoints.add(extremePoint6, 10, 20);
		
		DrawOnImage.show(extremePoints);
	}
	
    public static final Color blue      = new Color(200, 222, 255);

	private static void init(ExtremePoints p) {
		DrawingArea drawingArea = new DrawingArea(p.getWidth(), p.getDepth());

		for (ExtremePoint extremePoint : p.getValues()) {
			if(extremePoint.getXx() != p.getWidth() || extremePoint.getYy() != p.getDepth()) {
				//System.out.println("Paint blue " + extremePoint.getX() + "x" + extremePoint.getY() + " " + extremePoint.getXx() + "x" + extremePoint.getYy());
				drawingArea.fillRect(extremePoint.getX(), extremePoint.getY(), extremePoint.getXx(), extremePoint.getYy(), blue);
			}
		}
		
		for (ExtremePoint extremePoint : p.getValues()) {
			if(extremePoint.getXx() == p.getWidth() && extremePoint.getYy() == p.getDepth()) {
				//System.out.println("Paint white " + extremePoint.getX() + "x" + extremePoint.getY() + " " + extremePoint.getXx() + "x" + extremePoint.getYy());
				
				drawingArea.fillRect(extremePoint.getX(), extremePoint.getY(), extremePoint.getXx(), extremePoint.getYy(), Color.white);
			}
		}

		for (ExtremePoint extremePoint : p.getAllPoints()) {
			drawingArea.addRectangle(extremePoint.getItemX(), extremePoint.getItemY(), extremePoint.getItemXx(), extremePoint.getItemYy(), Color.green);
			drawingArea.addDashedLine(extremePoint.getItemX(), extremePoint.getItemY(), extremePoint.getItemXx(), extremePoint.getItemYy(), Color.red);
		}

		for (ExtremePoint extremePoint : p.getValues()) {
			
			Color c = Color.black;
			
			drawingArea.addLine(extremePoint.getX(), extremePoint.getY(), extremePoint.getX(), extremePoint.getYy(), c);
			drawingArea.addLine(extremePoint.getX(), extremePoint.getY(), extremePoint.getXx(), extremePoint.getY(), c);
			
			drawingArea.addCircle(extremePoint.getX(), extremePoint.getY(), c);

			drawingArea.addDashedLine(extremePoint.getX(), extremePoint.getY(), extremePoint.getX(), extremePoint.getYy(), Color.blue);
			drawingArea.addDashedLine(extremePoint.getXx(), extremePoint.getY(), extremePoint.getXx(), extremePoint.getY(), Color.blue);
		}

		drawingArea.addGuide(0, -5, p.getWidth(), -5, Color.blue);

		JFrame.setDefaultLookAndFeelDecorated(true);
		JFrame frame = new JFrame("Draw On Image");
		frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
		
		BufferedImage image = drawingArea.getImage();
		MainPanel mainPanel = new MainPanel(image);
		mainPanel.setBounds(0, 0, image.getWidth(), image.getHeight());
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.black));
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screenSize.getWidth();
        int height = (int) screenSize.getHeight();
        
		frame.getContentPane().add(mainPanel);

		frame.pack();
		frame.setLocationRelativeTo( null );
		frame.setVisible(true);
		
		frame.setBounds(0, 0, image.getWidth(), image.getHeight());

	}

	static class ButtonPanel extends JPanel implements ActionListener {
		private DrawingArea drawingArea;

		public ButtonPanel(DrawingArea drawingArea) {
			this.drawingArea = drawingArea;
		}


		public void actionPerformed(ActionEvent e) {
			JButton button = (JButton)e.getSource();

			if ("Clear Drawing".equals(e.getActionCommand()))
				drawingArea.clear();
			else
				drawingArea.setForeground( button.getBackground() );
		}
	}

	static class DrawingArea extends JPanel {
		
		private int width;
		private int depth;
		
		private BufferedImage image;
		private Rectangle shape;

		public DrawingArea(int width, int depth) {
			this.width = width;
			this.depth = depth;

			image = new BufferedImage(width + 50, depth + 50, BufferedImage.TYPE_INT_ARGB);
			
			setBackground(Color.WHITE);
			
			MyMouseListener ml = new MyMouseListener();
			addMouseListener(ml);
			addMouseMotionListener(ml);
			
			addRectangle(new Rectangle(0, 0, width, depth), Color.black);
			
		}

		public BufferedImage getImage() {
			return image;
		}

		public void fillRect(int x, int y, int xx, int yy, Color color) {
			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor(color);
			g2d.fillRect(x, depth - yy, xx - x, (yy - y));
			repaint();
			
		}

		@Override
		public Dimension getPreferredSize() {
			return isPreferredSizeSet() ?
				super.getPreferredSize() : new Dimension(image.getWidth(), image.getHeight());
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			//  Custom code to support painting from the BufferedImage

			if (image != null) {
				g.drawImage(image, 0, 0, null);
			}

			//  Paint the Rectangle as the mouse is being dragged

			if (shape != null) {
				Graphics2D g2d = (Graphics2D)g;
				g2d.draw( shape );
			}
		}

		public void addRectangle(int x1, int y1, int x2, int y2, Color color) {
			addRectangle(new Rectangle(x1, depth - y2, x2 - x1, y2 - y1), color);
		}
		

		public void addRectangle(Rectangle rectangle, Color color)
		{
			//  Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor( color );
			g2d.draw( rectangle );
			repaint();
		}

		public void addCircle(int x1, int y1, Color color) {
			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor( color );
			g2d.drawOval(x1 - 3, depth - y1 - 3, 6, 6);
			repaint();
		}

		public void addLine(int x1, int y1, int x2, int y2, Color color) {
			//  Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor( color );
			g2d.drawLine(x1, depth - y1, x2, depth - y2);
			repaint();
		}


		public void addGuide(int x1, int y1, int x2, int y2, Color color) {
			//  Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor( color );
			drawDashed(g2d, x1, depth - y1, x2, depth - y2, 1, 9);
			repaint();
		}
		
		public void addDashedLine(int x1, int y1, int x2, int y2, Color color) {
			//  Draw the Rectangle onto the BufferedImage

			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor( color );
			drawDashed(g2d, x1, depth - y1, x2, depth - y2, 5, 5);
			repaint();
		}

		public void clear()
		{
			createEmptyImage();
			repaint();
		}

		private void createEmptyImage()
		{
			image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = (Graphics2D)image.getGraphics();
			g2d.setColor(Color.BLACK);
			g2d.drawString("Add a rectangle by doing mouse press, drag and release!", 40, 15);
		}

		class MyMouseListener extends MouseInputAdapter
		{
			private java.awt.Point startPoint;

			public void mousePressed(MouseEvent e)
			{
				startPoint = e.getPoint();
				shape = new Rectangle();
			}

			public void mouseDragged(MouseEvent e)
			{
				int x = Math.min(startPoint.x, e.getX());
				int y = Math.min(startPoint.y, e.getY());
				int width = Math.abs(startPoint.x - e.getX());
				int height = Math.abs(startPoint.y - e.getY());

				shape.setBounds(x, y, width, height);
				repaint();
			}

			public void mouseReleased(MouseEvent e)
			{
				if (shape.width != 0 || shape.height != 0)
				{
					addRectangle(shape, e.getComponent().getForeground());
				}

				shape = null;
			}
		}
		
		public static void drawDashed(Graphics g, int x1, int y1, int x2, int y2, int dashSize, int gapSize) {
		    if(x2 < x1) {
		      int temp = x1;
		      x1 = x2;
		      x2 = temp;
		    }
		    if(y2 < y1) {
		      int temp = y1;
		      y1 = y2;
		      y2 = temp;
		    }
		    int totalDash = dashSize + gapSize;
		    if(y1 == y2) {
		      int virtualStartX = (x1 / totalDash) * totalDash;
		      for(int x = virtualStartX; x < x2; x += totalDash) {
		        int topX = x + dashSize;
		        if(topX > x2) {
		          topX = x2;
		        }
		        int firstX = x;
		        if(firstX < x1) {
		          firstX = x1;
		        }
		        if(firstX < topX) {
		          g.drawLine(firstX, y1, topX, y1);
		        }
		      }
		    }
		    else if(x1 == x2) {
		      int virtualStartY = (y1 / totalDash) * totalDash;
		      for(int y = virtualStartY; y < y2; y += totalDash) {
		        int topY = y + dashSize;
		        if(topY > y2) {
		          topY = y2;
		        }
		        int firstY = y;
		        if(firstY < y1) {
		          firstY = y1;
		        }
		        if(firstY < topY) {
		          g.drawLine(x1, firstY, x1, topY);
		        }
		      }     
		    }
		    else {
		      // Not supported
		      g.drawLine(x1, y1, x2, y2);
		    }		
		}
	}
}