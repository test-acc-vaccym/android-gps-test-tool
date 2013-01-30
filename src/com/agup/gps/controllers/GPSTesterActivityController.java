package com.agup.gps.controllers;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.acl.LastOwnerException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Vector;

import com.agup.gps.R;
import com.agup.gps.SatelliteDataActivity;
import com.agup.gps.SettingsActivity;
import com.agup.gps.fragments.GPSAlertDialogFragment;
import com.agup.gps.utils.ElapsedTimer;
import com.esri.android.map.MapView;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.ags.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.ags.geocode.LocatorReverseGeocodeResult;
import com.esri.quickstart.EsriQuickStart;
import com.esri.quickstart.EsriQuickStartEvent;
import com.esri.quickstart.MapViewEventType;
import com.esri.quickstart.EsriQuickStart.MapType;
import com.esri.quickstart.EsriQuickStartEventListener;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class GPSTesterActivityController {
	
	private Activity _activity;
	private TextView _networkLocationTextView;
	private static TextView _gpsLocationTextView;
	private TextView _gpsSatelliteTextView;
	private TextView _gpsNMEATextView;
	private TextView _allLocationProvidersTextView;
	private TextView _bestLocationProviderTextView;
	private TextView _cachedLocationNetworkProvider;
	private TextView _cachedLocationGPSProvider;
	private static TextView _bestAvailableInfoTextView;
	private TextView _elapsedTime;
	private ImageView _settings;
	private static ImageView _bestAvailableImageView;
	
	private static EsriQuickStart _map;
	private static EsriQuickStartEventListener _mapListener = null;

	private LocationListener _locationListenerNetworkProvider = null;
	private LocationListener _locationListenerGPSProvider = null;
	private LocationManager _locationManager;
	private Location _lastKnownLocationNetworkProvider = null;
	private Location _lastKnownLocationGPSProvider = null;	
	
	private static ElapsedTimer _elapsedTimer;
	private boolean _initialLapGPS = false;
	private boolean _initialLapNetwork = false;
	
	private static long _initialGPSTime = 0;
	private long _initialNetworkTime = 0;
	private long _finalNetworkTime = 0;
	
	private static double _gpsLatitude = 0.0;
	private static double _gpsLongitude = 0.0;
	private static double _gpsAccuracy = 0.0;
	private static double _cachedGPSLatitude = 0.0;
	private static double _cachedGPSLongitude = 0.0;
	private static double _cachedGPSAccuracy = 0.0;	
	private static double _cachedNetworkLatitude = 0.0;
	private static double _cachedNetworkLongitude = 0.0;
	private static double _cachedNetworkAccuracy = 0.0;
	private static long _cachedNetworkTime = 0;
	private static double _networkLatitude = 0.0;
	private static double _networkLongitude = 0.0;
	private static double _networkAccuracy = 0.0;
	
	private Button _pauseButton;
	private Button _startButton;
	
	private ImageView _imSatelliteActivity;	
	private ImageView _imGPS;
	private ImageView _imNetwork;
	private ImageView _imCriteria;
	
	private static SharedPreferences _preferences;	
	private static BestAvailableType _bestAvailableType;    	
	private static final DecimalFormat _decimalFormat = new DecimalFormat("#,###.00");		

	public GPSTesterActivityController(Activity activity, Context context, EsriQuickStart map){
		_map = map;
		_activity = activity;
		_elapsedTimer = new ElapsedTimer();
	    _elapsedTime = (TextView) _activity.findViewById(R.id.elapsedTime);
	    _elapsedTime.setTextColor(Color.YELLOW);

		_cachedLocationNetworkProvider = (TextView) _activity.findViewById(R.id.cachedNetworkProvider);
		_cachedLocationGPSProvider = (TextView) _activity.findViewById(R.id.cachedGPS);
		_networkLocationTextView = (TextView) _activity.findViewById(R.id.networkLocationProvider);
		_gpsLocationTextView = (TextView) _activity.findViewById(R.id.gpsLocationProvider);
		_gpsNMEATextView = (TextView) _activity.findViewById(R.id.gpsNMEAInfo);
		_gpsSatelliteTextView = (TextView) _activity.findViewById(R.id.gpsSatelliteInfo);
		_allLocationProvidersTextView = (TextView) _activity.findViewById(R.id.allLocationProviders);
		_bestLocationProviderTextView = (TextView) _activity.findViewById(R.id.bestLocationProviders);
		_bestAvailableInfoTextView = (TextView) _activity.findViewById(R.id.bestAvailableInfo);
		_bestAvailableImageView = (ImageView) _activity.findViewById(R.id.bestAvailableImageView);
		_settings = (ImageView) _activity.findViewById(R.id.settings);
		_pauseButton = (Button) _activity.findViewById(R.id.PauseButton);
		_startButton = (Button)_activity.findViewById(R.id.StartAllButton);
		_preferences = PreferenceManager.getDefaultSharedPreferences(_activity);
		_imGPS = (ImageView)_activity.findViewById(R.id.gpsEnabledIcon);
		_imNetwork = (ImageView)_activity.findViewById(R.id.networkEnabledIcon);	
		_imSatelliteActivity = (ImageView) _activity.findViewById(R.id.satellitedata);	
		_imCriteria = (ImageView) _activity.findViewById(R.id.criteriaEnabledIcon);

		setUI();
		setOnClickListeners();		

		//This is very expensive to run - but hey if you need it...it's here.
		//My recommendation is move it off the UI thread and put it on a timer.
//    	String cpu = Float.toString(getCPU() * 100);
//    	Log.d("GPSTester","CPU usage: " + cpu);
				
	}
	
	private void setOnClickListeners(){
		
		TableRow cachedNetworkProvider = (TableRow) _activity.findViewById(R.id.cachedNetworkProviderTableRow);
		cachedNetworkProvider.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(_map.isMapLoaded() == true && _cachedNetworkLatitude != 0.0){					
					_map.centerAt(_cachedNetworkLatitude, _cachedNetworkLongitude, 4500.00, true);
				}
			}
		});
		
		TableRow cachedGPSProvider = (TableRow) _activity.findViewById(R.id.cachedGPSTableRow);
		cachedGPSProvider.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(_map.isMapLoaded() == true && _cachedGPSLatitude != 0.0){
					_map.centerAt(_cachedGPSLatitude, _cachedGPSLongitude, 4500.00, true);
				}
			}
		});
		
		TableRow networkProvider = (TableRow) _activity.findViewById(R.id.networkLocationProviderTableRow);
		networkProvider.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(_map.isMapLoaded() == true && _networkLatitude != 0.0){
					_map.centerAt(_networkLatitude, _networkLongitude, 4500.00, true);
				}
			}
		});	
		
		TableRow gpsProvider = (TableRow) _activity.findViewById(R.id.gpsLocationProviderTableRow);
		gpsProvider.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(_map.isMapLoaded() == true && _gpsLatitude != 0.0){
					_map.centerAt(_gpsLatitude, _gpsLongitude, 4500.00, true);
				}
			}
		});		
		
		_settings.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(_activity.getApplicationContext(),SettingsActivity.class);
				_activity.startActivity(i);
			}
		});
		
