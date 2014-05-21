package org.commcare.areamap;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class Utilities {
	
	/**
	 * converts Location into LatLng out
	 */
	public static LatLng toLatLng(Location location) {
		return new LatLng(location.getLatitude(), location.getLongitude());
	}

	/**
	 * calculates the distance between two LatLng points using the haversine formula
	 */
	public static double findDistance(LatLng firstPoint, LatLng secondPoint) {
		final double radiusOfEarth = 6378;
		double firstLatitude = firstPoint.latitude;
		double firstLongitude = firstPoint.longitude;
		double secondLatitude = secondPoint.latitude;
		double secondLongitude = secondPoint.longitude;

		return haversine(firstLatitude, firstLongitude, secondLatitude, secondLongitude, radiusOfEarth);
	}

	/**
	 * Uses the haversine formula to determine what the spherical distance between two points are
	 * @param firstLat
	 * @param firstLong
	 * @param secondLat
	 * @param secondLong
	 * @param r
	 * @return spherical distance in kilometres
	 */
	public static double haversine(double firstLat, double firstLong, 
			double secondLat, double secondLong, double r) {
		double latitudeDistance = inRadians(firstLat - secondLat);
		double longitudeDistance = inRadians(firstLong - secondLong);
		double a = Math.pow((Math.sin(latitudeDistance / 2)) , 2)
				+ Math.cos(inRadians(firstLat)) * Math.cos(inRadians(secondLat))
				* Math.pow((Math.sin(longitudeDistance / 2)) , 2);
		return 2 * r * Math.atan2(Math.pow(a, 0.5), Math.pow(1 - a, 0.5));
	}

	/**
	 * converts degrees to radians
	 */
	private static double inRadians(Double degree) {
		return degree * Math.PI / 180.0;
	}
}
