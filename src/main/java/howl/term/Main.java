package howl.term;

import howl.term.service.InputSvc;
import howl.term.service.WindowSvc;
import howl.term.service.UserlandSvc;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.side_panel.SidePanel;
import howl.term.widget.term_surface.TerminalSurface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowSvc winSvc = new WindowSvc();
    private final InputSvc inputSvc = new InputSvc();
    private TerminalSurface termSfc;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private UserlandSvc userlandSvc;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        userlandSvc = new UserlandSvc(this);
        userlandSvc.start();

        final android.widget.FrameLayout root = winSvc.root(this);
        final android.widget.FrameLayout surfaceBox = winSvc.container(this);
        final android.view.View leftEdge = winSvc.container(this);

        termSfc = new TerminalSurface(userlandSvc);
        assistBar = new AssistBar(this, winSvc);
        sidePanel = new SidePanel(this, winSvc);
        winSvc.mount(root, surfaceBox, winSvc.fill());
        winSvc.mount(root, sidePanel.view(), winSvc.leftPanel(this, 280));
        winSvc.mount(root, assistBar.view(), winSvc.bottomBar(this, 66));
        winSvc.mount(root, leftEdge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        winSvc.mount(surfaceBox, termSfc.view(this), winSvc.fill());
        inputSvc.bindSwipeUpFromBottom(root, Math.round(54 * getResources().getDisplayMetrics().density), assistBar::show);
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindClose();
        winSvc.keepScreenOn(this);
        winSvc.setContent(this, root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (userlandSvc == null) {
            throw new IllegalStateException("userland runtime missing");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
