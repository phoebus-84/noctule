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
import com.arthenica.ffmpegkit.Statistics;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.arthenica.ffmpegkit.FFmpegKit;

import java.io.File;
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
}