package com.hydra.services.tdc.TEMP;

import com.hydra.services.tdc.TEMP.device.tdc.TDCDataProcessor;
import com.hydra.services.tdc.TEMP.device.tdc.TDCParser;
import com.hydra.services.tdc.TEMP.device.tdc.adapters.GroundTDCDataAdapter;
import com.xeiam.xchart.Chart;
import com.xeiam.xchart.Histogram;
import com.xeiam.xchart.Series;
import com.xeiam.xchart.StyleManager;
import com.xeiam.xchart.XChartPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import org.jscience.mathematics.number.Complex;
import org.jscience.mathematics.vector.ComplexMatrix;
import org.jscience.mathematics.vector.ComplexVector;

/**
 *
 * @author Hwaipy
 */
public class AppFrame extends javax.swing.JFrame {

    private static final String SAMPLING_MODE_SAMPLING = "Sampling";
    private static final String SAMPLING_MODE_VISIBILITY = "Visibility";
    private static final String HISTOGRAM_MODE_BIN = "Bin";
    private static final String HISTOGRAM_MODE_PULSE = "Pulse";
    private final Preferences preferences = Preferences.userNodeForPackage(AppFrame.class);
    private final XChartPanel histogramChartPanel;
    private final Chart histogramChart;
    private final XChartPanel samplingChartPanel;
    private final Chart samplingChart;
    private final JTextField[] counterFields;
    private final JTextField[] delayFields;
    private final JCheckBox[] inputBoxs;
    private int indexTrigger = 1;
    private int indexSignal = 2;
    private double viewFrom = -100;
    private double viewTo = 100;
    private double pulsePeriod = 13;
    private double gateWidth = 4;
    private String matrixFile = ".";
    private ComplexMatrix matrix;
    private boolean doHistogram = true;
    private boolean doSampling = false;
    private String histogramMode = HISTOGRAM_MODE_BIN;
    private String samplingMode = SAMPLING_MODE_SAMPLING;
    private boolean samplingIntegrate = false;
    private boolean triggerMode = false;
    private int[] coincidences = new int[1 << 20];
    private int photonCounts = 0;
    private int photonCountsTime = 0;
    private double[][] visibilities = new double[256][4];
    private ArrayList<Double> permenents;
    private int[] modes;
    private int delayedPulse = 1;
    private long[] infiniteCounters = new long[16];

