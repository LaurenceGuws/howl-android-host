package howl.term;

import howl.term.widget.AssistBar;
import howl.term.widget.SidePanel;
import howl.term.widget.TerminalWidget;

/**
 * Responsibility: own the Android activity boundary for the app shell.
 * Ownership: userland readiness, top-level layout composition, and lifecycle forwarding.
 * Reason: keep the application entrypoint on one boring host owner.
 */
public final class Main extends android.app.Activity {
    private final Window window = new Window();

    private Userland userland;
    private TerminalWidget terminalWidget;
    private AssistBar assistBar;
    private SidePanel sidePanel;

    @Override
    protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        userland = new Userland(this);
        userland.start();
        final Config cfg = Config.load(this);
        final Userland.Launch launchPlan = userland.resolveLaunch(cfg, 12000);
        if (!launchPlan.userlandReady || launchPlan.shellLaunch.shell == null || !new java.io.File(launchPlan.shellLaunch.shell).isFile()) {
            showUserlandRecovery(cfg);
            return;
        }
        launchTerminal(cfg, launchPlan.shellLaunch);
    }

    private void showUserlandRecovery(Config cfg) {
        final float density = getResources().getDisplayMetrics().density;
        final int pad = Math.round(20 * density);
        final int gap = Math.round(12 * density);

        final android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(0xFF101215);

        final android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Userland Not Ready");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(20f);

        final android.widget.TextView subtitle = new android.widget.TextView(this);
        subtitle.setText("Configured shell is unavailable. Run repair to install/restore userland.");
        subtitle.setTextColor(0xFFB9C0CC);
        subtitle.setTextSize(14f);
        subtitle.setGravity(android.view.Gravity.CENTER);

        final android.widget.Button retry = new android.widget.Button(this);
        retry.setText("Run Repair");

        final android.widget.TextView status = new android.widget.TextView(this);
        status.setTextColor(0xFF9AA4B2);
        status.setTextSize(13f);
        status.setText("Waiting...");

        retry.setOnClickListener(v -> userland.startRepair(new Userland.RepairListener() {
            @Override
            public void onStarted() {
                retry.setEnabled(false);
                status.setText("Repair in progress...");
            }

            @Override
            public void onFinished(boolean ok) {
                retry.setEnabled(true);
                if (!ok) {
                    status.setText("Repair failed. Check runtime logs, then retry.");
                    return;
                }
                final Userland.Launch launchPlan = userland.resolveLaunch(cfg, 2000);
                if (!launchPlan.userlandReady || launchPlan.shellLaunch.shell == null || !new java.io.File(launchPlan.shellLaunch.shell).isFile()) {
                    status.setText("Repair completed, but shell still unavailable.");
                    return;
                }
                launchTerminal(cfg, launchPlan.shellLaunch);
            }
        }));

        root.addView(title);
        final android.widget.LinearLayout.LayoutParams subtitleLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleLp.topMargin = gap;
        root.addView(subtitle, subtitleLp);
        final android.widget.LinearLayout.LayoutParams buttonLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLp.topMargin = gap * 2;
        root.addView(retry, buttonLp);
        final android.widget.LinearLayout.LayoutParams statusLp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusLp.topMargin = gap;
        root.addView(status, statusLp);

        setContentView(root);
    }

    private void launchTerminal(Config cfg, ShellLaunch shellLaunch) {

        final android.widget.FrameLayout root = window.root(this);
        final android.widget.FrameLayout app = window.container(this);
        final android.widget.FrameLayout overlay = window.container(this);
        final android.widget.FrameLayout surfaceBox = window.container(this);
        final android.view.View leftEdge = window.container(this);
        final android.view.View bottomEdge = window.container(this);
        final android.view.View scrim = window.container(this);

        terminalWidget = new TerminalWidget(cfg, shellLaunch);
        assistBar = new AssistBar(this, window);
        sidePanel = new SidePanel(this, window);
        final android.view.View termView = terminalWidget.view(this);

        final int assistDp = 66;
        final int assistPx = Math.round(assistDp * getResources().getDisplayMetrics().density);
        final int bottomEdgePx = Math.round(54 * getResources().getDisplayMetrics().density);

        window.mount(root, app, window.fill());
        window.mount(root, overlay, window.fill());
        window.mount(app, surfaceBox, window.fill());
        window.mount(app, assistBar.view(), window.bottomBar(this, assistDp));

        final android.widget.FrameLayout.LayoutParams leftEdgeParams = new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        );
        window.mount(app, leftEdge, leftEdgeParams);

        final android.widget.FrameLayout.LayoutParams bottomParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                bottomEdgePx
        );
        bottomParams.gravity = android.view.Gravity.BOTTOM;
        window.mount(app, bottomEdge, bottomParams);

        window.mount(surfaceBox, termView, window.fill());
        assistBar.setImeAnchor(terminalWidget.imeAnchor());

        window.setBackground(scrim, 0x55000000);
        window.mount(overlay, scrim, window.fill());
        window.mount(overlay, sidePanel.view(), window.leftPanel(this, 280));

        assistBar.view().setZ(20f);
        leftEdge.setZ(30f);
        scrim.setZ(40f);
        sidePanel.view().setZ(50f);

        Input.bindSwipeUp(bottomEdge, assistBar::show);
        Input.bindSwipeDown(assistBar.view(), assistBar::hide);
        assistBar.setVisibilityListener(visible -> {
            bottomEdge.setVisibility(visible ? android.view.View.GONE : android.view.View.VISIBLE);
            final android.widget.FrameLayout.LayoutParams p =
                    (android.widget.FrameLayout.LayoutParams) surfaceBox.getLayoutParams();
            p.bottomMargin = visible ? assistPx : 0;
            surfaceBox.setLayoutParams(p);
        });
        sidePanel.bindOpen(leftEdge);
        sidePanel.bindOverlayScrim(scrim);
        sidePanel.bindClose();

        window.keepScreenOn(this);
        window.setContent(this, root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (terminalWidget != null) terminalWidget.onResume();
    }

    @Override
    protected void onPause() {
        if (terminalWidget != null) terminalWidget.onPause();
        super.onPause();
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (terminalWidget != null && terminalWidget.dispatchKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
}
