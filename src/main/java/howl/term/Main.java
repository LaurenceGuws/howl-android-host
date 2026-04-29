package howl.term;

import howl.term.service.InputRuntime;
import howl.term.service.WindowRuntime;
import howl.term.service.UserlandRuntime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.side_panel.SidePanel;
import howl.term.widget.term_surface.TerminalSurface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowRuntime winRt = new WindowRuntime();
    private final InputRuntime inputRt = new InputRuntime();
    private TerminalSurface termSfc;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private UserlandRuntime userlandRt;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        userlandRt = new UserlandRuntime(this);
        userlandRt.start();

        final android.widget.FrameLayout root = winRt.root(this);
        final android.widget.FrameLayout surfaceBox = winRt.container(this);
        final android.view.View leftEdge = winRt.container(this);
        final android.view.View bottomEdge = winRt.container(this);

        termSfc = new TerminalSurface(userlandRt);
        assistBar = new AssistBar(this, winRt);
        sidePanel = new SidePanel(this, winRt);
        winRt.mount(root, surfaceBox, winRt.fill());
        winRt.mount(root, sidePanel.view(), winRt.leftPanel(this, 280));
        winRt.mount(root, assistBar.view(), winRt.bottomBar(this, 66));
        winRt.mount(root, leftEdge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        winRt.mount(root, bottomEdge, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                Math.round(36 * getResources().getDisplayMetrics().density),
                android.view.Gravity.BOTTOM
        ));
        winRt.mount(surfaceBox, termSfc.view(this), winRt.fill());
        inputRt.bindSwipeUp(bottomEdge, assistBar::show);
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindClose();
        winRt.keepScreenOn(this);
        winRt.setContent(this, root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userlandRt == null) {
            throw new IllegalStateException("userland runtime missing");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
