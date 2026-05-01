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

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        userlandSvc = new UserlandSvc(this);
        userlandSvc.start();

        final android.widget.FrameLayout root = winSvc.root(this);
        final android.widget.FrameLayout appWindow = winSvc.container(this);
        final android.widget.FrameLayout overlayWindow = winSvc.container(this);
        final android.widget.FrameLayout surfaceBox = winSvc.container(this);
        final android.view.View leftEdge = winSvc.container(this);
        final android.view.View bottomEdge = winSvc.container(this);
        final android.view.View overlayScrim = winSvc.container(this);
        final int assistBarDp = 66;
        final int assistBarPx = Math.round(assistBarDp * getResources().getDisplayMetrics().density);
        final int bottomEdgePx = Math.round(54 * getResources().getDisplayMetrics().density);

        termSfc = new TerminalSurface(userlandSvc);
        assistBar = new AssistBar(this, winSvc);
        sidePanel = new SidePanel(this, winSvc);
        final android.widget.FrameLayout.LayoutParams surfaceParams = winSvc.fill();
        surfaceParams.bottomMargin = 0;
        winSvc.mount(root, appWindow, winSvc.fill());
        winSvc.mount(root, overlayWindow, winSvc.fill());

        winSvc.mount(appWindow, surfaceBox, surfaceParams);
        winSvc.mount(appWindow, assistBar.view(), winSvc.bottomBar(this, assistBarDp));
        winSvc.mount(appWindow, leftEdge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        final android.widget.FrameLayout.LayoutParams bottomParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                bottomEdgePx
        );
        bottomParams.gravity = android.view.Gravity.BOTTOM;
        winSvc.mount(appWindow, bottomEdge, bottomParams);
        winSvc.mount(surfaceBox, termSfc.view(this), winSvc.fill());

        winSvc.setBackground(overlayScrim, 0x55000000);
        winSvc.mount(overlayWindow, overlayScrim, winSvc.fill());
        winSvc.mount(overlayWindow, sidePanel.view(), winSvc.leftPanel(this, 280));

        assistBar.view().setZ(20f);
        leftEdge.setZ(30f);
        bottomEdge.setZ(5f);
        overlayScrim.setZ(40f);
        sidePanel.view().setZ(50f);

        inputSvc.bindSwipeUp(bottomEdge, assistBar::show);
        inputSvc.bindSwipeDown(assistBar.view(), assistBar::hide);
        assistBar.setVisibilityListener(visible -> {
            bottomEdge.setVisibility(visible ? android.view.View.GONE : android.view.View.VISIBLE);
            final android.widget.FrameLayout.LayoutParams params =
                    (android.widget.FrameLayout.LayoutParams) surfaceBox.getLayoutParams();
            params.bottomMargin = visible ? assistBarPx : 0;
            surfaceBox.setLayoutParams(params);
            surfaceBox.requestLayout();
        });
        bottomEdge.setVisibility(android.view.View.VISIBLE);
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindOverlayScrim(overlayScrim);
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
        if (termSfc != null) termSfc.onResume();
    }

    @Override
    protected void onPause() {
        if (termSfc != null) termSfc.onPause();
        super.onPause();
    }
}
