package howl.term.service.android;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/** Android runtime for platform view operations and lifecycle hooks. */
public final class WindowRuntime {
    private final Activity activity;
    private final FrameLayout surfaceContainer;
    private final FrameLayout assistContainer;
    private final FrameLayout navContainer;
    private final View scrim;
    private final View edgeHotspot;

    public WindowRuntime(Activity activity) {
        this.activity = activity;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.setContentView(howl.term.R.layout.activity_main);
        surfaceContainer = activity.findViewById(howl.term.R.id.product_surface_container);
        assistContainer = activity.findViewById(howl.term.R.id.assist_bar_container);
        navContainer = activity.findViewById(howl.term.R.id.nav_bar_container);
        scrim = activity.findViewById(howl.term.R.id.drawer_scrim);
        edgeHotspot = activity.findViewById(howl.term.R.id.left_edge_swipe_hotspot);
    }

    public android.content.Context context() { return activity; }

    public void mountSurface(Object view) { mount(surfaceContainer, view); }
    public void mountAssistBar(Object view) { mount(assistContainer, view); }
    public void mountNavBar(Object view) { mount(navContainer, view); }

    public void initNavHidden() {
        navContainer.post(() -> {
            navContainer.setTranslationX(-navContainer.getWidth());
            navContainer.setVisibility(View.GONE);
        });
        scrim.setVisibility(View.GONE);
    }

    public Object leftEdgeHandle() { return edgeHotspot; }

    public void openWidget(Object handle) {
        navContainer.setVisibility(View.VISIBLE);
        navContainer.animate().translationX(0f).setDuration(160).start();
        scrim.setVisibility(View.GONE);
    }

    public void closeWidget(Object handle) {
        navContainer.animate().translationX(-navContainer.getWidth()).setDuration(160)
                .withEndAction(() -> navContainer.setVisibility(View.GONE)).start();
        scrim.setVisibility(View.GONE);
    }

    private void mount(FrameLayout container, Object handle) {
        final View view = (View) handle;
        container.removeAllViews();
        container.addView(view, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

}
