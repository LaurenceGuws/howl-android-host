package howl.term.widget.term_surface;

import howl.term.service.howl_term.GpuRuntime;

/** Terminal surface widget handle. */
public final class Surface {
    private final Object handle;

    public Surface(Object contextHandle) {
        this.handle = GpuRuntime.createSurface(contextHandle);
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
