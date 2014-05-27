package org.commcare.areamap;

import java.util.ArrayList;

import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract.Constants;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapp.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends FragmentActivity 
						  implements
						  GooglePlayServicesClient.ConnectionCallbacks,
					        GooglePlayServicesClient.OnConnectionFailedListener,
					        com.google.android.gms.location.LocationListener,
					        com.google.android.gms.maps.GoogleMap.OnMapClickListener {
	
	private GoogleMap mMap;
	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;
	private LocationManager mLocationManager;

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	//private TextView locationText;
	
	//default states for the two booleans
	private boolean allowAutomaticUpdates = true;
	
	//array to store the locations from the periodic updates
	private ArrayList<Location> walkingPoints;
	private ArrayList<SphericalTriangle> areaTriangles;
	private double computedLandArea;
	private final double THRESHOLD = .5;
	private Location lastLocation;
	
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
	    
	public static enum State {idle, walking, finished, manualEntry};
	public State state = State.idle;

    /**
     * determines what UI elements need to be present based on what state we're in
     * TODO: updating UI for each state
     */
	public void refreshUI() {
		Button calculateButton = (Button)findViewById(R.id.connect_points_button);
		Button walkButton = (Button)findViewById(R.id.start_walk_button);
		Button cancelButton = (Button)findViewById(R.id.cancel_walk_button);
		Button useAreaButton = (Button)findViewById(R.id.send_to_commcare);
		Button manualEntryButton = (Button)findViewById(R.id.manual_entry_button);
		TextView instructions = (TextView)findViewById(R.id.instructions_text);
		TextView finishedInstructions = (TextView)findViewById(R.id.finished_instructions_text);
		TextView displayArea = (TextView)findViewById(R.id.area_text);
		switch(state) {
			case idle:
				walkButton.setBackgroundResource(R.drawable.tag_unselected);
				manualEntryButton.setBackgroundResource(R.drawable.tag_unselected);
				walkButton.setVisibility(View.VISIBLE);
				manualEntryButton.setVisibility(View.VISIBLE);
				calculateButton.setVisibility(View.INVISIBLE);
				cancelButton.setVisibility(View.INVISIBLE);
				useAreaButton.setVisibility(View.INVISIBLE);
				instructions.setVisibility(View.VISIBLE);
				finishedInstructions.setVisibility(View.INVISIBLE);
				displayArea.setVisibility(View.INVISIBLE);
				break;
			case walking:
				walkButton.setBackgroundResource(R.drawable.tag_selected);
				manualEntryButton.setBackgroundResource(R.drawable.tag_unselected);
				manualEntryButton.setVisibility(View.VISIBLE);
				walkButton.setVisibility(View.VISIBLE);
				calculateButton.setVisibility(View.VISIBLE);
				cancelButton.setVisibility(View.VISIBLE);
				useAreaButton.setVisibility(View.INVISIBLE);
				instructions.setVisibility(View.INVISIBLE);
				finishedInstructions.setVisibility(View.INVISIBLE);
				displayArea.setVisibility(View.INVISIBLE);
				break;
			case finished:
				walkButton.setBackgroundResource(R.drawable.tag_unselected);
				manualEntryButton.setBackgroundResource(R.drawable.tag_unselected);
				manualEntryButton.setVisibility(View.VISIBLE);
				walkButton.setVisibility(View.VISIBLE);
				calculateButton.setVisibility(View.INVISIBLE);
				cancelButton.setVisibility(View.VISIBLE);
				useAreaButton.setVisibility(View.VISIBLE);
				instructions.setVisibility(View.INVISIBLE);
				finishedInstructions.setVisibility(View.VISIBLE);
				displayArea.setText("" + computedLandArea + " sq meters");
				displayArea.setVisibility(View.VISIBLE);
				break;
			case manualEntry:	
				walkButton.setBackgroundResource(R.drawable.tag_unselected);
				manualEntryButton.setBackgroundResource(R.drawable.tag_selected);
				calculateButton.setVisibility(View.VISIBLE);				
				cancelButton.setVisibility(View.VISIBLE);
				useAreaButton.setVisibility(View.INVISIBLE);
				manualEntryButton.setVisibility(View.VISIBLE);
				instructions.setVisibility(View.INVISIBLE);
				finishedInstructions.setVisibility(View.INVISIBLE);
				displayArea.setVisibility(View.INVISIBLE);
				walkButton.setVisibility(View.VISIBLE);
		}
	}
	
	private void setModeToIdle() {
		state = State.idle;
		refreshUI();
	}
	
	private void setModeToWalking() {
		state = State.walking;
		refreshUI(); 
	}
	
	private void setModeToFinished() {
		state = State.finished;
		refreshUI();
	}
	
	private void setModeToManual() {
		state = State.manualEntry;
		refreshUI();
	}
	
	private void addLineToMap(Location p1, Location p2) {
    	PolylineOptions line = new PolylineOptions().add(
    			Utilities.toLatLng(p1),Utilities.toLatLng(p2));
		mMap.addPolyline(line);
    }
	
	public void onDoneClicked(View v) {
		int sz = walkingPoints.size();
		if (sz >= 3) {
			Location firstPoint = walkingPoints.get(0);
			Location lastPoint = walkingPoints.get(sz-1);
			addLineToMap(firstPoint, lastPoint);
			calculateArea();
			//Toast.makeText(v.getContext(), "Calculated area is: " + computedLandArea 
					//+ " sq meters.", Toast.LENGTH_LONG).show();
			setModeToFinished();
		}
		else {
			Toast.makeText(v.getContext(), "You need at least 3 points to calculate an area!", 
					Toast.LENGTH_SHORT).show();
		}  
	}

	public void onStartWalkClicked(View v) {
		if (state == State.walking) {
			return;
		}
		setModeToWalking();
		if (walkingPoints == null) {
			walkingPoints = new ArrayList<Location>(); 
		}
	}
	
	public void onCancelClicked(View v) {
		mMap.clear();
		setModeToIdle();
	}
	
	public void onManualClicked(View v) {
		if (state == State.manualEntry) {
			return;
		}
		setModeToManual();
		if (walkingPoints == null) {
			walkingPoints = new ArrayList<Location>();
		}
	}
	
	@Override
	public void onMapClick(LatLng position) {
		Log.i("here","onMapClick entered");
		Location newLocation = new Location("");
		newLocation.setLatitude(position.latitude);
		newLocation.setLongitude(position.longitude);
		int sz = walkingPoints.size();
		if (sz != 0) {
			Location lastLocation = walkingPoints.get(sz-1);	
			addLineToMap(lastLocation,newLocation);
		}
		walkingPoints.add(newLocation);
	}
	
	
	/*
	 * send the calculated result back to ODK. The magic string odk_intent_data
	 * correctly directs this behavior.
	 */
	public void sendAnswerBackToApp(View v) {
		Intent intent = new Intent();
		intent.putExtra("odk_intent_data", computedLandArea);
		setResult(RESULT_OK, intent);
		finish(); //TODO: Do we want the app to automatically close after doing this?
	}
	
	private void calculateArea() {
		areaTriangles = new ArrayList<SphericalTriangle>();
		Location p1,p2,p3,lastP;
		p1 = walkingPoints.get(0);
		lastP = walkingPoints.get(1);
		for (int i = 2; i < walkingPoints.size(); i++) {
			p2 = lastP;
			p3 = walkingPoints.get(i);
			lastP = p3;
			SphericalTriangle t = new SphericalTriangle(p1,p2,p3);
			areaTriangles.add(t);
		}
		double area = 0;
		for (SphericalTriangle t : areaTriangles) {
			area += t.getArea();
		}
		this.computedLandArea = area;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.activity_main);
        // Try to obtain the map from the SupportMapFragment.
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
  
        //mMap.setOnMapClickListener(this); 
        //mMap.setBuildingsEnabled(false); 
        
        //locationText = (TextView) findViewById(R.id.LocationText);
    	mLocationRequest = LocationRequest.create();
    	mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    	mLocationRequest.setInterval(UPDATE_INTERVAL);
    	mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
       
        if (savedInstanceState != null) {
        	restoreSavedInstanceState(savedInstanceState);
        }
        else {
        	setModeToIdle();
        }
    }
	
	private void restoreSavedInstanceState(Bundle savedInstanceState) {
        //Toast.makeText(this, "instance state restored", Toast.LENGTH_LONG).show();
    	state = State.valueOf(savedInstanceState.getString("state"));
    	switch(state) {
    	case idle:
    		Log.i("here","state is idle");
    		break;
		case walking:
			Log.i("here","state is walking");
			break;
		case finished:
			Log.i("here", "state is finished");
			break;
		case manualEntry:
			Log.i("here", "state is manual");
    	}
	
    	allowAutomaticUpdates = savedInstanceState.getBoolean("allow_automatic_updates");
    	walkingPoints = savedInstanceState.getParcelableArrayList("user_points");
    	computedLandArea = savedInstanceState.getDouble("calculated_area");
    	if (walkingPoints != null) {
    		int sz = walkingPoints.size();
    		lastLocation = walkingPoints.get(sz-1);
    		for (int i = 0; i < sz-2; i++) {
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
	
	/**
     * gets current location if the service is available
     */
    public Location getCurrentLocation() {
    	if(mLocationClient.isConnected()) {
    		return mLocationClient.getLastLocation();
    	}
		Log.i("Error:","getCurrentLocation() failed");
    	return null;
    }
    
    private void centerMapOnMyLocation() {
        mMap.setMyLocationEnabled(true);
        Location location = getCurrentLocation();
        if(location != null) {
        	LatLng myLocation = new LatLng(location.getLatitude(), location.getLongitude());
        	mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,17), 2000, null);
        }
        else {
        	Log.i("Error:","Could not center map on starting location");
        }
    }

	
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (allowAutomaticUpdates) {
            startPeriodicUpdates();
        }
        centerMapOnMyLocation();
    }
        
    private void startPeriodicUpdates() {
    	Log.i("Location", "startPeriodicUpates called");
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
        /* Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.*/
    	Log.i("Error:", "onConnectionFailed entered");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /* Thrown if Google Play services canceled the original
                 * PendingIntent */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /* If no resolution is available, display a dialog to the
             * user with the error. */
            int errorCode = connectionResult.getErrorCode();
  		  	GooglePlayServicesUtil.getErrorDialog(errorCode, this, 0).show();
        }
    }
    

    @Override
    public void onLocationChanged(Location location) {
    	//only add location if in walking state
    	if (!state.equals(State.walking)) { 
    		return;
    	}
    	
    	//if this is not the first point, check for its distance from the last point
    	if (walkingPoints.size() != 0) {
    		double distFromLast = Utilities.findDistance(Utilities.toLatLng(location), 
    			Utilities.toLatLng(lastLocation));
    		Log.i("Threshold Check",""+distFromLast);
    		if (distFromLast < THRESHOLD) {
    			return;
    		}
        	addLineToMap(lastLocation, location);
    	}
    	
    	//add this location to list of points
    	walkingPoints.add(location);
    	lastLocation = location;
    	
    	//log the new location added
    	String msg = "Updated Location: " +
    			Double.toString(location.getLatitude()) + ","  +
    			Double.toString(location.getLongitude());
    	Log.i("Points", msg);
    }

    
    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        //testFindDistance();
    }
    
    public static void testFindDistance() {
    	LatLng l1 = new LatLng(42.374921,-71.110060);
		LatLng l2 = new LatLng(42.374911,-71.109964);
		double dist = Utilities.findDistance(l1,l2);
		Log.i("here", ""+dist);
    }
    
    @Override
    protected void onRestart() {
    	super.onRestart();
    	if (!mLocationClient.isConnected()) {
    		mLocationClient.connect();
    	}
    	refreshUI();
    }

    @Override
    protected void onPause() {
    	/*Save the current setting for updates
         mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
         mEditor.commit();*/
        super.onPause();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //Toast.makeText(this, "onSaveInstanceState called", Toast.LENGTH_LONG).show();
        super.onSaveInstanceState(outState);
        outState.putString("state",state.toString());
        outState.putBoolean("allow_automatic_updates", allowAutomaticUpdates);
        outState.putParcelableArrayList("user_points", walkingPoints);
        outState.putDouble("calculated_area", computedLandArea);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (!mLocationClient.isConnected()) {
            mLocationClient.connect();
        }
    	refreshUI();
    }
    
    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
    	if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
            mLocationClient.disconnect();
        }
        super.onStop();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
