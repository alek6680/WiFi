package com.als.wifi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.als.wifi.WifiActivity.add;
import com.als.wifi.WifiActivity.isConnected;
import com.als.wifi.WifiActivity.sync;

import android.net.wifi.ScanResult;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;

public class One extends Fragment implements OnClickListener {
	private String version = "-1";
	private String newversion;
	private String fullap;
	private IntentFilter intentFilter;
	private IntentFilter ifil;
	public boolean isWEP = false;
	private int netId;
	private WifiManager mWiFiManager;
	private BroadcastReceiver broadcastReceiver;
	private BroadcastReceiver br;
	private boolean receiverRegistered = false;
	private ArrayList<String> savedAP = new ArrayList<String>();
	private ArrayList<String> matchingAP = new ArrayList<String>();
	private ArrayList<String> he = new ArrayList<String>();
	private File path;
	private ListView lv;
	private ArrayAdapter<String> adapter;
	private ArrayAdapter<String> adapter1;
	WifiActivity wi_fi;
	private ArrayList<String> AP = new ArrayList<String>();
	public boolean isConnectedOrFailed = false;
	private View layout;
	private Button scan;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		 if(container == null){
	            return null;                                
	        }
		layout = inflater.inflate(R.layout.one, null);
		return layout;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		mWiFiManager = (WifiManager) getActivity().getSystemService(
				Context.WIFI_SERVICE);
		path = new File(Environment.getExternalStorageDirectory(),
				getActivity().getApplicationContext().getPackageName());
		if (!path.exists()) {
			path.mkdir();
		}
		File versionfile = new File(path, "version");
		if (!versionfile.exists()) {

			try {
				versionfile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		File collectionfile = new File(path, "collection");
		if (!collectionfile.exists()) {
			try {
				collectionfile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Check if there are exceptions saved in a local file, and send/delete
		 * them
		 */
		// checkBugs();

		/**
		 * Get the version of the list
		 */
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					versionfile));
			String sResponse;
			StringBuilder s = new StringBuilder();
			while ((sResponse = bufferedReader.readLine()) != null) {
				s = s.append(sResponse);
			}
			Log.d("xxx", s.toString());
			if (s.toString().length() > 0)
				version = s.toString();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		if (mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
			// togglebutton.setChecked(true);
			mWiFiManager.setWifiEnabled(true);
		} else if (!mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING)
			mWiFiManager.setWifiEnabled(false);
		// togglebutton.setChecked(false);

		// getAvailableAPs();
		// getSavedAPs();
		super.onCreate(savedInstanceState);

	}

	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// View v = inflater.inflate(R.layout.one, container, false);
	// // lv = (ListView) v.findViewById(R.id.androidll);
	//
	// he.add("eden");
	// he.add("dva");
	// String[] values = new String[] { "Enterprise", "Star Trek",
	// "Next Generation", "Deep Space 9", "Voyager" };
	// // adapter1 = new ArrayAdapter<String>(v.getContext(),
	// // android.R.layout.simple_list_item_1, he);
	// // lv.setAdapter(adapter1);
	// return v;
	// }

	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// layout = inflater.inflate(R.layout.one, null);
	// return layout;
	//
	// }

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		scan = (Button) layout.findViewById(R.id.buttonA);
		lv = (ListView) layout.findViewById(R.id.listda);
		// getAvailableAPs();
		broadcastReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String reason = intent
						.getStringExtra(ConnectivityManager.EXTRA_REASON);
				NetworkInfo currentNetworkInfo = (NetworkInfo) intent
						.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

				String state = currentNetworkInfo.getDetailedState().name();
				Log.d("xxxX", "reason: " + reason);

				// CONNECTED or DISCONNECTED
				Log.d("xxxX", "state: " + state);
				if (currentNetworkInfo.getTypeName().equalsIgnoreCase("WIFI")) {
					if (currentNetworkInfo.isConnected()) {
						Toast.makeText(getActivity(), "WIFI Connected...",
								Toast.LENGTH_SHORT).show();
						// If the phone has successfully connected to the AP,
						// save it!
						mWiFiManager.saveConfiguration();
						isConnectedOrFailed = true;
						getActivity().unregisterReceiver(broadcastReceiver);
						getActivity().unregisterReceiver(br);
						receiverRegistered = false;
					} else if (reason != null)
						Toast.makeText(getActivity(), reason,
								Toast.LENGTH_SHORT).show();
					else if (state.equalsIgnoreCase("DISCONNECTED")) {
						// SupplicantState s =
						// mWiFiManager.getConnectionInfo().getSupplicantState();
						// NetworkInfo.DetailedState supstate =
						// WifiInfo.getDetailedStateOf(s);
						Toast.makeText(getActivity(), "WIFI Disconnected!",
								Toast.LENGTH_SHORT).show();
						mWiFiManager.removeNetwork(netId);
						isConnectedOrFailed = true;
						getActivity().unregisterReceiver(broadcastReceiver);
						getActivity().unregisterReceiver(br);
						receiverRegistered = false;

					}
				}
				Log.d("xxx",
						reason + " *** " + currentNetworkInfo.getExtraInfo());
			}
		};

		intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		intentFilter.addAction(ConnectivityManager.EXTRA_REASON);

		/**
		 * Second broadcast receiver that listens for supplicant changes and
		 * detects if there was an error in the attempt to connect to a wifi
		 * access point. Also deletes the network from current configured
		 * networks if there was an error
		 */
		ifil = new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		br = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				boolean error = false;
				if (intent.getAction().equals(
						WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
					error = intent.hasExtra(WifiManager.EXTRA_SUPPLICANT_ERROR);

				}
				if (error) {
					Log.d("xxxX",
							"Imaaaaa:  "
									+ intent.getStringExtra(WifiManager.EXTRA_SUPPLICANT_ERROR));
					Log.d("xxxX", "Error: ");
					Toast.makeText(getActivity(),
							"WIFI Disconnected! The password may be incorrect",
							Toast.LENGTH_SHORT).show();
					mWiFiManager.removeNetwork(netId);
					isConnectedOrFailed = true;
					getActivity().unregisterReceiver(broadcastReceiver);
					getActivity().unregisterReceiver(br);
					receiverRegistered = false;
				}
			}
		};

		// getSavedAPs();
		// click();
		scan.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				if (mWiFiManager.isWifiEnabled()) {
					// getAllAPs();
					getAvailableAPs();
					getSavedAPs();
					ArrayList<String> he = new ArrayList<String>();
					he.add("eden");
					he.add("dva");
					ArrayList<String> ha = new ArrayList<String>();

					ha = matchingAP;
					// String[] values = new String[] { "Enterprise",
					// "Star Trek",
					// "Next Generation", "Deep Space 9", "Voyager" };
					// adapter1 = new ArrayAdapter<String>(getActivity(),
					// android.R.layout.simple_list_item_1, ha);
					// setListAdapter(adapter1);

					adapter = new ArrayAdapter<String>(getActivity(),
							R.layout.custom_layout, R.id.textView1, ha);
					lv.setAdapter(adapter);
					lv.setTextFilterEnabled(true);
					adapter.notifyDataSetChanged();
					lv.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View arg1,
								int arg2, long arg3) {
							String connection = arg0.getAdapter().getItem(arg2)
									.toString();
							// connectTo(connection);
							connectTo(connection);
							// TODO Auto-generated method stub

						}
					});
					// Intent in = new Intent(getApplicationContext(),
					// One.class);
					//
					// in.putStringArrayListExtra("name", matchingAP);
					// startActivity(in);
					// One newFragment = new One();
					// Bundle args = new Bundle();
					// args.putInt(ArticleFragment.ARG_POSITION, position);
					// newFragment.setArguments(args);
					// FragmentTransaction transaction =
					// getSupportFragmentManager()
					// .beginTransaction();

					// Replace whatever is in the fragment_container view
					// with
					// this
					// fragment,
					// and add the transaction to the back stack so the user
					// can
					// navigate back
					// transaction.replace(R.id.fragment_container,
					// newFragment);
					// transaction.addToBackStack(null);

					// Commit the transaction
					// transaction.commit();

				} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
					Toast.makeText(getActivity(),
							"Please wait a bit until your WiFi is enabled!",
							Toast.LENGTH_SHORT).show();
				} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
						|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
					Toast.makeText(getActivity(), "Please enable Your WiFi!",
							Toast.LENGTH_SHORT).show();
				}
			}

		});

		// ArrayList<String> he = new ArrayList<String>();
		// he.add("eden");
		// he.add("dva");
		// ArrayList<String> ha = new ArrayList<String>();
		//
		// ha = wi_fi.matchingAP;
		// // String[] values = new String[] { "Enterprise", "Star Trek",
		// // "Next Generation", "Deep Space 9", "Voyager" };
		// // adapter1 = new ArrayAdapter<String>(getActivity(),
		// // android.R.layout.simple_list_item_1, ha);
		// // setListAdapter(adapter1);
		//
		// adapter = new ArrayAdapter<String>(getActivity(),
		// R.layout.custom_layout, R.id.textView1, ha);
		// lv.setAdapter(adapter);
		// lv.setTextFilterEnabled(true);
		// adapter.notifyDataSetChanged();
		// setAdapter(adapter);

	}

	public void connectTo(String AP) {

		boolean exists = false;
		String bssid = AP.substring(AP.indexOf("BSSID: ") + 7,
				AP.indexOf("\nPassword"));
		String psk = AP.substring(AP.indexOf("Password: ") + 10,
				AP.indexOf("\nWEP"));
		String ssid = AP.substring(AP.indexOf("AP name: ") + 9,
				AP.indexOf("\nBSSID"));
		if (AP.contains("WEP:true"))
			isWEP = true;
		else
			isWEP = false;

		// List available networks
		List<WifiConfiguration> configs = mWiFiManager.getConfiguredNetworks();
		// Toast.makeText(getActivity(), "aha", Toast.LENGTH_LONG).show();
		for (WifiConfiguration config : configs) {

			Log.d("xxx", config.SSID + " ?= " + ssid);
			if (config.SSID.equalsIgnoreCase("\"" + ssid + "\"")) {
				exists = true;
			}
		}
		Log.d("xxx", "bssid: " + bssid + " psk: " + psk + "*");

		if (!exists) {

			WifiConfiguration wifiConfig = new WifiConfiguration();
			wifiConfig.SSID = "\"" + ssid + "\"";
			wifiConfig.BSSID = bssid;
			if (isWEP) {
				wifiConfig.wepKeys[0] = "\"" + psk + "\"";
			} else
				wifiConfig.preSharedKey = "\"" + psk + "\"";
			wifiConfig.status = WifiConfiguration.Status.ENABLED;

			mWiFiManager.setWifiEnabled(true);
			netId = mWiFiManager.addNetwork(wifiConfig);
			mWiFiManager.enableNetwork(netId, true);

			getActivity().registerReceiver(broadcastReceiver, intentFilter);
			getActivity().registerReceiver(br, ifil);
			receiverRegistered = true;

			new isConnected().execute();

		} else
			Toast.makeText(getActivity(), "Network is already configured!",
					Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show a progress dialog, while the status of the wifi is either CONNECTED,
	 * DISCONNECTED or there was an error in the process
	 * 
	 * @author drakuwa
	 * 
	 */
	public class isConnected extends AsyncTask<String, Void, String> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(getActivity());
			dialog.setTitle("Connecting!");
			dialog.setMessage("Please wait..");
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			dialog.show();
		}

		protected String doInBackground(String... vlezni) {
			while (true) {
				if (isConnectedOrFailed) {
					isConnectedOrFailed = false;
					break;
				}
			}
			return "";
		}

		public void onPostExecute(String result) {
			// Remove the progress dialog.
			dialog.dismiss();
		}
	}

	public void getAllAPs() {
		savedAP.clear();
		matchingAP.clear();
		File collectionfile = new File(path, "collection");
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(collectionfile));
			String sResponse;
			while ((sResponse = bufferedReader.readLine()) != null) {
				savedAP.add(sResponse);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < savedAP.size(); i++) {
			String localsavedAP = savedAP.get(i);
			if (localsavedAP.contains("*")) {
				String bssid = localsavedAP
						.substring(localsavedAP.indexOf("*") + 1);
				StringBuilder sb = new StringBuilder();
				sb.append("AP name: "
						+ localsavedAP.substring(0, localsavedAP.indexOf(";")));
				sb.append("\n");
				sb.append("BSSID: " + bssid);
				sb.append("\n");
				sb.append("Password: "
						+ localsavedAP.substring(localsavedAP.indexOf(";") + 1,
								localsavedAP.indexOf("*")));
				matchingAP.add(sb.toString());
			}
		}
		// lv = (ListView) findViewById(R.id.accesspointslist);

		if (matchingAP.isEmpty()) {
			Toast.makeText(getActivity(), "No APs!", Toast.LENGTH_SHORT).show();
		}
		// adapter = new ArrayAdapter<String>(getApplicationContext(),
		// android.R.layout.simple_list_item_1, matchingAP);
		// lv.setAdapter(adapter);
		// lv.setTextFilterEnabled(true);
		// adapter.notifyDataSetChanged();
		//
		// lv.setOnItemClickListener(new OnItemClickListener() {
		//
		// public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
		// long arg3) {
		// Toast.makeText(
		// getApplicationContext(),
		// "First scan for matching APs, then if found, you can connect!",
		// Toast.LENGTH_SHORT).show();
		// }
		// });
	}

	/**
	 * Get the currently available APs from the scan, and add them to an array
	 * list
	 */
	public void getAvailableAPs() {

		AP.clear();
		mWiFiManager.startScan();
		List<ScanResult> mScanResults = mWiFiManager.getScanResults();
		if (mScanResults != null)
			for (ScanResult sr : mScanResults) {
				Log.d("xxx", "Scan results: " + sr.toString());
				StringBuilder sb = new StringBuilder();
				sb.append("AP NAME: " + sr.SSID);
				sb.append("\n");
				sb.append("BSSID: " + sr.BSSID);
				if (sr.capabilities.contains("WEP")) {
					sb.append("\n");
					sb.append("WEP");
				}
				// sb.append("\n");
				// sb.append("SIGNAL: " + sr.level);
				AP.add(sb.toString());
			}
	}

	/**
	 * Get the saved APs from the application and check for matching APs. The
	 * matching is done via comparing the bssid-s of the scan result APs and
	 * saved APs. If there is a matching AP, add it to the list, and set on item
	 * click listener that tries to connect to the clicked AP
	 */
	public void getSavedAPs() {

		savedAP.clear();
		matchingAP.clear();
		File collectionfile = new File(path, "collection");
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(collectionfile));
			String sResponse;
			while ((sResponse = bufferedReader.readLine()) != null) {
				savedAP.add(sResponse);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < AP.size(); i++) {
			for (int j = 0; j < savedAP.size(); j++) {
				String currentAP = AP.get(i);
				String bssid = "";
				String localbssid = "";
				if (currentAP.contains("BSSID:")) {
					bssid = currentAP.substring(
							currentAP.indexOf("BSSID:") + 7,
							currentAP.indexOf("BSSID:") + 24);
					if (currentAP.contains("WEP"))
						isWEP = true;
					else
						isWEP = false;
					Log.d("xxx", "YES! it contains -> " + bssid);
				} else
					continue;
				String localsavedAP = savedAP.get(j);
				Log.d("xxx", "local saved ap: " + localsavedAP);
				if (localsavedAP.contains("*")) {
					localbssid = localsavedAP.substring(localsavedAP
							.lastIndexOf("*") + 1);
					Log.d("xxx", "YES again! it contains -> " + localbssid);
				} else
					continue;
				Log.d("xxx", bssid + "?=" + localbssid);

				if (bssid.equalsIgnoreCase(localbssid)) {
					StringBuilder sb = new StringBuilder();
					sb.append("AP name: "
							+ localsavedAP.substring(0,
									localsavedAP.indexOf(";")));
					sb.append("\n");
					sb.append("BSSID: " + bssid);
					sb.append("\n");
					sb.append("Password: "
							+ localsavedAP.substring(
									localsavedAP.indexOf(";") + 1,
									localsavedAP.lastIndexOf("*")));
					if (isWEP)
						sb.append("\nWEP:true");
					else
						sb.append("\nWEP:false");
					matchingAP.add(sb.toString());
					Log.d("xxx", "match in: " + bssid + "=" + localbssid);
				}
			}
		}
		Log.d("xxx", "Ne stiga do ovde?");
	}

	@Override
	public void onClick(View v) {

	}

}