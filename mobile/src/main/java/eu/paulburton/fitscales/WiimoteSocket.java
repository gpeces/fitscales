package eu.paulburton.fitscales;

import java.lang.reflect.Constructor;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

public final class WiimoteSocket
{
    private static final int TYPE_L2CAP = 3;

    public static BluetoothSocket create(BluetoothDevice dev, int port)
    {
        try {
            /*
             * BluetoothSocket(int type, int fd, boolean auth, boolean encrypt, BluetoothDevice device, int port, ParcelUuid uuid)
             */
            Constructor<BluetoothSocket> construct = BluetoothSocket.class.getDeclaredConstructor(int.class, int.class, boolean.class,
                    boolean.class, BluetoothDevice.class, int.class, ParcelUuid.class);
           
            construct.setAccessible(true);
            return construct.newInstance(TYPE_L2CAP, -1, false, false, dev, port, null);
        } catch (Exception ex) {
            return null;
        }
    }
}
