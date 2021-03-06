package com.samknows.measurement.test;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

import android.content.Context;
import android.util.Log;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.storage.ResultsContainer;

public class TestResultsManager {
	
	private static File storage;
	
	public static void setStorage(File storage) {
		TestResultsManager.storage = storage;
	}

	public static void saveResult(Context c, ResultsContainer rc){
		saveResult(c, rc.getJSON().toString());
		
	}
	
	public static void saveResult(Context c, List<String> results) {
		//if there is nothing to save returns immediately
		if(results.size() == 0){
			return;
		}
		DataOutputStream dos = openOutputFile(c);
		if( dos == null){
			SKLogger.e(TestResultsManager.class, "Impossible to save results");
			return;
		}
		try {
			for (String outRes : results) {
				dos.writeBytes(outRes);
				dos.writeBytes("\r\n");
			}
		} catch (IOException ioe) {
			SKLogger.e(TestResultsManager.class, "Error while saving results: " + ioe.getMessage());
		} finally {
			IOUtils.closeQuietly(dos);
		}
	}
	
	//Tries to open output file, in case of failures returns null
	private static DataOutputStream openOutputFile(Context c){
		DataOutputStream ret = null;
		try{
			FileOutputStream os = c.openFileOutput(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME, Context.MODE_APPEND);
			ret = new DataOutputStream(os);
		}catch(FileNotFoundException fnfe){
			SKLogger.e(TestResultsManager.class, SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME +" not found!");
			ret = null;
		}
		return ret;

	}
	
	public static void saveResult(Context c, String result) {

		Log.d("******** saveJSON result... ********", result); // TODO could remove this at some point, if we want - but very useful for debugging!

		DataOutputStream dos = openOutputFile(c);
		if( dos == null){
			SKLogger.e(TestResultsManager.class, "Impossible to save results");
			return;
		}
		try {
			dos.writeBytes(result);
			dos.writeBytes("\r\n");
		} catch (IOException ioe) {
			SKLogger.e(TestResultsManager.class, "Error while saving results: " + ioe.getMessage());
		} finally {
			IOUtils.closeQuietly(dos);
		}
	}

	public static void saveResult(Context c, TestResult[] result) {
		for (TestResult r : result) saveResult(c, r.results);
	}
	
	public static byte[] getJSONDataAsByteArray(Context c) {
		InputStream is = null;
		try {
			is = c.openFileInput(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME);
			return IOUtils.toByteArray(is);
		} catch (Exception e) {
			SKLogger.e(TestResultsManager.class, "no tests result file available");
			return null;
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	public static void clearResults(Context context) {
		context.deleteFile(SKConstants.TEST_RESULTS_TO_SUBMIT_FILE_NAME);
	}
	
	public static void saveSumbitedLogs(Context c, byte[] logs) {
		File logFile = new File(storage, SKConstants.TEST_RESULTS_SUBMITTED_FILE_NAME);
		FileOutputStream is = null;
		if (!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e) {
				SKLogger.e("TestResultsManager", "failed to save submitted logs to file", e);
				return;
			}
		}
		try {
			if (!logFile.exists()) {
				logFile.createNewFile();
			}
			is = new FileOutputStream(logFile, true);
			is.write(logs);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		
		verifyReduceSize(logFile);
	}
	
	public static File getSubmitedLogsFile(Context c) {
		return new File(storage, SKConstants.TEST_RESULTS_SUBMITTED_FILE_NAME);
	}

	private static void verifyReduceSize(File logFile) {
		if (logFile.length() > SKConstants.SUBMITED_LOGS_MAX_SIZE) {
			File temp = new File(logFile.getAbsolutePath() + "_tmp");
			BufferedReader reader = null;
			FileWriter writer = null;
			try {
				reader = new BufferedReader(new FileReader(logFile));
				reader.skip(logFile.length() - SKConstants.SUBMITED_LOGS_MAX_SIZE / 2);
				reader.readLine();
				
				writer = new FileWriter(temp);
				IOUtils.copy(reader, writer);
				writer.close();
				reader.close();
				
				logFile.delete();
				temp.renameTo(logFile);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				IOUtils.closeQuietly(reader);
				IOUtils.closeQuietly(writer);
			}
		}
	}
	

	
	public static String[] getJSONDataAsStringArray(Context context) {
		byte[] data = getJSONDataAsByteArray(context);
		if(data == null){
			return new String[] {};
		}
		String results = new String(data);
		return results.split("\r\n");
	}
}	
