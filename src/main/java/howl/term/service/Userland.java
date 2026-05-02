package howl.term.service;

import howl.term.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Android host userland runtime skeleton. */
public final class Userland {
    private static final String TAG = "howl.term.runtime";
    private final String prefix;
    private final String home;
    private final String tmp;
    private final String howlPm;
    private final String shell;
    private final String manifestUrl;
    private final String bootstrapMarker;
    private boolean started;
    private volatile boolean ready;
    private volatile boolean failed;
    private final Object stateLock;

    public Userland(android.content.Context context) {
        final String appRoot = context.getString(R.string.userland_app_root, context.getPackageName());
        this.prefix = appRoot + context.getString(R.string.userland_prefix_suffix);
        this.home = appRoot + context.getString(R.string.userland_home_suffix);
        this.tmp = appRoot + context.getString(R.string.userland_tmp_suffix);
        this.howlPm = this.prefix + context.getString(R.string.userland_howl_pm_suffix);
        this.shell = this.prefix + context.getString(R.string.userland_shell_suffix);
        this.manifestUrl = context.getString(R.string.userland_manifest_url);
        this.bootstrapMarker = this.prefix + "/var/lib/howl/bootstrap.v1.ok";
        this.started = false;
        this.ready = false;
        this.failed = false;
        this.stateLock = new Object();
    }

