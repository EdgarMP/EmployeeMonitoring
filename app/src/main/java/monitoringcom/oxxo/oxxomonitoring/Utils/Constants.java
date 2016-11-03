package monitoringcom.oxxo.oxxomonitoring.Utils;

/**
 * Created by 3104729 on 29/02/2016.
 */
public class Constants {

    /* LOCAL */
    public static final String PREFIX = "http://";
    public static final String IP_ADDRESS = "192.168.76.133";
    public static final String PORT = "8080";
    public static final String APP_CONTEXT = "FEMSACECON";

    public static final String LOCATION_SERVLET_NAME = "LocationServlet";

    public static final String LOCATION_SERVLET_URL = PREFIX + IP_ADDRESS + ":" + PORT + "/" + APP_CONTEXT + "/" + LOCATION_SERVLET_NAME;

    /* DEMO */
    public static final String PREFIX_DEMO = "http://";
    public static final String IP_ADDRESS_DEMO = "fcpocweb.cloudapp.net";
    public static final String APP_CONTEXT_DEMO = "FEMSACECON";
    public static final String LOCATION_SERVLET_URL_DEMO = PREFIX_DEMO + IP_ADDRESS_DEMO + "/" + APP_CONTEXT_DEMO + "/" + LOCATION_SERVLET_NAME;
    //Servlets


    //http://fcpocweb.cloudapp.net/FEMSACECON/LocationServlet




    //Location Updates
    public static final int LOCATION_UPDATES_MIN_TIME_IN_MILLISECONDS = 50000;
    public static final int LOCATION_UPDATES_MIN_DISTANCE_IN_METERS = 300;
}
