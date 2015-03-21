package eu.paulburton.fitscales.sync;

import android.content.IntentSender;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;

import java.util.concurrent.TimeUnit;

import eu.paulburton.fitscales.FitscalesActivity;
import eu.paulburton.fitscales.FitscalesApplication;
import eu.paulburton.fitscales.SyncService;

/**
 * Created by Young-Ho on 2015-03-21.
 */
public class GoogleFitSyncService extends OAuthSyncService {
    private static final String TAG = "GoogleFit";

    public GoogleFitSyncService() {
        super("GoogleFit", "googleFit");
    }
    @Override
    public void load() {

    }

    @Override
    public void connect()
    {
        synchronized (this) {
            Log.d(TAG, "connect");
            if (mClient == null) {
                mConnecting = true;
                buildFitnessClient();
                mClient.connect();
            }
        }
    }

    @Override
    public void reconnect() {
        if (mClient != null) {
            mAuthInProgress = false;
            if (!mClient.isConnecting() && !mClient.isConnected()) {
                mClient.connect();
            }
        }
    }

    @Override
    public void disconnect()
    {
        synchronized (this) {
            if (mClient != null) {
                mClient.disconnect();
                mClient = null;
            }
            mConnecting = false;
            enabled = false;
        }
    }

    @Override
    public boolean isConnecting()
    {
        synchronized (this) {
            return mConnecting;
        }
    }

    @Override
    public void setOAuthPageResult(String url) {

    }

    @Override
    public boolean syncWeight(float weight)
    {
        synchronized (this) {
            if (mClient == null) return false;
            if (mConnecting) return false;

            DataSource source = new DataSource.Builder()
                    .setAppPackageName(FitscalesApplication.inst)
                    .setDataType(DataType.TYPE_WEIGHT)
                    .setType(DataSource.TYPE_RAW)
                    .build();
            DataSet set = DataSet.create(source);
            set.add(set.createDataPoint()
                    .setFloatValues(weight)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS));
            return Fitness.HistoryApi.insertData(mClient, set).await(1, TimeUnit.MINUTES)
                .isSuccess();
        }
    }

    private GoogleApiClient mClient;
    private boolean mAuthInProgress = false;
    private boolean mConnecting = false;

    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(FitscalesApplication.activity)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                mConnecting = false;
                                enabled = true;
                                user = "user";
                                listener.oauthDone(GoogleFitSyncService.this);
                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.i(TAG, "Connection lost.  Cause: " + i);
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                                mConnecting = false;
                                mAuthInProgress = false;
                                enabled = false;
                                listener.oauthDone(GoogleFitSyncService.this);
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                enabled = false;
                                if (!result.hasResolution()) {
                                    mAuthInProgress = false;
                                    mConnecting = false;
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            FitscalesApplication.activity, 0).show();
                                    listener.oauthDone(GoogleFitSyncService.this);
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!mAuthInProgress) {
                                    try {
                                        Log.i(TAG, "Attempting to resolve failed connection");
                                        mAuthInProgress = true;
                                        result.startResolutionForResult(FitscalesApplication.activity,
                                                FitscalesActivity.REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG,
                                                "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();

    }
}
