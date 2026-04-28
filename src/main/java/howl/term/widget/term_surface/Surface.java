package howl.term.widget.term_surface;

import howl.term.service.howl_term.GpuRuntime;
import howl.term.service.android.WindowRuntime;

/** Terminal surface widget handle. */
public final class Surface {
    private final Object handle;

    public Surface(WindowRuntime runtime) {
        this.handle = GpuRuntime.createSurface(runtime.context());
    }

    public Object handle() {
        return handle;
    }

    public void resume() {
        GpuRuntime.resumeSurface(handle);
    }

    public void pause() {
        GpuRuntime.pauseSurface(handle);
    }
}
