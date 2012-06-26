package com.als.wifi;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;

public class Two extends ListFragment {
	WifiActivity wi_fi;
	private ArrayAdapter<String> adapter1;
	private ArrayAdapter<String> adapter;

	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// // TODO Auto-generated method stub
	// return inflater.inflate(R.layout.two, container, false);
	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
		ArrayList<String> he = new ArrayList<String>();
		he.add("eden");
		he.add("dva");
		ArrayList<String> ha = new ArrayList<String>();

		ha = wi_fi.matchingAP;
		// String[] values = new String[] { "Enterprise", "Star Trek",
		// "Next Generation", "Deep Space 9", "Voyager" };
//		adapter1 = new ArrayAdapter<String>(getActivity(),
//				android.R.layout.simple_list_item_1, ha);
//		setListAdapter(adapter1);
		
		adapter = new ArrayAdapter<String>(getActivity(),
				R.layout.custom_layout, R.id.textView1, ha);
		setListAdapter(adapter);

	}

	public void onListItemClick(ListView arg0, View arg1, int arg2, long arg3) {
		// String item = (String) getListAdapter().getItem(position);
		// DetailFrag frag = (DetailFrag) getFragmentManager().findFragmentById(
		// R.id.frag_capt);
		// if (frag != null && frag.isInLayout()) {
		// String connection = getListAdapter().getItem(arg2).toString();
		Toast.makeText(getActivity(),"First scan for matching APs, then if found, you can connect!",Toast.LENGTH_SHORT).show();
		// connectTo(connection);
		// frag.setText(getCapt(item));
		// }
	}
}
