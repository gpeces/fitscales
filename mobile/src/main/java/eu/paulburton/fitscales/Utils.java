package eu.paulburton.fitscales;

/**
 * Created by Young-Ho on 2015-03-20.
 */
public class Utils {
    static float arrayMean(float[] a, int last, int count)
    {
        float total = 0.0f;

        for (int i = 0; i < count; i++) {
            int idx = last - i;
            while (idx < 0)
                idx += count;
            total += a[idx];
        }

        return total / count;
    }

    static float arrayMin(float[] a, int last, int count)
    {
        float min = a[last];

        for (int i = 1; i < count; i++) {
            int idx = last - i;
            while (idx < 0)
                idx += count;
            min = Math.min(min, a[idx]);
        }

        return min;
    }

    static float arrayMax(float[] a, int last, int count)
    {
        float max = a[last];

        for (int i = 1; i < count; i++) {
            int idx = last - i;
            while (idx < 0)
                idx += count;
            max = Math.max(max, a[idx]);
        }

        return max;
    }
}
