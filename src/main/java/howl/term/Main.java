package howl.term;

import howl.term.service.WindowRuntime;
import howl.term.service.UserlandRuntime;
import howl.term.widget.assist_bar.AssistBar;
import howl.term.widget.side_panel.SidePanel;
import howl.term.widget.term_surface.TerminalSurface;

/** App activity. */
public final class Main extends android.app.Activity {
    private final WindowRuntime winRt = new WindowRuntime();
    private static final String MANIFEST_URL =
            "https://github.com/LaurenceGuws/howl-pm/releases/download/android-dev-2026.04.18.182005/android-dev-prefix.release.manifest.json";
    private TerminalSurface termSfc;
    private AssistBar assistBar;
    private SidePanel sidePanel;
    private UserlandRuntime userlandRt;

    @Override
    protected void onCreate(android.os.Bundle bundle) {
        super.onCreate(bundle);
        final String appRoot = "/data/data/" + getPackageName();
        final String prefix = appRoot + "/files/usr";
        final String home = appRoot + "/files/home";
        final String tmp = appRoot + "/cache/tmp";
        userlandRt = new UserlandRuntime(
                prefix,
                home,
                tmp,
                prefix + "/bin/howl-pm",
                prefix + "/bin/bash",
                MANIFEST_URL
        );
        userlandRt.start();

        final android.widget.FrameLayout root = winRt.root(this);
        final android.widget.FrameLayout surfaceBox = winRt.container(this);
        final android.view.View edge = winRt.container(this);

        termSfc = new TerminalSurface();
        assistBar = new AssistBar(this, winRt);
        sidePanel = new SidePanel(this, winRt);
        winRt.mount(root, surfaceBox, winRt.fill());
        winRt.mount(root, sidePanel.view(), winRt.leftPanel(this, 280));
        winRt.mount(root, assistBar.view(), winRt.bottomBar(this, 44));
        winRt.mount(root, edge, new android.widget.FrameLayout.LayoutParams(
                Math.round(24 * getResources().getDisplayMetrics().density),
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ));
        winRt.mount(surfaceBox, termSfc.view(this), winRt.fill());
        sidePanel.bindOpen(edge);
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
