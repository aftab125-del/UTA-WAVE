package com.uta.wave.desktop

import com.uta.wave.audio.UnifiedPlaybackState
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Windows System Media Transport Controls (SMTC) Native Bridge.
 *
 * Direct integration with Windows 10/11 WinRT SystemMediaTransportControls.
 * Enables real-time detection, album art display, and bidirectional transport control in:
 * - Windows Dynamic Island / notch utilities (Bezel, ModernFlyouts, Dynamic Island for Windows).
 * - Windows 11 hardware volume flyout now-playing card.
 * - Windows 10/11 lock screen media controls.
 * - Global hardware keyboard media keys (Play/Pause, Next, Previous).
 */
class WindowsSmtcManager : WindowsPlatformAdapter {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var process: Process? = null
    private var writer: PrintWriter? = null

    private var onPlay: (() -> Unit)? = null
    private var onPause: (() -> Unit)? = null
    private var onPlayPause: (() -> Unit)? = null
    private var onNext: (() -> Unit)? = null
    private var onPrevious: (() -> Unit)? = null
    private var onStop: (() -> Unit)? = null

    private var lastTitle: String? = null
    private var lastArtist: String? = null
    private var lastIsPlaying: Boolean? = null
    private var lastArtworkUrl: String? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    init {
        startBridgeProcess()
    }