    /**
     * Creates new form AppFrame
     */
    public AppFrame() {
        timeEvents = new ArrayList[16];
        for (int i = 0; i < timeEvents.length; i++) {
            timeEvents[i] = new ArrayList<>();
        }
        initServer();
        histogramChart = new Chart(100, 400);
        histogramChart.addSeries("Histogram", new int[]{0}, new int[]{0});
        histogramChart.getStyleManager().setLegendVisible(false);
        histogramChart.getStyleManager().setMarkerSize(0);
        histogramChartPanel = new XChartPanel(histogramChart);
        samplingChart = new Chart(640, 360);
        samplingChart.getStyleManager().setChartType(StyleManager.ChartType.Bar);
        samplingChart.addSeries("SamplingExpect", new int[]{0}, new int[]{0});
        samplingChart.addSeries("SamplingExperiment", new int[]{0}, new int[]{0});
        samplingChart.getStyleManager().setLegendVisible(false);
        samplingChart.getStyleManager().setMarkerSize(0);
        samplingChartPanel = new XChartPanel(samplingChart);
        initComponents();
        counterFields = new JTextField[]{jTextFieldCount0, jTextFieldCount1, jTextFieldCount2, jTextFieldCount3, jTextFieldCount4, jTextFieldCount5, jTextFieldCount6, jTextFieldCount7, jTextFieldCount8, jTextFieldCount9, jTextFieldCount10, jTextFieldCount11, jTextFieldCount12, jTextFieldCount13, jTextFieldCount14, jTextFieldCount15};
        delayFields = new JTextField[]{jTextFieldDelay1, jTextFieldDelay2, jTextFieldDelay3, jTextFieldDelay4, jTextFieldDelay5, jTextFieldDelay6, jTextFieldDelay7, jTextFieldDelay8, jTextFieldDelay9, jTextFieldDelay10, jTextFieldDelay11, jTextFieldDelay12, jTextFieldDelay13, jTextFieldDelay14, jTextFieldDelay15, jTextFieldDelay16};
        inputBoxs = new JCheckBox[]{jCheckBoxInputs1, jCheckBoxInputs2, jCheckBoxInputs3, jCheckBoxInputs4, jCheckBoxInputs5, jCheckBoxInputs6, jCheckBoxInputs7, jCheckBoxInputs8, jCheckBoxInputs9, jCheckBoxInputs10, jCheckBoxInputs11, jCheckBoxInputs12, jCheckBoxInputs13, jCheckBoxInputs14, jCheckBoxInputs15};
        for (JCheckBox inputBox : inputBoxs) {
            inputBox.setVisible(false);
        }
        loadPreferences();
        connectPreferences();
        postInit();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jFileChooser1 = new javax.swing.JFileChooser();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = histogramChartPanel;
        jLabel1 = new javax.swing.JLabel();
        jTextFieldIndexTrigger = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldIndexSignal = new javax.swing.JTextField();
        jCheckBoxInt = new javax.swing.JCheckBox();
        jButtonClear = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldViewFrom = new javax.swing.JTextField();
        jLabel43 = new javax.swing.JLabel();
        jTextFieldViewTo = new javax.swing.JTextField();
        jLabel44 = new javax.swing.JLabel();
        jTextFieldReletiveCoins = new javax.swing.JTextField();
        jLabel49 = new javax.swing.JLabel();
        jLabel50 = new javax.swing.JLabel();
        jTextFieldVisibility = new javax.swing.JTextField();
        jCheckBoxPulseMode = new javax.swing.JCheckBox();
        jCheckBoxTriggerMode = new javax.swing.JCheckBox();
        jCheckBoxLog = new javax.swing.JCheckBox();
        jPanel2 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jTextFieldModeShower = new javax.swing.JTextField();
        jPanel8 = new javax.swing.JPanel();
        jCheckBoxInputs1 = new javax.swing.JCheckBox();
        jCheckBoxInputs2 = new javax.swing.JCheckBox();
        jCheckBoxInputs4 = new javax.swing.JCheckBox();
        jCheckBoxInputs3 = new javax.swing.JCheckBox();
        jCheckBoxInputs5 = new javax.swing.JCheckBox();
        jCheckBoxInputs6 = new javax.swing.JCheckBox();
        jCheckBoxInputs7 = new javax.swing.JCheckBox();
        jCheckBoxInputs8 = new javax.swing.JCheckBox();
        jCheckBoxInputs9 = new javax.swing.JCheckBox();
        jCheckBoxInputs10 = new javax.swing.JCheckBox();
        jCheckBoxInputs11 = new javax.swing.JCheckBox();
        jCheckBoxInputs12 = new javax.swing.JCheckBox();
        jCheckBoxInputs13 = new javax.swing.JCheckBox();
        jCheckBoxInputs14 = new javax.swing.JCheckBox();
        jCheckBoxInputs15 = new javax.swing.JCheckBox();
        jPanel23 = samplingChartPanel;
        jCheckBoxSamplingIntegrate = new javax.swing.JCheckBox();
        jTextFieldMatrixFile = new javax.swing.JTextField();
        jLabelMatrixLoadError = new javax.swing.JLabel();
        jLabel51 = new javax.swing.JLabel();
        jTextFieldSimilarity = new javax.swing.JTextField();
        jCheckBoxVisibility = new javax.swing.JCheckBox();
        jLabel52 = new javax.swing.JLabel();
        jTextFieldDelayedPulse = new javax.swing.JTextField();
        jTextFieldEff1 = new javax.swing.JTextField();
        jTextFieldEff2 = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldCount0 = new javax.swing.JTextField();
        jTextFieldDelay1 = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldCount1 = new javax.swing.JTextField();
        jTextFieldDelay2 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jTextFieldCount2 = new javax.swing.JTextField();
        jTextFieldDelay3 = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jTextFieldCount3 = new javax.swing.JTextField();
        jTextFieldDelay4 = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jTextFieldCount4 = new javax.swing.JTextField();
        jTextFieldDelay5 = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jLabel17 = new javax.swing.JLabel();
        jTextFieldCount5 = new javax.swing.JTextField();
        jTextFieldDelay6 = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        jPanel13 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jTextFieldCount6 = new javax.swing.JTextField();
        jTextFieldDelay7 = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        jPanel14 = new javax.swing.JPanel();
        jLabel21 = new javax.swing.JLabel();
        jTextFieldCount7 = new javax.swing.JTextField();
        jTextFieldDelay8 = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jPanel15 = new javax.swing.JPanel();
        jLabel23 = new javax.swing.JLabel();
        jTextFieldCount8 = new javax.swing.JTextField();
        jTextFieldDelay9 = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        jPanel16 = new javax.swing.JPanel();
        jLabel25 = new javax.swing.JLabel();
        jTextFieldCount9 = new javax.swing.JTextField();
        jTextFieldDelay10 = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jPanel17 = new javax.swing.JPanel();
        jLabel27 = new javax.swing.JLabel();
        jTextFieldCount10 = new javax.swing.JTextField();
        jTextFieldDelay11 = new javax.swing.JTextField();
        jLabel28 = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        jLabel29 = new javax.swing.JLabel();
        jTextFieldCount11 = new javax.swing.JTextField();
        jTextFieldDelay12 = new javax.swing.JTextField();
        jLabel30 = new javax.swing.JLabel();
        jPanel19 = new javax.swing.JPanel();
        jLabel31 = new javax.swing.JLabel();
        jTextFieldCount12 = new javax.swing.JTextField();
        jTextFieldDelay13 = new javax.swing.JTextField();
        jLabel32 = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jLabel33 = new javax.swing.JLabel();
        jTextFieldCount13 = new javax.swing.JTextField();
        jTextFieldDelay14 = new javax.swing.JTextField();
        jLabel34 = new javax.swing.JLabel();
        jPanel21 = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        jTextFieldCount14 = new javax.swing.JTextField();
        jTextFieldDelay15 = new javax.swing.JTextField();
        jLabel36 = new javax.swing.JLabel();
        jPanel22 = new javax.swing.JPanel();
        jLabel37 = new javax.swing.JLabel();
        jTextFieldCount15 = new javax.swing.JTextField();
        jTextFieldDelay16 = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jTextFieldTriggerFrequency = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextFieldTriggerPeriod = new javax.swing.JTextField();
        jLabel39 = new javax.swing.JLabel();
        jTextFieldTriggerPeriodPM = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        jLabel41 = new javax.swing.JLabel();
        jLabel42 = new javax.swing.JLabel();
        jLabel45 = new javax.swing.JLabel();
        jTextFieldPulsePeriod = new javax.swing.JTextField();
        jLabel46 = new javax.swing.JLabel();
        jLabel47 = new javax.swing.JLabel();
        jTextFieldGateWidth = new javax.swing.JTextField();
        jLabel48 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jTabbedPane1.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTabbedPane1StateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 734, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 413, Short.MAX_VALUE)
        );

        jLabel1.setText("Trigger");

        jTextFieldIndexTrigger.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldIndexTrigger.setText("1");

        jLabel2.setText("Signal");

        jTextFieldIndexSignal.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldIndexSignal.setText("5");

        jCheckBoxInt.setText("INT");
        jCheckBoxInt.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxIntItemStateChanged(evt);
            }
        });

        jButtonClear.setText("Clear");
        jButtonClear.setEnabled(false);
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jLabel7.setText("View from");

        jTextFieldViewFrom.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldViewFrom.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldViewFrom.setText("-100.000");

        jLabel43.setText("ns to");

        jTextFieldViewTo.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldViewTo.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldViewTo.setText("100.000");

        jLabel44.setText("ns");

        jTextFieldReletiveCoins.setEditable(false);
        jTextFieldReletiveCoins.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldReletiveCoins.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldReletiveCoins.setText("0.000, 0.000, 0.000");

        jLabel49.setText("Reletive Coins");

        jLabel50.setText("Visibility");

        jTextFieldVisibility.setEditable(false);
        jTextFieldVisibility.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldVisibility.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldVisibility.setText("0.000");

        jCheckBoxPulseMode.setText("PulseMode");

        jCheckBoxTriggerMode.setText("TriggerMode");
        jCheckBoxTriggerMode.setEnabled(false);

        jCheckBoxLog.setText("log");
        jCheckBoxLog.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxLogItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel49)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldReletiveCoins, javax.swing.GroupLayout.PREFERRED_SIZE, 195, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel50)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldVisibility, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldIndexTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldIndexSignal, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(26, 26, 26)
                                .addComponent(jCheckBoxInt)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonClear)
                                .addGap(36, 36, 36)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldViewFrom, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel43)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldViewTo, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel44)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jCheckBoxPulseMode)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxTriggerMode)
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBoxLog)
                        .addGap(0, 0, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldIndexTrigger, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldIndexSignal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jCheckBoxInt)
                    .addComponent(jButtonClear)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldViewFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel43)
                    .addComponent(jTextFieldViewTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel44))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxPulseMode)
                    .addComponent(jCheckBoxTriggerMode)
                    .addComponent(jCheckBoxLog))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldReletiveCoins, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel49)
                    .addComponent(jLabel50)
                    .addComponent(jTextFieldVisibility, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("2-Fold", jPanel1);

        jButton1.setText("Load Matrix");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jTextFieldModeShower.setEditable(false);
        jTextFieldModeShower.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Inputs"));

        jCheckBoxInputs1.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs1.setText(" 1");

        jCheckBoxInputs2.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs2.setText(" 2");

        jCheckBoxInputs4.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs4.setText(" 4");

        jCheckBoxInputs3.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs3.setText(" 3");

        jCheckBoxInputs5.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs5.setText(" 5");

        jCheckBoxInputs6.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs6.setText(" 6");

        jCheckBoxInputs7.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs7.setText(" 7");

        jCheckBoxInputs8.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs8.setText(" 8");

        jCheckBoxInputs9.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs9.setText(" 9");

        jCheckBoxInputs10.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs10.setText("10");

        jCheckBoxInputs11.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs11.setText("11");

        jCheckBoxInputs12.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs12.setText("12");

        jCheckBoxInputs13.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs13.setText("13");

        jCheckBoxInputs14.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs14.setText("14");

        jCheckBoxInputs15.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jCheckBoxInputs15.setText("15");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCheckBoxInputs1)
                    .addComponent(jCheckBoxInputs2)
                    .addComponent(jCheckBoxInputs3)
                    .addComponent(jCheckBoxInputs4)
                    .addComponent(jCheckBoxInputs5)
                    .addComponent(jCheckBoxInputs6)
                    .addComponent(jCheckBoxInputs7)
                    .addComponent(jCheckBoxInputs8)
                    .addComponent(jCheckBoxInputs9)
                    .addComponent(jCheckBoxInputs10)
                    .addComponent(jCheckBoxInputs11)
                    .addComponent(jCheckBoxInputs12)
                    .addComponent(jCheckBoxInputs13)
                    .addComponent(jCheckBoxInputs14)
                    .addComponent(jCheckBoxInputs15))
                .addGap(0, 9, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addComponent(jCheckBoxInputs1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs6)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCheckBoxInputs14)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 17, Short.MAX_VALUE)
                .addComponent(jCheckBoxInputs15)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 640, Short.MAX_VALUE)
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 360, Short.MAX_VALUE)
        );

        jCheckBoxSamplingIntegrate.setText("Int");
        jCheckBoxSamplingIntegrate.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jCheckBoxSamplingIntegrateItemStateChanged(evt);
            }
        });

        jTextFieldMatrixFile.setEditable(false);

        jLabelMatrixLoadError.setForeground(new java.awt.Color(255, 0, 0));
        jLabelMatrixLoadError.setText("●");

        jLabel51.setText("Similarity");

        jTextFieldSimilarity.setEditable(false);
        jTextFieldSimilarity.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldSimilarity.setText("0");

        jCheckBoxVisibility.setText("Visibility");
        jCheckBoxVisibility.setEnabled(false);

        jLabel52.setText("delay");

        jTextFieldDelayedPulse.setText("20");

        jTextFieldEff1.setText("0");

        jTextFieldEff2.setText("0");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldModeShower, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jCheckBoxSamplingIntegrate)
                                        .addGap(18, 18, 18)
                                        .addComponent(jCheckBoxVisibility)
                                        .addGap(26, 26, 26)
                                        .addComponent(jLabel52)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldDelayedPulse, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(91, 91, 91)
                                        .addComponent(jTextFieldEff1, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldEff2, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(jLabel51)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldSimilarity, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jButton1)
                        .addGap(2, 2, 2)
                        .addComponent(jTextFieldMatrixFile, javax.swing.GroupLayout.PREFERRED_SIZE, 575, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabelMatrixLoadError)))
                .addContainerGap(44, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jTextFieldMatrixFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelMatrixLoadError))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldModeShower, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(16, 16, 16)
                        .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCheckBoxSamplingIntegrate)
                            .addComponent(jCheckBoxVisibility)
                            .addComponent(jLabel52)
                            .addComponent(jTextFieldDelayedPulse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldEff1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldEff2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel51)
                            .addComponent(jTextFieldSimilarity, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 48, Short.MAX_VALUE))))
        );

        jLabelMatrixLoadError.setVisible(false);

        jTabbedPane1.addTab("Samping", jPanel2);

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Channels"));

        jLabel3.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel3.setText("CH 0");

        jTextFieldCount0.setEditable(false);
        jTextFieldCount0.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount0.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount0.setText("0");

        jTextFieldDelay1.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay1.setText("0.00");

        jLabel4.setText("ns");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount0, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay1, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount0, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel3)
                .addComponent(jTextFieldDelay1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel4))
        );

        jLabel5.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel5.setText("CH 1");

        jTextFieldCount1.setEditable(false);
        jTextFieldCount1.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount1.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount1.setText("0");

        jTextFieldDelay2.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay2.setText("0.00");

        jLabel6.setText("ns");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay2, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel5)
                .addComponent(jTextFieldDelay2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel6))
        );

        jLabel11.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel11.setText("CH 2");

        jTextFieldCount2.setEditable(false);
        jTextFieldCount2.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount2.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount2.setText("0");

        jTextFieldDelay3.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay3.setText("0.00");

        jLabel12.setText("ns");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount2, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay3, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel11)
                .addComponent(jTextFieldDelay3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel12))
        );

        jLabel13.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel13.setText("CH 3");

        jTextFieldCount3.setEditable(false);
        jTextFieldCount3.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount3.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount3.setText("0");

        jTextFieldDelay4.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay4.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay4.setText("0.00");

        jLabel14.setText("ns");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount3, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay4, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel14)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel13)
                .addComponent(jTextFieldDelay4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel14))
        );

        jLabel15.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel15.setText("CH 4");

        jTextFieldCount4.setEditable(false);
        jTextFieldCount4.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount4.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount4.setText("0");

        jTextFieldDelay5.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay5.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay5.setText("0.00");

        jLabel16.setText("ns");

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay5, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel16)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel15)
                .addComponent(jTextFieldDelay5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel16))
        );

        jLabel17.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel17.setText("CH 5");

        jTextFieldCount5.setEditable(false);
        jTextFieldCount5.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount5.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount5.setText("0");

        jTextFieldDelay6.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay6.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay6.setText("0.00");

        jLabel18.setText("ns");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount5, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay6, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel18)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel17)
                .addComponent(jTextFieldDelay6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel18))
        );

        jLabel19.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel19.setText("CH 6");

        jTextFieldCount6.setEditable(false);
        jTextFieldCount6.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount6.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount6.setText("0");

        jTextFieldDelay7.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay7.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay7.setText("0.00");

        jLabel20.setText("ns");

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount6, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay7, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel20)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel19)
                .addComponent(jTextFieldDelay7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel20))
        );

        jLabel21.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel21.setText("CH 7");

        jTextFieldCount7.setEditable(false);
        jTextFieldCount7.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount7.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount7.setText("0");

        jTextFieldDelay8.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay8.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay8.setText("0.00");

        jLabel22.setText("ns");

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount7, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay8, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel22)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel21)
                .addComponent(jTextFieldDelay8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel22))
        );

        jLabel23.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel23.setText("CH 8");

        jTextFieldCount8.setEditable(false);
        jTextFieldCount8.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount8.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount8.setText("0");

        jTextFieldDelay9.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay9.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay9.setText("0.00");

        jLabel24.setText("ns");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addComponent(jLabel23)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount8, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay9, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel24)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel23)
                .addComponent(jTextFieldDelay9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel24))
        );

        jLabel25.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel25.setText("CH 9");

        jTextFieldCount9.setEditable(false);
        jTextFieldCount9.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount9.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount9.setText("0");

        jTextFieldDelay10.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay10.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay10.setText("0.00");

        jLabel26.setText("ns");

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addComponent(jLabel25)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount9, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay10, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel26)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel25)
                .addComponent(jTextFieldDelay10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel26))
        );

        jLabel27.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel27.setText("CH10");

        jTextFieldCount10.setEditable(false);
        jTextFieldCount10.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount10.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount10.setText("0");

        jTextFieldDelay11.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay11.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay11.setText("0.00");

        jLabel28.setText("ns");

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addComponent(jLabel27)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount10, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay11, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel28)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel27)
                .addComponent(jTextFieldDelay11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel28))
        );

        jLabel29.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel29.setText("CH11");

        jTextFieldCount11.setEditable(false);
        jTextFieldCount11.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount11.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount11.setText("0");

        jTextFieldDelay12.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay12.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay12.setText("0.00");

        jLabel30.setText("ns");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addComponent(jLabel29)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount11, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay12, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel30)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel29)
                .addComponent(jTextFieldDelay12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel30))
        );

        jLabel31.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel31.setText("CH12");

        jTextFieldCount12.setEditable(false);
        jTextFieldCount12.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount12.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount12.setText("0");

        jTextFieldDelay13.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay13.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay13.setText("0.00");

        jLabel32.setText("ns");

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addComponent(jLabel31)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount12, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay13, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel32)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel31)
                .addComponent(jTextFieldDelay13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel32))
        );

        jLabel33.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel33.setText("CH13");

        jTextFieldCount13.setEditable(false);
        jTextFieldCount13.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount13.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount13.setText("0");

        jTextFieldDelay14.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay14.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay14.setText("0.00");

        jLabel34.setText("ns");

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addComponent(jLabel33)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount13, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay14, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel34)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel33)
                .addComponent(jTextFieldDelay14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel34))
        );

        jLabel35.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel35.setText("CH14");

        jTextFieldCount14.setEditable(false);
        jTextFieldCount14.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount14.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount14.setText("0");

        jTextFieldDelay15.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay15.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay15.setText("0.00");

        jLabel36.setText("ns");

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addComponent(jLabel35)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount14, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay15, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel36)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel35)
                .addComponent(jTextFieldDelay15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel36))
        );

        jLabel37.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jLabel37.setText("CH15");

        jTextFieldCount15.setEditable(false);
        jTextFieldCount15.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldCount15.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldCount15.setText("0");

        jTextFieldDelay16.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldDelay16.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldDelay16.setText("0.00");

        jLabel38.setText("ns");

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addComponent(jLabel37)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldCount15, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldDelay16, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel38)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldCount15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel37)
                .addComponent(jTextFieldDelay16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabel38))
        );

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel11, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel17, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel18, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel12, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel14, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel15, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel16, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel17, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel18, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel19, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel20, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Trigger"));

        jLabel8.setText("Frequency");

        jTextFieldTriggerFrequency.setEditable(false);
        jTextFieldTriggerFrequency.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldTriggerFrequency.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldTriggerFrequency.setText("0");

        jLabel9.setText("Hz");

        jLabel10.setText("Period");

        jTextFieldTriggerPeriod.setEditable(false);
        jTextFieldTriggerPeriod.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldTriggerPeriod.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldTriggerPeriod.setText("0.000");

        jLabel39.setText("ns");

        jTextFieldTriggerPeriodPM.setEditable(false);
        jTextFieldTriggerPeriodPM.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldTriggerPeriodPM.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldTriggerPeriodPM.setText("0.000");

        jLabel40.setText("ns");

        jLabel41.setText("±");

        jLabel42.setText("Source                CH 0");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel42)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel7Layout.createSequentialGroup()
                                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel8)
                                    .addComponent(jLabel10))
                                .addGap(3, 3, 3))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                                .addComponent(jLabel41)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jTextFieldTriggerPeriodPM, javax.swing.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)
                            .addComponent(jTextFieldTriggerFrequency)
                            .addComponent(jTextFieldTriggerPeriod))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                        .addGap(2, 2, 2)
                        .addComponent(jLabel39))
                    .addComponent(jLabel9)
                    .addComponent(jLabel40))
                .addGap(12, 12, 12))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addComponent(jLabel42)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(jTextFieldTriggerFrequency, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jTextFieldTriggerPeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel39))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldTriggerPeriodPM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel40)
                    .addComponent(jLabel41))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel45.setText("Pulse Period");

        jTextFieldPulsePeriod.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldPulsePeriod.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldPulsePeriod.setText("13.150");

        jLabel46.setText("ns");

        jLabel47.setText("Gate Width");

        jTextFieldGateWidth.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N
        jTextFieldGateWidth.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldGateWidth.setText("3.0");

        jLabel48.setText("ns");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 767, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel45)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPulsePeriod, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel46)
                        .addGap(48, 48, 48)
                        .addComponent(jLabel47)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldGateWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel48)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 577, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel45)
                    .addComponent(jTextFieldPulsePeriod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel46)
                    .addComponent(jLabel47)
                    .addComponent(jTextFieldGateWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel48))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void jCheckBoxIntItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxIntItemStateChanged
      jButtonClear.setEnabled(jCheckBoxInt.isSelected());
  }//GEN-LAST:event_jCheckBoxIntItemStateChanged

  private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
      histogramChartPanel.updateSeries("Histogram", Arrays.asList(new Integer[]{0}), Arrays.asList(new Integer[]{0}), null);
  }//GEN-LAST:event_jButtonClearActionPerformed

  private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
      jFileChooser1.setCurrentDirectory(new File(matrixFile));
      jFileChooser1.setFileSelectionMode(JFileChooser.FILES_ONLY);
      jFileChooser1.setMultiSelectionEnabled(false);
      jFileChooser1.setFileFilter(new FileFilter() {
          @Override
          public boolean accept(File f) {
              return f.getName().toLowerCase().endsWith(".csv");
          }

          @Override
          public String getDescription() {
              return ".csv";
          }
      });
      if (jFileChooser1.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          jTextFieldMatrixFile.setText(matrixFile);
          loadMatrix();
      }
  }//GEN-LAST:event_jButton1ActionPerformed

  private void jTabbedPane1StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTabbedPane1StateChanged
      switch (jTabbedPane1.getSelectedIndex()) {
          case 0:
              doHistogram = true;
              doSampling = false;
              break;
          case 1:
              doHistogram = false;
              doSampling = true;
              break;
      }
  }//GEN-LAST:event_jTabbedPane1StateChanged

  private void jCheckBoxSamplingIntegrateItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxSamplingIntegrateItemStateChanged
      samplingIntegrate = jCheckBoxSamplingIntegrate.isSelected();
  }//GEN-LAST:event_jCheckBoxSamplingIntegrateItemStateChanged

  private void jCheckBoxLogItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jCheckBoxLogItemStateChanged
      this.histogramChart.getStyleManager().setYAxisLogarithmic(jCheckBoxLog.isSelected());
  }//GEN-LAST:event_jCheckBoxLogItemStateChanged

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void mainPort(String args[]) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AppFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JCheckBox jCheckBoxInputs1;
    private javax.swing.JCheckBox jCheckBoxInputs10;
    private javax.swing.JCheckBox jCheckBoxInputs11;
    private javax.swing.JCheckBox jCheckBoxInputs12;
    private javax.swing.JCheckBox jCheckBoxInputs13;
    private javax.swing.JCheckBox jCheckBoxInputs14;
    private javax.swing.JCheckBox jCheckBoxInputs15;
    private javax.swing.JCheckBox jCheckBoxInputs2;
    private javax.swing.JCheckBox jCheckBoxInputs3;
    private javax.swing.JCheckBox jCheckBoxInputs4;
    private javax.swing.JCheckBox jCheckBoxInputs5;
    private javax.swing.JCheckBox jCheckBoxInputs6;
    private javax.swing.JCheckBox jCheckBoxInputs7;
    private javax.swing.JCheckBox jCheckBoxInputs8;
    private javax.swing.JCheckBox jCheckBoxInputs9;
    private javax.swing.JCheckBox jCheckBoxInt;
    private javax.swing.JCheckBox jCheckBoxLog;
    private javax.swing.JCheckBox jCheckBoxPulseMode;
    private javax.swing.JCheckBox jCheckBoxSamplingIntegrate;
    private javax.swing.JCheckBox jCheckBoxTriggerMode;
    private javax.swing.JCheckBox jCheckBoxVisibility;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel31;
    private javax.swing.JLabel jLabel32;
    private javax.swing.JLabel jLabel33;
    private javax.swing.JLabel jLabel34;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel43;
    private javax.swing.JLabel jLabel44;
    private javax.swing.JLabel jLabel45;
    private javax.swing.JLabel jLabel46;
    private javax.swing.JLabel jLabel47;
    private javax.swing.JLabel jLabel48;
    private javax.swing.JLabel jLabel49;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel50;
    private javax.swing.JLabel jLabel51;
    private javax.swing.JLabel jLabel52;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelMatrixLoadError;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField jTextFieldCount0;
    private javax.swing.JTextField jTextFieldCount1;
    private javax.swing.JTextField jTextFieldCount10;
    private javax.swing.JTextField jTextFieldCount11;
    private javax.swing.JTextField jTextFieldCount12;
    private javax.swing.JTextField jTextFieldCount13;
    private javax.swing.JTextField jTextFieldCount14;
    private javax.swing.JTextField jTextFieldCount15;
    private javax.swing.JTextField jTextFieldCount2;
    private javax.swing.JTextField jTextFieldCount3;
    private javax.swing.JTextField jTextFieldCount4;
    private javax.swing.JTextField jTextFieldCount5;
    private javax.swing.JTextField jTextFieldCount6;
    private javax.swing.JTextField jTextFieldCount7;
    private javax.swing.JTextField jTextFieldCount8;
    private javax.swing.JTextField jTextFieldCount9;
    private javax.swing.JTextField jTextFieldDelay1;
    private javax.swing.JTextField jTextFieldDelay10;
    private javax.swing.JTextField jTextFieldDelay11;
    private javax.swing.JTextField jTextFieldDelay12;
    private javax.swing.JTextField jTextFieldDelay13;
    private javax.swing.JTextField jTextFieldDelay14;
    private javax.swing.JTextField jTextFieldDelay15;
    private javax.swing.JTextField jTextFieldDelay16;
    private javax.swing.JTextField jTextFieldDelay2;
    private javax.swing.JTextField jTextFieldDelay3;
    private javax.swing.JTextField jTextFieldDelay4;
    private javax.swing.JTextField jTextFieldDelay5;
    private javax.swing.JTextField jTextFieldDelay6;
    private javax.swing.JTextField jTextFieldDelay7;
    private javax.swing.JTextField jTextFieldDelay8;
    private javax.swing.JTextField jTextFieldDelay9;
    private javax.swing.JTextField jTextFieldDelayedPulse;
    private javax.swing.JTextField jTextFieldEff1;
    private javax.swing.JTextField jTextFieldEff2;
    private javax.swing.JTextField jTextFieldGateWidth;
    private javax.swing.JTextField jTextFieldIndexSignal;
    private javax.swing.JTextField jTextFieldIndexTrigger;
    private javax.swing.JTextField jTextFieldMatrixFile;
    private javax.swing.JTextField jTextFieldModeShower;
    private javax.swing.JTextField jTextFieldPulsePeriod;
    private javax.swing.JTextField jTextFieldReletiveCoins;
    private javax.swing.JTextField jTextFieldSimilarity;
    private javax.swing.JTextField jTextFieldTriggerFrequency;
    private javax.swing.JTextField jTextFieldTriggerPeriod;
    private javax.swing.JTextField jTextFieldTriggerPeriodPM;
    private javax.swing.JTextField jTextFieldViewFrom;
    private javax.swing.JTextField jTextFieldViewTo;
    private javax.swing.JTextField jTextFieldVisibility;
    // End of variables declaration//GEN-END:variables

    private void dataIncome(ArrayList<Long> dataList) {
        for (Long data : dataList) {
            long time = data >> 4;
            int channel = (int) (data & 0b1111);
            feedTimeEvent(channel, time);
        }
    }

    private final ArrayList<Long>[] timeEvents;
    private long unitEndTime = 0;
    private final double[] delays = new double[16];

    private void feedTimeEvent(int channel, long time) {
        if (time > unitEndTime) {
            flush();
        }
        timeEvents[channel].add(time);
    }

    private void flush() {
        ArrayList<Long>[] dataBlock = new ArrayList[timeEvents.length];
        for (int i = 0; i < timeEvents.length; i++) {
            dataBlock[i] = timeEvents[i];
            timeEvents[i] = new ArrayList<>();
            long delay = (long) (delays[i] * 1000);
            for (int j = 0; j < dataBlock[i].size(); j++) {
                dataBlock[i].set(j, dataBlock[i].get(j) + delay);
            }
        }
        dataBlockQueue.offer(new DataBlock(dataBlock));
        unitEndTime += 1000000000000l;
    }

    private final BlockingQueue<DataBlock> dataBlockQueue = new LinkedBlockingDeque<>();

    private void postInit() {
        for (JCheckBox inputBox : inputBoxs) {
            inputBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    int selectedInput = 0;
                    for (JCheckBox inputBox1 : inputBoxs) {
                        selectedInput += (inputBox1.isSelected() ? 1 : 0);
                    }
                    jCheckBoxVisibility.setEnabled(selectedInput == 2);
                    samplingMode = (selectedInput == 2 && jCheckBoxVisibility.isSelected()) ? SAMPLING_MODE_VISIBILITY : SAMPLING_MODE_SAMPLING;
                    updatePermenents();
                }
            });
        }
        jCheckBoxVisibility.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                int selectedInput = 0;
                for (JCheckBox inputBox1 : inputBoxs) {
                    selectedInput += (inputBox1.isSelected() ? 1 : 0);
                }
                samplingMode = (selectedInput == 2 && jCheckBoxVisibility.isSelected()) ? SAMPLING_MODE_VISIBILITY : SAMPLING_MODE_SAMPLING;
                updatePermenents();
            }
        });
        jCheckBoxPulseMode.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                histogramMode = jCheckBoxPulseMode.isSelected() ? HISTOGRAM_MODE_PULSE : HISTOGRAM_MODE_BIN;
            }
        });
        jCheckBoxTriggerMode.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                triggerMode = jCheckBoxTriggerMode.isSelected();
            }
        });
    }

    private double[][] calculateOverallVisibilities(ArrayList<Long>[] data) {
        int mode = matrix.getNumberOfColumns();
        double[][] vis = new double[mode * (mode - 1) / 2][4];
        int vi = 0;
        for (int i = 0; i < mode; i++) {
            for (int j = i + 1; j < mode; j++) {
                ArrayList<Long> tList = data[i];
                ArrayList<Long> sList = data[j];
                String oldMode = histogramMode;
                double oldViewFrom = viewFrom;
                double oldViewTo = viewTo;
                histogramMode = AppFrame.HISTOGRAM_MODE_PULSE;
                viewFrom = -1300;
                viewTo = 1300;
                Histo histo = doHistogramGodMode(tList, sList);
                int pulseInfiLimit = 80;
                int pulseInfiCount = 0;
                double C0 = 0;
                double Cp = 0;
                double Cm = 0;
                double Cinfi = 0;
                for (int k = 0; k < histo.xData.size(); k++) {
                    double x = histo.xData.get(k);
                    double y = histo.yData.get(k);
                    if (Math.abs(x) < 0.1) {
                        C0 += y;
                    } else if (Math.abs(x - delayedPulse) < 0.1) {
                        Cp += y;
                    } else if (Math.abs(x + delayedPulse) < 0.1) {
                        Cm += y;
                    } else if (x <= pulseInfiLimit) {
                        Cinfi += y;
                        pulseInfiCount++;
                    }
                }
                Cinfi /= pulseInfiCount;
                vis[vi][0] = C0;
                vis[vi][1] = Cp;
                vis[vi][2] = Cm;
                vis[vi][3] = Cinfi;
                vi++;
                histogramMode = oldMode;
                viewFrom = oldViewFrom;
                viewTo = oldViewTo;
            }
        }
        return vis;
    }

    private PrintWriter modesWriter;

    private void recordCoincidenceModes(Coincidencer coincidencer) {
        if (modesWriter == null) {
            try {
                modesWriter = new PrintWriter("CM" + System.currentTimeMillis() + ".txt");
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AppFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ArrayList<Integer> expectedModes = new ArrayList<>();
        for (int m : modes) {
            expectedModes.add(m);
        }
        for (Integer coinsMode : coincidencer.coincidenceList()) {
            if (expectedModes.contains(coinsMode)) {
                modesWriter.println(formatModes(coinsMode));
            }
        }
        modesWriter.flush();
    }

    private String formatModes(int mode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 7; i++) {
            int mask = (1 << (i - 1));
            if ((mode & mask) != 0) {
                sb.append(i).append(" ");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private class DataBlock {

        private final ArrayList<Long>[] data;

        private DataBlock(ArrayList<Long>[] data) {
            this.data = data;
        }
    }

    private void doFlushLoop() {
        while (true) {
            try {
                DataBlock dataBlock = dataBlockQueue.take();
                final int[] counters = new int[dataBlock.data.length];
                for (int i = 0; i < dataBlock.data.length; i++) {
                    ArrayList<Long> timeEventList = dataBlock.data[i];
                    counters[i] = timeEventList.size();
                }
                final Object[] triggerInfo = doAssessTrigger(dataBlock.data[0]);
                final Histo histogram = doHistogram ? doHistogram(dataBlock.data[indexTrigger], dataBlock.data[indexSignal]) : null;
                if (doSampling) {
                    doSampling(dataBlock);
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < counters.length; i++) {
                            counterFields[i].setText(formatComm(counters[i]));
                            infiniteCounters[i] += counters[i];
                            System.out.print(infiniteCounters[i] + "\t");
                        }
                        System.out.println();
                        jTextFieldTriggerFrequency.setText(formatComm((int) triggerInfo[0]));
                        jTextFieldTriggerPeriod.setText(formatDouble((double) triggerInfo[1]));
                        jTextFieldTriggerPeriodPM.setText(formatDouble((double) triggerInfo[2]));
                        if (histogram != null) {
                            List<Double> originalXData = histogram.getxAxisData();
                            ArrayList<Double> xData = new ArrayList<>(originalXData.size());
                            for (Double originalX : originalXData) {
                                xData.add(originalX / (histogram.pulseMode ? 1 : 1000));
                            }
                            Series series = histogramChart.getSeriesMap().get("Histogram");
                            Collection<? extends Number> originalYData = series.getYData();
                            ArrayList<Double> yData = new ArrayList<>(histogram.getyAxisData());
                            for (int i = 0; i < yData.size(); i++) {
                                yData.set(i, yData.get(i) + 1);
                            }
                            if (originalYData != null && originalYData.size() == histogram.getyAxisData().size() && jCheckBoxInt.isSelected()) {
                                Iterator<? extends Number> it = originalYData.iterator();
                                for (int i = 0; i < yData.size(); i++) {
                                    yData.set(i, yData.get(i) + it.next().doubleValue());
                                }
                            }
                            histogramChartPanel.updateSeries("Histogram", xData, yData, null);
                            if (histogram.pulseMode) {
                                System.out.println("Pulse Mode: " + yData);
                            }
                            double[] reletiveCoins = doVisibility(xData, yData);
                            double Q = reletiveCoins[0] - 0.035;
                            double C = (2 - reletiveCoins[1] - reletiveCoins[2]);
                            final double visibility = (C - Q) / C;
                            jTextFieldReletiveCoins.setText(formatDouble(reletiveCoins[0]) + ", " + formatDouble(reletiveCoins[1]) + ", " + formatDouble(reletiveCoins[2]));
                            jTextFieldVisibility.setText(formatDouble(visibility));
                        }
                        if (permenents != null && !permenents.isEmpty() && modes != null && modes.length > 0) {
                            ArrayList<Integer> xData = new ArrayList<>();
                            for (int i = 0; i < modes.length; i++) {
                                xData.add(i);
                            }
                            samplingChartPanel.updateSeries("SamplingExpect", xData, permenents, null);
                            switch (samplingMode) {
                                case AppFrame.SAMPLING_MODE_SAMPLING:
                                    double[] matchedCoins = new double[permenents.size()];
                                    for (int i = 0; i < matchedCoins.length; i++) {
                                        matchedCoins[i] = coincidences[modes[i]];
                                    }
                                    double totalCoins = 0;
                                    for (double coincidence : matchedCoins) {
                                        totalCoins += coincidence;
                                    }
                                    ArrayList<Double> coinsData = new ArrayList<>();
                                    for (int i = 0; i < matchedCoins.length; i++) {
                                        coinsData.add(matchedCoins[i] / totalCoins);
                                    }
                                    samplingChartPanel.updateSeries("SamplingExperiment", xData, coinsData, null);
                                    System.out.println("Sampling Coincidence Data: " + Arrays.toString(matchedCoins));
                                    double similarity = 0;
                                    for (int i = 0; i < permenents.size(); i++) {
                                        similarity += Math.sqrt(permenents.get(i) * coinsData.get(i));
                                    }
                                    jTextFieldSimilarity.setText("" + ((int) (similarity * 10000) / 10000.));
                                    double singlePhotonCountRate = photonCounts * 1. / photonCountsTime;
                                    double initCount = 6000000;
                                    double frequency = 1e9 / pulsePeriod;
                                    double eff1 = singlePhotonCountRate / initCount;
                                    int selectedRowCount = 0;
                                    for (JCheckBox ib : inputBoxs) {
                                        selectedRowCount += ib.isSelected() ? 1 : 0;
                                    }
                                    double miu = singlePhotonCountRate / frequency;
                                    double repeatRate = 500000;
                                    double validTimeRate = 263 * selectedRowCount / (1e9 / repeatRate);
                                    double eff2 = Math.pow(totalCoins / photonCountsTime / validTimeRate / frequency, 1. / selectedRowCount) / miu;
                                    jTextFieldEff1.setText(formatDouble(eff1));
                                    jTextFieldEff2.setText(formatDouble(eff2));
                                    break;
                                case AppFrame.SAMPLING_MODE_VISIBILITY:
                                    ArrayList<Double> vis = new ArrayList();
                                    for (int i = 0; i < permenents.size(); i++) {
                                        double[] vs = visibilities[i];
                                        double C0 = vs[0] / vs[3];
                                        double Cp = vs[1] / vs[3];
                                        double Cm = vs[2] / vs[3];
                                        double Q = C0 - 0.035;
                                        double C = 2 - Cp - Cm;
                                        vis.add((C - Q) / C);
                                    }
                                    samplingChartPanel.updateSeries("SamplingExperiment", xData, vis, null);
                                    break;
                                default:
                                    throw new RuntimeException();
                            }
                        }
                    }
                });
            } catch (InterruptedException ex) {
                Logger.getLogger(AppFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private double[] doVisibility(List<Double> xAxisData, List<Double> yAxisData) {
        double pulsePeriodPS = (pulsePeriod);
        double gateWidthPS = (gateWidth);
        double[] lines = new double[]{0, pulsePeriodPS, -pulsePeriodPS, 2 * pulsePeriodPS, 3 * pulsePeriodPS, -2 * pulsePeriodPS, - 3 * pulsePeriodPS};
        double[] reletiveCoins = new double[lines.length];
        for (int i = 0; i < xAxisData.size(); i++) {
            double x = xAxisData.get(i);
            for (int iLine = 0; iLine < lines.length; iLine++) {
                if (Math.abs(x - lines[iLine]) <= gateWidthPS / 2) {
                    reletiveCoins[iLine] += yAxisData.get(i);
                }
            }
        }
        double norm = (reletiveCoins[3] + reletiveCoins[4] + reletiveCoins[5] + reletiveCoins[6]) / 4;
        double p0 = reletiveCoins[0] / norm;
        double p1P = reletiveCoins[1] / norm;
        double p1M = reletiveCoins[2] / norm;
        return new double[]{p0, p1P, p1M};
    }

    private Histo doHistogram(ArrayList<Long> tList, ArrayList<Long> sList) {
//    if (triggerMode) {
//      return doHistogramTriggerMode(tList, sList);
//    } else {
//      return doHistogramNonTriggerMode(tList, sList);
//    }
        return doHistogramGodMode(tList, sList);
    }

    private Histo doHistogramGodMode(ArrayList<Long> tList, ArrayList<Long> sList) {
        long viewFromPS = (long) (viewFrom * 1000);
        long viewToPS = (long) (viewTo * 1000);
        ArrayList<Long> deltas = new ArrayList<>();
        if (!tList.isEmpty() && !sList.isEmpty()) {
            int preStartT = 0;
            int lengthT = tList.size();
            for (long s : sList) {
                while (preStartT < lengthT) {
                    long t = tList.get(preStartT);
                    long delta = s - t;
                    if (delta > viewToPS) {
                        preStartT++;
                    } else {
                        break;
                    }
                }
                for (int tIndex = preStartT; tIndex < lengthT; tIndex++) {
                    long t = tList.get(tIndex);
                    long delta = s - t;
                    if (delta > viewFromPS) {
                        deltas.add(delta);
                    } else {
                        break;
                    }
                }
            }
        }
        return makeHistogram(deltas, viewFromPS, viewToPS);
    }

    private Histo doHistogramNonTriggerMode(ArrayList<Long> tList, ArrayList<Long> sList) {
        long viewFromPS = (long) (viewFrom * 1000);
        long viewToPS = (long) (viewTo * 1000);
        ArrayList<Long> deltas = new ArrayList<>();
        if (!tList.isEmpty() && !sList.isEmpty()) {
            Iterator<Long> tIt = tList.iterator();
            Iterator<Long> sIt = sList.iterator();
            Long t = tIt.next();
            Long s = sIt.next();
            while (true) {
                Long delta = s - t;
                if (delta >= viewFromPS && delta <= viewToPS) {
                    deltas.add(delta);
                }
                if (tIt.hasNext() && sIt.hasNext()) {
                    if (s > t) {
                        t = tIt.next();
                    } else {
                        s = sIt.next();
                    }
                } else {
                    break;
                }
            }
        }
        return makeHistogram(deltas, viewFromPS, viewToPS);
    }

    private Histo doHistogramTriggerMode(ArrayList<Long> tList, ArrayList<Long> sList) {
        long viewFromPS = (long) (viewFrom * 1000);
        long viewToPS = (long) (viewTo * 1000);
        ArrayList<Long> deltas = new ArrayList<>();
        if (tList.size() >= 2 && !sList.isEmpty()) {
            Iterator<Long> tIt = tList.iterator();
            Iterator<Long> sIt = sList.iterator();
            Long t = tIt.next();
            Long tBackup = tIt.next();
            Long s = sIt.next();
            while (true) {
                Long delta = s - t;
                if (delta >= viewFromPS && delta <= viewToPS) {
                    deltas.add(delta);
                }
                if (tIt.hasNext() && sIt.hasNext()) {
                    if (s >= tBackup) {
                        t = tBackup;
                        tBackup = tIt.next();
                    } else {
                        s = sIt.next();
                    }
                } else {
                    break;
                }
            }
        }
        return makeHistogram(deltas, viewFromPS, viewToPS);
    }

    private Histo makeHistogram(ArrayList<Long> data, double min, double max) {
        switch (histogramMode) {
            case HISTOGRAM_MODE_BIN:
                return new Histo(new Histogram(data, 1000, min, max));
            case HISTOGRAM_MODE_PULSE:
                ArrayList<Integer> pulses = new ArrayList<>();
                for (long delta : data) {
                    boolean positive = true;
                    if (delta < 0) {
                        delta = -delta;
                        positive = false;
                    }
                    double deltaD = delta / 1000.;
                    if (deltaD < gateWidth / 2) {
                        pulses.add(0);
                    } else {
                        int A = (int) ((deltaD + gateWidth / 2) / pulsePeriod);
                        int B = (int) ((deltaD - gateWidth / 2) / pulsePeriod);
                        if (A == B + 1) {
                            pulses.add(positive ? A : -A);
                        }
                    }
                }
                return new Histo(pulses);
            default:
                throw new RuntimeException();
        }
    }

    private Object[] doAssessTrigger(ArrayList<Long> triggerList) {
        int frequency = triggerList.size();
        if (frequency > 0) {
            double mean = (triggerList.get(triggerList.size() - 1) - triggerList.get(0)) / ((double) (triggerList.size() - 1));
            double maxDelta = 0;
            Iterator<Long> iterator = triggerList.iterator();
            Long a = iterator.next();
            while (iterator.hasNext()) {
                Long b = iterator.next();
                double delta = Math.abs(b - a - mean);
                if (delta > maxDelta) {
                    maxDelta = delta;
                }
                a = b;
            }
            return new Object[]{frequency, mean / 1000, maxDelta / 1000};
        } else {
            return new Object[]{0, 0., 0.};
        }
    }

    private void doSampling(DataBlock dataBlock) {
        ArrayList<Long>[] dataO = dataBlock.data;
        ArrayList<Long>[] data = new ArrayList[dataO.length];
        for (int i = 0; i < dataO.length - 1; i++) {
            data[i] = dataO[i + 1];
        }
        data[data.length - 1] = dataO[0];
        switch (samplingMode) {
            case AppFrame.SAMPLING_MODE_SAMPLING:
//        Coincidencer coincidencer = new Coincidencer(data, (long) (gateWidth * 1000), null 263000);
                Coincidencer coincidencer = new Coincidencer(data, (long) (gateWidth * 1000), dataO[0], 263000);
                int[] coins = coincidencer.coincidences();
//                recordCoincidenceModes(coincidencer);
                if (samplingIntegrate) {
                    for (int i = 0; i < coins.length; i++) {
                        coincidences[i] += coins[i];
                        for (int ic = 0; ic < matrix.getNumberOfColumns(); ic++) {
                            photonCounts += data[ic].size();
                        }
                        photonCountsTime += 1;
                    }
                } else {
                    for (int i = 0; i < coins.length; i++) {
                        coincidences[i] = coins[i];
                        photonCounts = 0;
                        for (int ic = 0; ic < matrix.getNumberOfColumns(); ic++) {
                            photonCounts += data[ic].size();
                        }
                        photonCountsTime = 1;
                    }
                }
                break;
            case AppFrame.SAMPLING_MODE_VISIBILITY:
                double[][] vis = calculateOverallVisibilities(data);
                if (samplingIntegrate) {
                    for (int i = 0; i < vis.length; i++) {
                        for (int j = 0; j < vis[i].length; j++) {
                            visibilities[i][j] += vis[i][j];
                        }
                    }
                } else {
                    for (int i = 0; i < vis.length; i++) {
                        visibilities[i] = vis[i];
                    }
                }
                break;
            default:
                throw new RuntimeException();
        }
    }

    private void initServer() {
        final TDCParser tdcParser = new TDCParser(new TDCDataProcessor() {
            @Override
            public void process(Object data) {
                if (data instanceof ArrayList) {
                    ArrayList list = (ArrayList) data;
                    if (list.size() > 0) {
                        dataIncome(list);
                    }
                } else {
                    throw new RuntimeException();
                }
            }
        }, new GroundTDCDataAdapter(new int[]{0, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 21, 2, 3, 4, 5}));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(20156);
                    byte[] buffer = new byte[1024 * 1024 * 16];
                    while (!server.isClosed()) {
                        Socket socket = server.accept();
                        System.out.println("Connected");
                        try {
                            InputStream in = socket.getInputStream();
                            while (!socket.isClosed()) {
                                int read = in.read(buffer);
                                tdcParser.offer(Arrays.copyOfRange(buffer, 0, read));
                            }
                        } catch (Exception e) {
                        }
                        System.out.println("End");
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                doFlushLoop();
            }
        }).start();
    }

    private void loadPreferences() {
        for (int i = 0; i < delays.length; i++) {
            try {
                delays[i] = preferences.getDouble("Delay" + i, 0);
                delayFields[i].setText("" + delays[i]);
            } catch (Exception e) {
            }
        }
        indexTrigger = preferences.getInt("IndexTrigger", 1);
        jTextFieldIndexTrigger.setText("" + indexTrigger);
        indexSignal = preferences.getInt("IndexSignal", 2);
        jTextFieldIndexSignal.setText("" + indexSignal);
        viewFrom = preferences.getDouble("ViewFrom", -100);
        jTextFieldViewFrom.setText("" + viewFrom);
        viewTo = preferences.getDouble("ViewTo", 100);
        jTextFieldViewTo.setText("" + viewTo);
        pulsePeriod = preferences.getDouble("PulsePeriod", 13.15);
        jTextFieldPulsePeriod.setText("" + pulsePeriod);
        gateWidth = preferences.getDouble("GateWidth", 3);
        jTextFieldGateWidth.setText("" + gateWidth);
        matrixFile = preferences.get("MatrixFile", ".");
        jTextFieldMatrixFile.setText(matrixFile);
        delayedPulse = preferences.getInt("DelayedPulse", 1);
        jTextFieldDelayedPulse.setText("" + delayedPulse);
        loadMatrix();
    }

    private void savePreferences() {
        for (int i = 0; i < delays.length; i++) {
            try {
                preferences.putDouble("Delay" + i, delays[i]);
            } catch (Exception e) {
            }
        }
        preferences.putInt("IndexTrigger", indexTrigger);
        preferences.putInt("IndexSignal", indexSignal);
        preferences.putDouble("ViewFrom", viewFrom);
        preferences.putDouble("ViewTo", viewTo);
        preferences.putDouble("PulsePeriod", pulsePeriod);
        preferences.putDouble("GateWidth", gateWidth);
        preferences.put("MatrixFile", matrixFile);
        preferences.putInt("DelayedPulse", delayedPulse);
    }

    private void connectPreferences() {
        for (int i = 0; i < delayFields.length; i++) {
            final JTextField delayField = delayFields[i];
            final int index = i;
            delayField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    String text = delayField.getText();
                    try {
                        double delay = Double.parseDouble(text);
                        delays[index] = delay;
                        savePreferences();
                    } catch (Exception ex) {
                    }
                }
            });
        }
        jTextFieldIndexTrigger.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldIndexTrigger.getText();
                try {
                    indexTrigger = Integer.parseInt(text);
                    if (indexTrigger < 0) {
                        indexTrigger = 0;
                        jTextFieldIndexTrigger.setText("0");
                    } else if (indexTrigger >= timeEvents.length) {
                        indexTrigger = timeEvents.length - 1;
                        jTextFieldIndexTrigger.setText("" + (timeEvents.length - 1));
                    }
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jTextFieldIndexSignal.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldIndexSignal.getText();
                try {
                    indexSignal = Integer.parseInt(text);
                    if (indexSignal < 0) {
                        indexSignal = 0;
                        jTextFieldIndexSignal.setText("0");
                    } else if (indexSignal >= timeEvents.length) {
                        indexSignal = timeEvents.length;
                        jTextFieldIndexSignal.setText("" + (timeEvents.length - 1));
                    }
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jTextFieldViewFrom.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldViewFrom.getText();
                try {
                    viewFrom = Double.parseDouble(text);
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jTextFieldViewTo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldViewTo.getText();
                try {
                    viewTo = Double.parseDouble(text);
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jTextFieldPulsePeriod.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldPulsePeriod.getText();
                try {
                    pulsePeriod = Double.parseDouble(text);
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jTextFieldGateWidth.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldGateWidth.getText();
                try {
                    gateWidth = Double.parseDouble(text);
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });
        jFileChooser1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (JFileChooser.APPROVE_SELECTION.equals(e.getActionCommand())) {
                    matrixFile = jFileChooser1.getSelectedFile().getAbsolutePath();
                    savePreferences();
                }
            }
        });
        jTextFieldDelayedPulse.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                String text = jTextFieldDelayedPulse.getText();
                try {
                    delayedPulse = Integer.parseInt(text);
                    savePreferences();
                } catch (Exception ex) {
                }
            }
        });

    }

    private String formatComm(int value) {
        return NumberFormat.getInstance().format(value);
    }

    private String formatDouble(double value) {
        return NumberFormat.getInstance().format(value);
    }
    private final double[] detectionEfficiencies = new double[]{1.17, 1.19, 1.27, 1.08, 1.03, 1.01, 1};

    private void loadMatrix() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(matrixFile);
                if (!file.exists() || !file.getName().toLowerCase().endsWith(".csv")) {
                    return;
                }
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    ArrayList<String> lines = new ArrayList<>();
                    while (true) {
                        String line = reader.readLine();
                        if (line == null || line.length() == 0) {
                            break;
                        }
                        lines.add(line);
                    }
                    if (lines.size() % 2 != 0) {
                        throw new RuntimeException();
                    }
                    final int mode = lines.size() / 2;
                    ArrayList<Double> amps = new ArrayList<>(mode * mode);
                    ArrayList<Double> phases = new ArrayList<>(mode * mode);
                    ArrayList<Double> reals = new ArrayList<>(mode * mode);
                    ArrayList<Double> images = new ArrayList<>(mode * mode);
                    for (int i = 0; i < mode; i++) {
                        String ampLine = lines.get(i);
                        String phaseLine = lines.get(mode + i);
                        String[] ampSs = ampLine.split("[ *,\t]+");
                        String[] phaseSs = phaseLine.split("[ *,\t]+");
                        if (ampSs.length != mode || phaseSs.length != mode) {
                            throw new RuntimeException();
                        }
                        for (int j = 0; j < mode; j++) {
                            double amp = Double.parseDouble(ampSs[j]) * Math.sqrt(detectionEfficiencies[j]);
                            double phase = Double.parseDouble(phaseSs[j]);
                            amps.add(amp);
                            phases.add(phase);
                            reals.add(amp * Math.cos(phase));
                            images.add(amp * Math.sin(phase));
                        }
                    }
                    matrix = calculateMatrix(reals, images, mode);
                    updatePermenents();
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            jTextFieldModeShower.setText(mode + " modes");
                            for (int i = 0; i < inputBoxs.length; i++) {
                                inputBoxs[i].setVisible(i < mode);
                            }
                        }
                    });
                    jLabelMatrixLoadError.setVisible(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    jLabelMatrixLoadError.setVisible(true);
                }
            }
        }).start();
    }

    private ComplexMatrix calculateMatrix(ArrayList<Double> reals, ArrayList<Double> images, int mode) {
        Complex[][] data = new Complex[mode][mode];
        for (int i = 0; i < mode; i++) {
            for (int j = 0; j < mode; j++) {
                data[i][j] = Complex.valueOf(reals.get(i * mode + j), images.get(i * mode + j));
            }
        }
        ComplexMatrix M = ComplexMatrix.valueOf(data);
        return M;
    }

    private void updatePermenents() {
        if (matrix == null) {
            return;
        }
        int mode = matrix.getNumberOfRows();
        ArrayList<ComplexVector> rows = new ArrayList<>();
        for (int i = 0; i < mode; i++) {
            if (inputBoxs[i].isSelected()) {
                rows.add(matrix.getRow(i));
            }
        }
        int subMode = rows.size();
        if (subMode <= 1) {
            return;
        }
        ComplexMatrix permM = ComplexMatrix.valueOf(rows);
        permenents = calculatePermenents(permM);
        modes = calculateModes(mode, subMode);
        System.out.println("Permenents updated: " + permenents);
        if (subMode == 2 && jCheckBoxVisibility.isSelected()) {
            permenents = calculateVisibilities(permM);
            System.out.println("Visibilities updated: " + permenents);
        }
    }

    private int[] calculateModes(int mode, int subMode) {
        ArrayList<ArrayList<Integer>> arranges = listArrange(1, mode + 1, subMode);
        int[] modes = new int[arranges.size()];
        for (int i = 0; i < modes.length; i++) {
            ArrayList<Integer> arrange = arranges.get(i);
            int mask = 0;
            for (Integer a : arrange) {
                mask |= (1 << (a - 1));
            }
            modes[i] = mask;
        }
        return modes;
    }

    private ArrayList<Double> calculateVisibilities(ComplexMatrix permM) {
        int mode = permM.getNumberOfColumns();
        int subMode = permM.getNumberOfRows();
        ArrayList<ArrayList<Integer>> arranges = listArrange(1, mode + 1, subMode);
        ArrayList<Double> viss = new ArrayList<>();
        for (ArrayList<Integer> arrange : arranges) {
            viss.add(calculateVisibility(permM, arrange));
        }
        return viss;
    }

    private double calculateVisibility(ComplexMatrix permM, ArrayList<Integer> arrange) {
        ArrayList<ComplexVector> columns = new ArrayList<>();
        for (Integer cn : arrange) {
            columns.add(permM.getColumn(cn - 1));
        }
        ComplexMatrix subM = ComplexMatrix.valueOf(columns).transpose();
        double vis = doVisibility(subM);
        return vis;
    }

    private double doVisibility(ComplexMatrix m) {
        if (m.getNumberOfRows() != 2 || m.getNumberOfColumns() != 2) {
            throw new RuntimeException();
        }
        Complex ad = m.get(0, 0).times(m.get(1, 1));
        Complex bc = m.get(0, 1).times(m.get(1, 0));
        Complex perm = ad.plus(bc);
        double Q = modSquare(perm);
        double C = modSquare(ad) + modSquare(bc);
        double vis = (C - Q) / C;
        return vis;
    }

    private ArrayList<Double> calculatePermenents(ComplexMatrix permM) {
        int mode = permM.getNumberOfColumns();
        int subMode = permM.getNumberOfRows();
        ArrayList<ArrayList<Integer>> arranges = listArrange(1, mode + 1, subMode);
        ArrayList<Double> perms = new ArrayList<>();
        for (ArrayList<Integer> arrange : arranges) {
            perms.add(calculatePermenent(permM, arrange));
        }
        double sum = 0;
        for (Double perm : perms) {
            sum += perm;
        }
        for (int i = 0; i < perms.size(); i++) {
            perms.set(i, perms.get(i) / sum);
        }
        return perms;
    }

    private double calculatePermenent(ComplexMatrix permM, ArrayList<Integer> arrange) {
        int subMode = permM.getNumberOfRows();
        ArrayList<ComplexVector> columns = new ArrayList<>();
        for (Integer cn : arrange) {
            columns.add(permM.getColumn(cn - 1));
        }
        ComplexMatrix subM = ComplexMatrix.valueOf(columns).transpose();
        Complex perm = doPerm(subM);
        return modSquare(perm);
    }

    private Complex doPerm(ComplexMatrix m) {
        switch (m.getNumberOfRows()) {
            case 0:
                throw new RuntimeException();
            case 1:
                return m.get(0, 0);
            case 2:
                return m.get(0, 0).times(m.get(1, 1)).plus(m.get(0, 1).times(m.get(1, 0)));
            default:
                Complex p = Complex.ZERO;
                for (int i = 0; i < m.getNumberOfColumns(); i++) {
                    Complex key = m.get(0, i);
                    ComplexMatrix subMatrix = subMatrix(m, 0, i);
                    Complex subPerm = doPerm(subMatrix);
                    p = p.plus(key.times(subPerm));
                }
                return p;
        }
    }

    private ComplexMatrix subMatrix(ComplexMatrix m, int deleteRow, int deleteColumn) {
        m = subMatrix(m, deleteRow, true);
        m = subMatrix(m, deleteColumn, false);
        return m;
    }

    private ComplexMatrix subMatrix(ComplexMatrix m, int deleteIndex, boolean isRow) {
        if (!isRow) {
            m = m.transpose();
        }
        ArrayList<ComplexVector> rows = new ArrayList<>();
        for (int i = 0; i < m.getNumberOfRows(); i++) {
            if (i != deleteIndex) {
                rows.add(m.getRow(i));
            }
        }
        m = ComplexMatrix.valueOf(rows);
        if (!isRow) {
            m = m.transpose();
        }
        return m;
    }

    private double modSquare(Complex c) {
        return Math.pow(c.getReal(), 2) + Math.pow(c.getImaginary(), 2);
    }

    private ArrayList<ArrayList<Integer>> listArrange(int from, int to, int deepth) {
        ArrayList<ArrayList<Integer>> arranges = new ArrayList<>();
        for (int i = from; i < to; i++) {
            int current = i;
            if (deepth > 1) {
                ArrayList<ArrayList<Integer>> nexts = listArrange(i + 1, to, deepth - 1);
                for (ArrayList<Integer> next : nexts) {
                    next.add(0, current);
                    arranges.add(next);
                }
            } else {
                ArrayList<Integer> list = new ArrayList<>();
                list.add(current);
                arranges.add(list);
            }
        }
        return arranges;
    }

    private class Histo {

        private final ArrayList<Double> xData = new ArrayList<>();
        private final ArrayList<Double> yData = new ArrayList<>();
        private final boolean pulseMode;

        private Histo(Histogram histogram) {
            xData.addAll(histogram.getxAxisData());
            yData.addAll(histogram.getyAxisData());
            pulseMode = false;
        }

        private Histo(ArrayList<Integer> pulses) {
            int min = (int) (viewFrom / pulsePeriod);
            int max = (int) (viewTo / pulsePeriod);
            pulseMode = true;
            int size = max - min + 1;
            for (int i = 0; i < size; i++) {
                xData.add((double) (min + i));
                yData.add(0d);
            }
            for (Integer pulse : pulses) {
                int index = pulse - min;
                if (index < yData.size() && index >= 0) {
                    yData.set(index, yData.get(index) + 1);
                }
            }
        }

        public ArrayList<Double> getxAxisData() {
            return xData;
        }

        public ArrayList<Double> getyAxisData() {
            return yData;
        }

        public boolean isPulseMode() {
            return pulseMode;
        }
    }
}
