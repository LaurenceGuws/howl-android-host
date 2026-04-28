package howl.term;

import howl.term.widget.term_surface.View;

/** Window lifecycle coordinator. */
public final class Window {
    private final View surface;
    private final Runnable startAction;
    private final Runnable resumeAction;
    private final Runnable pauseAction;

    public Window(View surface, Runnable startAction, Runnable resumeAction, Runnable pauseAction) {
        this.surface = surface;
        this.startAction = startAction;
        this.resumeAction = resumeAction;
        this.pauseAction = pauseAction;
    }

    public void start() {
        startAction.run();
    }

    public void resume() {
        resumeAction.run();
    }

    public void pause() {
        pauseAction.run();
    }

    public View surface() {
        return surface;
    }
}
