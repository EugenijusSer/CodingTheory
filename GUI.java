import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;

public class GUI {
    private JTextArea inputArea;
    private JPanel mainPanel;
    private JTextArea decoderArea;
    private JButton encodeButton;
    private JTextArea encoderArea;
    private JTextArea channelArea;
    private JButton channelButton;
    private JButton decodeButton;
    private JTextField probabilityField;
    private JTextField errorNumberField;
    private JTextField errorPositionField;
    private JRadioButton binaryRadioButton;
    private JRadioButton imageRadioButton;
    private JRadioButton textRadioButton;
    private JTextArea binaryOutputArea;
    private JTextArea nonCodedChannelArea;
    private JTextArea nonCodedOutputArea;
    private JLabel originalImageLabel;
    private JLabel nonCodedImageLabel;
    private JLabel codedImageLabel;

    //masyvai saugoti koduotus bitus
    private int encodedBits[];
    private int channelBits[];
    private int decodedBits[];
    private int imageBits[];
    private int nonCodedImageBits[];

    private Engine engine = new Engine();

    private GUI() {

        //mygtuku paspaudimu vykdomos funkcijos
        encodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encodeInput();
            }
        });
        channelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    channelInput();
                } catch (Exception ex) {
                    System.out.println(ex.toString());
                }
            }
        });
        decodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                decodeInput();
            }
        });
        imageRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder builder = new StringBuilder();
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select image");
                if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                    File image = fileChooser.getSelectedFile();
                    try {
                        //paveiksliukas yra konvertuojamas i baitus, o veliau i bitus
                        byte[] fileContent = Files.readAllBytes(image.toPath());
                        for (byte b : fileContent)
                            builder.append(Integer.toBinaryString(b & 255 | 256).substring(1));
                        imageBits = convertStringToBitArray(builder.toString());

                        //paveiksliuko dydis yra pakeiciamas kad tilptu i nurodytus matmenis
                        Image img = ImageIO.read(fileChooser.getSelectedFile());
                        img = img.getScaledInstance(300, 300, img.SCALE_DEFAULT);
                        ImageIcon icon = new ImageIcon(img);
                        originalImageLabel.setIcon(icon);
                    } catch (Exception exc) {
                        System.out.println(exc.toString());
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame gui = new JFrame("GUI");
        gui.setContentPane(new GUI().mainPanel);
        gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gui.pack();
        gui.setVisible(true);
    }

    //uzkoduoja naudotojo ivesti
    private void encodeInput() {

        //vykdo jei yra pasirinktas dvejetainis rezimas
        if (binaryRadioButton.isSelected()) {
            if (inputArea.getText().matches("^[01]+$")) {
                encodedBits = engine.encode(convertStringToBitArray(inputArea.getText()));
                encoderArea.setText(convertIntArrayToString(encodedBits));
            } else
                JOptionPane.showMessageDialog(null, "Input can only be 1 or 0", "Warning", JOptionPane.PLAIN_MESSAGE);
        }
        //vykdo jei yra pasirinktas teksto rezimas
        else if (textRadioButton.isSelected()) {
            int convertedText[] = convertTextToIntArray(inputArea.getText());
            binaryOutputArea.setText(convertIntArrayToString(convertedText));
            encodedBits = engine.encode(convertedText);
            encoderArea.setText(convertIntArrayToString(encodedBits));
        }
        //vykdo jei yra pasirinktas paveiksliuko rezimas
        else if (imageRadioButton.isSelected()) {
            encodedBits = engine.encode(imageBits);
            encoderArea.setText("Image encoded");
        }
    }

    //siuncia naudotojo ivesti kanalu
    private void channelInput() {
        double probability = Double.parseDouble(probabilityField.getText());
        channelBits = engine.sendThroughNoisyChannel(encodedBits, probability);

        if (imageRadioButton.isSelected()) {
            nonCodedImageBits = engine.sendThroughNoisyChannel(imageBits, probability);
            channelArea.setText("Image was sent through noisy channel");
        } else {
            channelArea.setText(convertIntArrayToString(channelBits));
            nonCodedChannelArea.setText(convertIntArrayToString(engine.sendThroughNoisyChannel(convertStringToBitArray(binaryOutputArea.getText()), probability)));
            findErrors();
        }
    }

    //dekoduoja vartotojo ivesti
    private void decodeInput() {
        if (imageRadioButton.isSelected()) {
            decoderArea.setText("");
            decodedBits = engine.decode(channelBits);
            nonCodedImageLabel.setIcon(convertToImage(nonCodedImageBits));
            codedImageLabel.setIcon(convertToImage(decodedBits));
            decoderArea.setText("Image decoded\n\n" + decoderArea.getText());
        } else {
            if (channelArea.getText().matches("^[01]+$")) {
                decodedBits = engine.decode(convertStringToBitArray(channelArea.getText()));

                if (binaryRadioButton.isSelected())
                    decoderArea.setText(convertIntArrayToString(decodedBits));
                else if (textRadioButton.isSelected()) {
                    decoderArea.setText(convertIntArrayToText(decodedBits));
                    nonCodedOutputArea.setText(convertIntArrayToText(convertStringToBitArray(nonCodedChannelArea.getText())));
                }
            } else
                JOptionPane.showMessageDialog(null, "Input can only be 1 or 0", "Warning", JOptionPane.PLAIN_MESSAGE);
        }
    }

    //konvertuoja bitu seka i paveiksliuka (ima bitu masyva ir grazina paveiksliuka)
    private ImageIcon convertToImage(int[] bits) {
        ArrayList<Integer> arrayList = new ArrayList<>();

        //konvertuoja is bitu i baitus
        for (String str : convertIntArrayToString(bits).split("(?<=\\G.{8})"))
            arrayList.add(Integer.parseInt(str, 2));

        Iterator<Integer> iterator = arrayList.iterator();

        byte[] imageInByte = new byte[arrayList.size()];
        int index = 0;
        while (iterator.hasNext()) {
            Integer i = iterator.next();
            imageInByte[index] = i.byteValue();
            index++;
        }
        //konvertuoja is baitu i paveiksliuka
        InputStream in = new ByteArrayInputStream(imageInByte);
        ImageIcon icon = null;
        try {
            Image bImageFromConvert = ImageIO.read(in);
            bImageFromConvert = bImageFromConvert.getScaledInstance(300, 300, bImageFromConvert.SCALE_DEFAULT);
            icon = new ImageIcon(bImageFromConvert);
        } catch (Exception except) {
            System.out.println(except.toString());
            decoderArea.setText("Unable to display one or more images due to header info corruption");
        }
        return icon;
    }

    //konvertuoja tekstine bitu israiska i bitu masyva (ima tekstine bitu masyvo israiska ir grazina bitu masyva)
    private int[] convertStringToBitArray(String string) {
        int bits[] = new int[string.length()];
        for (int i = 0; i < string.length(); i++) {
            bits[i] = Character.getNumericValue(string.charAt(i));
        }
        return bits;
    }

    //konvertuoja bitu masyva i tekstine bitu masyvo israiska (ima bitu masyva ir grazina tekstine bitu masyvo israiska)
    private String convertIntArrayToString(int array[]) {
        StringBuilder builder = new StringBuilder();
        for (int value : array) {
            builder.append(value);
        }
        return builder.toString();
    }

    //konvertuoja teksta i bitu seka (ima teksta ir grazina bitu seka)
    private int[] convertTextToIntArray(String text) {
        String binary = "0";
        binary += new BigInteger(text.getBytes()).toString(2);
        return convertStringToBitArray(binary);
    }

    //konvertuoja bitu seka i teksta (ima bitu seka ir grazina teksta)
    private String convertIntArrayToText(int bits[]) {
        String binary = convertIntArrayToString(bits);
        return new String(new BigInteger(binary, 2).toByteArray());
    }

    //suranda kiek buvo padaryta klaidu ir kokiose pozicijose jos buvo prasiuntus pro kanala
    private void findErrors() {
        errorPositionField.setText("");
        int errorNo = 0;
        for (int i = 0; i < encodedBits.length; i++) {
            if (encodedBits[i] != channelBits[i]) {
                errorNo++;
                errorPositionField.setText(errorPositionField.getText() + (i + 1) + ",");
            }
        }
        errorNumberField.setText(String.valueOf(errorNo));
        try {
            errorPositionField.setText("" + errorPositionField.getText().substring(0, errorPositionField.getText().length() - 1));
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(10, 8, new Insets(0, 0, 0, 0), -1, -1));
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(false);
        mainPanel.add(inputArea, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 3, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(500, 50), null, 0, false));
        decoderArea = new JTextArea();
        decoderArea.setEditable(false);
        decoderArea.setLineWrap(true);
        decoderArea.setText("");
        mainPanel.add(decoderArea, new com.intellij.uiDesigner.core.GridConstraints(8, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(150, 50), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setHorizontalAlignment(10);
        label1.setHorizontalTextPosition(11);
        label1.setText("Enter input:");
        mainPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(54, 16), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("  Decoder output:");
        mainPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(8, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(54, 16), null, 0, false));
        encoderArea = new JTextArea();
        encoderArea.setEditable(false);
        encoderArea.setLineWrap(true);
        mainPanel.add(encoderArea, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(150, 50), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Encoder output:");
        mainPanel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(54, 16), null, 0, false));
        channelArea = new JTextArea();
        channelArea.setLineWrap(true);
        mainPanel.add(channelArea, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 3, 4, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 100), new Dimension(150, 50), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Channel output:");
        mainPanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(54, 16), null, 0, false));
        encodeButton = new JButton();
        encodeButton.setText("Encode");
        mainPanel.add(encodeButton, new com.intellij.uiDesigner.core.GridConstraints(3, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        channelButton = new JButton();
        channelButton.setText("Channel");
        mainPanel.add(channelButton, new com.intellij.uiDesigner.core.GridConstraints(7, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decodeButton = new JButton();
        decodeButton.setText("Decode");
        mainPanel.add(decodeButton, new com.intellij.uiDesigner.core.GridConstraints(8, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Error probability");
        mainPanel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(5, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        probabilityField = new JTextField();
        probabilityField.setText("0");
        mainPanel.add(probabilityField, new com.intellij.uiDesigner.core.GridConstraints(6, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("No. of errors:");
        mainPanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        errorNumberField = new JTextField();
        errorNumberField.setEditable(false);
        mainPanel.add(errorNumberField, new com.intellij.uiDesigner.core.GridConstraints(4, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(53, 24), new Dimension(53, 24), new Dimension(53, 24), 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Errors in positions:");
        mainPanel.add(label7, new com.intellij.uiDesigner.core.GridConstraints(4, 3, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(120, 16), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new com.intellij.uiDesigner.core.GridConstraints(4, 4, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 48), new Dimension(-1, 48), null, 0, false));
        errorPositionField = new JTextField();
        errorPositionField.setEditable(false);
        scrollPane1.setViewportView(errorPositionField);
        binaryRadioButton = new JRadioButton();
        binaryRadioButton.setSelected(true);
        binaryRadioButton.setText("Binary");
        mainPanel.add(binaryRadioButton, new com.intellij.uiDesigner.core.GridConstraints(0, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        imageRadioButton = new JRadioButton();
        imageRadioButton.setText("Image");
        mainPanel.add(imageRadioButton, new com.intellij.uiDesigner.core.GridConstraints(2, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textRadioButton = new JRadioButton();
        textRadioButton.setText("Text");
        mainPanel.add(textRadioButton, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        binaryOutputArea = new JTextArea();
        binaryOutputArea.setEditable(false);
        binaryOutputArea.setLineWrap(true);
        binaryOutputArea.setVisible(true);
        mainPanel.add(binaryOutputArea, new com.intellij.uiDesigner.core.GridConstraints(3, 7, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(400, -1), new Dimension(150, 50), null, 0, false));
        nonCodedChannelArea = new JTextArea();
        nonCodedChannelArea.setEditable(false);
        nonCodedChannelArea.setLineWrap(true);
        mainPanel.add(nonCodedChannelArea, new com.intellij.uiDesigner.core.GridConstraints(5, 7, 3, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        nonCodedOutputArea = new JTextArea();
        nonCodedOutputArea.setEditable(false);
        nonCodedOutputArea.setLineWrap(true);
        mainPanel.add(nonCodedOutputArea, new com.intellij.uiDesigner.core.GridConstraints(8, 7, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Binary conversion:");
        mainPanel.add(label8, new com.intellij.uiDesigner.core.GridConstraints(3, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Non coded channel:");
        mainPanel.add(label9, new com.intellij.uiDesigner.core.GridConstraints(6, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Non coded output:");
        mainPanel.add(label10, new com.intellij.uiDesigner.core.GridConstraints(8, 6, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("Fields below are only used with text");
        mainPanel.add(label11, new com.intellij.uiDesigner.core.GridConstraints(2, 7, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(9, 0, 1, 8, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("Original image");
        panel1.add(label12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        originalImageLabel = new JLabel();
        originalImageLabel.setText("");
        panel1.add(originalImageLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 300), new Dimension(300, 300), new Dimension(300, 300), 0, false));
        codedImageLabel = new JLabel();
        codedImageLabel.setText("");
        panel1.add(codedImageLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 300), new Dimension(300, 300), new Dimension(300, 300), 0, false));
        nonCodedImageLabel = new JLabel();
        nonCodedImageLabel.setText("");
        panel1.add(nonCodedImageLabel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 300), new Dimension(300, 300), new Dimension(300, 300), 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Non coded image");
        panel1.add(label13, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Coded image");
        panel1.add(label14, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(binaryRadioButton);
        buttonGroup.add(imageRadioButton);
        buttonGroup.add(textRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}
