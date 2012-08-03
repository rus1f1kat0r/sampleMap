package com.sample.common;

import java.lang.ref.WeakReference;

import android.os.Binder;

public class LocalBinder<S> extends Binder {

	private final WeakReference<S> service;
	
	public LocalBinder(S service){
		this.service=new WeakReference<S>(service);
	}
	
	public S getService(){
		return service.get();
	}
}
