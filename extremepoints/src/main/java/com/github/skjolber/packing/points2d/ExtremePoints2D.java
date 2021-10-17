package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

/**
 * 
 * 
 *
 */

public class ExtremePoints2D<P extends Placement2D> {
	
	private final int containerMaxX;
	private final int containerMaxY;

	private List<Point2D> values = new ArrayList<>();
	private List<P> placements = new ArrayList<>();

	public ExtremePoints2D(int dx, int dy) {
		super();
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;
		
		values.add(new DefaultXYSupportPoint2D(0, 0, containerMaxX, containerMaxY, 0, containerMaxX, 0, containerMaxY));
	}

	public boolean add(int index, P placement) {
		return add(index, placement, placement.getAbsoluteEndX() - placement.getAbsoluteX() + 1, placement.getAbsoluteEndY() - placement.getAbsoluteY() + 1);
	}

	public boolean add(int index, P placement, int boxDx, int boxDy) {
		
		System.out.println("Add " + placement);
		for(Point2D v : values) {
			System.out.println(" " + v);
		}
		
		// overall approach:
		//
		// project points points swallowed by the placement, then delete them
		// project points shadowed by the placement to the other side, then constrain them
		// add points shadowed by the two new points, if they could be moved in the negative direction

		// keep track of placement borders, where possible
		
		Point2D source = values.get(index);
		
		List<Point2D> deleted = new ArrayList<>();
		
		int xx = source.getMinX() + boxDx;
		int yy = source.getMinY() + boxDy;

		List<Point2D> addX = null;
		
		boolean xSupport = source.isXSupport(xx); // i.e. is minY the y coordinate?
		boolean xEdge = source.isXEdge(xx);
		
		boolean ySupport = source.isYSupport(yy); // i.e. is minX the x coordinate?
		boolean yEdge = source.isYEdge(yy);

		if(xSupport || xEdge) {
			addX = new ArrayList<>();
			if(xSupport) {
				DefaultXYSupportPoint2D addSupportedX = addSupportedAtXX(source, xx, yy);
				if(addSupportedX != null) {
					addX.add(addSupportedX);
				}
			}
			if(ySupport) {
				addSwallowedXX(placement, source, deleted, xx, yy, addX);
			} else {
				int maxY = constrainIfNotMaxY(source, xx);
				addSwallowedOrShadowedAtXX(placement, source, deleted, xx, yy, addX, maxY);
			}
		} else {
			addX = addAtXX(placement, source, deleted, xx, yy);
		}

		List<Point2D> addY = null;

		if(ySupport || yEdge) {
			addY = new ArrayList<>();
			if(ySupport) {
				DefaultXYSupportPoint2D addSupportedY = addSupportedAtYY(source, xx, yy);
				if(addSupportedY != null) {
					addY.add(addSupportedY);
				}
			}
			if(xSupport) {
				addSwallowedYY(placement, source, deleted, xx, yy, addY);
			} else {
				int maxX = constrainIfNotMaxX(source, yy);
				addSwallowedOrShadowedAtYY(placement, source, deleted, xx, yy, addY, maxX);
			}

		} else {
			addY = addAtYY(placement, source, deleted, xx, yy);
		}
		
		deleted.add(source);
		values.removeAll(deleted);
		
		for(int i = 0; i < values.size(); i++) {
			Point2D point2d = values.get(i);
			if(!constrainMax(point2d, placement)) {
				values.remove(i);
				i--;
			}
		}
		
		if(xSupport || xEdge) {
			addX(addX, xx);
		} else {
			values.addAll(addX);
		}

		if(ySupport || yEdge) {
			addY(addY, yy);
		} else {
			values.addAll(addY);
		}

		placements.add(placement);
		Collections.sort(values, Point2D.COMPARATOR);

		return !values.isEmpty();
	}

