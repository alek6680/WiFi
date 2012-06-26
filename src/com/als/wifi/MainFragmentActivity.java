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

import android.R.anim;
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
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.R.menu;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MainFragmentActivity extends SherlockFragmentActivity implements
		OnClickListener {
	ActionBar actionBar;
	private WifiManager mWiFiManager;
	private ArrayList<String> mAccessPoints = new ArrayList<String>();
	private ArrayList<String> mSavedAP = new ArrayList<String>();
	public static ArrayList<String> mMatchingAP = new ArrayList<String>();
	private File mPath;
	public boolean isWEP = false;
	public static BroadcastReceiver broadcastReceiver;
	public static BroadcastReceiver br;
	private boolean receiverRegistered = false;
	private int netId;
	public boolean isConnectedOrFailed = false;
	private IntentFilter intentFilter;
	private IntentFilter ifil;
	private String version = "-1";
	private String newversion;
	private String fullap;
	FragmentManager fragMan;
	private TextView mTextView = null;

	ConnectionChangeReceiver mConnectionReceiver;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.icon);

		MatchingAccesPointsFragment newFragment = new MatchingAccesPointsFragment();
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		transaction.setCustomAnimations(R.anim.anim, R.anim.anim_right);
		transaction.add(R.id.fragment_container, newFragment);
		transaction.commit();

		/**
		 * Register Uncaught Exception Handler that saves the Force Close
		 * exceptions in a local file
		 */
		Thread.setDefaultUncaughtExceptionHandler(new FCExceptionHandler(this));

		/**
		 * Get the path to the app folder on the sd card and check if the needed
		 * two files (the version number, and the list of APs) exist
		 */
		mPath = new File(Environment.getExternalStorageDirectory(),
				this.getPackageName());
		if (!mPath.exists()) {
			mPath.mkdir();
		}
		File versionfile = new File(mPath, "version");
		if (!versionfile.exists()) {

			try {
				versionfile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		File collectionfile = new File(mPath, "collection");
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
		checkBugs();

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

		// Initialize the WifiManager
		mWiFiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		// WifiInfo w = mWiFiManager.getConnectionInfo();

		/**
		 * First broadcast receiver that listens for changes in the connectivity
		 * manager. If the network is connected, add the AP to the configured
		 * networks list, or else, remove the newly added network because it
		 * can't connect, and unregister the receivers
		 */
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
						Toast.makeText(getApplicationContext(),
								"WIFI Connected...", Toast.LENGTH_SHORT).show();
						// If the phone has successfully connected to the
						// AP,
						// save it!
						mWiFiManager.saveConfiguration();
						isConnectedOrFailed = true;
						unregisterReceiver(broadcastReceiver);
						unregisterReceiver(br);
						receiverRegistered = false;
					} else if (reason != null)
						Toast.makeText(getApplicationContext(), reason,
								Toast.LENGTH_SHORT).show();
					else if (state.equalsIgnoreCase("DISCONNECTED")) {

						Toast.makeText(getApplicationContext(),
								"WIFI Disconnected!", Toast.LENGTH_SHORT)
								.show();
						mWiFiManager.removeNetwork(netId);
						isConnectedOrFailed = true;
						unregisterReceiver(broadcastReceiver);
						unregisterReceiver(br);
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
					Toast.makeText(getApplicationContext(),
							"WIFI Disconnected! The password may be incorrect",
							Toast.LENGTH_SHORT).show();
					mWiFiManager.removeNetwork(netId);
					isConnectedOrFailed = true;
					unregisterReceiver(broadcastReceiver);
					unregisterReceiver(br);
					receiverRegistered = false;
				}
			}
		};
	}

	/**
	 * Initialize and set the on click listener to the scan button that will be
	 * used for scanning for matching APs (comparing the list from the
	 * application and currently available networks from the AP scan)
	 */

	@Override
	protected void onResume() {
		mConnectionReceiver = new ConnectionChangeReceiver();
		this.registerReceiver(mConnectionReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
		super.onResume();
	}

	/**
	 * A function that checks the existence of stack.trace file and calls a
	 * function alert for sending/deleting it
	 */
	private void checkBugs() {

		File file = new File("/data/data/com.app.wifipass/files/stack.trace");
		if (file.exists()) {

			String line = "";
			String trace = "";
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								MainFragmentActivity.this
										.openFileInput("stack.trace")));
				while ((line = reader.readLine()) != null) {
					trace += line + "\n";
				}
			} catch (FileNotFoundException fnfe) {
				// ...
			} catch (IOException ioe) {
				// ...
			}

			syncExceptionsAlert(trace);
		}
	}

	/**
	 * A function that shows an AlertDialog for deleting/sending the stack.trace
	 * file to the developers email
	 * 
	 * @param trace
	 */
	public void syncExceptionsAlert(final String trace) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"There are unsent bugs, report them via email to the developer, or delete them? ")
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.app_name)
				.setCancelable(true)
				.setPositiveButton("Report",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								Intent sendIntent = new Intent(
										Intent.ACTION_SEND);
								String subject = "Bug report";
								String body = "Mail bugs to drakuwa@gmail.com: "
										+ "\n\n" + trace + "\n\n";

								sendIntent.putExtra(Intent.EXTRA_EMAIL,
										new String[] { "drakuwa@gmail.com" });
								sendIntent.putExtra(Intent.EXTRA_TEXT, body);
								sendIntent.putExtra(Intent.EXTRA_SUBJECT,
										subject);
								sendIntent.setType("message/rfc822");

								MainFragmentActivity.this.startActivity(Intent
										.createChooser(sendIntent, "Title:"));

								MainFragmentActivity.this
										.deleteFile("stack.trace");
							}
						});
		builder.setNegativeButton("Delete",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						File file = new File(
								"/data/data/com.app.wifipass/files/stack.trace");
						if (file.exists()) {
							MainFragmentActivity.this.deleteFile("stack.trace");
						}
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
	}

	public boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_items, menu);
		return true;
		// super.onCreateOptionsMenu(menu);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case android.R.id.home:

			Intent intent = new Intent(this, MainFragmentActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);

			return true;

		case R.id.Sync:
			if (HaveNetworkConnection()) {
				new sync().execute();

			} else {
				createInternetDisabledAlert();
			}
			return true;
		case R.id.AddNew:

			if (HaveNetworkConnection()) {
				addAP();
			} else {
				createInternetDisabledAlert();
			}
			return true;
		case R.id.ShowAll:

			AllAccesPointsFragment newFragment = new AllAccesPointsFragment();

			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			transaction.setCustomAnimations(R.anim.anim, R.anim.anim_right);
			transaction.replace(R.id.fragment_container, newFragment);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			transaction.addToBackStack(null);

			// Commit the transaction
			transaction.commit();
			getAllAPs();
			return true;
		case R.id.OnOff:
			if (mWiFiManager.isWifiEnabled()
					|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
				mWiFiManager.setWifiEnabled(false);
				// MenuItem swbutt = ((Menu) item).findItem(R.id.OnOff);
				// swbutt.setIcon(R.drawable.ic_launcher);
			} else if (!mWiFiManager.isWifiEnabled()
					|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING) {
				// MenuItem swbutt = ((Menu) item).findItem(R.id.OnOff);
				// swbutt.setIcon(R.drawable.abs__ic_cab_done_holo_dark);
				// togglebutton.setChecked(false);
				mWiFiManager.setWifiEnabled(true);
			}
		default:
		}
		return super.onOptionsItemSelected(item);

	}

	@Override
	protected void onStart() {

		super.onStart();

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem switchButton = menu.findItem(R.id.OnOff);

		if (HaveNetworkConnection()
				|| mWiFiManager.isWifiEnabled()
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
			switchButton.setIcon(R.drawable.btn_toggle_on);
			switchButton.setTitle("On");
			switchButton.setChecked(true);
		} else if (mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING
				|| mWiFiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
			switchButton.setIcon(R.drawable.btn_toggle_off);
			switchButton.setTitle("Off");
			switchButton.setChecked(false);
		}
		return super.onPrepareOptionsMenu(menu);

	}

	/**
	 * Show all saved APs in a list.
	 */
	public void getAllAPs() {
		mSavedAP.clear();
		mMatchingAP.clear();
		File collectionfile = new File(mPath, "collection");
		BufferedReader bufferedReader;
		try {
			bufferedReader = new BufferedReader(new FileReader(collectionfile));
			String sResponse;
			while ((sResponse = bufferedReader.readLine()) != null) {
				mSavedAP.add(sResponse);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < mSavedAP.size(); i++) {
			String localsavedAP = mSavedAP.get(i);
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
				mMatchingAP.add(sb.toString());
			}
		}

		if (mMatchingAP.isEmpty()) {
			Toast.makeText(getApplicationContext(), "No APs!",
					Toast.LENGTH_SHORT).show();
		}

	}

	/**
	 * An asynchronous task class that connects to the web service and retrieves
	 * all of the saved APs in the online list, while showing a progress dialog
	 * in the UI thread
	 * 
	 * @author drakuwa
	 * 
	 */
	public class sync extends AsyncTask<String, Void, String> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(MainFragmentActivity.this);
			dialog.setTitle("Syncing!");
			dialog.setMessage("Please wait..");
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			dialog.show();
		}

		protected String doInBackground(String... vlezni) {
			String result = "";
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(
						"http://drakuwa.admin.mk/test.php");

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("command", "get"));
				nameValuePairs.add(new BasicNameValuePair("content", version));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse response = httpclient.execute(httppost);

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent(), "UTF-8"));

				String sResponse;
				StringBuilder s = new StringBuilder();

				sResponse = reader.readLine();
				Log.d("xxx", "First row: " + sResponse);
				if (sResponse.substring(0, 8).equalsIgnoreCase("version:"))
					newversion = sResponse.substring(sResponse
							.indexOf("version: ") + 9);
				else if (sResponse.equalsIgnoreCase("same version"))
					return sResponse;
				while ((sResponse = reader.readLine()) != null) {
					s = s.append(sResponse + "\n");
				}
				Log.d("xxx", s.toString());
				result = s.toString();

			} catch (Exception e) {
				// handle exception here
				e.printStackTrace();
			}
			return result;
		}

		/**
		 * What to do after the calculations are finished.
		 */
		public void onPostExecute(String result) {
			// Remove the progress dialog.
			dialog.dismiss();
			if (result.equalsIgnoreCase("same version")) {
				Toast.makeText(getApplicationContext(), "Already up to date!",
						Toast.LENGTH_SHORT).show();
			} else if (result.startsWith("AP list:")) {
				try {
					File version = new File(mPath, "version");
					FileWriter fWriter = new FileWriter(version);
					fWriter.write(newversion);
					fWriter.flush();
					fWriter.close();

					File collection = new File(mPath, "collection");
					FileWriter fWriter2 = new FileWriter(collection);
					fWriter2.write(result);
					fWriter2.flush();
					fWriter2.close();
					Toast.makeText(getApplicationContext(),
							"Updated successfully!", Toast.LENGTH_SHORT).show();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				Log.d("xxx", "New version: " + newversion + " old: " + version
						+ "rezultat: " + result);
				version = newversion;
			} else
				Toast.makeText(getApplicationContext(),
						"Connection error, try again!", Toast.LENGTH_SHORT)
						.show();
		}
	}

	/**
	 * An asynchronous task class that connects to the web service and adds an
	 * APs in the online list, while showing a progress dialog in the UI thread
	 * 
	 * @author drakuwa
	 * 
	 */
	public class add extends AsyncTask<String, Void, String> {
		ProgressDialog dialog;

		@Override
		protected void onPreExecute() {
			dialog = new ProgressDialog(MainFragmentActivity.this);
			dialog.setTitle("Syncing!");
			dialog.setMessage("Please wait..");
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			dialog.show();
		}

		protected String doInBackground(String... vlezni) {
			String result = "";
			fullap = vlezni[0];
			try {
				HttpClient httpclient = new DefaultHttpClient();
				HttpPost httppost = new HttpPost(
						"http://drakuwa.admin.mk/test.php");

				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
						2);
				nameValuePairs.add(new BasicNameValuePair("command", "add"));
				nameValuePairs.add(new BasicNameValuePair("content", fullap));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				HttpResponse response = httpclient.execute(httppost);

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(
								response.getEntity().getContent(), "UTF-8"));

				result = reader.readLine();
				Log.d("xxx", "First row: " + result);

			} catch (Exception e) {
				// handle exception here
				e.printStackTrace();
			}
			return result;
		}

		/**
		 * What to do after the calculations are finished.
		 */
		public void onPostExecute(String result) {
			// Remove the progress dialog.
			dialog.dismiss();
			if (result.equalsIgnoreCase("Success!")) {
				try {
					File versionfile = new File(mPath, "version");
					FileWriter fWriter = new FileWriter(versionfile);
					int v = Integer.parseInt(version);
					v++;
					Log.d("xxx",
							v
									+ " ova e novata verzija koja shto ke se zapishe...");
					fWriter.write(v + "");
					fWriter.flush();
					fWriter.close();
					version = v + "";

					File collection = new File(mPath, "collection");
					FileWriter fWriter2 = new FileWriter(collection, true);
					fWriter2.write("\n" + fullap);
					fWriter2.flush();
					fWriter2.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				Toast.makeText(getApplicationContext(),
						"AP successfully updated!", Toast.LENGTH_SHORT).show();
			} else if (result.equalsIgnoreCase("AP already exists!")) {
				Toast.makeText(getApplicationContext(), "AP already exists!",
						Toast.LENGTH_SHORT).show();
			} else
				Toast.makeText(getApplicationContext(),
						"Connection error, try again!", Toast.LENGTH_SHORT)
						.show();
		}
	}

	/**
	 * Create the dialog for adding an AP. The dialog is called with the
	 * currently connected AP name (SSID) and the AP BSSID, which can be
	 * changed, and an additional password field which you need to fill. On
	 * save, the dialog calls the "add" AsyncTask
	 */
	public void addAP() {
		final Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.add_dialog);
		dialog.setTitle("Add AP!");

		WifiInfo w = mWiFiManager.getConnectionInfo();
		final EditText apn = (EditText) dialog.findViewById(R.id.apn);
		apn.setText(w.getSSID());

		final EditText password = (EditText) dialog.findViewById(R.id.password);
		final EditText bssid = (EditText) dialog.findViewById(R.id.location);
		bssid.setText(w.getBSSID());

		Button save = (Button) dialog.findViewById(R.id.save);
		Button cancel = (Button) dialog.findViewById(R.id.cancel);

		save.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				String ap = apn.getText().toString();
				String pass = password.getText().toString();
				String BSSID = bssid.getText().toString();
				if (BSSID
						.matches("[[a-f][0-9]]{2}:[[a-f][0-9]]{2}:[[a-f][0-9]]{2}:[[a-f][0-9]]{2}:[[a-f][0-9]]{2}:[[a-f][0-9]]{2}")) {
					if (ap.length() > 0 && pass.length() > 0
							&& BSSID.length() > 0) {
						new add().execute(ap + ";" + pass + "*" + BSSID);
						dialog.dismiss();
					} else
						Toast.makeText(getApplicationContext(),
								"Please enter AP name, Password and BSSID!",
								Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(getApplicationContext(),
							"BSSID must be in this form xx:xx:xx:xx:xx:xx",
							Toast.LENGTH_SHORT).show();
			}
		});

		cancel.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		dialog.show();
	}

	/**
	 * Check if there is an internet connection (mobile or wifi) established for
	 * syncing and adding a new wifi AP to the web service
	 * 
	 * @return
	 */
	public boolean HaveNetworkConnection() {
		boolean HaveConnectedWifi = false;
		boolean HaveConnectedMobile = false;

		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] netInfo = cm.getAllNetworkInfo();
		for (NetworkInfo ni : netInfo) {
			if (ni.getTypeName().equalsIgnoreCase("WIFI"))
				if (ni.isConnected())
					HaveConnectedWifi = true;
			if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
				if (ni.isConnected())
					HaveConnectedMobile = true;
		}
		return HaveConnectedWifi || HaveConnectedMobile;
	}

	/**
	 * Create an alert dialog that redirects you to the internet options on the
	 * phone, so you can enable an internet connection
	 */
	public void createInternetDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(
				"Your internet connection is disabled! Please enable WiFi, or mobile internet")
				.setIcon(R.drawable.ic_launcher)
				.setTitle(R.string.app_name)
				.setCancelable(false)
				.setPositiveButton("Internet options",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								showNetOptions();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Start the wireless settings activity
	 */
	public void showNetOptions() {
		Intent netOptionsIntent = new Intent(
				android.provider.Settings.ACTION_WIRELESS_SETTINGS);
		this.startActivity(netOptionsIntent);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

	private class ConnectionChangeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);

			NetworkInfo activeNetInfo = connectivityManager
					.getActiveNetworkInfo();
			if (activeNetInfo != null) {

			} else {

			}
		}
	}

}