    public String getShell() {
        return shell;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getHome() {
        return home;
    }

    public String buildShellCommand() {
        return "export APP_DATA_DIR=\"" + appDataDir() + "\";"
                + "export PREFIX=\"" + getPrefix() + "\";"
                + "export HOME=\"" + home + "\";"
                + "export TMPDIR=\"" + tmp + "\";"
                + "export TERM=\"xterm-256color\";"
                + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\";"
                + "export LD_LIBRARY_PATH=\"$PREFIX/lib\";"
                + "export HOWL_PM_HOST_PLATFORM=\"android\";"
                + "cd \"$HOME\";"
                + "exec \"" + shell + "\" -i";
    }

    public String buildLoginCommand(String shellPath, String startPath, String commandText) {
        final String useShell = (shellPath != null && !shellPath.trim().isEmpty()) ? shellPath.trim() : shell;
        final String useStart = (startPath != null && !startPath.trim().isEmpty()) ? startPath.trim() : home;
        if (commandText != null && !commandText.trim().isEmpty()) {
            return "cd -- \"" + escapeDoubleQuoted(useStart) + "\" && " + commandText;
        }
        return "cd -- \"" + escapeDoubleQuoted(useStart) + "\" && exec \"" + escapeDoubleQuoted(useShell) + "\" -il";
    }

    public ShellLaunch resolveLaunch(Config cfg, long timeoutMs) {
        final boolean readyNow = waitUntilReady(timeoutMs);
        String shellPath = cfg.term.shell != null ? cfg.term.shell : getShell();
        String startPath = cfg.term.startPath != null ? cfg.term.startPath : getHome();
        String commandText = null;
        final boolean explicitCommand = cfg.term.command != null && !cfg.term.command.trim().isEmpty();
        final boolean explicitStartPath = cfg.term.startPath != null
                && !cfg.term.startPath.trim().isEmpty()
                && !cfg.term.startPath.trim().equals(getHome());
        if (explicitCommand || explicitStartPath) {
            commandText = buildLoginCommand(shellPath, startPath, cfg.term.command);
        }
        if (!readyNow || shellPath == null || !new File(shellPath).isFile()) commandText = null;
        return new ShellLaunch(shellPath, commandText);
    }

    public boolean waitUntilReady(long timeoutMs) {
        final long deadline = android.os.SystemClock.uptimeMillis() + Math.max(1L, timeoutMs);
        synchronized (stateLock) {
            while (!ready && !failed) {
                final long remaining = deadline - android.os.SystemClock.uptimeMillis();
                if (remaining <= 0) break;
                try {
                    stateLock.wait(remaining);
                } catch (InterruptedException err) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return ready;
        }
    }

    public void start() {
        if (started) {
            return;
        }
        started = true;
        new Thread(this::initUserland, "howl-userland-init").start();
    }

    public boolean isShellReady() {
        final File shellFile = new File(getShell());
        return shellFile.isFile() && shellFile.canExecute() && isGnuBashBinary();
    }

    public boolean repairAndRecheck() {
        synchronized (stateLock) {
            ready = false;
            failed = false;
        }
        if (!ensureDir(home) || !ensureDir(tmp)) {
            failReady();
            return false;
        }
        if (isShellReady()) {
            ensureShellErgonomics();
            markReady();
            return true;
        }
        if (!new File(howlPm).isFile()) {
            failReady();
            return false;
        }
        final int installRc = runHowlPmInstallWithRetry();
        if (installRc != 0 || !waitForBashValidity(8, 200)) {
            failReady();
            return false;
        }
        ensureShellErgonomics();
        markReady();
        return true;
    }

    private void initUserland() {
        synchronized (stateLock) {
            ready = false;
            failed = false;
        }
        if (!ensureDir(home)) {
            failReady();
            return;
        }
        if (!ensureDir(tmp)) {
            failReady();
            return;
        }

        final File shellFile = new File(getShell());
        final boolean shellExists = shellFile.isFile();
        final boolean shellExec = shellFile.canExecute();
        final boolean shellIsBash = shellExists && shellExec && isGnuBashBinary();
        final boolean pmExists = new File(howlPm).isFile();
        android.util.Log.i(TAG, "userland.detect shellExists=" + shellExists + " shellExec=" + shellExec + " shellIsBash=" + shellIsBash + " pmExists=" + pmExists);

        if (shellExists && shellExec && shellIsBash) {
            ensureShellErgonomics();
            markReady();
            return;
        }
        if (!pmExists) {
            android.util.Log.e(TAG, "userland init failed: howl-pm missing at " + howlPm);
            failReady();
            return;
        }

        // Keep install out of session startup path; startup must not race app update sync.
        android.util.Log.e(TAG, "userland init failed: bash missing or invalid at " + getShell());
        failReady();
        return;
    }

    private void markReady() {
        synchronized (stateLock) {
            ready = true;
            failed = false;
            stateLock.notifyAll();
        }
    }

    private void failReady() {
        synchronized (stateLock) {
            failed = true;
            ready = false;
            stateLock.notifyAll();
        }
    }

    private boolean ensureDir(String path) {
        final File dir = new File(path);
        if (dir.isDirectory()) {
            return true;
        }
        final boolean ok = dir.mkdirs();
        return ok || dir.isDirectory();
    }

    private int runHowlPmInstall() {
        final int rc = runHowlPm(
                "install",
                "dev-baseline",
                "--prefix",
                getPrefix(),
                "--manifest",
                manifestUrl);
        if (rc != 0) android.util.Log.e(TAG, "howl-pm install rc=" + rc);
        return rc;
    }

    private int runHowlPmInstallWithRetry() {
        int last = -1;
        for (int i = 0; i < 8; i++) {
            last = runHowlPmInstall();
            if (last == 0) return 0;
            try {
                Thread.sleep(150L);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                return last;
            }
        }
        return last;
    }

    private boolean waitForBashValidity(int attempts, long delayMs) {
        for (int i = 0; i < attempts; i++) {
            final File bash = new File(getShell());
            if (bash.isFile() && bash.canExecute() && isGnuBashBinary()) return true;
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void runHowlPmDoctorAndList() {
        final int doctorRc = runHowlPm("doctor", "--prefix", getPrefix());
        if (doctorRc != 0) {
            android.util.Log.e(TAG, "howl-pm doctor rc=" + doctorRc);
        }
        final int listRc = runHowlPm("list-available", "--manifest", manifestUrl, "--prefix", getPrefix());
        if (listRc != 0) {
            android.util.Log.e(TAG, "howl-pm list rc=" + listRc);
        }
    }

    private void runFirstBootPackageBootstrap() {
        final File marker = new File(bootstrapMarker);
        if (marker.isFile()) return;

        int rc = runHowlPm("pkg", "install", "ascii-rain", "btop", "nvim");
        if (rc != 0) {
            rc = runHowlPm("pkg", "install", "ascii-rain", "btop", "neovim");
        }
        if (rc != 0) {
            android.util.Log.e(TAG, "userland bootstrap pkg install failed rc=" + rc);
            return;
        }

        final File parent = marker.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        writeFile(marker, "ok\n");
    }

    // Keep install/doctor work out of shell startup path.
    // Zide's stable path separates readiness/session startup from maintenance side effects.

    private boolean isGnuBashBinary() {
        final File bash = new File(getShell());
        if (!bash.isFile()) return false;
        final int rc = runCommand(getShell(), "--version");
        return rc == 0;
    }

    private void ensureShellErgonomics() {
        ensurePkgShim();
        ensureBashRc();
    }

    private void ensurePkgShim() {
        final File shim = new File(getPrefix() + "/bin/pkg");
        if (shim.isFile()) {
            return;
        }
        final String content = "#!/data/data/" + extractPackageName() + "/files/usr/bin/bash\n"
                + "exec \"" + howlPm + "\" pkg --prefix \"$PREFIX\" \"$@\"\n";
        writeExecutableFile(shim, content);
    }

    private void ensureBashRc() {
        final File bashrc = new File(home + "/.bashrc");
        final String content =
                "# ~/.bashrc: executed by bash(1) for non-login shells.\n"
                        + "# Howl Android userland profile (Debian-style prompt + sane defaults).\n"
                        + "\n"
                        + "case $- in\n"
                        + "    *i*) ;;\n"
                        + "      *) return;;\n"
                        + "esac\n"
                        + "\n"
                        + "export APP_DATA_DIR=\"" + appDataDir() + "\"\n"
                        + "export PREFIX=\"" + getPrefix() + "\"\n"
                        + "export HOME=\"" + home + "\"\n"
                        + "export TMPDIR=\"" + tmp + "\"\n"
                        + "export TERM=\"xterm-256color\"\n"
                        + "export USER=\"home\"\n"
                        + "export LOGNAME=\"home\"\n"
                        + "export HOSTNAME=\"howl\"\n"
                        + "export PATH=\"$PREFIX/bin:/system/bin:$PATH\"\n"
                        + "export LD_LIBRARY_PATH=\"$PREFIX/lib\"\n"
                        + "export HOWL_PM_HOST_PLATFORM=\"android\"\n"
                        + "\n"
                        + "HISTCONTROL=ignoreboth\n"
                        + "shopt -s histappend\n"
                        + "HISTSIZE=1000\n"
                        + "HISTFILESIZE=2000\n"
                        + "shopt -s checkwinsize\n"
                        + "\n"
                        + "debian_chroot=\"howl\"\n"
                        + "color_prompt=yes\n"
                        + "\n"
                        + "howl_prompt_path() {\n"
                        + "  local p=\"$PWD\"\n"
                        + "  if [ -n \"$HOME\" ] && [ \"$p\" = \"$HOME\" ]; then\n"
                        + "    printf '~'\n"
                        + "  elif [ -n \"$HOME\" ] && [ \"${p#\"$HOME\"/}\" != \"$p\" ]; then\n"
                        + "    printf '~/%s' \"${p#\"$HOME\"/}\"\n"
                        + "  else\n"
                        + "    printf '%s' \"$p\"\n"
                        + "  fi\n"
                        + "}\n"
                        + "\n"
                        + "if [ \"$color_prompt\" = yes ]; then\n"
                        + "    PS1='${debian_chroot:+($debian_chroot)}\\[\\033[01;32m\\]home@howl\\[\\033[00m\\]:\\[\\033[01;34m\\]$(howl_prompt_path)\\[\\033[00m\\]\\$ '\n"
                        + "else\n"
                        + "    PS1='${debian_chroot:+($debian_chroot)}home@howl:$(howl_prompt_path)\\$ '\n"
                        + "fi\n"
                        + "unset color_prompt\n"
                        + "PROMPT_DIRTRIM=3\n"
                        + "\n"
                        + "case \"$TERM\" in\n"
                        + "xterm*|rxvt*)\n"
                        + "    PS1=\"\\[\\e]0;${debian_chroot:+($debian_chroot)}home@howl: $(howl_prompt_path)\\a\\]$PS1\"\n"
                        + "    ;;\n"
                        + "*) ;;\n"
                        + "esac\n"
                        + "\n"
                        + "LS_COLORS='di=34:ln=35:ex=31'\n"
                        + "export LS_COLORS\n"
                        + "\n"
                        + "alias ls='ls --color=auto'\n"
                        + "alias grep='grep --color=auto'\n"
                        + "alias fgrep='fgrep --color=auto'\n"
                        + "alias egrep='egrep --color=auto'\n"
                        + "alias ll='ls -l --color=auto'\n"
                        + "alias la='ls -A'\n"
                        + "alias l='ls -la --color=auto'\n"
                        + "alias pkg='howl-pm pkg'\n"
                        + "\n"
                        + "if [ -f ~/.bash_aliases ]; then\n"
                        + "  . ~/.bash_aliases\n"
                        + "fi\n"
                        + "\n"
                        + "if ! shopt -oq posix; then\n"
                        + "  if [ -f /usr/share/bash-completion/bash_completion ]; then\n"
                        + "    . /usr/share/bash-completion/bash_completion\n"
                        + "  elif [ -f /etc/bash_completion ]; then\n"
                        + "    . /etc/bash_completion\n"
                        + "  fi\n"
                        + "fi\n";
        writeFile(bashrc, content);
    }

    private String extractPackageName() {
        final String marker = "/data/data/";
        if (!prefix.startsWith(marker)) {
            return "howl.term";
        }
        final String rest = prefix.substring(marker.length());
        final int slash = rest.indexOf('/');
        if (slash < 0) {
            return "howl.term";
        }
        return rest.substring(0, slash);
    }

    private String appDataDir() {
        final String marker = "/data/data/";
        if (!prefix.startsWith(marker)) {
            return "/data/data/" + extractPackageName() + "/files";
        }
        final String rest = prefix.substring(marker.length());
        final int slash = rest.indexOf('/');
        if (slash < 0) {
            return "/data/data/" + extractPackageName() + "/files";
        }
        return marker + rest.substring(0, slash) + "/files";
    }

    private void writeExecutableFile(File file, String content) {
        writeFile(file, content);
        //noinspection ResultOfMethodCallIgnored
        file.setExecutable(true, false);
        //noinspection ResultOfMethodCallIgnored
        file.setReadable(true, false);
    }

    private String escapeDoubleQuoted(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void writeFile(File file, String content) {
        final File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(content);
        } catch (IOException err) {
        }
    }

    private int runHowlPm(String... args) {
        final String pmExecutable = prepareHowlPmRunner();
        final String[] cmd = new String[args.length + 1];
        cmd[0] = pmExecutable;
        System.arraycopy(args, 0, cmd, 1, args.length);
        try {
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(home));
            pb.environment().put("HOME", home);
            pb.environment().put("TMPDIR", tmp);
            pb.environment().put("PREFIX", getPrefix());
            pb.environment().put("PATH", getPrefix() + "/bin:/system/bin");
            pb.environment().put("SHELL", getShell());
            pb.environment().put("LD_LIBRARY_PATH", getPrefix() + "/lib");
            pb.environment().put("HOWL_PM_HOST_PLATFORM", "android");
            final Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    android.util.Log.i(TAG, "howl-pm> " + line);
                }
            }
            final int rc = process.waitFor();
            if (rc != 0) {
                android.util.Log.e(TAG, "howl-pm failed rc=" + rc + " cmd=" + String.join(" ", cmd));
            }
            return rc;
        } catch (IOException err) {
            android.util.Log.e(TAG, "howl-pm exec io error", err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            android.util.Log.e(TAG, "howl-pm exec interrupted", err);
        }
        return -1;
    }

    private String prepareHowlPmRunner() {
        final File source = new File(howlPm);
        if (!source.isFile()) return howlPm;
        final File runner = new File(tmp, "howl-pm-runner");
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(runner, false)) {
            final byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.getFD().sync();
            //noinspection ResultOfMethodCallIgnored
            runner.setExecutable(true, false);
            //noinspection ResultOfMethodCallIgnored
            runner.setReadable(true, false);
            return runner.getAbsolutePath();
        } catch (IOException err) {
            android.util.Log.e(TAG, "prepareHowlPmRunner failed; using direct howl-pm", err);
            return howlPm;
        }
    }

    private int runCommand(String... cmd) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(home));
            pb.environment().put("HOME", home);
            pb.environment().put("TMPDIR", tmp);
            pb.environment().put("PREFIX", getPrefix());
            pb.environment().put("PATH", getPrefix() + "/bin:/system/bin");
            pb.environment().put("SHELL", getShell());
            pb.environment().put("LD_LIBRARY_PATH", getPrefix() + "/lib");
            pb.environment().put("HOWL_PM_HOST_PLATFORM", "android");
            final Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {}
            }
            final int rc = process.waitFor();
            if (rc != 0) {
                android.util.Log.e(TAG, "runCommand failed rc=" + rc + " cmd=" + String.join(" ", cmd));
            }
            return rc;
        } catch (IOException err) {
            android.util.Log.e(TAG, "runCommand io error", err);
            return -1;
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            android.util.Log.e(TAG, "runCommand interrupted", err);
            return -1;
        }
    }

}
