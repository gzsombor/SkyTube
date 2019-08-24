package free.rm.skytube.app;

import java.io.PrintWriter;
import java.io.StringWriter;

import free.rm.skytube.businessobjects.Logger;

public class Debug {

    public static String caller() {
        StringWriter s = new StringWriter();
        new Exception("Stack trace").printStackTrace(new PrintWriter(s));
        return s.toString();
    }

    public static void logCaller(Object obj) {
	Logger.i(obj, "called from " + caller());
    }

    public static void logCaller(Object obj, String message) {
	Logger.i(obj, message + " - called from " + caller());
    }
}
