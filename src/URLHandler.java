import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class URLHandler {

    /*
     * Takes a profile handle and checks if the user exists.
     *
     * @param profile
     */
    public static boolean profileExists(String profile) {
        String baseURL = "https://www.ask.fm/";
        String charset = StandardCharsets.UTF_8.name();

        try {
            //Adding profile handle to base URl after encoding it (not necessary but feels nice)
            profile = URLEncoder.encode(profile, charset);
            String url = baseURL + profile;

            //Build up a URL connection
            HttpURLConnection cn = (HttpURLConnection) new URL(url).openConnection();
            cn.setRequestMethod("POST");

            //Evaluate depending on response code
            int status = cn.getResponseCode();
            if (status >= 300) {
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private URLConnection cn;

    public void connect(String url) {
        try {
            //Connect to webpage
            this.cn = new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            log(e.getMessage());
        }
    }

    public String read() {
        try {
            //Scanner to 'read' the webpage line by line (read the HTML line by line)
            Scanner scanner = new Scanner(this.cn.getInputStream());
            scanner.useDelimiter("\\Z");
            String content = scanner.next();
            scanner.close();

            return content;
        } catch (IOException e) {
            e.printStackTrace();
            log(e.getMessage());
            return "";
        }
    }

    private void log(String message) {
        //GrabberGUI.log.append(message);
        System.out.println(message);
    }
}
