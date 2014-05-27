package org.commcare.areamap;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

public class Utilities {
	
	final static double RADIUS_OF_EARTH = 6378; //kilometers

	
	/**
	 * converts Location into LatLng out
	 */
	public static LatLng toLatLng(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}
	
	/**
	 * calculates the distance between two LatLng points using the 
	 * haversine formula, in meters
	 */
	public static double findDistance(LatLng firstPoint, LatLng secondPoint) {
		double firstLatitude = firstPoint.latitude;
		double firstLongitude = firstPoint.longitude;
		double secondLatitude = secondPoint.latitude;
		double secondLongitude = secondPoint.longitude;
		return haversine(firstLatitude, firstLongitude, secondLatitude, secondLongitude, RADIUS_OF_EARTH);
	}

	/**
	 * Uses the haversine formula to determine what the spherical distance between two points are
	 * @param firstLat
	 * @param firstLong
	 * @param secondLat
	 * @param secondLong
	 * @param r
	 * @return spherical distance in meters
	 */
	public static double haversine(double firstLat, double firstLong, 
			double secondLat, double secondLong, double r) {
		double latitudeDistance = inRadians(firstLat - secondLat);
		double longitudeDistance = inRadians(firstLong - secondLong);
		double a = Math.pow((Math.sin(latitudeDistance / 2)) , 2)
				+ Math.cos(inRadians(firstLat)) * Math.cos(inRadians(secondLat))
				* Math.pow((Math.sin(longitudeDistance / 2)) , 2);
		double distInKM = 2 * r * Math.atan2(Math.pow(a, 0.5), Math.pow(1 - a, 0.5));
		return 1000 * distInKM;
	}
	
	/**
	 * Uses the planar law of cosines to compute the measure of an angle in a triangle, 
	 * based on its side lengths
	 * @param toLeft -- side to left of angle
	 * @param toRight -- side to right of angle
	 * @param opposing -- side opposing angle
	 * @return measure of the angle opposite 'opposing', in radians
	 */
	public static double computeAngle(double toLeft, double toRight, double opposing) {
		double numerator = toLeft*toLeft + toRight*toRight - opposing*opposing;
		double denominator = 2*toLeft*toRight;
		return Math.acos(numerator/denominator);
	}

	/**
	 * converts degrees to radians
	 */
	private static double inRadians(Double degree) {
		return degree * Math.PI / 180.0;
	}
	 
}