//		_pauseButton.setBackgroundColor(Color.LTGRAY);
		_pauseButton.setOnClickListener(new View.OnClickListener() {			
			
			@Override
			public void onClick(View v) {				
				pauseLocation();
			}
		});
		
		_startButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(_locationManager == null){
					startLocation();
				}
				else{
					stopLocation();
				}
			}
		});	
		
		_imSatelliteActivity.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(_activity.getApplicationContext(),SatelliteDataActivity.class);
				_activity.startActivity(i);
			}
		});
	}
	
	private void setUI(){
		
		Display display = _activity.getWindowManager().getDefaultDisplay();
		int width = display.getWidth(); //WARNING: this method was deprecated at API level 13
		int height = (int)(display.getHeight() * .3333); //WARNING: this method was deprecated at API level 13
		
		String bestAvailableText = "";
		String cachedLocationNetworkProviderText = "";
		String cachedLocationGPSProviderText = "";
		String gpsLocationText = "";
		String networkLocationText = "";
		
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
		MapView temp = (MapView)_activity.findViewById(R.id.map);
		temp.setLayoutParams(layoutParams);
		
		//Load the map
		if(!_map.layerExists(MapType.TOPO)){
			//_map.setLayoutParams(layoutParams);
			_map.addLayer(MapType.TOPO, null, null, null,true); 
		}
		else{
			_map.clearAllGraphics();
		}
		
		bestAvailableText = "<b><font color='yellow'>Best Accuracy = N/A</b></font>" + 
				"<br><b>Lat/Lon:</b> N/A" +  
				"<br><b>Accuracy:</b> N/A";
		
  		setBestAvailableImageView(BestAvailableType.NULL);
		
		cachedLocationNetworkProviderText = "<b><font color='yellow'>Cached Location Provider</b></font>" + 
				"<br><b>Lat/Lon:</b> N/A" +  
				"<br><b>Accuracy:</b> N/A";
		
		cachedLocationGPSProviderText = "<b><font color='yellow'>Cached GPS Provider</b></font>" + 
				"<br><b>Lat/Lon:</b> N/A" + 
				"<br><b>Accuracy:</b> N/A";
		
		gpsLocationText = "<b><font color='yellow'>GPS Provider</b></font>" + 
				"<br><b>Lat/Lon:</b> N/A" + 
				"<br><b>Accuracy:</b> N/A";	
		
		networkLocationText = "<b><font color='yellow'>Network Provider</b></font>" + 
				"<br><b>Lat/Lon:</b> N/A" + 
				"<br><b>Accuracy:</b> N/A";
		
		_bestAvailableInfoTextView.setText(Html.fromHtml(bestAvailableText));
		_cachedLocationNetworkProvider.setText(Html.fromHtml(cachedLocationNetworkProviderText));
		_cachedLocationGPSProvider.setText(Html.fromHtml(cachedLocationGPSProviderText));
		_gpsLocationTextView.setText(Html.fromHtml(gpsLocationText));
		_networkLocationTextView.setText(Html.fromHtml(networkLocationText));
		_allLocationProvidersTextView.setText(Html.fromHtml("<b><font color='yellow'>List of available providers</b></font><br><br><br>"));		
		_bestLocationProviderTextView.setText(Html.fromHtml("<b><font color='yellow'>List of best providers</b></font><br><br><br>"));
		
	}
	
	protected void setLocationManagerUI(boolean gpsProviderEnabled, boolean networkProviderEnabled){
		
		String cachedLocationGPSProvider = "";
		String cachedLocationNetworkProvider = "";
		
		
		try{
			_lastKnownLocationNetworkProvider = _locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			_lastKnownLocationGPSProvider = _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		catch(Exception exc){
			Log.d("GPSTester","setLocationManagerUI() " + exc.toString());
		}
	
		//NOTE: This does not take into account if _cachedGPSTime == _cachedNetworkTime
		//The changes of that happening are small.
		if(_lastKnownLocationGPSProvider != null && _lastKnownLocationNetworkProvider != null){
			
			_cachedGPSLatitude = _lastKnownLocationGPSProvider.getLatitude();
			_cachedGPSLongitude = _lastKnownLocationGPSProvider.getLongitude();
			_cachedGPSAccuracy = _lastKnownLocationGPSProvider.getAccuracy();
			_cachedNetworkLatitude = _lastKnownLocationNetworkProvider.getLatitude();
			_cachedNetworkLongitude = _lastKnownLocationNetworkProvider.getLongitude();	
			_cachedNetworkAccuracy = _lastKnownLocationNetworkProvider.getAccuracy();
			final long cachedGPSTime = _lastKnownLocationGPSProvider.getTime();		
			final long cachedNetworkTime = _lastKnownLocationNetworkProvider.getTime();			
			
			String bestAvailableText = "";
				    	
	    	if(cachedGPSTime > cachedNetworkTime){
	    		bestAvailableText = "<b><font color='yellow'>Most Recent Accuracy</font> = <font color='red'>Cached GPS</b></font>" +
	    				"<br><b>GPS Accuracy:</b> " + _cachedGPSAccuracy + " meters" +
	    				"<br><b>GPS Lat/Lon:</b> " + _cachedGPSLatitude + ", " + _cachedGPSLongitude;
				setBestAvailableImageView(BestAvailableType.CACHED_GPS);	    		
	    	}  
	    	else if(cachedNetworkTime > cachedGPSTime){
	    		bestAvailableText = "<b><font color='yellow'>Most Recent Accuracy</font> = <font color='red'>Cached Network</b></font>" +
	    				"<br><b>GPS Accuracy:</b> " + _cachedNetworkAccuracy + " meters" +
	    				"<br><b>GPS Lat/Lon:</b> " + _cachedNetworkLatitude + ", " + _cachedNetworkLongitude;
				setBestAvailableImageView(BestAvailableType.CACHED_NETWORK);	    		
	    	} 	    	    	
	    	else{
	    		bestAvailableText = "<b><font color='yellow'>Most Recent Accuracy = N/A</b></font>" + 
	    				"<br><b>Lat/Lon:</b> N/A" +  
	    				"<br><b>Accuracy:</b> N/A";	    		
	    	}		
		
	    	_bestAvailableInfoTextView.setText(Html.fromHtml(bestAvailableText));	    	
		
		}
		
    	//setMapListeners(_lastKnownLocationGPSProvider, _lastKnownLocationNetworkProvider);
    	delayedStartCachedLocationProviders();
		
		if(_lastKnownLocationNetworkProvider == null){
			cachedLocationNetworkProvider = "<b><font color='yellow'>Cached Network Provider</font></b>" + 
					"<br><b>Lat/Lon:</b> N/A" + 
					"<br><b>Accuracy:</b> N/A";	
		}
		else{
			
			cachedLocationNetworkProvider = "<b><font color='yellow'>Cached Network Provider</font></b>" + 
					"<br><b>Updated:</b> " + _elapsedTimer.convertMillisToMDYHMSS(_lastKnownLocationNetworkProvider.getTime()) +
					"<br><b>Retrieved in:</b> " +
					_elapsedTimer.getMinutes() + ":" +
					_elapsedTimer.getSeconds() + ":" +
					_elapsedTimer.getMillis() +					
					"<br><b>Lat/Lon:</b> " + _cachedNetworkLatitude + ", " + _cachedNetworkLongitude +
			  		"<br><b>DMSS:</b> " + 
			  			Location.convert(_cachedNetworkLatitude, Location.FORMAT_SECONDS) + ", " +					  			
			  			Location.convert(_cachedNetworkLongitude, Location.FORMAT_SECONDS) +					
					"<br><b>Accuracy:</b> " + _cachedNetworkAccuracy + " meters";										
		}
		
		//If the phone was restarted this can be null
		if(_lastKnownLocationGPSProvider == null){
			cachedLocationGPSProvider = "<b><font color='yellow'>Cached GPS Provider</font></b>" + 
					"<br><b>Lat/Lon:</b> N/A" + 
					"<br><b>Accuracy:</b> N/A";
						
		}		
		else{			
			
			cachedLocationGPSProvider = "<b><font color='yellow'>Cached GPS Provider</font></b>" + 
					"<br><b>Updated:</b> " + _elapsedTimer.convertMillisToMDYHMSS(_lastKnownLocationGPSProvider.getTime()) +
					"<br><b'>Retrieved in:</b> " + 
					_elapsedTimer.getMinutes() + ":" +
					_elapsedTimer.getSeconds() + ":" +
					_elapsedTimer.getMillis() +					
					"<br><b>Lat/Lon:</b> " + _cachedGPSLatitude + ", " + _cachedGPSLongitude +
			  		"<br><b>DMSS:</b> " + 
			  			Location.convert(_cachedGPSLatitude, Location.FORMAT_SECONDS) + ", " +					  			
			  			Location.convert(_cachedGPSLongitude, Location.FORMAT_SECONDS) +					
					"<br><b>Accuracy:</b> " + Double.toString(_cachedGPSAccuracy) + " meters";			
		}	
		
		_cachedLocationNetworkProvider.setText(Html.fromHtml(cachedLocationNetworkProvider));
		_cachedLocationGPSProvider.setText(Html.fromHtml(cachedLocationGPSProvider));		
		
	    List<String> providers = _locationManager.getProviders(true);
	    String finalList = "<b><font color='yellow'>List of available providers</font></b><br>";
	    for (String provider : providers) {
	    	finalList = finalList + provider + "<br>";
	    }
	    
	    _allLocationProvidersTextView.setText(Html.fromHtml(finalList));	  
	    
	    getBestProviderNameViaCriteria();
	    
//	    Preferences.setSharedPreferences(
//	    		PreferenceKey.CACHED_GPS_LATLON, Double.toString(lastKnownLocationGPSProvider.getLatitude()) + "," +
//	    				Double.toString(lastKnownLocationGPSProvider.getLongitude()),_activity 
//	    );
//	    
//	    Preferences.setSharedPreferences(
//	    		PreferenceKey.CACHED_NETWORK_LATLON, Double.toString(lastKnownLocationNetworkProvider.getLatitude()) + "," +
//	    				Double.toString(lastKnownLocationNetworkProvider.getLongitude()),_activity 
//	    );	    
				

	}
	
	/**
	 * Let's you test scenarios based on various criteria settings. Does <b>not</b> currently include
	 * all criteria from: http://developer.android.com/reference/android/location/Criteria.html. 
	 * You can view the results of your settings changes in the Best Provider window at the
	 * bottom of the main activity screen.<br><br>
	 * <b>NOTE:</b> This does <b>override</b> how the default application operates.  
	 */
	private String getBestProviderNameViaCriteria(){
	    final int power = Integer.parseInt(_preferences.getString("pref_key_setPower", "1"));
	    final int accuracy = Integer.parseInt(_preferences.getString("pref_key_setAccuracy", "1"));	    
	    final boolean cost = Boolean.valueOf(_preferences.getString("pref_key_setCost", "true"));
	    	    
	    Criteria criteria = new Criteria();
	    criteria.setAccuracy(accuracy);
	    criteria.setCostAllowed(cost);
	    criteria.setPowerRequirement(power);
	    
	    String finalBestProvider = "<b><font color='yellow'>Best Provider (via Criteria)</font></b><br>";
	    String bestProviderName = _locationManager.getBestProvider(criteria, true);
	    
	    if(bestProviderName != null){
	    	_bestLocationProviderTextView.setText(Html.fromHtml(finalBestProvider + bestProviderName));	    
	    }
	    else{
	    	_bestLocationProviderTextView.setText(Html.fromHtml(finalBestProvider + "N/A"));
	    }
	    
	    return bestProviderName;
	}
	
	/**
	 * Returns CPU usage info. Careful using this it's CPU intensive by itself! 
	 * @return Percentage
	 */
	private float getCPU() {
	    try {
	        RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
	        String load = reader.readLine();

	        String[] toks = load.split(" ");

	        long idle1 = Long.parseLong(toks[5]);
	        long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	              + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        try {
	            Thread.sleep(360);
	        } catch (Exception e) {}

	        reader.seek(0);
	        load = reader.readLine();
	        reader.close();

	        toks = load.split(" ");

	        long idle2 = Long.parseLong(toks[5]);
	        long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
	            + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

	        return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));

	    } catch (IOException ex) {
	        ex.printStackTrace();
	    }

	    return 0;
	}
	
	private static void writeResultToTable(
			Location location,
			LocationManager locationManager,
			String elapsedTimeGPSProvider){
		
    	_gpsLatitude = location.getLatitude();
    	_gpsLongitude = location.getLongitude();
    	_gpsAccuracy = location.getAccuracy();  
    	final double speed = location.getSpeed();
    	final double altitude = location.getAltitude();
    	String bestAvailableText = "";
    	
		boolean networkProviderEnabled = false; 			
		
		try{
			networkProviderEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}
		catch(Exception exc){
			Log.d("GPSTester","WriteResultToTable(): " + exc.getMessage());
		}
		
    	if(_networkAccuracy > _gpsAccuracy && _gpsAccuracy > 0.0){
    		_bestAvailableType = BestAvailableType.GPS;
    		bestAvailableText = "<b><font color='yellow'>Best Accuracy</font> = <font color='red'>GPS</b></font>" +
    				"<br><b>GPS Accuracy:</b> " + _gpsAccuracy + " meters" +
    				"<br><b>GPS Lat/Lon:</b> " + _gpsLatitude + ", " + _gpsLongitude;
    	}  	
    	else if(_gpsAccuracy > _networkAccuracy && networkProviderEnabled == true){
    		_bestAvailableType = BestAvailableType.NETWORK;	    		
    		bestAvailableText = "<b><font color='yellow'><b><font color='yellow'>Best Accuracy</font> = <font color='red'>Network</b></font></b></font>" +
    				"<br><b>Network Accuracy:<b/> " + _networkAccuracy + " meters" +
    				"<br><b>Network Lat/Lon:<b/> " + _networkLatitude + ", " + _networkLongitude;
    	}
    	else if(networkProviderEnabled == false){
    		_bestAvailableType = BestAvailableType.GPS;
    		bestAvailableText = "<b><font color='yellow'>Best Accuracy</font> = <font color='red'>GPS</b></font>" +
    				"<br><b>GPS Accuracy:</b> " + _gpsAccuracy + " meters" +
    				"<br><b>GPS Lat/Lon:</b> " + _gpsLatitude + ", " + _gpsLongitude;
    	}	    	
    	else{
    		_bestAvailableType = BestAvailableType.NULL;
    		bestAvailableText = "<b><font color='yellow'>Best Accuracy = N/A</b></font>" + 
    				"<br><b>Lat/Lon:</b> N/A" +  
    				"<br><b>Accuracy:</b> N/A";	    		
    	}
    	
	  	setBestAvailableImageView(_bestAvailableType);
		
		String elapsedTimeSinceLastGPS = _elapsedTimer.calculateTimeDifference(_initialGPSTime, _elapsedTimer.getElapsedtime());			  	
    	_initialGPSTime = _elapsedTimer.getElapsedtime();		
    	
	  	final String gpsLocationText = "<b><font color='yellow'>GPS Provider</b></font>" +
				"<br><b>1st update elapsed time:</b> " + elapsedTimeGPSProvider +
			  	"<br><b>Since last update:</b> " + elapsedTimeSinceLastGPS +
		  		"<br><b>Lat/Lon:</b> " + _gpsLatitude + ", " + _gpsLongitude +
		  		"<br><b>DMSS:</b> " + 
		  			Location.convert(_gpsLatitude, Location.FORMAT_SECONDS) + ", " +					  			
		  			Location.convert(_gpsLongitude, Location.FORMAT_SECONDS) +
		  		"<br><b>Accuracy:</b> " + _gpsAccuracy + " meters" +
		  		"<br><b>Speed:</b> " + _decimalFormat.format((speed * 2.2369)) + " mph" + ", " +
		  		_decimalFormat.format(((speed * 3600)/1000)) + " km/h" +
		  		"<br><b>Altitude:</b> " + _decimalFormat.format(altitude) + " m, " +
		  		_decimalFormat.format(altitude * 3.2808) + " ft" +
		  		"<br><b>Bearing:</b> " + location.getBearing() + " deg";				  	
			  	
    	
	  	_gpsLocationTextView.setText(Html.fromHtml(gpsLocationText));	  	
    	_bestAvailableInfoTextView.setText(Html.fromHtml(bestAvailableText));
    	
		final int redMapGraphicSize = Integer.valueOf(_preferences.getString("pref_key_gpsGraphicSize", "10"));    	
    	
    	// Called when a new location is found by the network location provider.
    	if(_preferences.getBoolean("pref_key_centerOnGPSCoords", true) == true){
    		_map.centerAt(_gpsLatitude, _gpsLongitude, true);
    	}
    	if(_preferences.getBoolean("pref_key_accumulateMapPoints", true) == false){
    		_map.clearPointsGraphicLayer();
    	}
    	
    	_map.addGraphicLatLon(_gpsLatitude, _gpsLongitude, null, SimpleMarkerSymbol.STYLE.CIRCLE,Color.RED,redMapGraphicSize);	    	
	}
	
	private void setLocationListenerGPSProvider(){
		
		_locationListenerGPSProvider = new LocationListener() {
	
			boolean initialLapNetwork = true;	
			String elapsedTimeGPSProvider = "N/A";
			
			public void onLocationChanged(Location location) {
					
				if(initialLapNetwork == true){
			    	  
					//NOTE: You can also use location.getTime()
					elapsedTimeGPSProvider = 
							_elapsedTimer.getMinutes() + ":" +
							_elapsedTimer.getSeconds() + ":" +
							_elapsedTimer.getMillis();
					
					initialLapNetwork = false;
				}				
				
				writeResultToTable(
						location, 
						_locationManager, 
						elapsedTimeGPSProvider);
		      
//			    Preferences.setSharedPreferences(
//			    		PreferenceKey.GPS_LATLON, location.getLatitude() + "," +
//			    				location.getLongitude(),_activity 
//			    );
			    
//			    String t = Preferences.getSharedPreferences(PreferenceKey.GPS_LATLON, _activity);
//			    String x = t;
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {
				switch (status) {
				case LocationProvider.OUT_OF_SERVICE:
					Log.d("GPSTester", "Location Status Changed: GPS Out of Service");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: GPS Out of Service",
					//		Toast.LENGTH_LONG).show();
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					Log.d("GPSTester", "Location Status Changed: GPS Temporarily Unavailable");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: GPS Temporarily Unavailable",
					//		Toast.LENGTH_LONG).show();
					break;
				case LocationProvider.AVAILABLE:
					Log.d("GPSTester", "Status Changed: GPS Available");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: GPS Available",
					//		Toast.LENGTH_LONG).show();
					break;
				}		    	
		    }

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};
		
		try{
			long minDistance = Long.valueOf(_preferences.getString("pref_key_updateGPSMinDistance", "0"));
			long minTime = Long.valueOf(_preferences.getString("pref_key_updateGPSMinTime", "0"));
			
			// Register the listener with the Location Manager to receive location updates
			_locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, minTime, minDistance, _locationListenerGPSProvider);		
		}
		catch(Exception exc){
			Log.d("GPSTester", "Unable to start GPS provider. Bad value. " + exc.getMessage());
		}		
	}
	
	private void setLocationListenerNetworkProvider(){
		
		final int blueMapGraphicSize = Integer.valueOf(_preferences.getString("pref_key_networkGraphicSize", "10"));
		final boolean centerUsingGPS = _preferences.getBoolean("pref_key_centerOnGPSCoords", true);		
		final boolean accumlateMapPts = _preferences.getBoolean("pref_key_accumulateMapPoints", true);		
		
		_locationListenerNetworkProvider = new LocationListener() {
			
			String elapsedTimeNetworkProvider ="N/A";
			String elapsedTimeSinceLastNetworkUpdate = "N/A";
			String networkLocationText = "";
			String bestAvailableText = "";		
			boolean initialLapNetwork = true;	
			
		    public void onLocationChanged(Location location) {
		    	
		    	_networkLatitude = location.getLatitude();
		    	_networkLongitude = location.getLongitude();	
		    	_networkAccuracy = location.getAccuracy();
		    	
				boolean gpsProviderEnabled = false;		
				
				try{
					gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
				}
				catch(Exception exc){
					Log.d("GPSTester","setLocationListenerNetworkProvider(): " + exc.toString());
				}		    	
		    	
		    	if(_networkAccuracy > _gpsAccuracy && _gpsAccuracy > 0.0 && gpsProviderEnabled == true){
		    		bestAvailableText = "<b><font color='yellow'>Best Accuracy</font> = <font color='red'>GPS</b></font>" +
		    				"<br><b>GPS Accuracy:</b> " + _gpsAccuracy + " meters" +
		    				"<br><b>GPS Lat/Lon:</b> " + _gpsLatitude + ", " + _gpsLongitude;
		    		
		    		setBestAvailableImageView(BestAvailableType.GPS);
		    	}
		    	else{
		    		bestAvailableText = "<b><font color='yellow'>Best Accuracy</font> = <font color='red'>Network</b></font>" +
		    				"<br><b>Network Accuracy:<b/> " + _networkAccuracy + " meters" +
		    				"<br><b>Network Lat/Lon:<b/> " + _networkLatitude + ", " + _networkLongitude;
		    		
		    		setBestAvailableImageView(BestAvailableType.NETWORK);		    		
		    	}	
		    	
		    	_bestAvailableInfoTextView.setText(Html.fromHtml(bestAvailableText));		    	
		    	
				if(_initialLapNetwork == true){
						    	  
					//NOTE: You can also use location.getTime()
					elapsedTimeNetworkProvider = 
							_elapsedTimer.getMinutes() + ":" +
							_elapsedTimer.getSeconds() + ":" +
							_elapsedTimer.getMillis();
					
					_initialLapNetwork = false;
					
				}

		    	if(centerUsingGPS == false){
		    		_map.centerAt(_networkLatitude, _networkLongitude, true);
		    	}		
		    	if(accumlateMapPts == false){
		    		_map.clearPointsGraphicLayer();
		    	}		    	
		    	_map.addGraphicLatLon(_networkLatitude, _networkLongitude, null, SimpleMarkerSymbol.STYLE.CIRCLE,Color.BLUE,blueMapGraphicSize);


	      
		      _finalNetworkTime = _elapsedTimer.getElapsedtime();		      
		      
		      if(_finalNetworkTime > _initialNetworkTime){  
		    	  //_elapsedTimeSinceLastNetworkProvider.setText("Since last Network Provider update: " + _elapsedTimer.calculateTimeDifference(_initialNetworkTime, _finalNetworkTime));
		    	  elapsedTimeSinceLastNetworkUpdate = _elapsedTimer.calculateTimeDifference(_initialNetworkTime, _finalNetworkTime);
		    	  _initialNetworkTime = _elapsedTimer.getElapsedtime();		    	  
		      }
		      
		      // Called when a new location is found by the network location provider.
		      networkLocationText = "<b><font color='yellow'>Network Provider</b></font>" + 
		    		  	"<br><b>1st update elapsed time:</b> " + elapsedTimeNetworkProvider +
		    		  	"<br><b>Since last update:</b> " + elapsedTimeSinceLastNetworkUpdate +
		    		  	"<br><b>Lat/Lon:</b> " + _networkLatitude + ", " + _networkLongitude +
				  		"<br><b>DMSS:</b> " + 
				  			Location.convert(_networkLatitude, Location.FORMAT_SECONDS) + ", " +					  			
				  			Location.convert(_networkLongitude, Location.FORMAT_SECONDS) +						
						"<br><b>Accuracy:</b> " + _networkAccuracy + " meters";	
		      
		      _networkLocationTextView.setText(Html.fromHtml(networkLocationText));
		      
//			    Preferences.setSharedPreferences(
//			    		PreferenceKey.NETWORK_LATLON, Double.toString(latitude) + "," +
//			    				Double.toString(longitude),_activity 
//			    );		      
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {
				switch (status) {
				case LocationProvider.OUT_OF_SERVICE:
					Log.d("GPSTester", "Location Status Changed: Network Provider Out of Service");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: Network Provider Out of Service",
					//		Toast.LENGTH_LONG).show();
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					Log.d("GPSTester", "Location Status Changed: Network Provider Temporarily Unavailable");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: Network Provider Temporarily Unavailable",
					//		Toast.LENGTH_LONG).show();
					break;
				case LocationProvider.AVAILABLE:
					Log.d("GPSTester", "Location Status Changed: Network Provider Available");
					//Toast.makeText(_activity.getApplicationContext(), "Location Status Changed: Network Provider Available",
					//		Toast.LENGTH_LONG).show();
					break;
				}	
		    }

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		};

		try{
			long minDistance = Long.valueOf(_preferences.getString("pref_key_updateNetworkMinDistance", "0"));
			long minTime = Long.valueOf(_preferences.getString("pref_key_updateNetworkMinTime", "0"));

			// Register the listener with the Location Manager to receive location updates
			_locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, minTime, minDistance, _locationListenerNetworkProvider);			
		
		}
		catch(Exception exc){
			Log.d("GPSTester", "Unable to start network provider. Bad value. " + exc.getMessage());
		}
	}
	
	/**
	 * Uses a Handler to check at intervals until the base map has fully initialized. 
	 * Fails if an initialized map is not detected after 5 tries. Map has to be loaded in 
	 * order to manipulate it and draw graphics on it.
	 * @param startGPS if you set this to false it will attempt to start Network Listeners. 
	 */	
	public void delayedStartCachedLocationProviders(){
		final Handler handler = new Handler();		
		final boolean centerUsingGPS = _preferences.getBoolean("pref_key_centerOnGPSCoords", true);		 		
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{

					handler.postDelayed(new Runnable() {
						int counter = 0;
						
						@Override
						public void run() {
							counter++;
							try{ 
								boolean test = _map.isMapLoaded();
								Log.d("GPSTester","delayedStartCachedLocationProviders(): Testing if layer is loaded. Attempt #" + counter);
								if(test == true){
									
									Log.d("GPSTester","delayedStartCachedLocationProviders(): map is loaded.");
									Boolean mapLoaded = _map.layerExists(MapType.TOPO);															
									
									if(mapLoaded == true && _lastKnownLocationGPSProvider != null && _lastKnownLocationNetworkProvider != null){			

										if(centerUsingGPS == false){
											_map.centerAt(_cachedNetworkLatitude, _cachedNetworkLongitude, 4500.0, true);
										}
										else{
								    		_map.centerAt(_cachedGPSLatitude, _cachedGPSLongitude, 4500.0, true);
										}
										
								    	//Add Network location to map and give it a unique symbol
										_map.addGraphicLatLon(
												_cachedNetworkLatitude,
												_cachedNetworkLongitude, 
								    			null, 
								    			SimpleMarkerSymbol.STYLE.DIAMOND,Color.GREEN,
								    			15);		
										
								    	//Add GPS location to the map and give it a unique symbol
										_map.addGraphicLatLon(
												_cachedGPSLatitude, 
												_cachedGPSLongitude, 
								    			null, 
								    			SimpleMarkerSymbol.STYLE.DIAMOND,Color.RED,
								    			15);					
									}
								}
								else{
									if(counter < 5){
										handler.postDelayed(this, 5000);
									}
									else{
										Log.d("GPSTester","delayedStartCachedLocationProviders(): Unable to start Location Listener.");
										_map.displayToast("Map not loading? Try restarting app.", Toast.LENGTH_LONG);
									}
								}
							}
							catch(Exception exc){
								Log.d("GPSTester","delayedStartCachedLocationProviders() exception: " + exc.toString());
							}
							
						}
					}, 250);					
				}
				catch(Exception exc){
					Log.d("GPSTester","delayedStartCachedLocationProviders() exception: " + exc.toString());
				}
			}
		};
		
		task.run();
	}		
	
	/**
	 * Uses a Handler to check at intervals until the base map has fully initialized. 
	 * Fails if an initialized map is not detected after 5 tries. Map has to be loaded in 
	 * order to manipulate it and draw graphics on it.
	 * @param startGPS if you set this to false it will attempt to start Network Listeners. 
	 */	
	public void delayedStartLocationProvider(final Boolean startGPS){
		final Handler handler = new Handler();		
		final boolean centerUsingGPS = _preferences.getBoolean("pref_key_centerOnGPSCoords", true);		
		final int blueMapGraphicSize = Integer.valueOf(_preferences.getString("pref_key_networkGraphicSize", "10"));	
		final int redMapGraphicSize = Integer.valueOf(_preferences.getString("pref_key_gpsGraphicSize", "10"));  		
		
		Runnable task = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try{

					handler.postDelayed(new Runnable() {
						int counter = 0;
						
						@Override
						public void run() {
							counter++;
							try{ 
								boolean test = _map.isMapLoaded();
								Log.d("GPSTester","delayedStartLocationProvider(): Testing if layer is loaded. Attempt #" + counter);
								if(test == true){
									
									Log.d("GPSTester","delayedStartLocationProvider(): map is loaded.");
									if(startGPS == true){
										setLocationListenerGPSProvider();
									}
									else{
										setLocationListenerNetworkProvider();
									}
								}
								else{
									if(counter < 5){
										handler.postDelayed(this, 5000);
									}
									else{
										Log.d("GPSTester","delayedStartLocationProvider(): Unable to start Location Listener.");
										_map.displayToast("Map not loading? Try restarting app.", Toast.LENGTH_LONG);
									}
								}
							}
							catch(Exception exc){
								Log.d("GPSTester","delayedStartLocationProvider() exception: " + exc.toString());
							}
							
						}
					}, 250);					
				}
				catch(Exception exc){
					Log.d("GPSTester","centerAndZoomIfMapLoaded() exception: " + exc.toString());
				}
			}
		};
		
		task.run();
	}	
	
	/**
	 * Start and reset everything
	 */
	public void startLocation(){
		
		if(_locationManager == null){

			if(_startButton != null){
				_startButton.setTextColor(Color.RED);
				_startButton.setText("Stop");
			}
			
			_elapsedTimer.startTimer(1,_elapsedTime);
			_initialGPSTime = _elapsedTimer.getElapsedtime();
			_initialNetworkTime = _elapsedTimer.getElapsedtime();
			_locationManager = (LocationManager) _activity.getSystemService(Context.LOCATION_SERVICE);			
			
			//GpsStatus gpsStatus = new GpsStatus();
			//int time = gpsStatus.getTimeToFirstFix();
			Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			Boolean networkProviderEnabled = _locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			
		    //boolean useCriteria = Boolean.valueOf(_preferences.getString("pref_key_useCriteria", "false"));
		    Boolean useCriteria = _preferences.getBoolean("pref_key_useCriteria", false);
		    String bestProviderName = getBestProviderNameViaCriteria();			
			
		    if(useCriteria == true){
		    	_imCriteria.setImageResource(R.drawable.greensphere31);
		    }
		    else{
		    	_imCriteria.setImageResource(R.drawable.redsphere31);
		    }
		    
			setUI();
			setLocationManagerUI(gpsProviderEnabled,networkProviderEnabled);			
			
			if(gpsProviderEnabled == true && _preferences.getBoolean("pref_key_gps", true) == true){
				if(useCriteria == false || (useCriteria == true && bestProviderName.equalsIgnoreCase("gps") == true)){
					//setLocationListenerGPSProvider();
					delayedStartLocationProvider(true);
					_imGPS.setImageResource(R.drawable.greensphere31);						
				}			
			}
			else if(gpsProviderEnabled == true && _preferences.getBoolean("pref_key_gps", true) == false){
				Log.d("GPSTester","GPS Provider not enabled. Unable to set location listener.");
				_imGPS.setImageResource(R.drawable.redsphere31);
			}
			else{
				Log.d("GPSTester","GPS Provider not enabled. Unable to set location listener.");
				_imGPS.setImageResource(R.drawable.redsphere31);
	
				//Inflate alert dialog
				DialogFragment gpsFragment = new GPSAlertDialogFragment();
				gpsFragment.show(_activity.getFragmentManager(), "GPSAlert");
			}
			
			if(networkProviderEnabled == true && _preferences.getBoolean("pref_key_network", true) == true){
				if(useCriteria == false || (useCriteria == true && bestProviderName.equalsIgnoreCase("network") == true)){
					//setLocationListenerNetworkProvider();
					delayedStartLocationProvider(false);
					_imNetwork.setImageResource(R.drawable.greensphere31);				
				}

			}
			else if(networkProviderEnabled == true && _preferences.getBoolean("pref_key_network", true) == false){
				Log.d("GPSTester","Network Provider not enabled. Unable to set location listener.");
				_imNetwork.setImageResource(R.drawable.redsphere31);	
			}
			else{
				Log.d("GPSTester","Network Provider not enabled. Unable to set location listener.");
				_imNetwork.setImageResource(R.drawable.redsphere31);	
				_map.displayToast("No network connection available.", Toast.LENGTH_LONG);
			}				
			
			gpsProviderEnabled = null;
			networkProviderEnabled = null;
		}
	}
	
	public void stopLocation(){

		if(_locationManager != null){
			if(_startButton != null){
				_startButton.setTextColor(Color.WHITE);
				_startButton.setText("Start");
			}			
			
			_elapsedTimer.stopTimer();
			if(_locationListenerGPSProvider != null){
				_locationManager.removeUpdates(_locationListenerGPSProvider);
			}
			if(_locationListenerNetworkProvider != null){
				_locationManager.removeUpdates(_locationListenerNetworkProvider);
			}
			_locationManager = null;
		}
		
		if(_locationListenerGPSProvider != null) {
			_locationListenerGPSProvider = null;
		}
		if(_locationListenerNetworkProvider != null){
			_locationListenerNetworkProvider = null;
		}
		
		_imGPS.setImageResource(R.drawable.redsphere31);	
		_imNetwork.setImageResource(R.drawable.redsphere31);		
		
		_initialLapGPS = false;
	}
	
	public void pauseLocation(){
		String startButtonText = _startButton.getText().toString();
		if(_locationManager != null && startButtonText == "Stop"){
			_pauseButton.setTextColor(Color.RED); 
			_elapsedTimer.pauseTimer();
			if(_locationListenerGPSProvider != null){
				_locationManager.removeUpdates(_locationListenerGPSProvider);
			}
			if(_locationListenerNetworkProvider != null){
				_locationManager.removeUpdates(_locationListenerNetworkProvider);
			}
			_locationManager = null;
			
			if(_locationListenerGPSProvider != null) {
				_locationListenerGPSProvider = null;
			}
			if(_locationListenerNetworkProvider != null){
				_locationListenerNetworkProvider = null;
			}		
			
			_imGPS.setImageResource(R.drawable.redsphere31);	
			_imNetwork.setImageResource(R.drawable.redsphere31);				
		}
		else if(_locationManager == null && startButtonText == "Stop"){
			_pauseButton.setTextColor(Color.WHITE);
			_initialGPSTime = _elapsedTimer.getElapsedtime();
			_initialNetworkTime = _elapsedTimer.getElapsedtime();
			_elapsedTimer.unpauseTimer();
			_locationManager = (LocationManager) _activity.getSystemService(Context.LOCATION_SERVICE);
			
			//GpsStatus gpsStatus = new GpsStatus();
			//int time = gpsStatus.getTimeToFirstFix();
			Boolean gpsProviderEnabled = _locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			Boolean networkProviderEnabled = _locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
							
			if(gpsProviderEnabled == true && _preferences.getBoolean("pref_key_gps", true) == true){
				setLocationListenerGPSProvider();				
				_imGPS.setImageResource(R.drawable.greensphere31);				
			}
			else{
				Log.d("GPSTester","GPS Provider not enabled. Unable to set location listener.");
				_imGPS.setImageResource(R.drawable.redsphere31);				
			}
			if(networkProviderEnabled == true && _preferences.getBoolean("pref_key_network", true) == true){
				setLocationListenerNetworkProvider();	
				_imNetwork.setImageResource(R.drawable.greensphere31);					
			}
			else{
				Log.d("GPSTester","Network Provider not enabled. Unable to set location listener.");
				_imNetwork.setImageResource(R.drawable.redsphere31);					
			}				
			
			gpsProviderEnabled = null;
			networkProviderEnabled = null; 
		}
		
	}
	
	public enum BestAvailableType{
		CACHED_NETWORK,CACHED_GPS,GPS,NETWORK,NULL
	}
	
	private static void setBestAvailableImageView(BestAvailableType type){
	
		switch(type){
		case CACHED_NETWORK:
			_bestAvailableImageView.setImageResource(R.drawable.greendiamond18);
			break;
		case CACHED_GPS:
			_bestAvailableImageView.setImageResource(R.drawable.reddiamond18);
			break;
		case GPS:
			_bestAvailableImageView.setImageResource(R.drawable.redcircle18);
			break;
		case NETWORK:
			_bestAvailableImageView.setImageResource(R.drawable.bluecircle18);
			break;
		case NULL:
			_bestAvailableImageView.setImageResource(R.drawable.blackdiamond18);
			break;
		}
	}	
}
