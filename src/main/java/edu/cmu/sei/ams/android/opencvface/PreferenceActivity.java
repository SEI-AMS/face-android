
package edu.cmu.sei.ams.android.opencvface;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.text.InputType;

public class PreferenceActivity extends android.preference.PreferenceActivity implements OnSharedPreferenceChangeListener, OnPreferenceClickListener
{
	private EditTextPreference ipaddressPref;
	private EditTextPreference portnumberPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);

		ipaddressPref = (EditTextPreference)getPreferenceScreen()
		.findPreference( getString(R.string.pref_ipaddress) );

		portnumberPref = (EditTextPreference)getPreferenceScreen()
		.findPreference( getString( R.string.pref_portnumber));
		portnumberPref.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

		updatePreferneces();

		getPreferenceScreen()
		.getSharedPreferences()
		.registerOnSharedPreferenceChangeListener( this );
	}

	public void updatePreferneces()
	{
		String ipAddress = getPreferenceScreen()
		.getSharedPreferences()
		.getString( getString(R.string.pref_ipaddress), getString(R.string.default_ipaddress));
		ipaddressPref.setSummary(ipAddress);

		String portNumber = getPreferenceScreen()
		.getSharedPreferences()
		.getString(getString(R.string.pref_portnumber), getString(R.string.default_portnumber));
		portnumberPref.setSummary(portNumber);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
	{
		updatePreferneces();	
	}

	@Override
	public boolean onPreferenceClick(Preference preference) 
	{
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		switch( requestCode )
		{
		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
