import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class Grabber {
    //Variablen
    private String baseURL = "https://www.ask.fm/", profileHandle, profileURL;
    private String mainPath;
    private ResourceBundle rb = ResourceBundle.getBundle("grabber");
    private String  fS = System.getProperty("file.separator"),
                    lS = System.getProperty("line.separator");

    /*
     * Grabber class. Takes a profile handle and then grabs the whole profile
     *
     * @paramas profileURL
     */
    public Grabber(String profile, File path) {
        //Save profile handle
        this.profileHandle = profile;

        //Create URL to profile
        try {
            this.profileURL = "https://ask.fm/" + URLEncoder.encode(profile, StandardCharsets.UTF_8.name());
            log("Grabbing data from " + this.profileURL);
        } catch (IOException e) {
            e.printStackTrace();
            log(e.getMessage());
        }

        //Create working directory
        this.mainPath = path.getPath() + this.fS + profile + this.fS;
    }

    public boolean testDir() {
        File workingDirectory = new File(mainPath);
        if (workingDirectory.exists()) {
            return true;
        } else {
            workingDirectory.mkdirs();
            return false;
        }
    }

    public void grab() {
        //Grab content
        log("Grabbing content...");
        String content = getContent();

        log("\n------\n");

        //Grab posts
        log("Grabbing posts...");
        getPosts(content);

        //Grab avatar(s)
        log("Grabbing avatar(s)...");
        getProfilePictures();

        //Grab background image
        log("Grabbing background image...");
        getBackgroundImage();
    }

    private String getContent() {
        //Get all posts (q&a) and save them to content
        String content = "", rawContent;
        boolean endOfPage = false;
        do {
            //Open a URL connection
            URLHandler uh = new URLHandler();
            uh.connect(this.profileURL);

            //Read HTML
            rawContent = uh.read();

            //Cut anything but the posts/q&a
            content += rawContent.substring(rawContent.indexOf("<div class=\"item-pager\">"), rawContent.indexOf("<div class=\"emptyResults\""));

            //Check if the end of the profiles posts has been reached.
            //Will be identified by the presence of a divider that would contain an URL to the next 'page'
            String check = "<a class=\"item-page-next\"";
            if (content.contains(check)) {
                //Retrieve the URL to the next page
                String nextPage = content.substring(content.lastIndexOf(check));
                nextPage = nextPage.substring(nextPage.indexOf("href=\"/") + 7, nextPage.indexOf("\">Next</a>"));
                log("Grabbing additional data from " + nextPage);

                //Check if next page is not already the current page
                if (this.profileURL.contains(nextPage)) {
                    endOfPage = true;
                    exportToFile(content);
                    //exportToFile(rawContent, true);
                } else {
                    this.profileURL = this.baseURL + nextPage;
                }
            } else {
                endOfPage = true;
                exportToFile(content);
                //exportToFile(rawContent, true);
            }
        } while (!endOfPage);

        return content;
    }

    public void getPosts(String content) {
        //Get every individual post
        StringBuilder sb = new StringBuilder();
        String check = "<article class=\"item streamItem streamItem-answer \">";
        boolean endOfPage = false;
        do {
            //Check if 'check' does appear in content
            if (content.contains(check)) {
                //'Free'/Isolate the top-most post
                content = content.substring(content.indexOf(check) + check.length());
                String post = content.substring(0, content.indexOf("</article>"));

                //Get question
                String question = post.substring(post.indexOf("<h2>") + 4, post.indexOf("</h2>"));

                //Check if a URL is in the question
                do if (question.contains("<a") && question.contains("</a>")) {
                    String link = question.substring(question.indexOf("<a"));
                    link = link.substring(link.indexOf(">") + 1);
                    link = link.substring(0, link.lastIndexOf("</a>"));

                    String  start   = question.substring(0, question.indexOf("<a")),
                            end		= question.substring(question.indexOf("</a>") + 4);

                    question = start + link + end;
                } while(question.contains("<a") && question.contains("</a>"));

                //Get properties
                String  properties = post.substring(post.indexOf("<div class=\"streamItem_properties\">") + 35);
                properties = properties.substring(0, post.indexOf("</div>"));

                //Get and format date
                String  date = properties.substring(properties.indexOf("<time datetime=\"") + 16);
                date = date.substring(0, properties.indexOf("\">"));
                date = date.replaceAll("T", "");
                Date postDate = null;
                try {
                    postDate = new SimpleDateFormat("Y-M-dH:m:s").parse(date);
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                    log(e.getMessage());
                }

                //Get post number
                String  postNumber = properties.substring(properties.indexOf("href=\"/"));
                postNumber = postNumber.substring(postNumber.indexOf("/answers/") + 9, postNumber.indexOf("\">"));
                log("Post number: " + postNumber);

                //Check if answer contains an actual answer
                String answer = "";
                if (post.contains("streamItem_content")) {
                    answer = post.substring(post.indexOf("streamItem_content\">") + 20, post.indexOf("<p class=\"readMore\">"));
                }

                //Check if answer contains an image
                File image = null;
                if (post.contains("streamItem_visual")) {
                    //Get the image's URL
                    String  pictureURL = post.substring(post.indexOf("<img"));
                    pictureURL = pictureURL.substring(pictureURL.indexOf(" src=\"") + 6);
                    pictureURL = pictureURL.substring(0, pictureURL.indexOf("\""));

                    //Get content type
                    String fileType = pictureURL;
                    if (fileType.contains("?")) {
                        fileType = fileType.substring(0, fileType.indexOf("?"));
                    }
                    fileType = fileType.substring(fileType.lastIndexOf("."));

                    //Create file and save image
                    String imagePath = mainPath + "images" + this.fS;
                    new File(imagePath).mkdirs();
                    image = new File(imagePath + postNumber + fileType);
                    saveImage(pictureURL, image);
                }

                //Build text for file
                //Date
                if (postDate != null) {
                    sb.append(postDate).append(this.lS);
                }

                //Question
                sb.append("Q:\t").append(question);

                //Answer
                if (!answer.equals("")) {
                    sb.append(this.lS).append("A:\t").append(answer);
                }

                //Image
                if (image != null) {
                    sb.append(this.lS).append("I:\t").append(image.getPath());
                }

                //Spacing
                sb.append(this.lS).append(this.lS);
            } else {
                endOfPage = true;
            }
        } while (!endOfPage);

        try {
            //Append credits
            sb.append(this.rb.getString("credit"));

            //Create File
            File qa = new File(mainPath + "answers.txt");
            qa.createNewFile();

            //Write content to file
            FileWriter fw = new FileWriter(qa);
            fw.write(sb.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getProfilePictures() {
        //Find and download the profile picture(s)
        boolean endOfPage = false;
        int counter = 1;
        while(!endOfPage) {
            //Create URL
            String avatarURL = this.baseURL + this.profileHandle + "/avatar/" + counter;

            //Get HTML
            URLHandler uh = new URLHandler();
            uh.connect(avatarURL);
            String content = uh.read();

            //Check if no more avatars are found
            if (!content.equals("")) {
                //Get the avatars URL
                content = content.substring(content.indexOf("<img src=\"") + 10);
                content = content.substring(0, content.indexOf("\" />"));
                log("URL of Avatar " + counter + ": " + content);

                //Create directory for avatars
                File output = new File(mainPath + "avatar");
                output.mkdirs();
                output = new File(output.getPath() + this.fS + counter + ".jpg");

                //Save image
                saveImage(content, output);
                counter++;
            } else {
                endOfPage = true;
            }
        }
    }

    private void getBackgroundImage() {
        //Create URL
        String url = this.baseURL + this.profileHandle;

        //Get content
        URLHandler uh = new URLHandler();
        uh.connect(url);
        String content = uh.read();

        //Get URL of background image
        String  backgroundURL = content.substring(content.indexOf("<div class=\"backgroundWrap withImage\">") + 38);
                backgroundURL = backgroundURL.substring(0, backgroundURL.indexOf("</div>"));
                backgroundURL = backgroundURL.substring(backgroundURL.indexOf("src=\"") + 5);
                backgroundURL = backgroundURL.substring(0, backgroundURL.indexOf("\""));
        log("URL of background image: " + backgroundURL);

        //Create new directory and file
        File backgroundImg = new File(mainPath + "background");
        backgroundImg.mkdirs();
        backgroundImg = new File(backgroundImg.getPath() + this.fS + "background.jpg");

        //Save the image
        saveImage(backgroundURL, backgroundImg);
    }

    private void exportToFile(String content) {
        String name = "content.txt";

        //Datei erstellen
        File f = new File(mainPath + name);
        try {
            f.createNewFile();

            FileWriter fr = new FileWriter(f);
            fr.write(content);
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveImage(String picture, File image) {
        //Save image of given URL to given file
        try {
            image.createNewFile();

            URL url = new URL(picture);
            InputStream is = url.openStream();
            OutputStream os = new FileOutputStream(image);

            byte[] b = new byte[2048];
            int length;

            while ((length = is.read(b)) != -1) {
                os.write(b, 0, length);
            }

            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        //GrabberGUI.log;
        System.out.println(message);
    }
}