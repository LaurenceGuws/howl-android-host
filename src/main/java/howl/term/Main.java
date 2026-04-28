package howl.term;

import howl.term.databinding.ActivityMainBinding;
import howl.term.service.android.WindowRuntime;

/** Activity entrypoint for host runtime lifecycle. */
public final class Main extends android.app.Activity {
    private Window window;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        final ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowRuntime.keepScreenOn(this);
        final Object contextHandle = this;
        final Object surfaceContainer = binding.productSurfaceContainer;
        final Object assistContainer = binding.assistBarContainer;
        final Object navContainer = binding.navBarContainer;
        final Object scrim = binding.drawerScrim;
        final Object leftEdge = binding.leftEdgeSwipeHotspot;
        window = new Window(contextHandle, surfaceContainer, assistContainer, navContainer, scrim, leftEdge);
        window.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (window != null) {
            window.resume();
        }
    }

    @Override
    protected void onPause() {
        if (window != null) {
            window.pause();
        }
        super.onPause();
    }
}
