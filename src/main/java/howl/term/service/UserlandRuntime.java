package howl.term.service;

import howl.term.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Android host userland runtime skeleton. */
public final class UserlandRuntime {
    private final String tag;
    private final String prefix;
    private final String home;
    private final String tmp;
    private final String howlPm;
    private final String shell;
    private final String manifestUrl;
    private boolean started;

    public UserlandRuntime(android.content.Context context) {
        this.tag = context.getString(R.string.userland_log_tag);
        final String appRoot = context.getString(R.string.userland_app_root, context.getPackageName());
        this.prefix = appRoot + context.getString(R.string.userland_prefix_suffix);
        this.home = appRoot + context.getString(R.string.userland_home_suffix);
        this.tmp = appRoot + context.getString(R.string.userland_tmp_suffix);
        this.howlPm = this.prefix + context.getString(R.string.userland_howl_pm_suffix);
        this.shell = this.prefix + context.getString(R.string.userland_shell_suffix);
        this.manifestUrl = context.getString(R.string.userland_manifest_url);
        this.started = false;
        android.util.Log.i(tag, "userland.init.01 runtime.constructed");
    }

    public String getShell() {
        return shell;
    }

    public String getPrefix() {
        return prefix;
    }

    public void start() {
        if (started) return;
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    private void initUserland() {
        android.util.Log.i(tag, "userland.init.02 release.manifest_url=" + manifestUrl);
        android.util.Log.i(tag, "userland.init.03 path.prefix=" + getPrefix());
        android.util.Log.i(tag, "userland.init.04 path.home=" + home);
        android.util.Log.i(tag, "userland.init.05 path.tmp=" + tmp);
        if (!ensureDir(home, "userland.init.06")) {
            return;
        }
        if (!ensureDir(tmp, "userland.init.07")) {
            return;
        }

        final boolean shellExists = new File(getShell()).isFile();
        final boolean pmExists = new File(howlPm).isFile();
        android.util.Log.i(tag, "userland.init.08 check.shell exists=" + shellExists + " path=" + getShell());
        android.util.Log.i(tag, "userland.init.09 check.howl_pm exists=" + pmExists + " path=" + howlPm);

        if (shellExists) {
            android.util.Log.i(tag, "userland.ready.01 shell_present");
            runHowlPmDoctorAndList();
            return;
        }
        if (!pmExists) {
            android.util.Log.e(tag, "userland.blocked.01 howl_pm_missing path=" + howlPm);
            return;
        }

        android.util.Log.i(tag, "userland.install.01 begin profile=dev-baseline");
        runHowlPmInstall();
        final boolean shellAfter = new File(getShell()).isFile();
        android.util.Log.i(tag, "userland.install.02 postcheck.shell exists=" + shellAfter + " path=" + getShell());
        if (shellAfter) {
            android.util.Log.i(tag, "userland.ready.02 install_success");
            runHowlPmDoctorAndList();
        } else {
            android.util.Log.e(tag, "userland.failed.01 install_completed_without_shell");
        }
    }

    private boolean ensureDir(String path, String step) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            android.util.Log.i(tag, step + " mkdir.skip exists=true path=" + path);
            return true;
        }
        final boolean ok = dir.mkdirs();
        android.util.Log.i(tag, step + " mkdir.created=" + ok + " path=" + path);
        if (!ok && !dir.isDirectory()) {
            android.util.Log.e(tag, "userland.failed.00 mkdir path=" + path);
            return false;
        }
        return true;
    }

    private void runHowlPmInstall() {
        runHowlPm(
                "userland.install",
                "install",
                "--manifest",
                manifestUrl,
                "--prefix",
                getPrefix(),
                "dev-baseline");
    }

    private void runHowlPmDoctorAndList() {
        android.util.Log.i(tag, "userland.pm.01 doctor.begin");
        runHowlPm("userland.pm.doctor", "doctor", "--prefix", getPrefix());
        android.util.Log.i(tag, "userland.pm.02 list_available.begin");
        runHowlPm("userland.pm.list_available", "list-available", "--manifest", manifestUrl, "--prefix", getPrefix());
    }

    private int runHowlPm(String logPrefix, String... args) {
        try {
            final String[] cmd = new String[args.length + 1];
            cmd[0] = howlPm;
            System.arraycopy(args, 0, cmd, 1, args.length);
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.environment().put("HOME", home);
            pb.environment().put("TMPDIR", tmp);
            pb.environment().put("PREFIX", getPrefix());
            pb.environment().put("PATH", getPrefix() + "/bin:/system/bin");
            pb.environment().put("SHELL", getShell());
            pb.environment().put("LD_LIBRARY_PATH", getPrefix() + "/lib");
            final Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                int n = 0;
                while ((line = reader.readLine()) != null) {
                    n += 1;
                    android.util.Log.i(tag, logPrefix + ".output line=" + n + " " + line);
                }
            }
            final int exit = process.waitFor();
            android.util.Log.i(tag, logPrefix + ".exit_code=" + exit);
            return exit;
        } catch (IOException err) {
            android.util.Log.e(tag, logPrefix + ".io_exception " + err.getClass().getSimpleName() + ": " + err.getMessage());
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            android.util.Log.e(tag, logPrefix + ".interrupted");
        }
        return -1;
    }

}
