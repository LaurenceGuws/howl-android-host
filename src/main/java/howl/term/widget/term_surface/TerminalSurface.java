package howl.term.widget.term_surface;

import howl.term.service.GpuRuntime;
import howl.term.service.TerminalRuntime;
import howl.term.service.UserlandRuntime;

/** Starts GPU runtime */
public final class TerminalSurface {
    private static final String TAG = "howl.term.runtime";
    private final GpuRuntime gpuRt;
    private final TerminalRuntime termRt;
    private boolean termStarted;
    private boolean stopRequested;
    private int lastCols;
    private int lastRows;

    public TerminalSurface(UserlandRuntime userland) {
        this.gpuRt = new GpuRuntime();
        this.termRt = new TerminalRuntime(userland);
        this.termStarted = false;
        this.stopRequested = false;
        this.lastCols = 0;
        this.lastRows = 0;
    }

    public android.view.View view(android.app.Activity activity) {
        return gpuRt.surface(activity, new GpuRuntime.FrameHooks() {
            @Override
            public void onSurfaceCreated() {
                stopRequested = false;
                termStarted = termRt.start();
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                final int cols = Math.max(1, width / 8);
                final int rows = Math.max(1, height / 16);
                if (termStarted && (cols != lastCols || rows != lastRows)) {
                    final int rc = termRt.resize(cols, rows);
                    android.util.Log.i(TAG, "runtime.resize cols=" + cols + " rows=" + rows + " rc=" + rc);
                    lastCols = cols;
                    lastRows = rows;
                }
            }

            @Override
            public void onDrawFrame() {
                if (stopRequested && termStarted) {
                    termRt.stop();
                    termStarted = false;
                    stopRequested = false;
                    lastCols = 0;
                    lastRows = 0;
                    android.util.Log.i(TAG, "runtime.stop surface_destroyed");
                    return;
                }
                if (!termStarted) return;
                final int rc = termRt.tick();
                if (rc < 0) {
                    android.util.Log.e(TAG, "runtime.tick rc=" + rc);
                }
            }

            @Override
            public void onSurfaceDestroyed() {
                stopRequested = true;
            }
        });
    }
}
