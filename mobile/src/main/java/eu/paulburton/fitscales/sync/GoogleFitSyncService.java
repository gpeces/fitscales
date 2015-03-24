package eu.paulburton.fitscales.sync;

import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import eu.paulburton.fitscales.FitscalesActivity;
import eu.paulburton.fitscales.FitscalesApplication;
import eu.paulburton.fitscales.Prefs;
import eu.paulburton.fitscales.SyncService;

/**
 * Created by Young-Ho on 2015-03-21.
 */
public class GoogleFitSyncService extends OAuthSyncService {
    private static final String TAG = "GoogleFit";
    private static final String UUID = "0x057e0306"; // vid + pid
    private static final Device BALANCE_BOARD =
            new Device("Nintendo", "Wii Balance Board", UUID, Device.TYPE_SCALE);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat();
    private static final boolean DEBUG = true;

    public GoogleFitSyncService() {
        super("GoogleFit", "googleFit");
    }
    @Override
    public void load() {
        user = FitscalesApplication.inst.prefs.getString(prefName + KEY_USER, null);
        if (!TextUtils.isEmpty(user)) {
            enabled = true;
        }
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
            SharedPreferences.Editor edit = FitscalesApplication.inst.prefs.edit();
            edit.remove(prefName + KEY_USER);
            Prefs.save(edit);
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
    public boolean syncWeight(float weight) {
        synchronized (this) {
            if (mClient == null) return false;
            if (mConnecting) return false;

            Calendar cal = Calendar.getInstance();
            Date now = new Date();
            cal.setTime(now);
            long endTime = cal.getTimeInMillis();
            cal.add(Calendar.MINUTE, -1);
            long startTime = cal.getTimeInMillis();

            DataSource source = new DataSource.Builder()
                    .setAppPackageName(FitscalesApplication.inst)
                    .setDataType(DataType.TYPE_WEIGHT)
                    .setType(DataSource.TYPE_RAW)
                    .setDevice(BALANCE_BOARD)
                    .build();
            DataSet set = DataSet.create(source);
            DataPoint point = set.createDataPoint()
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
            point.getValue(Field.FIELD_WEIGHT).setFloat(weight);
            set.add(point);
            Status status = Fitness.HistoryApi.insertData(mClient, set)
                    .await(1, TimeUnit.MINUTES);
            if (DEBUG) {
                Log.i(TAG, "set = " + set + " point = " + point);
                Log.i(TAG, "insert status = " + status + ": msg: " + status.getStatusMessage());
                printInsertedData();
            }
            return status.isSuccess();
        }
    }

    private void printInsertedData() {
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date());
        long    endTime = cal.getTimeInMillis();
        cal.add(Calendar.DATE, -7);
        long   startTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_WEIGHT, DataType.AGGREGATE_WEIGHT_SUMMARY)
                .bucketByTime(1, TimeUnit.HOURS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .setLimit(500)
                .build();
        DataReadResult dataReadResult =
                Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
        Log.i(TAG, "read result :" + dataReadResult);

        if (dataReadResult.getBuckets().size() > 0) {
            for (Bucket b : dataReadResult.getBuckets()) {
                for (DataSet s : b.getDataSets()) {
                    dumpDataSet(s);
                }
            }
        } else {
            for (DataSet s: dataReadResult.getDataSets()) {
                dumpDataSet(s)    ;
            }
        }
    }

    private void dumpDataSet(DataSet set) {
        Log.i(TAG, "Data returned for Data type: " + set.getDataType().getName());
        for (DataPoint dp : set.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + DATE_FORMAT.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + DATE_FORMAT.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
        }
    }

    private GoogleApiClient mClient;
    private boolean mAuthInProgress = false;
    private boolean mConnecting = false;

    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(FitscalesApplication.activity)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_BODY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                mConnecting = false;
                                enabled = true;
                                user = "user";
                                SharedPreferences.Editor edit = FitscalesApplication.inst.prefs.edit();
                                edit.putString(prefName + KEY_USER, user);
                                Prefs.save(edit);
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