	private void addX(List<Point2D> add, int x) {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			if(existing.getMinX() == x) {
				for (int i = 0; i < add.size(); i++) {
					Point2D p1 = add.get(i);
					
					if(existing.getMinY() == p1.getMinY()) {
						// same point, do not add the new point
						add.remove(i);
						i--;
						
						continue;
					}
					
					// is the new point shadowed, or shadowing an existing point?
					if(p1.getMaxY() == existing.getMaxY() && p1.getMaxX() == existing.getMaxX()) {
						add.remove(i);
						i--;

						if(p1.getMinY() < existing.getMinY()) {
							values.set(index, p1);
							existing = p1;
						}
					}
				}
			}
			index++;
		}
		
		values.addAll(add);	
	}
	
	private void addY(List<Point2D> add, int y) {
		int index = 0;
		while(index < values.size()) {
			Point2D existing = values.get(index);
			if(existing.getMinY() == y) {
				for (int i = 0; i < add.size(); i++) {
					Point2D p1 = add.get(i);
					
					if(existing.getMinX() == p1.getMinX()) {
						// same point, do not add the new point
						add.remove(i);
						i--;
						
						continue;
					}
					
					// is the new point shadowed, or shadowing an existing point?
					if(p1.getMaxY() == existing.getMaxY() && p1.getMaxX() == existing.getMaxX()) {
						add.remove(i);
						i--;

						if(p1.getMinX() < existing.getMinX()) {
							values.set(index, p1);
							existing = p1;
						}
					}
				}
			}
			index++;
		}
		
		values.addAll(add);	
	}

	private List<Point2D> addAtXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy) {
		List<Point2D> added = new ArrayList<>();

		int maxY = constrainIfNotMaxY(source, xx);

		Point2D dx = unsupportedX(source, xx, yy, maxY);
		if(dx != null) {
			if(constrainMaxX(dx)) {
				added.add(dx);
				
				if(dx.getMinY() < source.getMinY()) {
					for (int i = 0; i < values.size(); i++) {
						Point2D point = values.get(i);
					
						// Add point shadowed by the new point (without constraining them)
							
						//
						//      |         |-------------------|
						//      |         |                   |
						//      |         |                   |
						//      |         |                   |
						//      |         |--------------------         
						//      |                                       ▲ 
						//      |     ◄----------------------------►    |           
						//      |                                       ▼ 
						//      |-----------------------------*-----    
							
						if(point.innerY(dx.getMinY(), source.getMinY())) { // vertical constraint
							if(point.crossesX(xx)) { // horizontal constraint (crosses xx)'
								added.add(point);
							}
						}
					}
				}
			}
		}
		
		addSwallowedOrShadowedAtXX(placement, source, deleted, xx, yy, added, maxY);
		
		return added;
	}

	private void addSwallowedOrShadowedAtXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy,
			List<Point2D> added, int maxY) {
		
		// Move points swallowed or shadowed by the placement
		
		//    swallowed:
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|---------------			
		//
		//    shadowed:
		//
		//    |                          |
		//    |                          |
		//    |                          |
		//    |         |-------|        |
		//    |         |       |        |
		//    *         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|-------|--------|---
		//			
		//    |                          |
		//    |                          |
		//    |                          |
		//    |         |-------|        |
		//    |         |       |        |
		//    *         |       *        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |         |       |        |
		//    |---------|-------|--------|---
		//    			
		//  point     source   xx      point
		//  minX       minX            maxX
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
		
			// Move points swallowed or shadowed by the placement
			if(point.shadowedOrSwallowedX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					// TODO contrain max y per point?
					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), maxY,
							xx, point.getMinY()
							);
					
					added.add(next);
				}
				
				if(point.getMinX() < source.getMinX()) {
					// constrain current point
					point.setMaxX(source.getMinX() - 1);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
		removeShadowedY(added);
		
		// complete adding
		// if some were only shadowed, add a new point
		for (int i = 0; i < added.size(); i++) {
			Point2D point = added.get(i);					
			
			if(point.getMinX() < xx) {
				Point2D p = new DefaultYSupportPoint2D(xx, point.getMinY(), point.getMaxX(), maxY, point.getMinY(), yy);

				added.set(i, p);
			}
		}
	}

	private DefaultXYSupportPoint2D addSupportedAtXX(Point2D source, int xx, int yy) {
		// in other words there is a placement in negative y direction at XX 
		// so the y coordinate is minY

		//       |
		//       |
		// yy    |    |--------|
		//       |    |        |
		// smaxY |----|        | 	
		//       |    |        | 
		//       |    |        |
		//       |    |        |
		//       |    |        | 
		//       |    |        |
		//  minY |    |---------------|   <---- Y-support
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//           minX      xx    smaxX

		// or
		
		//
		// smaxY |----|
		//       |    |   dx
		// yy    |    |--------|
		//       |    |        |
		//       |    |        | dy
		//       |    |        |
		// minY  |    |---------------|  <---- Y-support
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//          minX      xx    smaxX

		// using dx
		//
		// yy   |             |
		//      |             |
		//      |             |
		// minY |             *-------     <---- Y-support
		//      |                    
		//      |                    
		//      |--------------------------
		//                    xx   fmaxX
		
		if(source.getMaxX() > xx) {
			XSupportPoint2D fixedPointY = (XSupportPoint2D)source;
			
			int maxY = constrainIfNotMaxY(source, xx);

			if(maxY > source.getMinY()) {
				return new DefaultXYSupportPoint2D(xx, source.getMinY(), source.getMaxX(), maxY, xx, fixedPointY.getXSupportMaxX(), source.getMinY(), yy - 1);
			}
		}
		return null;
	}
	
	private void addSwallowedXX(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added) {
		//    swallowed:
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------
		//
		//    |
		//    |
		//    |---------|
		//    |         |
		//    |         *
		//    |         | 
		//    |---------|---------------			
		//
		
		int maxY = constrainIfNotMaxY(source, xx);
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);

			// Move points swallowed by the placement
			if(point.swallowedX(source.getMinX(), xx) && withinY(point.getMinY(), placement)) {
				if(point.getMaxX() > xx) {
					// add point on the other side
					// vertical support

					DefaultYSupportPoint2D next = new DefaultYSupportPoint2D(
							xx, point.getMinY(),
							point.getMaxX(), maxY,
							xx, point.getMinY()
							);

					added.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
		removeShadowedY(added);
	}
	
	private void addSwallowedYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added) {
		//    swallowed:
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|---------------
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    |         |
		//    |         | 
		//    |---------|---------------
		//
		//

		int maxX = constrainIfNotMaxX(source, yy);

		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
		
			// Move points swallowed by the placement
			if(point.swallowedY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() > yy) {
					// add point
					// horizontal support
					
					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							maxX, point.getMaxY(), 
							point.getMinX(), xx
							);
					
					added.add(next);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}

		removeShadowedX(added);
	}
	

	private void removeShadowedY(List<Point2D> added) {
		Collections.sort(added, Point2D.Y_COMPARATOR);

		// remove those points which have the same extreme points (they have the same x coordinate)
		for (int j = 0; j < added.size(); j++) {
			Point2D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point2D p = added.get(i);

				if(point2d.getMaxX() == p.getMaxX() && point2d.getMaxY() == p.getMaxY()) {
					added.remove(i);
					i--;
				}
			}
		}
	}

	private int constrainIfNotMaxY(Point2D source, int x) {
		int maxY;
		if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
			maxY = containerMaxY;
		} else {
			maxY = constrainY(x, source.getMinY());
		}
		return maxY;
	}

	private List<Point2D> addAtYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy) {
		List<Point2D> added = new ArrayList<>();
		int maxX = constrainIfNotMaxX(source, yy);
		
		// using dy
		Point2D negativeX = unsupportedY(source, xx, yy, maxX);
		if(negativeX != null) {
			
			if(constrainMaxY(negativeX)) {
				added.add(negativeX);

				if(negativeX.getMinX() < source.getMinX()) {
				
					// supported one way (by container border)
					//
					//       |  
					//       |  
					// yy    |-----------------|
					//       |        |        |
					//       |        |        |
					//       |   *    |        |
					//		         minX      xx
					//
					//       |  
					//       |  
					// yy    |---*-------------|  <--- move up
					//       |        |        |
					//       |        |        |
					//       |        |        |
					//		         minX      xx
					
					for (int i = 0; i < values.size(); i++) {
						Point2D point = values.get(i);

						// Add shadowed points
						if(point.innerX(negativeX.getMinX(), source.getMinX())) { // vertical constraint
							if(point.crossesY(yy)) { // horizontal constraint (crosses xx)
								added.add(point);
							}
						}							
					}
				}
			}
		}
		
		addSwallowedOrShadowedAtYY(placement, source, deleted, xx, yy, added, maxX);
		
		return added;
	}

	private void addSwallowedOrShadowedAtYY(P placement, Point2D source, List<Point2D> deleted, int xx, int yy, List<Point2D> added, int maxX) {
		// Move points swallowed or shadowed by the placement
		
		//    swallowed:
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    | *       |
		//    |         | 
		//    |---------|----
		//
		//    |
		//    |
		//    |-*-------|
		//    |         |
		//    |         |
		//    |         | 
		//    |---------|-----	
		//
		//    shadowed:
		//
		// point maxY  |--------------------------
		//             |
		//             |
		//             |
		//          yy |---------------------|
		//             |                     |
		// source minY |---------|-----------|
		//             |         | 
		// point minY  |---------|----*----------
		//
		//
		//
		// point maxY  |--------------------------
		//             |
		//             |
		//             |
		//          yy |--------------*------|    <-- added
		//             |                     |
		// source minY |---------|-----------|
		//             |         | 
		// point minY  |---------|----*---------- <-- constrained
		//
		
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
		
			// Move points swallowed or shadowed by the placement
			if(point.shadowedOrSwallowedY(source.getMinY(), yy) && withinX(point.getMinX(), placement)) {
				if(point.getMaxY() > yy) {
					// add point
					// horizontal support
					
					DefaultXSupportPoint2D next = new DefaultXSupportPoint2D(
							point.getMinX(), yy, 
							maxX, point.getMaxY(), 
							point.getMinX(), xx
							);

					added.add(next);
				}
				
				if(point.getMinY() < source.getMinY()) {
					// constrain current point
					point.setMaxY(source.getMinY() - 1);
				} else {
					// delete current point (which was swallowed)
					deleted.add(point);
				}
			}
		}
		
		removeShadowedX(added);

		for (int i = 0; i < added.size(); i++) {
			Point2D point = added.get(i);					
			
			if(point.getMinY() < yy) {
				Point2D p = new DefaultXSupportPoint2D(point.getMinX(), yy, maxX, point.getMaxY(), point.getMinX(), xx);

				added.set(i, p);
			}
		}
	}
	
	private DefaultXYSupportPoint2D addSupportedAtYY(Point2D source, int xx, int yy) {
		// in other words there is a placement in negative x direction at YY
		// so the x coordinate is minX
		
		//
		// vmaxY |----|                      <-- x-support max y
		//       |    |          
		//  yy   |    *-------------------|
		//       |    |                   |
		//       |    |                   | 
		//       |    |                   |
		//  minY |    |--------------------  <-- x-support min y
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//           minX            maxX
		
		// or
		
		//
		// vmaxY |----|                      <-- x-support max y
		//       |    |   
		// yy    |    *--------|
		//       |    |        |
		//       |    |        | 
		//       |    |        |
		// minY  |    |---------------|      <-- x-support min y
		//       |    |               |
		//       |    |               |
		//       |----|---------------|-----
		//          minX      xx    fmaxX
		
		// using dy
		//
		//
		// vmaxY |    |                      <-- x-support max y
		//       |    |   
		// yy    |    *---------
		//       |
		//       |
		//       |
		//       |
		//       |
		//       |
		//       |--------------------------
		//           minX      xx    		
		//
		
		if(source.getMaxY() > yy) {
			YSupportPoint2D fixedPointX = (YSupportPoint2D)source;
			
			int maxX = constrainIfNotMaxX(source, yy);

			if(maxX > source.getMinX()) {
				return new DefaultXYSupportPoint2D(source.getMinX(), yy, maxX, source.getMaxY(), source.getMinX(), xx - 1, yy, fixedPointX.getYSupportMaxY());
			}
		}
		return null;
	}

	private void removeShadowedX(List<Point2D> added) {
		Collections.sort(added, Point2D.X_COMPARATOR);

		// remove those points which have the same extreme points (they have the same y coordinate)
		for (int j = 0; j < added.size(); j++) {
			Point2D point2d = added.get(j);
			
			for (int i = j + 1; i < added.size(); i++) {
				Point2D p = added.get(i);

				if(point2d.getMaxX() == p.getMaxX() && point2d.getMaxY() == p.getMaxY()) {
					added.remove(i);
					i--;
				}
			}
		}
	}

	private int constrainIfNotMaxX(Point2D source, int yy) {
		int maxX;
		if(source.getMaxX() == containerMaxX && source.getMaxY() == containerMaxY) {
			maxX = containerMaxX;
		} else {
			maxX = constrainX(source.getMinX(), yy);
		}
		return maxX;
	}

	protected boolean constrainMaxX(Point2D dx) {
		// constrain to right
		P closestX = closestPositiveX(dx.getMinX(), dx.getMinY());
		if(closestX != null) {
			dx.setMaxX(closestX.getAbsoluteX() - 1);
		} else {
			dx.setMaxX(containerMaxX);
		}
		if(dx.getMaxX() <= dx.getMinX()) {
			return false;
		}

		return true;
	}
	
	protected boolean constrainMaxY(Point2D dx) {
		// constrain up
		P closestY = closestPositiveY(dx.getMinX(), dx.getMinY());
		if(closestY != null) {
			dx.setMaxY(closestY.getAbsoluteY() - 1);
		} else {
			dx.setMaxY(containerMaxY);
		}
		if(dx.getMaxY() <= dx.getMinY()) {
			return false;
		}
		return true;
	}
	
	protected Point2D unsupportedX(Point2D source, int xx, int yy, int maxY) {
		if(xx >= containerMaxX) {
			return null;
		}
		P moveY = projectNegativeY(xx, yy);
		if(moveY == null) {
			
			// supported one way (by container border)
			//
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			// minY |    |--------------------
			//      |    |               |   |
			//      |    |               |   ↓
			//      |----|---------------|---*-----
			//          minX            maxX
			
			return new DefaultXSupportPoint2D(xx, 0, containerMaxX, maxY, xx, containerMaxX);
			
		} else if(moveY.getAbsoluteEndY() < source.getMinY()) {
			
			// supported one way
			//
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			// minY |    |--------------------
			//      |    |               |   ↓
			//      |    |               |   *--------|
			//      |    |               |   |        |
			//      |----|---------------|---|--------|--------
			//          minX            maxX

			return new DefaultXSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, maxY, xx, moveY.getAbsoluteEndX());
		} else if(moveY.getAbsoluteEndY() + 1 < yy) {
			
			// supported both ways
			// 
			//      |    |-------------------|
			//      |    |                   |
			//      |    |                   |
			//      |    |                   |
			//      |    |                   ↓
			//      |    |                   *--------|
			//      |    |                   |        |
			// minY |    |-------------------|        |
			//      |    |               |   |        |
			//      |    |               |   |        |
			//      |    |               |   |        |
			//      |----|---------------|---|--------|--------
			//          minX            maxX

			return new DefaultXYSupportPoint2D(xx, moveY.getAbsoluteEndY() + 1, containerMaxX, maxY, xx, moveY.getAbsoluteEndX(), moveY.getAbsoluteEndY(), yy);
		}
		
		// no space to move
		// 
		//      |    |-------------------*---------
		//      |    |                   |        |
		//      |    |                   |        | 
		//      |    |                   |        |
		//      |    |                   |        |
		//      |    |                   |        |
		//      |    |                   |        |
		// minY |    |-------------------|        |
		//      |    |               |   |        |
		//      |    |               |   |        |
		//      |    |               |   |        |
		//      |----|---------------|---|--------|--------
		//          minX            maxX
		
		return null;
	}

	private Point2D unsupportedY(Point2D source, int xx, int yy, int maxX) {
		if(yy >= containerMaxY) {
			return null;
		}
		P moveX = projectNegativeX(xx, yy);
		if(moveX == null) {
			
			// supported one way (by container border)
			//
			//       |  
			//       |  
			// yy    |←------------|
			//       |    |        |
			//       |    |        |
			//       |    |        |
			// fmaxY |----|        |
			//
			
			return new DefaultYSupportPoint2D(0, yy, maxX, containerMaxY, yy, containerMaxY);
		} else if(moveX.getAbsoluteEndX() < source.getMinX()) {
			
			// supported one way
			//
			// aendy |-|
			//       | |
			// yy    | |←----------|
			//       |    |        |
			//       |    |        |
			//       | |  |        |
			// fmaxY |----|        |
			//
			//       aendx
			
			return new DefaultYSupportPoint2D(moveX.getAbsoluteEndX() + 1, yy, maxX, containerMaxY, yy, moveX.getAbsoluteEndY());

		} else if(moveX.getAbsoluteEndX() + 1 < xx) {
			
			// supported both ways
			//
			//
			// aendy |-------|
			//       |       |
			//       |       |
			// yy    |    |--*←----|
			//       |    |        |
			//       |    |        |
			//       |    |        |
			// fmaxY |----|        |
			//
			//             aendx
			
			return new DefaultXYSupportPoint2D(moveX.getAbsoluteX() + 1, yy, maxX, containerMaxY,  moveX.getAbsoluteEndX(), xx, yy, moveX.getAbsoluteEndY());
		}
		
		// no space to move
		//
		//
		// aendy |-------------|
		//       |             |
		//       |             |
		// yy    |    |--------*
		//       |    |        |
		//       |    |        |
		//       |    |        |
		// fmaxY |----|        |
		//
		//  
		
		return null;
	}	


	protected boolean constrainMaxX(Point2D point, P placement) {
		int maxX = projectPositiveX(point.getMinX(), point.getMinY(), placement, point.getMaxX());
		if(maxX <= point.getMinX()) {
			return false;
		}
		point.setMaxX(maxX);

		return true;
	}
	
	protected boolean constrainMaxY(Point2D point, P placement) {
		int maxY = projectPositiveY(point.getMinX(), point.getMinY(), placement, point.getMaxY());
		if(maxY <= point.getMinY()) {
			return false;
		}
		
		point.setMaxY(maxY);

		return true;
	}
	
	protected boolean constrainMax(Point2D point, P placement) {
		if(placement.getAbsoluteX() >= point.getMinX()) {
			if(withinY(point.getMinY(), placement)) {
				int limit = placement.getAbsoluteX() - 1;
				if(limit <= point.getMinX()) {
					return false;
				}
				if(point.getMaxX() > limit) {
					point.setMaxX(limit);
				}
			}
		}
		
		if(placement.getAbsoluteY() >= point.getMinY()) {
			if(withinX(point.getMinX(), placement)) {
				int limit = placement.getAbsoluteY() - 1;
				if(limit <= point.getMinY()) {
					return false;
				}
				if(point.getMaxY() > limit) {
					point.setMaxY(limit);
				}
			}
		}
		return true;
	}	

	protected int projectPositiveY(int x, int y, P placement, int maxY) {
		if(placement.getAbsoluteY() >= y) {
			if(withinX(x, placement)) {
				if(placement.getAbsoluteY() < maxY) {
					maxY = placement.getAbsoluteY();
				}
			}
		}
		
		return maxY;
	}

	protected int projectPositiveX(int x, int y, P placement, int maxX) {
		if(placement.getAbsoluteX() >= x) {
			if(withinY(y, placement)) {
				if(placement.getAbsoluteX() < maxX) {
					maxX = placement.getAbsoluteX();
				}
			}
		}
		return maxX;
	}
	
	protected P projectNegativeX(int x, int y) {
		
		// excluded:
		//
		//         |
		// absEndy-|-----|
		//         |     |
		//         |     |
		//         |-----|
		//         |
		//         |        *
		//         |                
		//         |--------------------------
		//               |
		//            absEndX
		//
		//         |
		// absEndy-|-----------|
		//         |           |
		//         |           |
		//   absY  |-----------|
		//         |
		//         |        *
		//         |                
		//         |--------------------------
		//         |           |
		//        absX       absEndX
		//
		//
		// included:
		//
		//         |
		//         |
		//         |
		// absEndy-|-----|
		//         |     |
		//         |     |  *
		//         |     |          
		//         |-----|--------------------
		//               |
		//            absEndX
		
		P rightmost = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndX() <= x && withinY(y, placement)) {
				// most to the right
				if(rightmost == null || placement.getAbsoluteEndX() > rightmost.getAbsoluteEndX()) {
					rightmost = placement;
				}
			}
		}
		
		return rightmost;
	}	

	protected P projectNegativeY(int x, int y) {

		// excluded:
		//
		// |
		// |
		// |                    |-----| absEndY
		// |                 *  |     |
		// |                    |     |
		// |                    |     |
		// |--------------------|-----|  absY
		//                      |     |
		//                    absX absEndX
		//
		// |
		// |                  
		// |                 *
		// |                    |-----| absEndY
		// |                    |     |
		// |--------------------|-----|- absY
		//                      |     |
		//                    absX absEndX
		//
		// included:
		//
		// |                  
		// |                 *
		// |              |------------| absEndY
		// |              |            |
		// |--------------|------------|  absY
		//                |            |
		//               absX       absEndX
		//
		
		// TODO what if one or more placements start and begin within minY and maxY?
		
		P mostPositive = null;
		for (P placement : placements) {
			if(placement.getAbsoluteEndY() <= y && withinX(x, placement)) {
				
				// the highest
				if(mostPositive == null || placement.getAbsoluteEndY() > mostPositive.getAbsoluteEndY()) {
					mostPositive = placement;
				}
			}
		}
		
		return mostPositive;
	}
	
	protected P closestPositiveY(int x, int y) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteY() >= y) {
				if(withinX(x, placement)) {
					if(closest == null || placement.getAbsoluteY() < closest.getAbsoluteY() || (placement.getAbsoluteY() == closest.getAbsoluteY() && placement.getAbsoluteX() < closest.getAbsoluteX())) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}

	protected boolean withinX(int x, P placement) {
		return placement.getAbsoluteX() <= x && x <= placement.getAbsoluteEndX();
	}	

	protected P closestPositiveX(int x, int y) {
		P closest = null;
		for (P placement : placements) {
			if(placement.getAbsoluteX() >= x) {
				if(withinY(y, placement)) {
					if(closest == null || placement.getAbsoluteX() < closest.getAbsoluteX() || (placement.getAbsoluteX() == closest.getAbsoluteX() && placement.getAbsoluteY() < closest.getAbsoluteY())) {
						closest = placement;
					}
				}
			}
		}
		
		return closest;
	}
	
	protected int constrainX(int x, int y) {
		// constrain up
		P closestX = closestPositiveX(x, y);
		if(closestX != null) {
			return closestX.getAbsoluteX() - 1;
		} else {
			return containerMaxX;
		}
	}

	protected int constrainY(int x, int y) {
		// constrain up
		P closestY = closestPositiveY(x, y);
		if(closestY != null) {
			return closestY.getAbsoluteY() - 1;
		} else {
			return containerMaxY;
		}
	}

	protected boolean withinY(int y, P placement) {
		return placement.getAbsoluteY() <= y && y <= placement.getAbsoluteEndY();
	}
	
	public int getDepth() {
		return containerMaxY;
	}
	
	public int getWidth() {
		return containerMaxX;
	}

	@Override
	public String toString() {
		return "ExtremePoints2D [width=" + containerMaxX + ", depth=" + containerMaxY + ", values=" + values + "]";
	}
	
	public List<P> getPlacements() {
		return placements;
	}
	
	public int get(int x, int y) {
		for (int i = 0; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinY() == y && point.getMinX() == x) {
				return i;
			}
		}
		return -1;
	}

	public Point2D getValue(int i) {
		return values.get(i);
	}
	
	public List<Point2D> getValues() {
		return values;
	}
	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D point = values.get(i);
			
			if(point.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}	

}
