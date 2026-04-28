package howl.term.service.android;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Generic Android widget wrapper primitives. */
public final class WidgetRuntime {
    private WidgetRuntime() {}

    public static Object horizontalScroll(Object contextHandle) {
        final Context context = (Context) contextHandle;
        final HorizontalScrollView view = new HorizontalScrollView(context);
        view.setHorizontalScrollBarEnabled(false);
        return view;
    }

    public static Object linearHorizontal(Object contextHandle) {
        final Context context = (Context) contextHandle;
        final LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.HORIZONTAL);
        return view;
    }

    public static Object linearVertical(Object contextHandle) {
        final Context context = (Context) contextHandle;
        final LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    public static Object text(Object contextHandle, String value) {
        final Context context = (Context) contextHandle;
        final TextView view = new TextView(context);
        view.setText(value);
        return view;
    }

    public static Object button(Object contextHandle, String value) {
        final Context context = (Context) contextHandle;
        final Button view = new Button(context);
        view.setText(value);
        return view;
    }

    public static Object borderlessButton(Object contextHandle, String value) {
        final Context context = (Context) contextHandle;
        final Button view = new Button(context, null, android.R.attr.borderlessButtonStyle);
        view.setText(value);
        return view;
    }

    public static void setPaddingDp(Object handle, Object contextHandle, int l, int t, int r, int b) {
        final Context context = (Context) contextHandle;
        final View view = (View) handle;
        view.setPadding(dp(context, l), dp(context, t), dp(context, r), dp(context, b));
    }

    public static void setGravityCenterVertical(Object handle) {
        final LinearLayout view = (LinearLayout) handle;
        view.setGravity(Gravity.CENTER_VERTICAL);
    }

    public static void setTextColor(Object handle, int argb) {
        final TextView view = (TextView) handle;
        view.setTextColor(argb);
    }

    public static void setTextSizeSp(Object handle, float sizeSp) {
        final TextView view = (TextView) handle;
        view.setTextSize(sizeSp);
    }

    public static void setMinWidthDp(Object handle, Object contextHandle, int widthDp) {
        final Context context = (Context) contextHandle;
        final Button view = (Button) handle;
        view.setMinWidth(dp(context, widthDp));
    }

    public static void addChild(Object parent, Object child, int w, int h) {
        final ViewGroup vg = (ViewGroup) parent;
        vg.addView((View) child, new ViewGroup.LayoutParams(w, h));
    }

    public static void addChildTopMarginDp(Object parent, Object child, Object contextHandle, int w, int h, int topDp) {
        final Context context = (Context) contextHandle;
        final LinearLayout vg = (LinearLayout) parent;
        final LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.topMargin = dp(context, topDp);
        vg.addView((View) child, p);
    }

    public static void removeAllChildren(Object handle) {
        final ViewGroup vg = (ViewGroup) handle;
        vg.removeAllViews();
    }

    public static void mountFill(Object containerHandle, Object childHandle) {
        final ViewGroup container = (ViewGroup) containerHandle;
        final View child = (View) childHandle;
        container.removeAllViews();
        container.addView(child, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    public static void hideOffscreenLeft(Object handle) {
        final View view = (View) handle;
        view.post(() -> {
            view.setTranslationX(-view.getWidth());
            view.setVisibility(View.GONE);
        });
    }

    public static void showAtOrigin(Object handle) {
        final View view = (View) handle;
        view.setVisibility(View.VISIBLE);
        view.animate().translationX(0f).setDuration(160).start();
    }

    public static void hideAnimatedLeft(Object handle) {
        final View view = (View) handle;
        view.animate().translationX(-view.getWidth()).setDuration(160)
                .withEndAction(() -> view.setVisibility(View.GONE)).start();
    }

    public static void setGone(Object handle) {
        ((View) handle).setVisibility(View.GONE);
    }

    public static final class Size {
        public static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
        public static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

        private Size() {}
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
