package android.support.v13;

/**
 * Created by taoxj on 16-4-27.
 */

        import android.app.Fragment;

class FragmentCompatICS {
    public static void setMenuVisibility(Fragment f, boolean visible) {
        f.setMenuVisibility(visible);
    }
}