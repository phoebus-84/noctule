package ffnoctuleroom.ionic.io;

import static android.content.ContentValues.TAG;

import static java.util.logging.Logger.getLogger;
import static java.util.stream.Collectors.toMap;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.arthenica.ffmpegkit.AbiDetect;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.FFprobeSession;
import com.arthenica.ffmpegkit.Level;
import com.arthenica.ffmpegkit.LogRedirectionStrategy;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationJsonParser;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.Packages;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;
import com.arthenica.ffmpegkit.Signal;
import com.arthenica.ffmpegkit.Statistics;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.arthenica.ffmpegkit.FFmpegKit;

import org.json.JSONException;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@CapacitorPlugin(name = "FFmpeg")
public class FFmpegPlugin extends Plugin {

    public static final String LIBRARY_NAME = "ffmpeg-kit-react-native";
    public static final String PLATFORM_NAME = "android";

    // LOG CLASS
    public static final String KEY_LOG_SESSION_ID = "sessionId";
    public static final String KEY_LOG_LEVEL = "level";
    public static final String KEY_LOG_MESSAGE = "message";

    // STATISTICS CLASS
    public static final String KEY_STATISTICS_SESSION_ID = "sessionId";
    public static final String KEY_STATISTICS_VIDEO_FRAME_NUMBER = "videoFrameNumber";
    public static final String KEY_STATISTICS_VIDEO_FPS = "videoFps";
    public static final String KEY_STATISTICS_VIDEO_QUALITY = "videoQuality";
    public static final String KEY_STATISTICS_SIZE = "size";
    public static final String KEY_STATISTICS_TIME = "time";
    public static final String KEY_STATISTICS_BITRATE = "bitrate";
    public static final String KEY_STATISTICS_SPEED = "speed";

    // SESSION CLASS
    public static final String KEY_SESSION_ID = "sessionId";
    public static final String KEY_SESSION_CREATE_TIME = "createTime";
    public static final String KEY_SESSION_START_TIME = "startTime";
    public static final String KEY_SESSION_COMMAND = "command";
    public static final String KEY_SESSION_TYPE = "type";
    public static final String KEY_SESSION_MEDIA_INFORMATION = "mediaInformation";

    // SESSION TYPE
    public static final int SESSION_TYPE_FFMPEG = 1;
    public static final int SESSION_TYPE_FFPROBE = 2;
    public static final int SESSION_TYPE_MEDIA_INFORMATION = 3;

    // EVENTS
    public static final String EVENT_LOG_CALLBACK_EVENT = "FFmpegKitLogCallbackEvent";
    public static final String EVENT_STATISTICS_CALLBACK_EVENT = "FFmpegKitStatisticsCallbackEvent";
    public static final String EVENT_COMPLETE_CALLBACK_EVENT = "FFmpegKitCompleteCallbackEvent";

    // REQUEST CODES
    public static final int READABLE_REQUEST_CODE = 10000;
    public static final int WRITABLE_REQUEST_CODE = 20000;

    private static final int asyncWriteToPipeConcurrencyLimit = 10;

    private final AtomicBoolean logsEnabled;
    private final AtomicBoolean statisticsEnabled;
    private final ExecutorService asyncExecutorService;


    public FFmpegPlugin() {
        this.logsEnabled = new AtomicBoolean(false);
        this.statisticsEnabled = new AtomicBoolean(false);
        this.asyncExecutorService = Executors.newFixedThreadPool(asyncWriteToPipeConcurrencyLimit);

    }

    @PluginMethod()
    public void execute(PluginCall call) {
        Uri input = Uri.parse(call.getString("input"));
        String name = call.getString("outputName");
        String filter = call.getString("command");
        assert name != null;
        assert input != null;
        Context mContext = getContext();
        String path = PathUtils.getRealPath(mContext, input);
        final File videoFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                String.format("FFNoctuleRoom/%s", name));
        String command = String.format("-i %s %s %s", path, filter, videoFile.getAbsolutePath());
        FFmpegSession session = FFmpegKit.execute(command);
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            JSObject ret = new JSObject();
            ret.put("value", session.getAllLogs());
            call.resolve(ret);
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            JSObject ret = new JSObject();
            ret.put("value", "cancel");
            call.resolve(ret);
        } else {
            // FAILURE
            Log.d(TAG, String.format("Command failed with state %s and rc %s.%s", session.getState(),
                    session.getReturnCode(), session.getFailStackTrace()));
            JSObject ret = new JSObject();
            ret.put("value", session.getAllLogs());
            ret.put("stacktrace", session.getFailStackTrace());
            call.resolve(ret);
        }
    }

