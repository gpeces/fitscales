package eu.paulburton.fitscales;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import eu.paulburton.fitscales.sync.OAuthSyncService;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;

public class FitscalesActivity extends AppCompatActivity implements BoardFragment.Listener, SettingsFragment.Listener, WeighInDialogFragment.Listener
{
    private ScaleView scale;
    private View viewSettings;
    private BoardFragment fragBoard;

    public static final int REQUEST_OAUTH = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        FitscalesApplication.activity = this;
        setContentView(R.layout.activity);

        scale = (ScaleView)findViewById(R.id.scale);
        viewSettings = findViewById(R.id.viewSettings);

        fragBoard = (BoardFragment)getSupportFragmentManager().findFragmentById(R.id.fragBoard);

        getSupportActionBar().setHomeButtonEnabled(true);

        viewSettings.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v)
            {
                viewSettings.setVisibility(View.GONE);
                fragBoard.setMenuIcons(false);
            }
        });

        setScalePrev();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FitscalesApplication.activity = null;
    }

    private void setScalePrev()
    {
        double weight = Prefs.getLastWeight();
        double height = Prefs.getHeight().doubleValue();
        double bmi = weight / (height * height);
        scale.setPrevious((float)bmi, (float)weight);
    }

    @Override
    public void onBoardData(float tl, float tr, float bl, float br)
    {
        double weight = tl + tr + bl + br;
        double height = Prefs.getHeight().doubleValue();
        double bmi = weight / (height * height);
        scale.setCurrent((float)bmi, (float)weight);
    }

    @Override
    public void onBoardWeighIn(float weight)
    {
        float prevWeight = Prefs.getLastWeight();

        SharedPreferences.Editor edit = FitscalesApplication.inst.prefs.edit();
        edit.putFloat(Prefs.KEY_LASTWEIGHT, weight);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
            edit.apply();
        else
            edit.commit();

        setScalePrev();

        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null)
            trans.remove(prev);
        trans.addToBackStack(null);

        DialogFragment nfrag = WeighInDialogFragment.newInstance(weight, prevWeight);
        nfrag.show(trans, "dialog");
    }

    public boolean onOptionsItemPressed(MenuItem item)
    {
        switch (item.getItemId()) {
        case android.R.id.home:
            showSettings(false);
            return true;

        case R.id.menuitem_settings:
            if (viewSettings.getVisibility() == View.VISIBLE) {
                showSettings(false);
                return true;
            }
            showSettings(true);
            return true;
        }

        return false;
    }

    private void showSettings(boolean show)
    {
        if (show)
            ((SettingsFragment)getSupportFragmentManager().findFragmentById(R.id.fragSettings)).loadSettings();
        fragBoard.setMenuIcons(show);
        viewSettings.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBoardOverlayChange(boolean showing)
    {
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) < Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            /* gray out the scale to match the board overlay */
            scale.setOverlay(showing ? 0xd0303030 : 0x0);
        }
    }

    @Override
    public void oauthShowPage(OAuthSyncService svc, String url)
    {
        FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null)
            trans.remove(prev);
        trans.addToBackStack(null);

        DialogFragment nfrag = OAuthDialogFragment.newInstance(url, svc);
        nfrag.show(trans, "dialog");
    }

    @Override
    public void onWeighInDialogCreated()
    {
        if (fragBoard != null)
            fragBoard.cancelWeighIn();
    }

    @Override
    public void onWeighInDialogDestroyed()
    {
        fragBoard.beginWeighIn();
    }

    @Override
    public boolean dispatchGenericMotionEvent (MotionEvent ev) {
        if (fragBoard.dispatchGenericEvent(ev))
            return true;
        return super.dispatchGenericMotionEvent(ev);
    }

    @Override
    public boolean dispatchKeyEvent  (KeyEvent ev) {
        if (fragBoard.dispatchKeyEvent(ev))
            return true;
        return super.dispatchKeyEvent(ev);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            if (resultCode == RESULT_OK) {
                FitscalesApplication.inst.reconnect();;
            }
        }
    }

}
