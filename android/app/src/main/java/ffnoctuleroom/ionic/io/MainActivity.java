package ffnoctuleroom.ionic.io;

import android.os.Bundle;
import android.os.Environment;

import com.getcapacitor.BridgeActivity;

import java.io.File;


public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FFmpegPlugin.class);
        registerPlugin(TEE.class);
        super.onCreate(savedInstanceState);

        final File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "FFNoctuleRoom");

        if (!f.exists()) {
            f.mkdir();
        }
    }
}