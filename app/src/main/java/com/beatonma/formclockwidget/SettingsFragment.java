package com.beatonma.formclockwidget;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

/**
 * Created by Michael on 29/05/2015.
 */
public class SettingsFragment extends Fragment {
	ConfigActivity context;

	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		context = (ConfigActivity) getActivity();
	}

	public static SettingsFragment newInstance() {
		SettingsFragment fragment = new SettingsFragment();
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle saved) {
		View v = inflater.inflate(R.layout.fragment_settings, parent, false);

		ImageButton buttonColor1 = (ImageButton) v.findViewById(R.id.button_color1);
		ImageButton buttonColor2 = (ImageButton) v.findViewById(R.id.button_color2);
		ImageButton buttonColor3 = (ImageButton) v.findViewById(R.id.button_color3);

//		int highlightColor = getResources().getColor(R.color.AccentLight);
		Utils.setBackground(buttonColor1, context.color1);
		Utils.setBackground(buttonColor2, context.color2);
		Utils.setBackground(buttonColor3, context.color3);

		buttonColor1.setOnClickListener(new OnButtonClickedListener(1));
		buttonColor2.setOnClickListener(new OnButtonClickedListener(2));
		buttonColor3.setOnClickListener(new OnButtonClickedListener(3));

		return v;
	}

	private class OnButtonClickedListener implements View.OnClickListener {
		int colorIndex = 0;

		public OnButtonClickedListener(int colorIndex) {
			this.colorIndex = colorIndex;
		}

		@Override
		public void onClick(View v) {
			switch (colorIndex) {
				case 1:
					// TODO implement ColorPickerDialog
					break;
				case 2:

					break;
				case 3:

					break;
			}
		}
	}

}
