package org.commcare.areamap;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class SphericalTriangle {
	
	private LatLng p1;
	private LatLng p2;
	private LatLng p3;
	private double sideA;
	private double sideB;
	private double sideC;
	private double angleA;
	private double angleB;
	private double angleC;
	private double area;
	
	public SphericalTriangle(Location p1, Location p2, Location p3) {
		this.p1 = Utilities.toLatLng(p1);
		this.p2 = Utilities.toLatLng(p2);
		this.p3 = Utilities.toLatLng(p3);
		computeSideLengths();
		computeAngles();
		computeArea();
	}
	
	//computes side lengths for a triangle in meters
	private void computeSideLengths() {
		sideA = Utilities.findDistance(p1,p2);
		sideB = Utilities.findDistance(p2,p3);
		sideC = Utilities.findDistance(p3,p1);
		Log.i("Triangle", sideA + ", " + sideB + ", " + sideC);
	}
	
	/*NOTE: Using cosine rule of plane geometry because side lengths are MUCH 
	 * smaller than radius of earth
	 */
	private void computeAngles() {
		angleA = Utilities.computeAngle(sideC, sideB, sideA);
		angleB = Utilities.computeAngle(sideA, sideC, sideB);
		angleC = Utilities.computeAngle(sideA, sideB, sideC);
		Log.i("Triangle", angleA + ", " + angleB + ", " + angleC);

	}
	
	//Using Girard's Theorem for area of a triangle on a unit sphere
	private void computeArea() {
		if (Double.isNaN(sideA) || Double.isNaN(sideB) || Double.isNaN(angleC)) {
			area = 0;
		}
		else {
			area = sideA * sideB * Math.sin(angleC) / 2;
		}
		Log.i("Triangle", "" + area);
	}
	
	public double getArea() {
		return this.area;
	}
	

}
