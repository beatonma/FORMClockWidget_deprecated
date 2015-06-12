package com.beatonma.formclockwidget;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 17/03/2015.
 */
public class FileUtils {
	private final static String TAG = "FileUtils";
	Context context;

	public FileUtils(Context context) {
		this.context = context;
	}

	// Read file
	public static List<String> readFile(Context context, String filename) {
		List<String> results = new ArrayList<String>();

		try {
			FileInputStream fis = context.openFileInput(filename);
			InputStreamReader isReader = new InputStreamReader(fis);
			BufferedReader bf = new BufferedReader(isReader);
			String line;
			while ((line = bf.readLine()) != null) {
				try {
					results.add(line);
				}
				catch (Exception e) {
					Log.e(TAG, "Error while reading subs file: " + e.toString());
				}
			}
			isReader.close();
		}
		catch (Exception e) {
			Log.e(TAG, "Error getting subs: " + e.toString());
		}

		Collections.sort(results);

		return results;
	}

	public static void writeFile(Context context, String filename, List<String> list) {
		Iterator<String> iter = list.listIterator();
		Log.d(TAG, "Writing to file: " + filename);

		try {
			FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
			while (iter.hasNext()) {
				String line = iter.next() + "\n";
				fos.write(line.getBytes());
			}
			fos.close();
		}
		catch (Exception e) {
			Log.e(TAG, "Error writing to file: " + e.toString());
		}
	}

	public static String getLineContaining(Context context, String filename, String query) {
		try {
			FileInputStream fis = context.openFileInput(filename);
			InputStreamReader isReader = new InputStreamReader(fis);
			BufferedReader bf = new BufferedReader(isReader);
			String line;
			while ((line = bf.readLine()) != null) {
				if (line.contains(query)) {
					return line;
				}
			}
			isReader.close();
			Log.d(TAG, "Query not found in file.");
		}
		catch (Exception e) {
			Log.e(TAG, "Error getting line from file: " + e.toString());
		}

		return null;
	}

	public static boolean fileExists(Context context, String path) {
		File file = new File(context.getFilesDir(), path);
		return file.exists();
	}
}
