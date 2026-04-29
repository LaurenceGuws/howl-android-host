package howl.term.widget.term_surface;

import howl.term.service.GpuRuntime;

/** Starts GPU runtime */
public final class TerminalSurface {
    private final GpuRuntime gpu;

    public TerminalSurface() {
        this.gpu = new GpuRuntime();
    }

    public android.view.View view(android.app.Activity activity) {
        return gpu.surface(activity);
    }
}
