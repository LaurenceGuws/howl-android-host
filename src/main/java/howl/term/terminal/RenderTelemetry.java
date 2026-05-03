package howl.term.terminal;

/** Render telemetry leaf for throttled terminal logging. */
public final class RenderTelemetry {
    private long lastMissingGlyphs;
    private long lastFallbackHits;
    private long lastFallbackMisses;
    private long lastShapedClusters;
    private int frameCounter;

    public void reset() {
        lastMissingGlyphs = 0L;
        lastFallbackHits = 0L;
        lastFallbackMisses = 0L;
        lastShapedClusters = 0L;
        frameCounter = 0;
    }

    public void onRenderedFrame(long handle) {
        frameCounter += 1;
        if (frameCounter < 30 || handle == 0L) return;
        frameCounter = 0;

        final long missing = NativeBinding.renderMissingGlyphs(handle);
        final long hits = NativeBinding.renderFallbackHits(handle);
        final long misses = NativeBinding.renderFallbackMisses(handle);
        final long shaped = NativeBinding.renderShapedClusters(handle);
        final int stage = NativeBinding.renderResolveStage(handle);
        if (missing != lastMissingGlyphs || hits != lastFallbackHits || misses != lastFallbackMisses || shaped != lastShapedClusters) {
            android.util.Log.i(
                    "howl.term.runtime",
                    "render.telemetry missing=" + missing +
                            " fallback_hits=" + hits +
                            " fallback_misses=" + misses +
                            " shaped=" + shaped +
                            " stage=" + stage);
            lastMissingGlyphs = missing;
            lastFallbackHits = hits;
            lastFallbackMisses = misses;
            lastShapedClusters = shaped;
        }
    }
}
