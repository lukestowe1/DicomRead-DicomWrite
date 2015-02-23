import com.pixelmed.display.SingleImagePanel;
import com.pixelmed.display.SourceImage;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Luke on 23/02/15.
 */
public class Test {
    public static void main(String[] argv) throws Exception {
        JFrame frame = new JFrame();
        String test = "/Users/Luke/Desktop/Output.dcm";
        SourceImage img = new SourceImage(test);
        SingleImagePanel single = new SingleImagePanel(img);
        frame.add(single);
        frame.setBackground(Color.BLACK);
        frame.setSize(img.getWidth(),img.getHeight());
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JFrame frame2 = new JFrame();
        SourceImage img2 = new SourceImage("/Users/Luke/IdeaProjects/DicomRead-DicomWrite/Phantom_Artifact.dcm");
        SingleImagePanel single2 = new SingleImagePanel(img2);
        frame2.add(single2);
        frame2.setBackground(Color.BLACK);
        frame2.setSize(img.getWidth(),img.getHeight());
        frame2.setVisible(true);
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}

