package eu.paulburton.fitscales;

import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Created by Young-Ho on 2015-03-20.
 */
public interface IBalanceBoard {

    public void disconnect();
    public boolean getLed(int index);
    public void setLed(int index, boolean on);
    public void setBlinking(boolean blinking);
    public void setCalibrating(boolean cal);

    public boolean dispatchGenericEvent(MotionEvent ev);
    public boolean dispatchKeyEvent(KeyEvent ev);

    public interface Data {
        public float getTopRight();
        public float getBottomLeft();
        public float getBottomRight();
        public float getTopLeft();
    }

    public interface Listener
    {
        void onWiimoteConnecting(IBalanceBoard wm);

        void onWiimoteConnected(IBalanceBoard wm);

        void onWiimoteDisconnected(IBalanceBoard wm);

        void onWiimoteLEDChange(IBalanceBoard wm);

        void onWiimoteData(IBalanceBoard wm, IBalanceBoard.Data data);
    }

}
