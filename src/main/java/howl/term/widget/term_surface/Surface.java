package howl.term.widget.term_surface;

import howl.term.obj.android.GpuRuntime;

/** Starts GPU runtime */
public final class Surface {
    private final GpuRuntime gpu;

    public Surface() {
        this.gpu = new GpuRuntime();
    }

    public android.view.View view(android.app.Activity activity) {
        return gpu.surface(activity);
    }
}
