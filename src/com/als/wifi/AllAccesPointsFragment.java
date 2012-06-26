package com.als.wifi;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class AllAccesPointsFragment extends ListFragment {
	private MainFragmentActivity mWiFi;
	private ArrayAdapter<String> mShowAllAPsAdapter;
	public static ArrayList<String> mMatchingAP = new ArrayList<String>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);

		ArrayList<String> mAllAccessPoints = new ArrayList<String>();
		mAllAccessPoints = mWiFi.mMatchingAP;

		mShowAllAPsAdapter = new ArrayAdapter<String>(getActivity(),
				R.layout.custom_layout, R.id.textViewAP, mAllAccessPoints);
		setListAdapter(mShowAllAPsAdapter);

	}

	public void onListItemClick(ListView arg0, View arg1, int arg2, long arg3) {

		Toast.makeText(getActivity(),
				"First scan for matching APs, if exists, you can connect!",
				Toast.LENGTH_SHORT).show();

	}
}
