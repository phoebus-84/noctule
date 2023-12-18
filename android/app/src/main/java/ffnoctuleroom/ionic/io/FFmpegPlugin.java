package ffnoctuleroom.ionic.io;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.arthenica.ffmpegkit.FFmpegKit;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;






@CapacitorPlugin(name = "FFmpeg")
public class FFmpegPlugin extends Plugin {

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
            ret.put("logs", session.getAllLogs());
            ret.put("statistics", session.getAllStatistics());
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
            ret.put("logs", session.getAllLogs());
            ret.put("stacktrace", session.getFailStackTrace());
            call.resolve(ret);
        }
    }
    @PluginMethod()
    public void getSession(PluginCall call)  {
        int sessionId = call.getInt("sessionId");
        Session session = FFmpegKitConfig.getSession(sessionId);
        JSObject ret = new JSObject();
        ret.put("logs", session.getAllLogs());
        ret.put("command", session.getCommand());
        ret.put("arguments", session.getArguments());
        ret.put("startTime", session.getStartTime());
        ret.put("endTime", session.getEndTime());
        ret.put("duration", session.getDuration());
        call.resolve(ret);
    }
    @PluginMethod()
    public void getSessions(PluginCall call) {
        List<Session> sessionList = FFmpegKitConfig.getSessions();
        List<Object> mappedList = new ArrayList<>();
        for (Session session : sessionList) {
            long sessionId = session.getSessionId();
            Date endTime = session.getEndTime();
            String command = session.getCommand();
            SessionState state = session.getState();
            ReturnCode returnCode = session.getReturnCode();

            // Create a map for each session
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonSessionData = "";
            try {
                jsonSessionData = objectMapper.writeValueAsString(
                        new SessionData(sessionId, endTime, command, state, returnCode)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the JSON representation of the session to the list
            mappedList.add(jsonSessionData);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String json = objectMapper.writeValueAsString(mappedList);
            JSObject ret = new JSObject();
            ret.put("sessions", json);
            call.resolve(ret);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
class SessionData {
    private long sessionId;
    private Date endTime;
    private String command;
    private SessionState state;
    private ReturnCode returnCode;

    public SessionData(long sessionId, Date endTime, String command, SessionState state, ReturnCode returnCode) {
        this.sessionId = sessionId;
        this.endTime = endTime;
        this.command = command;
        this.state = state;
        this.returnCode = returnCode;
    }

    // Getters for each property
    public long getSessionId() {
        return sessionId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getCommand() {
        return command;
    }

    public SessionState getState() {
        return state;
    }

    public ReturnCode getReturnCode() {
        return returnCode;
    }
}
