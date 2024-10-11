package sandipchitale.portmon;

import com.intellij.ide.util.PropertiesComponent;

public class PortmonSettings {
    private static final String PORTMON_PORTS = "sandipchitale.portmon.ports";
    private static final String PORTMON_PORTS_DEFAULT_VALUE = "";

    private static final String CLOSE_WAIT = "sandipchitale.portmon.CLOSE_WAIT";
    private static final String ESTABLISHED = "sandipchitale.portmon.ESTABLISHED";
    private static final String LISTENING = "sandipchitale.portmon.LISTENING";
    private static final String TIME_WAIT = "sandipchitale.portmon.TIME_WAIT";


    static String getPorts() {
        return PropertiesComponent.getInstance().getValue(PORTMON_PORTS, PORTMON_PORTS_DEFAULT_VALUE);
    }

    static void setPorts(String ports) {
        PropertiesComponent.getInstance().setValue(PORTMON_PORTS, ports);
    }

    static String resetPorts() {
        PropertiesComponent.getInstance().setValue(PORTMON_PORTS, PORTMON_PORTS_DEFAULT_VALUE);
        return PORTMON_PORTS_DEFAULT_VALUE;
    }

    static boolean isCloseWait() {
        return PropertiesComponent.getInstance().getBoolean(CLOSE_WAIT, false);
    }

    static void setCloseWait(boolean closeWait) {
        PropertiesComponent.getInstance().setValue(CLOSE_WAIT, closeWait);
    }

    static boolean isEstablished() {
        return PropertiesComponent.getInstance().getBoolean(ESTABLISHED, false);
    }

    static void setEstablished(boolean established) {
        PropertiesComponent.getInstance().setValue(ESTABLISHED, established);
    }

    static boolean isListening() {
        return PropertiesComponent.getInstance().getBoolean(LISTENING, false);
    }

    static void setListening(boolean listening) {
        PropertiesComponent.getInstance().setValue(LISTENING, listening);
    }

    static boolean isTimeWait() {
        return PropertiesComponent.getInstance().getBoolean(TIME_WAIT, false);
    }

    static void setTimeWait(boolean timeWait) {
        PropertiesComponent.getInstance().setValue(TIME_WAIT, timeWait);
    }
}
