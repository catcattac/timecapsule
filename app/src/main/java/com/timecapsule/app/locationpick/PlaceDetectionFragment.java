package com.timecapsule.app.locationpick;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.timecapsule.app.R;
import com.timecapsule.app.locationpick.controller.LocationAdapter;
import com.timecapsule.app.locationpick.controller.MediaListener;
import com.timecapsule.app.locationpick.model.NearbyLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by catwong on 3/14/17.
 */

public class PlaceDetectionFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<List<NearbyLocation>> {

    private static final String TAG = PlaceDetectionFragment.class.getSimpleName();
    private static final int PLACES_DETECTION_LOADER = 0;
    private View mRoot;
    private String mediaType;
    private TextView tv_place_name;
    private TextView tv_place_address;
    private RecyclerView recyclerView;
    private List<NearbyLocation> nearbyLocationList;
    private LocationAdapter mLocationAdapter;
    private MediaListener listener;
    private TextView location;


    public PlaceDetectionFragment() {

    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public void setListener(MediaListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(PLACES_DETECTION_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // request a window without the title
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup parent, Bundle savedInstanceState) {
        mRoot = inflater.inflate(R.layout.fragment_place_picker, parent, false);
        location = (TextView) mRoot.findViewById(R.id.tv_nearby_locations);
        location.setVisibility(View.INVISIBLE);
        mRoot.setVisibility(View.GONE);
        return mRoot;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onCreateView: " + mediaType);
        recyclerView = (RecyclerView) mRoot.findViewById(R.id.rv_nearbyLocation);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mLocationAdapter = new LocationAdapter(getActivity(), new ArrayList<NearbyLocation>(), mediaType, listener);
        recyclerView.setAdapter(mLocationAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
    }

    @Override
    public Loader<List<NearbyLocation>> onCreateLoader(int id, Bundle args) {
        Log.d(TAG, "onCreateLoader() called");
        return new LocationLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<NearbyLocation>> loader, List<NearbyLocation> nearbyLocations) {
        Log.d(TAG, "onLoadFinished() called");
        location.setVisibility(View.VISIBLE);
        mRoot.setVisibility(View.VISIBLE);
        mLocationAdapter.addPlaces(nearbyLocations);

    }

    @Override
    public void onLoaderReset(Loader<List<NearbyLocation>> loader) {
        Log.d(TAG, "onLoaderReset() called");
        mLocationAdapter.removeNearByPlaces();
    }

}
