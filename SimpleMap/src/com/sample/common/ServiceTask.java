package com.sample.common;

import java.io.IOException;

import com.sample.common.ServiceDelegate.BoundMode;

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.util.Log;

/**
 * An {@link AsyncTask} designed for easy usage inside the {@link ServiceActivity}. 
 * Or in pair with {@link ServiceDelegate}
 * <br></br>
 * You should override {@link #doInBackground(Service, Object...)} method 
 * instead of {@link #doInBackground(Object...)}to perform your actions
 * with the {@link Service}
 * @author Rus1F1Kat0R
 * @see AsyncTask
 */
public abstract class ServiceTask<Params, Progress, Result, S extends Service> extends AsyncTask<Params, Progress, Result>{
	
	private ServiceDelegate<? extends S> mDelegate;
	private BoundMode mode;
	private boolean wasBound=false;
	private Status mStatus;
	private boolean mNetWorkTask;
	
	public ServiceTask(ServiceDelegate<? extends S> delegate) {
		this(delegate, BoundMode.RESTORE_STATE);
	}
	
	public ServiceTask(ServiceDelegate<? extends S> delegate, BoundMode mode) {
		this(delegate, mode, true);
	}
	
	public ServiceTask(ServiceDelegate<? extends S> delegate, BoundMode mode, boolean networkTaks) {
		super();
		this.mDelegate = delegate;
		this.mode = mode;
		this.mNetWorkTask = networkTaks;
	}
	/**
	 * Use onPreExecuteService instead.
	 */
	@Override
	protected final void onPreExecute(){
		synchronized (mDelegate.getServiceConntection()) {
			if (mDelegate.getService() != null|| mDelegate.isBounding())
				wasBound=true;
		}
		mDelegate.doBindService(null);
		onPreExecuteService();
	}
	
	@Override
	protected final Result doInBackground(Params... params) {
		if (!mNetWorkTask || isNetworkAvailable()){
			try {
				synchronized (mDelegate.getServiceConntection()) {
					while (mDelegate.getService() == null){
						mDelegate.getServiceConntection().wait();
					}
				}
				try {
					Result result = doInBackgroundService(mDelegate.getService(), params);
					mStatus = Status.SUCCESS;
					return result;
				} catch (IOException e){
					e.printStackTrace();
					mStatus = Status.CONNECTION_ERROR;
				} catch (Exception e) {
					mStatus = Status.UNKNOWN_ERROR;
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				Log.e(ServiceTask.class.getSimpleName(), "error while doInBacground", e);
			} 
		} else {
			mStatus = Status.NETWORK_UNAVAILABLE;
		}
		return null;
	}
	
	protected boolean isNetworkAvailable(){
		boolean result = false;
		ConnectivityManager cm = (ConnectivityManager) mDelegate.getActivity()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo[] infos = cm.getAllNetworkInfo();
		for (NetworkInfo info : infos){
			if (info.getState() == State.CONNECTED || info.getState() == State.CONNECTING){
				return true;
			}
		}
		return result;
	}
	/**
	 * Use onPostExecuteService instead.
	 */
	@Override
	protected final void onPostExecute(Result r){
		if(mode==BoundMode.UNBIND_AFTER||
				(mode==BoundMode.RESTORE_STATE&&!wasBound))
			mDelegate.doUnbindService();
		switch (mStatus) {
		case SUCCESS:
			onPostExecuteService(r);
			break;
		default:
			onError(mStatus);
			break;
		}
	}
	
	/**
	 * called inside {@link #doInBackground(Object...)} method when service
	 * becomes available for interactions with
	 * @param service - {@link Service} to interact with
	 * @param params all available parameters
	 * @return a Result of performed operation
	 */
	protected abstract Result doInBackgroundService(S service, Params...params) throws Exception;
	
	protected void onPostExecuteService(Result r){
		
	}
	
	protected void onPreExecuteService(){
		
	}
	
	protected void onError(Status status){
		Log.w("ServiceTask", "Status is " + status);
	}
	/**
	 * status of the task execution result.
	 * @author Rus1F1KaT0R
	 */
	public static enum Status {
		/**
		 * task succeeded
		 */
		SUCCESS,
		/**
		 * there are connection errors occurred while execution 
		 */
		CONNECTION_ERROR,
		/**
		 * there are errors while response parsing occurred
		 */
		RESPONSE_PARSE_FAILED,
		/**
		 * Network state is unavailable
		 */
		NETWORK_UNAVAILABLE,
		/**
		 * unknown error occurred while task execution
		 */
		UNKNOWN_ERROR
	}
}