    private fun getArtCacheDir(): File {
        val localAppData = System.getenv("LOCALAPPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
        val dir = File(localAppData, "UTA-wave" + File.separator + "art_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getArtFile(url: String): File {
        val hash = abs(url.hashCode()).toString(16)
        return File(getArtCacheDir(), "art_$hash.jpg")
    }

    private fun getBridgeDirectory(): File {
        val localAppData = System.getenv("LOCALAPPDATA")
            ?: (System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Local")
        val dir = File(localAppData, "UTA-wave")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getBridgeExe(): File? {
        val dir = getBridgeDirectory()
        val exeFile = File(dir, "smtc_bridge.exe")
        val csFile = File(dir, "smtc_bridge.cs")

        val csContent = """
using System;
using System.IO;
using Windows.Foundation;
using Windows.Media;
using Windows.Media.Playback;
using Windows.Storage;
using Windows.Storage.Streams;

class SmtcBridge {
    private static readonly object consoleLock = new object();

    private static StorageFile GetStorageFile(string path) {
        try {
            var op = StorageFile.GetFileFromPathAsync(path);
            int waited = 0;
            while (op.Status == AsyncStatus.Started && waited < 1500) {
                System.Threading.Thread.Sleep(10);
                waited += 10;
            }
            if (op.Status == AsyncStatus.Completed) {
                return op.GetResults();
            }
        } catch { }
        return null;
    }

    static void Main(string[] args) {
        try {
            Console.OutputEncoding = System.Text.Encoding.UTF8;
            Console.InputEncoding = System.Text.Encoding.UTF8;
        } catch { }

        MediaPlayer player = null;
        SystemMediaTransportControls smtc = null;

        try {
            player = new MediaPlayer();
            player.CommandManager.IsEnabled = true;
            player.CommandManager.PlayBehavior.EnablingRule = MediaCommandEnablingRule.Always;
            player.CommandManager.PauseBehavior.EnablingRule = MediaCommandEnablingRule.Always;
            player.CommandManager.NextBehavior.EnablingRule = MediaCommandEnablingRule.Always;
            player.CommandManager.PreviousBehavior.EnablingRule = MediaCommandEnablingRule.Always;

            player.CommandManager.PlayReceived += (sender, e) => {
                e.Handled = true;
                lock (consoleLock) {
                    Console.WriteLine("EVENT:Play");
                    Console.Out.Flush();
                }
            };

            player.CommandManager.PauseReceived += (sender, e) => {
                e.Handled = true;
                lock (consoleLock) {
                    Console.WriteLine("EVENT:Pause");
                    Console.Out.Flush();
                }
            };

            player.CommandManager.NextReceived += (sender, e) => {
                e.Handled = true;
                lock (consoleLock) {
                    Console.WriteLine("EVENT:Next");
                    Console.Out.Flush();
                }
            };

            player.CommandManager.PreviousReceived += (sender, e) => {
                e.Handled = true;
                lock (consoleLock) {
                    Console.WriteLine("EVENT:Previous");
                    Console.Out.Flush();
                }
            };

            smtc = player.SystemMediaTransportControls;

            smtc.IsPlayEnabled = true;
            smtc.IsPauseEnabled = true;
            smtc.IsNextEnabled = true;
            smtc.IsPreviousEnabled = true;
            smtc.IsStopEnabled = true;

            smtc.ButtonPressed += (sender, e) => {
                try {
                    lock (consoleLock) {
                        Console.WriteLine("EVENT:" + e.Button.ToString());
                        Console.Out.Flush();
                    }
                } catch { }
            };

            var updater = smtc.DisplayUpdater;
            updater.Type = MediaPlaybackType.Music;
            updater.MusicProperties.Title = "UTA-WAVE";
            updater.MusicProperties.Artist = "A Universe of Music";
            updater.Update();

            lock (consoleLock) {
                Console.WriteLine("READY");
                Console.Out.Flush();
            }

            string line;
            while ((line = Console.ReadLine()) != null) {
                line = line.Trim();
                if (line.Length == 0) continue;

                if (line.StartsWith("UPDATE:")) {
                    var payload = line.Substring(7);
                    var parts = payload.Split('|');
                    if (parts.Length >= 4) {
                        string title = parts[0];
                        string artist = parts[1];
                        string album = parts[2];
                        bool isPlaying = false;
                        bool.TryParse(parts[3], out isPlaying);
                        string artPath = parts.Length >= 5 ? parts[4] : "";

                        updater.MusicProperties.Title = title;
                        updater.MusicProperties.Artist = artist;
                        if (!string.IsNullOrWhiteSpace(album)) {
                            updater.MusicProperties.AlbumTitle = album;
                        }

                        if (!string.IsNullOrWhiteSpace(artPath)) {
                            try {
                                if (File.Exists(artPath)) {
                                    var file = GetStorageFile(artPath);
                                    if (file != null) {
                                        updater.Thumbnail = RandomAccessStreamReference.CreateFromFile(file);
                                    } else {
                                        updater.Thumbnail = null;
                                    }
                                } else if (artPath.StartsWith("http://") || artPath.StartsWith("https://")) {
                                    updater.Thumbnail = RandomAccessStreamReference.CreateFromUri(new Uri(artPath));
                                } else {
                                    updater.Thumbnail = null;
                                }
                            } catch {
                                updater.Thumbnail = null;
                            }
                        } else {
                            updater.Thumbnail = null;
                        }

                        updater.Update();

                        smtc.PlaybackStatus = isPlaying ? MediaPlaybackStatus.Playing : MediaPlaybackStatus.Paused;
                    }
                } else if (line.StartsWith("STATUS:")) {
                    var status = line.Substring(7).Trim();
                    if (status == "Playing") {
                        smtc.PlaybackStatus = MediaPlaybackStatus.Playing;
                    } else if (status == "Paused") {
                        smtc.PlaybackStatus = MediaPlaybackStatus.Paused;
                    } else if (status == "Stopped") {
                        smtc.PlaybackStatus = MediaPlaybackStatus.Stopped;
                    }
                } else if (line == "QUIT") {
                    break;
                }
            }
        } catch (Exception ex) {
            lock (consoleLock) {
                Console.WriteLine("ERROR:" + ex.Message);
                Console.Out.Flush();
            }
        } finally {
            if (player != null) {
                try { player.Dispose(); } catch { }
            }
        }
    }
}
""".trimIndent()

        val needsCompile = !exeFile.exists() || !csFile.exists() || csFile.readText(StandardCharsets.UTF_8) != csContent

        if (needsCompile) {
            try {
                csFile.writeText(csContent, StandardCharsets.UTF_8)

                val csc64 = File("C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe")
                val csc32 = File("C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\csc.exe")
                val csc = if (csc64.exists()) csc64 else if (csc32.exists()) csc32 else null

                if (csc != null) {
                    val frameworkDir = csc.parentFile.absolutePath
                    val compilePb = ProcessBuilder(
                        csc.absolutePath,
                        "/target:winexe",
                        "/optimize",
                        "/nologo",
                        "/reference:$frameworkDir\\System.Runtime.dll",
                        "/reference:$frameworkDir\\System.Runtime.InteropServices.WindowsRuntime.dll",
                        "/reference:C:\\Windows\\System32\\WinMetadata\\Windows.Media.winmd",
                        "/reference:C:\\Windows\\System32\\WinMetadata\\Windows.Foundation.winmd",
                        "/reference:C:\\Windows\\System32\\WinMetadata\\Windows.Storage.winmd",
                        "/reference:System.Runtime.WindowsRuntime.dll",
                        "/out:${exeFile.absolutePath}",
                        csFile.absolutePath
                    )
                    compilePb.redirectErrorStream(true)
                    val p = compilePb.start()
                    p.waitFor(10, TimeUnit.SECONDS)
                    if (exeFile.exists()) {
                        println("[WindowsSMTC] Compiled native WinRT SMTC bridge executable successfully.")
                        return exeFile
                    }
                }
            } catch (e: Exception) {
                println("[WindowsSMTC] Could not compile native bridge: ${e.message}")
            }
        } else if (exeFile.exists()) {
            return exeFile
        }

        return null
    }

    private fun getScriptFile(): File {
        val dir = getBridgeDirectory()
        val script = File(dir, "smtc_bridge.ps1")

        val scriptContent = """
# UTA-WAVE Windows SMTC Native WinRT Bridge Fallback
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

try {
    [Windows.Media.Playback.MediaPlayer, Windows.Media, ContentType=WindowsRuntime] | Out-Null
    [Windows.Media.MediaPlaybackType, Windows.Media, ContentType=WindowsRuntime] | Out-Null
    [Windows.Media.MediaPlaybackStatus, Windows.Media, ContentType=WindowsRuntime] | Out-Null
    [Windows.Storage.Streams.RandomAccessStreamReference, Windows.Storage.Streams, ContentType=WindowsRuntime] | Out-Null
} catch {
    [Console]::WriteLine("ERROR:Failed to load WinRT types: " + ${'$'}_.Exception.Message)
    exit 1
}

${'$'}player = New-Object Windows.Media.Playback.MediaPlayer
${'$'}player.CommandManager.IsEnabled = ${'$'}false
${'$'}smtc = ${'$'}player.SystemMediaTransportControls

${'$'}smtc.IsPlayEnabled = ${'$'}true
${'$'}smtc.IsPauseEnabled = ${'$'}true
${'$'}smtc.IsNextEnabled = ${'$'}true
${'$'}smtc.IsPreviousEnabled = ${'$'}true
${'$'}smtc.IsStopEnabled = ${'$'}true

${'$'}updater = ${'$'}smtc.DisplayUpdater
${'$'}updater.Type = [Windows.Media.MediaPlaybackType]::Music
${'$'}updater.MusicProperties.Title = "UTA-WAVE"
${'$'}updater.MusicProperties.Artist = "A Universe of Music"
${'$'}updater.Update()

[Console]::WriteLine("READY")

while (${'$'}true) {
    ${'$'}line = [Console]::In.ReadLine()
    if (${'$'}line -eq ${'$'}null) { break }
    if (${'$'}line.Trim() -eq "") { continue }

    try {
        if (${'$'}line.StartsWith("UPDATE:")) {
            ${'$'}payload = ${'$'}line.Substring(7)
            ${'$'}parts = ${'$'}payload.Split('|')
            if (${'$'}parts.Length -ge 4) {
                ${'$'}title = ${'$'}parts[0]
                ${'$'}artist = ${'$'}parts[1]
                ${'$'}album = ${'$'}parts[2]
                ${'$'}isPlaying = [bool]::Parse(${'$'}parts[3])
                ${'$'}artPath = if (${'$'}parts.Length -ge 5) { ${'$'}parts[4] } else { "" }

                ${'$'}updater.MusicProperties.Title = ${'$'}title
                ${'$'}updater.MusicProperties.Artist = ${'$'}artist
                if (![string]::IsNullOrWhiteSpace(${'$'}album)) {
                    ${'$'}updater.MusicProperties.AlbumTitle = ${'$'}album
                }

                if (![string]::IsNullOrWhiteSpace(${'$'}artPath) -and (Test-Path -LiteralPath ${'$'}artPath)) {
                    try {
                        ${'$'}uri = New-Object System.Uri(${'$'}artPath)
                        ${'$'}updater.Thumbnail = [Windows.Storage.Streams.RandomAccessStreamReference]::CreateFromUri(${'$'}uri)
                    } catch {
                        ${'$'}updater.Thumbnail = ${'$'}null
                    }
                } else {
                    ${'$'}updater.Thumbnail = ${'$'}null
                }

                ${'$'}updater.Update()

                if (${'$'}isPlaying) {
                    ${'$'}smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Playing
                } else {
                    ${'$'}smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Paused
                }
            }
        } elseif (${'$'}line.StartsWith("STATUS:")) {
            ${'$'}status = ${'$'}line.Substring(7).Trim()
            if (${'$'}status -eq "Playing") {
                ${'$'}smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Playing
            } elseif (${'$'}status -eq "Paused") {
                ${'$'}smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Paused
            } elseif (${'$'}status -eq "Stopped") {
                ${'$'}smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Stopped
            }
        } elseif (${'$'}line -eq "QUIT") {
            break
        }
    } catch {
        # ignore parse issues
    }
}

${'$'}player.Dispose()
""".trimIndent()

        script.writeText(scriptContent, StandardCharsets.UTF_8)
        return script
    }

    private fun startBridgeProcess() {
        scope.launch {
            try {
                val exe = getBridgeExe()
                val pb = if (exe != null && exe.exists()) {
                    ProcessBuilder(exe.absolutePath)
                } else {
                    println("[WindowsSMTC] Using PowerShell bridge fallback...")
                    val script = getScriptFile()
                    ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-NonInteractive",
                        "-ExecutionPolicy", "Bypass",
                        "-File", script.absolutePath
                    )
                }

                pb.redirectErrorStream(true)
                val p = pb.start()
                process = p
                writer = PrintWriter(OutputStreamWriter(p.outputStream, StandardCharsets.UTF_8), true)

                // Add shutdown hook to cleanly close child process
                Runtime.getRuntime().addShutdownHook(Thread {
                    try {
                        writer?.println("QUIT")
                        p.destroyForcibly()
                    } catch (_: Exception) {}
                })

                val reader = BufferedReader(InputStreamReader(p.inputStream, StandardCharsets.UTF_8))
                var line: String? = null
                while (p.isAlive) {
                    line = reader.readLine() ?: break
                    val l = line.trim()
                    if (l.startsWith("EVENT:")) {
                        val event = l.substring(6).trim()
                        println("[WindowsSMTC] Received transport control event: $event")
                        withContext(Dispatchers.Main) {
                            when (event.lowercase()) {
                                "play" -> onPlay?.invoke() ?: onPlayPause?.invoke()
                                "pause" -> onPause?.invoke() ?: onPlayPause?.invoke()
                                "playpause" -> onPlayPause?.invoke()
                                "next" -> onNext?.invoke()
                                "previous" -> onPrevious?.invoke()
                                "stop" -> onStop?.invoke()
                            }
                        }
                    } else if (l.startsWith("READY")) {
                        println("[WindowsSMTC] System Media Transport Controls bridge active!")
                    }
                }
            } catch (e: Exception) {
                println("[WindowsSMTC] Failed to start SMTC bridge: ${e.message}")
            }
        }
    }

    override fun initSystemMediaTransportControls(
        onPlayPauseClicked: () -> Unit,
        onNextClicked: () -> Unit,
        onPreviousClicked: () -> Unit,
        onStopClicked: () -> Unit
    ) {
        this.onPlayPause = onPlayPauseClicked
        this.onPlay = onPlayPauseClicked
        this.onPause = onPlayPauseClicked
        this.onNext = onNextClicked
        this.onPrevious = onPreviousClicked
        this.onStop = onStopClicked
    }

    override fun initSystemMediaTransportControls(
        onPlayClicked: () -> Unit,
        onPauseClicked: () -> Unit,
        onNextClicked: () -> Unit,
        onPreviousClicked: () -> Unit,
        onStopClicked: () -> Unit
    ) {
        this.onPlay = onPlayClicked
        this.onPause = onPauseClicked
        this.onPlayPause = {
            onPlayClicked()
        }
        this.onNext = onNextClicked
        this.onPrevious = onPreviousClicked
        this.onStop = onStopClicked
    }

    override fun updateSmtcPlaybackState(state: UnifiedPlaybackState) {
        val title = state.title.ifBlank { "UTA-WAVE" }
        val artist = state.artist.ifBlank { "A Universe of Music" }
        val isPlaying = state.isPlaying
        val artworkUrl = state.artworkUrl

        if (title == lastTitle && artist == lastArtist && isPlaying == lastIsPlaying && artworkUrl == lastArtworkUrl) {
            return
        }

        lastTitle = title
        lastArtist = artist
        lastIsPlaying = isPlaying
        lastArtworkUrl = artworkUrl

        val cleanTitle = title.replace('|', '-').replace('\n', ' ')
        val cleanArtist = artist.replace('|', '-').replace('\n', ' ')
        val cleanAlbum = "UTA-WAVE"

        // Check if artwork file is already cached locally or is a local file
        var localArtPath = ""
        if (!artworkUrl.isNullOrBlank()) {
            val localCandidate = File(artworkUrl.removePrefix("file://").removePrefix("file:/"))
            if (localCandidate.exists() && localCandidate.isFile) {
                localArtPath = localCandidate.absolutePath
            } else {
                val cachedFile = getArtFile(artworkUrl)
                if (cachedFile.exists() && cachedFile.length() > 0) {
                    localArtPath = cachedFile.absolutePath
                }
            }
        }

        // Send immediate title/artist/status update (with cached art if available)
        sendUpdateCommand(cleanTitle, cleanArtist, cleanAlbum, isPlaying, localArtPath)

        // If artwork exists as remote URL but not yet downloaded to cache, download asynchronously
        if (!artworkUrl.isNullOrBlank() && localArtPath.isBlank() && (artworkUrl.startsWith("http://") || artworkUrl.startsWith("https://"))) {
            scope.launch {
                try {
                    val targetFile = getArtFile(artworkUrl)
                    val req = Request.Builder().url(artworkUrl).build()
                    httpClient.newCall(req).execute().use { resp ->
                        if (resp.isSuccessful) {
                            val bytes = resp.body?.bytes()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val tmpFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                                tmpFile.writeBytes(bytes)
                                Files.move(
                                    tmpFile.toPath(),
                                    targetFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                                )
                                // Dispatch updated payload with the downloaded artwork path
                                if (lastArtworkUrl == artworkUrl) {
                                    sendUpdateCommand(cleanTitle, cleanArtist, cleanAlbum, isPlaying, targetFile.absolutePath)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("[WindowsSMTC] Could not cache album art: ${e.message}")
                }
            }
        }
    }

    private fun sendUpdateCommand(title: String, artist: String, album: String, isPlaying: Boolean, artPath: String) {
        try {
            writer?.println("UPDATE:$title|$artist|$album|$isPlaying|$artPath")
        } catch (_: Exception) {}
    }

    override fun setupSystemTray(
        onRestoreWindow: () -> Unit,
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onExitApp: () -> Unit
    ) {
        // Handled via AWT SystemTray if needed
    }

    override fun applyWindowMicaBackdrop(windowHandle: Long, isDarkTheme: Boolean) {
        // Reserved for DwmSetWindowAttribute Mica
    }

    override fun registerGlobalMediaKeys(
        onPlayPause: () -> Unit,
        onNext: () -> Unit,
        onPrevious: () -> Unit
    ) {
        // SMTC CommandManager handles global media keys natively
    }

    fun release() {
        try {
            writer?.println("QUIT")
            process?.destroyForcibly()
        } catch (_: Exception) {}
    }
}
