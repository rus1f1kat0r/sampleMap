package com.sample.common;

import java.util.HashSet;
import java.util.Set;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class ServiceDelegate<S extends Service>{
	
	private final Context mActivity;
	/**
	 * {@link Service} this activity will interact with 
	 */
	private S mService;
	/**
	 * a flag that indicates whether the {@link Service} is bounding now
	 */
	private boolean isBounding;
	/**
	 * a {@link ServiceConnection} implementation to get access to be 
	 * informed when Service was connected
	 * Used as lock for this service delegate state synchronization
	 */
	private final ServiceConnection mServiceConntection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			synchronized (mServiceConntection) {
				mService = null;
				isBounding=false;
			}
			notifyDisconnected();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			synchronized (mServiceConntection) {
				ServiceDelegate.this.mService = ((LocalBinder<S>)service).getService();
				mServiceConntection.notifyAll();
				isBounding=false;
			}
			notifyConnected();
		}
	};	
	/**
	 * Intent that used by default to bind service.
	 */
	private Intent mDefIntent;
	/**
	 * set of listeners about binding and unbinding
	 * in case we use delegate from UI thread
	 */
	private final Set<BoundListener> mListeners;
	
	public ServiceDelegate(Context mActivity) {
		super();
		this.mActivity = mActivity;
		this.mListeners = new HashSet<BoundListener>();
	}
	/**
	 * Binds to the service. You should call it in order to manager service connection life-cycle.
	 * @param intent an {@link Intent} to bind to the service with specified actions and flags
	 * @see #doUnbindService()
	 */
	public final void doBindService (Intent intent){
		synchronized (mServiceConntection) {
			if(mService==null&&!isBounding){				
				/**
				 * bind to service with default intent if not specified
				 */
				mActivity.getApplicationContext().bindService(
						intent == null ? getDefaultIntent() : intent, 
								mServiceConntection, Context.BIND_AUTO_CREATE);
				isBounding=true;
			}
		}
	}
	/**
	 * Unbinds the Service. You should call it in order to manager service connection life-cycle.
	 * @see #doBindService(Intent)
	 */
	public final void doUnbindService (){
		synchronized (mServiceConntection) {
			try{
				if(mService!=null){
					mActivity.getApplicationContext().unbindService(mServiceConntection);
					isBounding = false;
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		notifyDisconnected();
	}
	
	public void release() {
		doUnbindService();
	}

	public static enum BoundMode{
		KEEP_ALIVE,
		UNBIND_AFTER,
		RESTORE_STATE
	}

	/**
	 * @return the mService
	 */
	public S getService() {
		synchronized (mServiceConntection) {
			return mService;			
		}
	}
	
	public S getServiceBlocking(){
		synchronized (mServiceConntection) {
			while(mService == null){
				try {
					mServiceConntection.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
					return null;
				}
			}
			return mService;
		}
	}
	/**
	 * @return the mServiceConntection
	 */
	public ServiceConnection getServiceConntection() {
		return mServiceConntection;
	}
	/**
	 * @return the isBounding
	 */
	public boolean isBounding() {
		synchronized(mServiceConntection){
			return isBounding;			
		}
	}
	/**
	 * @return the mActivity
	 */
	public Context getActivity() {
		return mActivity;
	}
	
	public void setDefaultIntent(Intent intent) {
		this.mDefIntent = intent;
	}
	
	public Intent getDefaultIntent() {
		return mDefIntent;
	}
	
	public void addListener(BoundListener listener){
		mListeners.add(listener);
	}
	
	public void removeListener(BoundListener listener){
		mListeners.remove(listener);
	}
	
	private void notifyConnected(){
		for (BoundListener l : mListeners){
			l.onBound();
		}
	}
	
	private void notifyDisconnected(){
		for (BoundListener l : mListeners){
			l.onUnbound();
		}
	}
	
	public interface BoundListener{
		void onBound();
		void onUnbound();
	}
}
