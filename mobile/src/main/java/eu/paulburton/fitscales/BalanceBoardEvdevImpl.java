package eu.paulburton.fitscales;

import android.app.Activity;
import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import eu.paulburton.fitscales.IBalanceBoard;

/**
 * Created by Young-Ho on 2015-03-20.
 */
public class BalanceBoardEvdevImpl implements IBalanceBoard, InputManager.InputDeviceListener {

    private final static String TAG = "BalanceBoard";
    private ArrayList<WeakReference<Listener>> listeners =
            new ArrayList<WeakReference<IBalanceBoard.Listener>>();
    private InputManager mIm;
    private Handler mHandler;
    private static final int VENDOR_ID = 0x057e;
    private static final int PRODUCT_ID = 0x0306;
    private InputDevice mDevice;

    public BalanceBoardEvdevImpl(Activity activity, IBalanceBoard.Listener... listeners) {

        mHandler = new Handler();
        mIm = (InputManager)activity.getSystemService(Context.INPUT_SERVICE);
        mIm.registerInputDeviceListener(this, mHandler);
        for (IBalanceBoard.Listener l : listeners) {
            this.listeners.add(new WeakReference<IBalanceBoard.Listener>(l));
            l.onWiimoteConnecting(this);
        }

        for (int id: mIm.getInputDeviceIds()) {
            onInputDeviceAdded(id);
        }
    }

    @Override
    public void disconnect() {
        mIm.unregisterInputDeviceListener(this);
        if (mDevice != null) {
            for (WeakReference<IBalanceBoard.Listener> wl : this.listeners) {
                IBalanceBoard.Listener l = wl.get();
                if (l != null)
                    l.onWiimoteDisconnected(this);

            }
            mDevice = null;
        }
    }

    @Override
    public boolean getLed(int index) {
        return false;
    }

    @Override
    public void setLed(int index, boolean on) {

    }

    @Override
    public void setBlinking(boolean blinking) {

    }

    @Override
    public void setCalibrating(boolean cal) {

    }

    private static  final int BOARD_AXISES[] = {MotionEvent.AXIS_HAT_X, // TR
            MotionEvent.AXIS_HAT_Y, // BR
            MotionEvent.AXIS_GENERIC_1, // TL
            MotionEvent.AXIS_GENERIC_2 }; // BL
    private float[] BOARD_AXISES_MIN = new float[BOARD_AXISES.length];
    private float[] BOARD_AXISES_RANGE = new float[BOARD_AXISES.length];
    private float[] BOARD_AXISES_VALUE = new float[BOARD_AXISES.length];

    private IBalanceBoard.Data mData = new IBalanceBoard.Data() {
        @Override
        public float getTopRight() {
            return BOARD_AXISES_VALUE[0];
        }

        @Override
        public float getBottomLeft() {
            return BOARD_AXISES_VALUE[3];
        }

        @Override
        public float getBottomRight() {
            return BOARD_AXISES_VALUE[1];
        }

        @Override
        public float getTopLeft() {
            return BOARD_AXISES_VALUE[2];
        }
    };

    @Override
    public boolean dispatchGenericEvent(MotionEvent ev) {
        if (mDevice == null) return false;
        if (ev.getDeviceId() != mDevice.getId()) return false;
        if (ev.getHistorySize() == 0) return false;

        for (int i = 0; i < BOARD_AXISES.length; i++) {
            int axis = BOARD_AXISES[i];
            float value = ev.getAxisValue(axis);
            float min = BOARD_AXISES_MIN[i];
            if (value == min) return false;
            BOARD_AXISES_VALUE[i] = (value - min) * 65536f
                    / BOARD_AXISES_RANGE[i] / 100f;
        }

        for (WeakReference<IBalanceBoard.Listener> wl : this.listeners) {
            IBalanceBoard.Listener l = wl.get();
            if (l != null) {
                l.onWiimoteData(this, mData);
            }
        }

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev)
    {
        if ((ev.getSource() & InputDevice.SOURCE_JOYSTICK) != 0) {
            return true;
        }
        return false;
    }
    private boolean isBalanceBoard(InputDevice dev) {
        return dev.getProductId() == PRODUCT_ID &&
                dev.getVendorId() == VENDOR_ID;
    }
    @Override
    public void onInputDeviceAdded(int i) {
        InputDevice dev = mIm.getInputDevice(i);
        Log.d(TAG, "input device added " + dev);
        if (isBalanceBoard(dev)) {
            mDevice = dev;
            for (int idx = 0; idx < BOARD_AXISES.length; idx++) {
                int axis = BOARD_AXISES[idx];
                InputDevice.MotionRange range = mDevice.getMotionRange(axis);
                BOARD_AXISES_MIN[idx] = range.getMin();
                BOARD_AXISES_RANGE[idx] = range.getRange();
            }
            for (WeakReference<IBalanceBoard.Listener> wl : this.listeners) {
                IBalanceBoard.Listener l = wl.get();
                if (l != null) {
                    l.onWiimoteConnected(this);
                }
            }
        }
    }

    @Override
    public void onInputDeviceRemoved(int i) {
        InputDevice dev = mIm.getInputDevice(i);
        Log.d(TAG, "input device removed " + dev);
        if (isBalanceBoard(dev)) {
            for (WeakReference<IBalanceBoard.Listener> wl : this.listeners) {
                IBalanceBoard.Listener l = wl.get();
                if (l != null)
                    l.onWiimoteDisconnected(this);

            }
            mDevice = null;
        }
    }

    @Override
    public void onInputDeviceChanged(int i) {

    }
}