//    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
//    public void getStatistics(PluginCall call) {
//        call.setKeepAlive(true);
//        int id = call.getInt("sessionId");
//        List<Session> sessions = FFmpegKitConfig.getSessions();
//        Session session = sessions.get(id);
//        FFmpegKitConfig.enableStatisticsCallback(new StatisticsCallback() {
//            @Override
//            public void apply(final Statistics newStatistics) {
//                JSObject ret = new JSObject();
//                ret.put("value", newStatistics);
//                call.resolve(ret);
//            }
//        });
//    }


    @PluginMethod
    public void addListener(PluginCall call) {
        String eventName = call.getString("eventName");
        Log.i(LIBRARY_NAME, String.format("Listener added for %s event.", eventName));
        call.resolve();
    }

    @PluginMethod
    public void removeListeners(PluginCall call) {
        int count = call.getInt("count", 0);
        // Implement logic to remove listeners based on count
        call.resolve();
    }



    @PluginMethod
    public void abstractSessionGetEndTime(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                Date endTime = session.getEndTime();
                if (endTime == null) {
                    call.resolve(null);
                } else {
                    call.resolve(endTime.getTime());
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void abstractSessionGetDuration(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                call.resolve((double) session.getDuration());
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void abstractSessionGetAllLogs(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        double waitTimeout = call.getDouble("waitTimeout", -1);

        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                int timeout = (isValidPositiveNumber(waitTimeout)) ? (int) waitTimeout : AbstractSession.DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT;
                List<com.arthenica.ffmpegkit.Log> allLogs = session.getAllLogs(timeout);
                call.resolve(toLogArray(allLogs));
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void abstractSessionGetLogs(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                List<com.arthenica.ffmpegkit.Log> allLogs = session.getLogs();
                call.resolve(toLogArray(allLogs));
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void abstractSessionGetReturnCode(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                ReturnCode returnCode = session.getReturnCode();
                if (returnCode == null) {
                    call.resolve(null);
                } else {
                    call.resolve(returnCode.getValue());
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void abstractSessionGetFailStackTrace(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                call.resolve(session.getFailStackTrace());
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void thereAreAsynchronousMessagesInTransmit(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                call.resolve(session.thereAreAsynchronousMessagesInTransmit());
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

// ArchDetect

    @PluginMethod
    public void getArch(PluginCall call) {
        call.resolve(AbiDetect.getAbi());
    }

// FFmpegSession

    @PluginMethod
    public void ffmpegSession(PluginCall call) {
        ReadableArray readableArray = call.getArray("readableArray");
        call.resolve(toMap(FFmpegSession.create(toArgumentsArray(readableArray), null, null, null, LogRedirectionStrategy.NEVER_PRINT_LOGS)));
    }

    @PluginMethod
    public void ffmpegSessionGetAllStatistics(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        double waitTimeout = call.getDouble("waitTimeout", -1);

        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFmpeg()) {
                    int timeout = (isValidPositiveNumber(waitTimeout)) ? (int) waitTimeout : AbstractSession.DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT;
                    List<Statistics> allStatistics = ((FFmpegSession) session).getAllStatistics(timeout);
                    call.resolve(toStatisticsArray(allStatistics));
                } else {
                    call.reject("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void ffmpegSessionGetStatistics(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFmpeg()) {
                    List<Statistics> statistics = ((FFmpegSession) session).getStatistics();
                    call.resolve(toStatisticsArray(statistics));
                } else {
                    call.reject("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

// FFprobeSession

    @PluginMethod
    public void ffprobeSession(PluginCall call) {
        ReadableArray readableArray = call.getArray("readableArray");
        call.resolve(toMap(FFprobeSession.create(toArgumentsArray(readableArray), null, null, LogRedirectionStrategy.NEVER_PRINT_LOGS)));
    }

// MediaInformationSession

    @PluginMethod
    public void mediaInformationSession(PluginCall call) {
        ReadableArray readableArray = call.getArray("readableArray");
        call.resolve(toMap(MediaInformationSession.create(toArgumentsArray(readableArray), null, null)));
    }

// MediaInformationJsonParser

    @PluginMethod
    public void mediaInformationJsonParserFrom(PluginCall call) {
        String ffprobeJsonOutput = call.getString("ffprobeJsonOutput");
        try {
            MediaInformation mediaInformation = MediaInformationJsonParser.fromWithError(ffprobeJsonOutput);
            call.resolve(toMap(mediaInformation));
        } catch (JSONException e) {
            getLogger().info("Parsing MediaInformation failed.", e);
            call.resolve(null);
        }
    }

    @PluginMethod
    public void mediaInformationJsonParserFromWithError(PluginCall call) {
        String ffprobeJsonOutput = call.getString("ffprobeJsonOutput");
        try {
            MediaInformation mediaInformation = MediaInformationJsonParser.fromWithError(ffprobeJsonOutput);
            call.resolve(toMap(mediaInformation));
        } catch (JSONException e) {
            getLogger().info("Parsing MediaInformation failed.", e);
            call.reject("PARSE_FAILED", "Parsing MediaInformation failed with JSON error.");
        }
    }

// FFmpegKitConfig

    @PluginMethod
    public void enableRedirection(PluginCall call) {
        enableLogs();
        enableStatistics();
        FFmpegKitConfig.enableRedirection();
        call.resolve();
    }

    @PluginMethod
    public void disableRedirection(PluginCall call) {
        FFmpegKitConfig.disableRedirection();
        call.resolve();
    }

    @PluginMethod
    public void enableLogs(PluginCall call) {
        enableLogs();
        call.resolve();
    }

    @PluginMethod
    public void disableLogs(PluginCall call) {
        disableLogs();
        call.resolve();
    }

    @PluginMethod
    public void enableStatistics(PluginCall call) {
        enableStatistics();
        call.resolve();
    }

    @PluginMethod
    public void disableStatistics(PluginCall call) {
        disableStatistics();
        call.resolve();
    }

    @PluginMethod
    public void setFontconfigConfigurationPath(PluginCall call) {
        String path = call.getString("path");
        FFmpegKitConfig.setFontconfigConfigurationPath(path);
        call.resolve();
    }

    @PluginMethod
    public void setFontDirectory(PluginCall call) {
        String fontDirectoryPath = call.getString("fontDirectoryPath");
        ReadableMap fontNameMap = call.getMap("fontNameMap");
        Context mContext = getContext();

        if (mContext != null) {
            FFmpegKitConfig.setFontDirectory(mContext, fontDirectoryPath, toMap(fontNameMap));
            call.resolve();
        } else {
            call.reject("INVALID_CONTEXT", "React context is not initialized.");
        }
    }

//    @PluginMethod
//    public void setFontDirectoryList(PluginCall call) {
//        ReadableArray readableArray = call.getArray("readableArray");
//        ReadableMap fontNameMap = call.getMap("fontNameMap");
//        ReactApplicationContext reactContext = getReactApplicationContext();
//
//        if (reactContext != null) {
//            FFmpegKitConfig.setFontDirectoryList(reactContext, Arrays.asList(toArgumentsArray(readableArray)), toMap(fontNameMap));
//            call.resolve();
//        } else {
//            call.reject("INVALID_CONTEXT", "React context is not initialized.");
//        }
//    }

    @PluginMethod
    public void registerNewFFmpegPipe(PluginCall call) {
        ReactApplicationContext reactContext = getReactApplicationContext();
        if (reactContext != null) {
            call.resolve(FFmpegKitConfig.registerNewFFmpegPipe(reactContext));
        } else {
            call.reject("INVALID_CONTEXT", "Context is not initialized.");
        }
    }

    @PluginMethod
    public void closeFFmpegPipe(PluginCall call) {
        String ffmpegPipePath = call.getString("ffmpegPipePath");
        FFmpegKitConfig.closeFFmpegPipe(ffmpegPipePath);
        call.resolve();
    }

    @PluginMethod
    public void getFFmpegVersion(PluginCall call) {
        call.resolve(FFmpegKitConfig.getFFmpegVersion());
    }

    @PluginMethod
    public void isLTSBuild(PluginCall call) {
        call.resolve(FFmpegKitConfig.isLTSBuild());
    }

    @PluginMethod
    public void getBuildDate(PluginCall call) {
        call.resolve(FFmpegKitConfig.getBuildDate());
    }

    @PluginMethod
    public void setEnvironmentVariable(PluginCall call) {
        String variableName = call.getString("variableName");
        String variableValue = call.getString("variableValue");
        FFmpegKitConfig.setEnvironmentVariable(variableName, variableValue);
        call.resolve();
    }

    @PluginMethod
    public void ignoreSignal(PluginCall call) {
        double signalValue = call.getDouble("signalValue");
        Signal signal = null;

        if (signalValue == Signal.SIGINT.getValue()) {
            signal = Signal.SIGINT;
        } else if (signalValue == Signal.SIGQUIT.getValue()) {
            signal = Signal.SIGQUIT;
        } else if (signalValue == Signal.SIGPIPE.getValue()) {
            signal = Signal.SIGPIPE;
        } else if (signalValue == Signal.SIGTERM.getValue()) {
            signal = Signal.SIGTERM;
        } else if (signalValue == Signal.SIGXCPU.getValue()) {
            signal = Signal.SIGXCPU;
        }

        if (signal != null) {
            FFmpegKitConfig.ignoreSignal(signal);
            call.resolve();
        } else {
            call.reject("INVALID_SIGNAL", "Signal value not supported.");
        }
    }

    @PluginMethod
    public void ffmpegSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFmpeg()) {
                    FFmpegSessionExecuteTask ffmpegSessionExecuteTask = new FFmpegSessionExecuteTask(
                            (FFmpegSession) session, call);
                    asyncExecutorService.submit(ffmpegSessionExecuteTask);
                } else {
                    call.reject("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void ffprobeSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFprobe()) {
                    FFprobeSessionExecuteTask ffprobeSessionExecuteTask = new FFprobeSessionExecuteTask(
                            (FFprobeSession) session, call);
                    asyncExecutorService.submit(ffprobeSessionExecuteTask);
                } else {
                    call.reject("NOT_FFPROBE_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void mediaInformationSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        double waitTimeout = call.getDouble("waitTimeout", -1);

        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isMediaInformation()) {
                    int timeout = (isValidPositiveNumber(waitTimeout)) ? (int) waitTimeout
                            : AbstractSession.DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT;
                    MediaInformationSessionExecuteTask mediaInformationSessionExecuteTask = new MediaInformationSessionExecuteTask(
                            (MediaInformationSession) session, timeout, call);
                    asyncExecutorService.submit(mediaInformationSessionExecuteTask);
                } else {
                    call.reject("NOT_MEDIA_INFORMATION_SESSION",
                            "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void asyncFFmpegSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFmpeg()) {
                    FFmpegKitConfig.asyncFFmpegExecute((FFmpegSession) session);
                    call.resolve();
                } else {
                    call.reject("NOT_FFMPEG_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void asyncFFprobeSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isFFprobe()) {
                    FFmpegKitConfig.asyncFFprobeExecute((FFprobeSession) session);
                    call.resolve();
                } else {
                    call.reject("NOT_FFPROBE_SESSION", "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void asyncMediaInformationSessionExecute(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        double waitTimeout = call.getDouble("waitTimeout", -1);

        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                if (session.isMediaInformation()) {
                    int timeout = (isValidPositiveNumber(waitTimeout)) ? (int) waitTimeout
                            : AbstractSession.DEFAULT_TIMEOUT_FOR_ASYNCHRONOUS_MESSAGES_IN_TRANSMIT;
                    FFmpegKitConfig.asyncGetMediaInformationExecute((MediaInformationSession) session, timeout);
                    call.resolve();
                } else {
                    call.reject("NOT_MEDIA_INFORMATION_SESSION",
                            "A session is found but it does not have the correct type.");
                }
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void getLogLevel(PluginCall call) {
        call.resolve(toInt(FFmpegKitConfig.getLogLevel()));
    }

    @PluginMethod
    public void setLogLevel(PluginCall call) {
        double level = call.getDouble("level");
        if (level != -1) {
            FFmpegKitConfig.setLogLevel(Level.from((int) level));
            call.resolve();
        } else {
            call.reject("INVALID_LEVEL", "Invalid level value.");
        }
    }

    @PluginMethod
    public void getSessionHistorySize(PluginCall call) {
        call.resolve(FFmpegKitConfig.getSessionHistorySize());
    }

    @PluginMethod
    public void setSessionHistorySize(PluginCall call) {
        double sessionHistorySize = call.getDouble("sessionHistorySize");
        if (sessionHistorySize != -1) {
            FFmpegKitConfig.setSessionHistorySize((int) sessionHistorySize);
            call.resolve();
        } else {
            call.reject("INVALID_SIZE", "Invalid session history size value.");
        }
    }

    @PluginMethod
    public void getSession(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            Session session = FFmpegKitConfig.getSession((long) sessionId);
            if (session == null) {
                call.reject("SESSION_NOT_FOUND", "Session not found.");
            } else {
                call.resolve(toMap(session));
            }
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void getLastSession(PluginCall call) {
        Session session = FFmpegKitConfig.getLastSession();
        call.resolve(toMap(session));
    }

    @PluginMethod
    public void getLastCompletedSession(PluginCall call) {
        Session session = FFmpegKitConfig.getLastCompletedSession();
        call.resolve(toMap(session));
    }

    @PluginMethod
    public void getSessions(PluginCall call) {
        call.resolve(toSessionArray(FFmpegKitConfig.getSessions()));
    }

    @PluginMethod
    public void clearSessions(PluginCall call) {
        FFmpegKitConfig.clearSessions();
        call.resolve();
    }

    @PluginMethod
    public void getSessionsByState(PluginCall call) {
        double sessionState = call.getDouble("sessionState");
        try {
            call.resolve(toSessionArray(FFmpegKitConfig.getSessionsByState(toSessionState((int) sessionState))));
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void getLogRedirectionStrategy(PluginCall call) {
        call.resolve(toInt(FFmpegKitConfig.getLogRedirectionStrategy()));
    }

    @PluginMethod
    public void setLogRedirectionStrategy(PluginCall call) {
        double logRedirectionStrategy = call.getDouble("logRedirectionStrategy");
        if (logRedirectionStrategy != -1) {
            FFmpegKitConfig.setLogRedirectionStrategy(toLogRedirectionStrategy((int) logRedirectionStrategy));
            call.resolve();
        } else {
            call.reject("INVALID_LOG_REDIRECTION_STRATEGY", "Invalid log redirection strategy value.");
        }
    }

    @PluginMethod
    public void messagesInTransmit(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        try {
            call.resolve(FFmpegKitConfig.messagesInTransmit((long) sessionId));
        } catch (Exception e) {
            call.reject("ERROR", e.getMessage());
        }
    }

    @PluginMethod
    public void getPlatform(PluginCall call) {
        call.resolve(PLATFORM_NAME);
    }

    @PluginMethod
    public void writeToPipe(PluginCall call) {
        String inputPath = call.getString("inputPath");
        String namedPipePath = call.getString("namedPipePath");

        WriteToPipeTask asyncTask = new WriteToPipeTask(inputPath, namedPipePath, call);
        asyncExecutorService.submit(asyncTask);
    }

    // @PluginMethod
    // public void selectDocument(PluginCall call) {
    //     boolean writable = call.getBoolean("writable");
    //     String title = call.getString("title");
    //     String type = call.getString("type");
    //     ReadableArray extraTypes = call.getArray("extraTypes");

    //     Intent intent;
    //     if (writable) {
    //         intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
    //         intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    //     } else {
    //         intent = new Intent(Intent.ACTION_GET_CONTENT);
    //         intent.addCategory(Intent.CATEGORY_OPENABLE);
    //         intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    //     }

    //     if (type != null) {
    //         intent.setType(type);
    //     } else {
    //         intent.setType("*/*");
    //     }

    //     if (title != null) {
    //         intent.putExtra(Intent.EXTRA_TITLE, title);
    //     }

    //     if (extraTypes != null) {
    //         intent.putExtra(Intent.EXTRA_MIME_TYPES, toArgumentsArray(extraTypes));
    //     }

    //     final ReactApplicationContext reactContext = getReactApplicationContext();
    //     if (reactContext != null) {
    //         final Activity currentActivity = reactContext.getCurrentActivity();

    //         if (currentActivity != null) {
    //             reactContext.addActivityEventListener(new BaseActivityEventListener() {
    //                 @Override
    //                 public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    //                     reactContext.removeActivityEventListener(this);

    //                     Log.d(LIBRARY_NAME, String.format("selectDocument using parameters writable: %s, type: %s, title: %s and extra types: %s completed with requestCode: %d, resultCode: %d, data: %s.", writable, type, title, extraTypes == null ? null : Arrays.toString(toArgumentsArray(extraTypes)), requestCode, resultCode, data == null ? null : data.toString()));

    //                     if (requestCode == READABLE_REQUEST_CODE || requestCode == WRITABLE_REQUEST_CODE) {
    //                         if (resultCode == Activity.RESULT_OK) {
    //                             if (data == null) {
    //                                 call.resolve();
    //                             } else {
    //                                 final Uri uri = data.getData();
    //                                 call.resolve(uri == null ? null : uri.toString());
    //                             }
    //                         } else {
    //                             call.reject("SELECT_CANCELLED", String.valueOf(resultCode));
    //                         }
    //                     } else {
    //                         super.onActivityResult(activity, requestCode, resultCode, data);
    //                     }
    //                 }
    //             });

    //             try {
    //                 currentActivity.startActivityForResult(intent, writable ? WRITABLE_REQUEST_CODE : READABLE_REQUEST_CODE);
    //             } catch (final Exception e) {
    //                 Log.i(LIBRARY_NAME, String.format("Failed to selectDocument using parameters writable: %s, type: %s, title: %s and extra types: %s!", writable, type, title, extraTypes == null ? null : Arrays.toString(toArgumentsArray(extraTypes))), e);
    //                 call.reject("SELECT_FAILED", e.getMessage());
    //             }
    //         } else {
    //             Log.w(LIBRARY_NAME, String.format("Cannot selectDocument using parameters writable: %s, type: %s, title: %s and extra types: %s. Current activity is null.", writable, type, title, extraTypes == null ? null : Arrays.toString(toArgumentsArray(extraTypes))));
    //             call.reject("INVALID_ACTIVITY", "Activity is null.");
    //         }
    //     } else {
    //         Log.w(LIBRARY_NAME, String.format("Cannot selectDocument using parameters writable: %s, type: %s, title: %s and extra types: %s. React context is null.", writable, type, title, extraTypes == null ? null : Arrays.toString(toArgumentsArray(extraTypes))));
    //         call.reject("INVALID_CONTEXT", "Context is null.");
    //     }
    // }

    @PluginMethod
    public void getSafParameter(PluginCall call) {
        String uriString = call.getString("uriString");
        String openMode = call.getString("openMode");
        final Context mContext = getContext();

        final Uri uri = Uri.parse(uriString);
        if (uri == null) {
            Log.w(LIBRARY_NAME, String.format("Cannot getSafParameter using parameters uriString: %s, openMode: %s. Uri string cannot be parsed.", uriString, openMode));
            call.reject("GET_SAF_PARAMETER_FAILED", "Uri string cannot be parsed.");
        } else {
            final String safParameter;
            safParameter = FFmpegKitConfig.getSafParameter(mContext, uri, openMode);

            Log.d(LIBRARY_NAME, String.format("getSafParameter using parameters uriString: %s, openMode: %s completed with saf parameter: %s.", uriString, openMode, safParameter));

            call.resolve(safParameter);
        }
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        FFmpegKit.cancel();
        call.resolve();
    }

    @PluginMethod
    public void cancelSession(PluginCall call) {
        double sessionId = call.getDouble("sessionId", -1);
        if (sessionId != -1) {
            FFmpegKit.cancel((long) sessionId);
        } else {
            FFmpegKit.cancel();
        }
        call.resolve();
    }

    @PluginMethod
    public void getFFmpegSessions(PluginCall call) {
        call.resolve(toSessionArray(FFmpegKit.listSessions()));
    }

    @PluginMethod
    public void getFFprobeSessions(PluginCall call) {
        call.resolve(toSessionArray(FFprobeKit.listFFprobeSessions()));
    }

    @PluginMethod
    public void getMediaInformationSessions(PluginCall call) {
        call.resolve(toSessionArray(FFprobeKit.listMediaInformationSessions()));
    }

    @PluginMethod
    public void getMediaInformation(PluginCall call) {
        double sessionId = call.getDouble("sessionId");
        final Session session = FFmpegKitConfig.getSession((long) sessionId);
        if (session == null) {
            call.reject("SESSION_NOT_FOUND", "Session not found.");
        } else {
            if (session.isMediaInformation()) {
                final MediaInformationSession mediaInformationSession = (MediaInformationSession) session;
                final MediaInformation mediaInformation = mediaInformationSession.getMediaInformation();
                if (mediaInformation != null) {
                    call.resolve(toMap(mediaInformation));
                } else {
                    call.resolve(null);
                }
            } else {
                call.reject("NOT_MEDIA_INFORMATION_SESSION", "A session is found but it does not have the correct type.");
            }
        }
    }

    @PluginMethod
    public void getPackageName(PluginCall call) {
        call.resolve(Packages.getPackageName());
    }

    @PluginMethod
    public void getExternalLibraries(PluginCall call) {
        call.resolve(toStringArray(Packages.getExternalLibraries()));
    }

    @PluginMethod
    public void uninit(PluginCall call) {
        this.asyncExecutorService.shutdown();
        call.resolve();
    }



    // Helper method to convert List<com.arthenica.ffmpegkit.Log> to JSObject[]
    protected void enableLogs() {
        logsEnabled.compareAndSet(false, true);
    }

    protected void disableLogs() {
        logsEnabled.compareAndSet(true, false);
    }

    protected void enableStatistics() {
        statisticsEnabled.compareAndSet(false, true);
    }

    protected void disableStatistics() {
        statisticsEnabled.compareAndSet(true, false);
    }

    protected static int toInt(final Level level) {
        return (level == null) ? Level.AV_LOG_TRACE.getValue() : level.getValue();
    }

    protected static WritableMap toMap(final Session session) {
        if (session == null) {
            return null;
        }

        final WritableMap sessionMap = Arguments.createMap();

        sessionMap.putDouble(KEY_SESSION_ID, session.getSessionId());
        sessionMap.putDouble(KEY_SESSION_CREATE_TIME, toLong(session.getCreateTime()));
        sessionMap.putDouble(KEY_SESSION_START_TIME, toLong(session.getStartTime()));
        sessionMap.putString(KEY_SESSION_COMMAND, session.getCommand());

        if (session.isFFmpeg()) {
            sessionMap.putDouble(KEY_SESSION_TYPE, SESSION_TYPE_FFMPEG);
        } else if (session.isFFprobe()) {
            sessionMap.putDouble(KEY_SESSION_TYPE, SESSION_TYPE_FFPROBE);
        } else if (session.isMediaInformation()) {
            final MediaInformationSession mediaInformationSession = (MediaInformationSession) session;
            final MediaInformation mediaInformation = mediaInformationSession.getMediaInformation();
            if (mediaInformation != null) {
                sessionMap.putMap(KEY_SESSION_MEDIA_INFORMATION, toMap(mediaInformation));
            }
            sessionMap.putDouble(KEY_SESSION_TYPE, SESSION_TYPE_MEDIA_INFORMATION);
        }

        return sessionMap;
    }

    protected static long toLong(final Date date) {
        if (date != null) {
            return date.getTime();
        } else {
            return 0;
        }
    }

    protected static int toInt(final LogRedirectionStrategy logRedirectionStrategy) {
        switch (logRedirectionStrategy) {
            case ALWAYS_PRINT_LOGS:
                return 0;
            case PRINT_LOGS_WHEN_NO_CALLBACKS_DEFINED:
                return 1;
            case PRINT_LOGS_WHEN_GLOBAL_CALLBACK_NOT_DEFINED:
                return 2;
            case PRINT_LOGS_WHEN_SESSION_CALLBACK_NOT_DEFINED:
                return 3;
            case NEVER_PRINT_LOGS:
            default:
                return 4;
        }
    }

    protected static LogRedirectionStrategy toLogRedirectionStrategy(final int value) {
        switch (value) {
            case 0:
                return LogRedirectionStrategy.ALWAYS_PRINT_LOGS;
            case 1:
                return LogRedirectionStrategy.PRINT_LOGS_WHEN_NO_CALLBACKS_DEFINED;
            case 2:
                return LogRedirectionStrategy.PRINT_LOGS_WHEN_GLOBAL_CALLBACK_NOT_DEFINED;
            case 3:
                return LogRedirectionStrategy.PRINT_LOGS_WHEN_SESSION_CALLBACK_NOT_DEFINED;
            case 4:
            default:
                return LogRedirectionStrategy.NEVER_PRINT_LOGS;
        }
    }

    protected static SessionState toSessionState(final int value) {
        switch (value) {
            case 0:
                return SessionState.CREATED;
            case 1:
                return SessionState.RUNNING;
            case 2:
                return SessionState.FAILED;
            case 3:
            default:
                return SessionState.COMPLETED;
        }
    }

    protected static WritableArray toStringArray(final List<String> list) {
        final WritableArray array = Arguments.createArray();

        if (list != null) {
            for (String item : list) {
                array.pushString(item);
            }
        }

        return array;
    }

    protected static Map<String, String> toMap(final ReadableMap readableMap) {
        final Map<String, String> map = new HashMap<>();

        if (readableMap != null) {
            final ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
            while (iterator.hasNextKey()) {
                final String key = iterator.nextKey();
                final ReadableType type = readableMap.getType(key);

                if (type == ReadableType.String) {
                    map.put(key, readableMap.getString(key));
                }
            }
        }

        return map;
    }

    protected static WritableMap toMap(final com.arthenica.ffmpegkit.Log log) {
        final WritableMap logMap = Arguments.createMap();

        logMap.putDouble(KEY_LOG_SESSION_ID, log.getSessionId());
        logMap.putDouble(KEY_LOG_LEVEL, toInt(log.getLevel()));
        logMap.putString(KEY_LOG_MESSAGE, log.getMessage());

        return logMap;
    }

    protected static WritableMap toMap(final Statistics statistics) {
        final WritableMap statisticsMap = Arguments.createMap();

        if (statistics != null) {
            statisticsMap.putDouble(KEY_STATISTICS_SESSION_ID, statistics.getSessionId());
            statisticsMap.putDouble(KEY_STATISTICS_VIDEO_FRAME_NUMBER, statistics.getVideoFrameNumber());
            statisticsMap.putDouble(KEY_STATISTICS_VIDEO_FPS, statistics.getVideoFps());
            statisticsMap.putDouble(KEY_STATISTICS_VIDEO_QUALITY, statistics.getVideoQuality());
            statisticsMap.putDouble(KEY_STATISTICS_SIZE, statistics.getSize());
            statisticsMap.putDouble(KEY_STATISTICS_TIME, statistics.getTime());
            statisticsMap.putDouble(KEY_STATISTICS_BITRATE, statistics.getBitrate());
            statisticsMap.putDouble(KEY_STATISTICS_SPEED, statistics.getSpeed());
        }

        return statisticsMap;
    }

    protected static WritableMap toMap(final MediaInformation mediaInformation) {
        if (mediaInformation != null) {
            WritableMap map = Arguments.createMap();

            JSONObject allProperties = mediaInformation.getAllProperties();
            if (allProperties != null) {
                map = toMap(allProperties);
            }

            return map;
        } else {
            return null;
        }
    }

    protected static WritableMap toMap(final JSONObject jsonObject) {
        final WritableMap map = Arguments.createMap();

        if (jsonObject != null) {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.opt(key);
                if (value != null) {
                    if (value instanceof JSONArray) {
                        map.putArray(key, toList((JSONArray) value));
                    } else if (value instanceof JSONObject) {
                        map.putMap(key, toMap((JSONObject) value));
                    } else if (value instanceof String) {
                        map.putString(key, (String) value);
                    } else if (value instanceof Number) {
                        if (value instanceof Integer) {
                            map.putInt(key, (Integer) value);
                        } else {
                            map.putDouble(key, ((Number) value).doubleValue());
                        }
                    } else if (value instanceof Boolean) {
                        map.putBoolean(key, (Boolean) value);
                    } else {
                        Log.i(LIBRARY_NAME, String.format("Cannot map json key %s using value %s:%s", key, value.toString(), value.getClass().toString()));
                    }
                }
            }
        }

        return map;
    }

    protected static WritableArray toList(final JSONArray array) {
        final WritableArray list = Arguments.createArray();

        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value != null) {
                if (value instanceof JSONArray) {
                    list.pushArray(toList((JSONArray) value));
                } else if (value instanceof JSONObject) {
                    list.pushMap(toMap((JSONObject) value));
                } else if (value instanceof String) {
                    list.pushString((String) value);
                } else if (value instanceof Number) {
                    if (value instanceof Integer) {
                        list.pushInt((Integer) value);
                    } else {
                        list.pushDouble(((Number) value).doubleValue());
                    }
                } else if (value instanceof Boolean) {
                    list.pushBoolean((Boolean) value);
                } else {
                    Log.i(LIBRARY_NAME, String.format("Cannot map json value %s:%s", value.toString(), value.getClass().toString()));
                }
            }
        }

        return list;
    }

    protected static String[] toArgumentsArray(final ReadableArray readableArray) {
        final List<String> arguments = new ArrayList<>();
        for (int i = 0; i < readableArray.size(); i++) {
            final ReadableType type = readableArray.getType(i);

            if (type == ReadableType.String) {
                arguments.add(readableArray.getString(i));
            }
        }

        return arguments.toArray(new String[0]);
    }

    protected static WritableArray toSessionArray(final List<? extends Session> sessionList) {
        final WritableArray sessionArray = Arguments.createArray();

        for (int i = 0; i < sessionList.size(); i++) {
            sessionArray.pushMap(toMap(sessionList.get(i)));
        }

        return sessionArray;
    }

    protected static WritableArray toLogArray(final List<com.arthenica.ffmpegkit.Log> logList) {
        final WritableArray logArray = Arguments.createArray();

        for (int i = 0; i < logList.size(); i++) {
            logArray.pushMap(toMap(logList.get(i)));
        }

        return logArray;
    }

    protected static WritableArray toStatisticsArray(final List<com.arthenica.ffmpegkit.Statistics> statisticsList) {
        final WritableArray statisticsArray = Arguments.createArray();

        for (int i = 0; i < statisticsList.size(); i++) {
            statisticsArray.pushMap(toMap(statisticsList.get(i)));
        }

        return statisticsArray;
    }

    protected static boolean isValidPositiveNumber(final Double value) {
        return (value != null) && (value.intValue() >= 0);
    }



    // Additional helper methods as needed

}

