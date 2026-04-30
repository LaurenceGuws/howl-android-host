package howl.term;

import howl.term.service.InputSvc;
import howl.term.service.WindowSvc;
import howl.term.service.UserlandSvc;
import howl.term.widget.AssistBar;
import howl.term.widget.SidePanel;
import howl.term.widget.TerminalSurface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowSvc winSvc = new WindowSvc();
    private final InputSvc inputSvc = new InputSvc();
    private TerminalSurface termSfc;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private UserlandSvc userlandSvc;
    private android.view.View rootView;
    private int leftEdgePx;
    private int bottomEdgePx;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        userlandSvc = new UserlandSvc(this);
        userlandSvc.start();

        final android.widget.FrameLayout root = winSvc.root(this);
        final android.widget.FrameLayout surfaceBox = winSvc.container(this);
        final int assistBarDp = 66;
        final int assistBarPx = Math.round(assistBarDp * getResources().getDisplayMetrics().density);
        final float density = getResources().getDisplayMetrics().density;
        leftEdgePx = Math.round(24 * density);
        bottomEdgePx = Math.round(54 * density);

        termSfc = new TerminalSurface(userlandSvc);
        assistBar = new AssistBar(this, winSvc);
        sidePanel = new SidePanel(this, winSvc);
        final android.widget.FrameLayout.LayoutParams surfaceParams = winSvc.fill();
        surfaceParams.bottomMargin = assistBarPx;
        winSvc.mount(root, surfaceBox, surfaceParams);
        winSvc.mount(root, sidePanel.view(), winSvc.leftPanel(this, 280));
        winSvc.mount(root, assistBar.view(), winSvc.bottomBar(this, assistBarDp));
        winSvc.mount(surfaceBox, termSfc.view(this), winSvc.fill());
        sidePanel.view().setZ(20f);
        assistBar.view().setZ(10f);
        sidePanel.bindClose();
        winSvc.keepScreenOn(this);
        winSvc.setContent(this, root);
        rootView = root;
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

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (rootView != null && sidePanel != null && assistBar != null) {
            final boolean consumedByShell = inputSvc.handleAppShellTouch(
                    rootView,
                    event,
                    leftEdgePx,
                    bottomEdgePx,
                    sidePanel::open,
                    assistBar::show
            );
            if (consumedByShell) return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
