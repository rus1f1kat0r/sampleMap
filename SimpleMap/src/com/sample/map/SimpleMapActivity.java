package com.sample.map;

import com.sample.map.tile.DefaultTileProvider;
import com.sample.map.view.MapView;

import android.app.Activity;
import android.os.Bundle;

public class SimpleMapActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        MapView map = (MapView) findViewById(R.id.mapView1);
        map.setTileProvider(new DefaultTileProvider(this));
    }
}