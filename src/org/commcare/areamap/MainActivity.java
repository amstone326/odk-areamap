package org.commcare.areamap;

import java.util.ArrayList;

import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity 
						  implements
						  GooglePlayServicesClient.ConnectionCallbacks,
					        GooglePlayServicesClient.OnConnectionFailedListener,
					        com.google.android.gms.location.LocationListener {
	
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	private TextView locationText;
	
	//default states for the two booleans
	private boolean allowAutomaticUpdates = true;
	
	//array to store the locations from the periodic updates
	private ArrayList<Location> walkingPoints;
	
	// Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
	    
	public static enum State {idle, walking, finished};
	public State state = State.idle;

    /**
     * determines what UI elements need to be present based on what state we're in
     * TODO: updating UI for each state
     */
	public void refreshUI() {
		switch(state) {
			case idle:
				/* 1) startWalk button IS visible
				 * 2) calculateArea button NOT Visible
				 */
				break;
			case walking:
				/* 1) startWalk button NOT visible
				 * 2) calculateArea button IS visible 
				 */
				break;
			case finished:
				/* 1) Calculated area is displayed (plus options for saving/sending?)
				 * 2) startWalk button visible
				 * 3) calculateArea button NOT visible
				 */
		}
	}
	
	//TODO implement this UI mode
	private void setModeToIdle() {
		state = State.idle;
		refreshUI();
	}
	
	//TODO implement this UI mode
	private void setModeToWalking() {
		state = State.walking;
		walkingPoints = new ArrayList<Location>(); //starts walking list afresh for new walk
		refreshUI(); 
	}
	
	private void setModeToFinished() {
		state = State.finished;
		refreshUI();
	}
	
	private void addLineToMap(Location p1, Location p2) {
    	PolylineOptions line = new PolylineOptions().add(
				Utilities.toLatLng(p1),
				Utilities.toLatLng(p2));
		mMap.addPolyline(line);
    }
	
	private void calculateArea() {
		//TODO: IMPLEMENT 
	}
	
	public void onDonePressed(View v) {
		if (walkingPoints.size() >= 3) {
			Location firstPoint = walkingPoints.get(0);
			Location lastPoint = walkingPoints.get(walkingPoints.size()-1);
			addLineToMap(firstPoint, lastPoint);
			setModeToFinished();
			calculateArea();
		}
		else {
			Toast.makeText(v.getContext(), "You need at least 3 points to calculate an area!", 
					Toast.LENGTH_SHORT).show();
		}  
	}

	public void onStartWalkPressed(View v) {
		setModeToWalking();
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.activity_main);
        // Try to obtain the map from the SupportMapFragment.
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
        //mMap.setOnMapClickListener(this); 

        /*TODO center map around user's location (not working currently)
         * mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(getCurrentLocation().getLatitude(), 
         * getCurrentLocation().getLongitude()),10), 2000, null); */
         
        //mMap.setBuildingsEnabled(false); 
        mMap.setMyLocationEnabled(true);
                
        locationText = (TextView) findViewById(R.id.LocationText);
    	mLocationRequest = LocationRequest.create();
    	mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    	mLocationRequest.setInterval(UPDATE_INTERVAL);
    	mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationClient = new LocationClient(this, this, this);
        
        if (savedInstanceState != null) {
        	state = State.valueOf(savedInstanceState.getString("state"));
        	allowAutomaticUpdates = savedInstanceState.getBoolean("allow_automatic_updates");
        	walkingPoints = savedInstanceState.getParcelableArrayList("user_points");
        	//TODO: decide what the conditions for this should be
        	if (state.equals(State.walking) && walkingPoints != null) {
        		for (int i = 0; i < walkingPoints.size()-2; i++) {
            		PolylineOptions line = new PolylineOptions().add(
            				Utilities.toLatLng(walkingPoints.get(i)),
            				Utilities.toLatLng(walkingPoints.get(i+1))
            				);
            		mMap.addPolyline(line);
        		}
        	}
        	else {
        		walkingPoints = new ArrayList<Location>();
        	}
        }
        else {
        	setModeToIdle();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
    	if (!state.equals(State.walking)) { //only add location if in walking state
    		return;
    	}

    	// Report to the UI that the location is not certain -- debugging only
    	//TODO Tune this value, Remove this debugging check
    	if (location.getAccuracy() > 10000.0) {
    		locationText.setText(String.valueOf(location.getAccuracy()));
    	}

    	//if this is not the first point that is collected, add a line to the map
    	if (walkingPoints.size() != 0) {
    		addLineToMap(location, walkingPoints.get(walkingPoints.size()-1));
    	}
    	//add this location to list of points
    	walkingPoints.add(location);
    	
    	//display the new location added
    	String msg = "Updated Location: " +
    			Double.toString(location.getLatitude()) + ","  +
    			Double.toString(location.getLongitude());
    	Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * gets current location if the service is available
     */
    public Location getCurrentLocation() {
    	if(servicesConnected()) {
    		return mLocationClient.getLastLocation();
    	}
    	return null;
    }

	
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (allowAutomaticUpdates) {
            startPeriodicUpdates();
        }
    }
        
    private void startPeriodicUpdates() {
    	mLocationClient.requestLocationUpdates(mLocationRequest, this);    	
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
        Toast.makeText(this, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            int errorCode = connectionResult.getErrorCode();
  		  	GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
        }
    }
    
    
    private boolean servicesConnected() {
    	// Check that Google Play services is available
    	int errorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    	if (errorCode != ConnectionResult.SUCCESS) {
    		GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
    		return false;
    	}
    	return true;
    }
    
    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        mLocationClient.connect();
    }

    @Override
    protected void onPause() {
//        // Save the current setting for updates
//        mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
//        mEditor.commit();
        super.onPause();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("state",state.toString());
        outState.putBoolean("allow_automatic_updates", allowAutomaticUpdates);
        outState.putParcelableArrayList("user_points", walkingPoints);
    }
    
    @Override
    protected void onResume() {
        /*
         * Get any previous setting for location updates
         * Gets "false" if an error occurs
         *
         *Otherwise, turn off location updates
         *else {
         *mEditor.putBoolean("KEY_UPDATES_ON", false);
         * mEditor.commit();
         * } */
    	super.onResume();
    }
    
    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
    	
    	if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(this);
        }
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();
        super.onStop();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	

	/*@Override
	public void onMapClick(LatLng position) {
		if (plottingPoints != null) {
			LatLng lastPosition = plottingPoints.get(plottingPoints.size()-1);
    		PolylineOptions line = new PolylineOptions().add(position, lastPosition);
    		mMap.addPolyline(line);
		}
		else
		{
			plottingPoints = new ArrayList<LatLng>();
		}
		
		plottingPoints.add(position);
		
		//TODO figure out best way to remove previous marker
		mMap.addMarker(new MarkerOptions().position(position));
	}*/
}
