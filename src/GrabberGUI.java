import javax.swing.*;
import java.awt.*;
import java.io.File;

public class GrabberGUI extends JFrame {
    //GUI components
    private JTextField path_tf;
    private JButton path_btn;
    private JTextField url_tf;
    private JButton url_btn;
    private JTextField status;
    private JTextArea log;
    private JLabel path_lbl;
    private JLabel url_lbl;
    private JPanel contentPane;

    //Other variables
    private File path;

    public static void main(String[] args) {
        JFrame frame = new JFrame("GrabberGUI");
        frame.setContentPane(new GrabberGUI().contentPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public GrabberGUI() {
        //Set default starting directory
        setPath(System.getProperty("user.home") + System.getProperty("file.separator") + "ASKfmProfileGrabber");

        //Action Listener for path changing button
        path_btn.addActionListener(e -> {
            //Display a file chooser to let the user select a destination folder
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setMultiSelectionEnabled(false);

            //Show the actual file selection dialog
            switch (fc.showOpenDialog(null)) {
                case JFileChooser.APPROVE_OPTION:
                    //If selection has been performed save path to variable and
                    //update the path text field
                    setPath(fc.getSelectedFile());
                    break;
                case JFileChooser.CANCEL_OPTION:
                default:
                    break;
            }
        });

        //Action Listener for grabbing button
        url_btn.addActionListener(e -> {
            //Save the content of the url text field
            String profile = url_tf.getText();

            //This will first check if a path is selected and a username entered
            if (path != null && !profile.equals("")) {
                //Check if the profile exists
                if (URLHandler.profileExists(profile)) {
                    setStatus("Profile exists!", Color.GREEN);
                    Grabber gr = new Grabber(profile, path);
                    if (gr.testDir()) {
                        switch (JOptionPane.showOptionDialog(null, "The directory of the profile handle already seems to exist? Continue anyways?", "Directory already existing", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null)) {
                            case JOptionPane.YES_OPTION:
                                gr.grab();
                                break;
                            case JOptionPane.CANCEL_OPTION:
                            default:
                                setStatus("Directory already existed. Aborted by user", Color.ORANGE);
                                break;
                        }
                    } else {
                        gr.grab();
                    }
                } else {
                    setStatus("This profile handle does not seem to exist", Color.RED);
                    url_tf.grabFocus();
                }
            }
        });
    }

    private void setStatus(String txt, Color c) {
        this.status.setForeground(c);
        this.status.setText(txt);
    }

    private void setPath(File f) {
        this.path = f;
        this.path_tf.setText(f.getAbsolutePath());
    }

    private void setPath(String s) {
        File f = new File(s);
        this.path = f;
        this.path_tf.setText(f.getAbsolutePath());
    }
}