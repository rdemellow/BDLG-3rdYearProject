/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GraphGUI;

import org.apache.commons.collections15.functors.ConstantTransformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import java.io.File;
import javax.swing.JFileChooser;

import comp30040.GraphImporter;
import comp30040.BiDynamicLineGraph;
import comp30040.BiDynamicLineGraphLayout;
import comp30040.Edge;
import comp30040.KnowledgeDiffusionCalculator;
import comp30040.VertexBDLG;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import JungModedClasss.EditingModalGraphMouse;
import comp30040.PathFinderType;
import static comp30040.PathFinderType.BFS_ALL_PATHS;
import static comp30040.PathFinderType.SHORTEST_PATHS;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.ScrollPane;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.Transformer;

/**
 *
 * @author Richard de Mellow
 */
public class BiDynamicLineGraphGUI extends javax.swing.JFrame {

    private Layout<VertexBDLG, Edge> layout = null;
    private Layout<String, String> layoutOneMode = null;
    private VisualizationViewer<VertexBDLG, Edge> vv = null;
    private VisualizationViewer<String, String> vvOneMode = null;
    private ScrollPane graphJPane = null;
    private File currentCVSFile = null;
    private BiDynamicLineGraph<VertexBDLG, Edge> currentBidlg = null;
    private KnowledgeDiffusionCalculator kDC = null;
    private int currentIndexOfSelectedView = 0;
    private Mode currentMouseMode = ModalGraphMouse.Mode.TRANSFORMING;
    private PathFinderType pFType = BFS_ALL_PATHS;

    /**
     * Creates new form DynamicLineGraphGUI
     */
    public BiDynamicLineGraphGUI() {
        initComponents();
        /* Create and display the form */
    }

    public void drawWindow() {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new BiDynamicLineGraphGUI().setVisible(true);
            }
        });
    }

    private void createImageOfGraph(VisualizationViewer vvLocal, File fileToWriteTo) {
        BufferedImage buffer = new BufferedImage(this.graphJPane.getWidth(), this.graphJPane.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = buffer.createGraphics();
        vvLocal.paint(g);
        try {
            ImageIO.write(buffer, "png", fileToWriteTo);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private VisualizationViewer genrateVisualizationViewer(File fileToUse) throws FileNotFoundException {
        if (!fileToUse.exists()) {
            throw new FileNotFoundException();
        }

        GraphImporter gi = new GraphImporter(fileToUse);
        if(this.currentBidlg == null)
            this.currentBidlg = new BiDynamicLineGraph<>(gi);
        
        this.layout = new BiDynamicLineGraphLayout<>(this.currentBidlg, new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
        Transformer<VertexBDLG, Shape> newVertexSize = new Transformer<VertexBDLG, Shape>() {
            @Override
            public Shape transform(VertexBDLG v) {
                double radius;
                if (v.getKnowlage() != 0) {
                    radius = 6 + 3 * v.getKnowlage();
                } else {
                    radius = 6;
                }
                return new Ellipse2D.Double(-radius / 2, -radius / 2, radius, radius);
            }
        };
        Transformer<VertexBDLG, Paint> newVertexColour = new Transformer<VertexBDLG, Paint>() {
            @Override
            public Paint transform(VertexBDLG v) {
                return (Paint) v.getActor().getColor();
            }
        };
        VisualizationViewer<VertexBDLG, Edge> vvTmp = new VisualizationViewer<>(this.layout);

        Transformer<Context<Graph<VertexBDLG, Edge>, Edge>, Shape> newEdgeTypes = new Transformer<Context<Graph<VertexBDLG, Edge>, Edge>, Shape>() {
            @Override
            public Shape transform(Context<Graph<VertexBDLG, Edge>, Edge> e) {
                if (e.element.getEdgeType() == EdgeType.DIRECTED) {
                    return (new EdgeShape.Line<VertexBDLG, Edge>()).transform(e);
                }
                return (new EdgeShape.QuadCurve<VertexBDLG, Edge>()).transform(e);
            }
        };
        vvTmp.getRenderContext().setEdgeShapeTransformer(newEdgeTypes);
        vvTmp.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vvTmp.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.GRAY));
        vvTmp.setVertexToolTipTransformer(new Transformer<VertexBDLG, String>() {
            @Override
            public String transform(VertexBDLG v) {
                return "Vertex " + v.toString() + " knowlage: " + Double.toString(v.getKnowlage());
            }
        });

        Factory<VertexBDLG> vertexFactory = new VertexFactory(this.currentBidlg);
        Factory<Edge> edgeFactory = new EdgeFactory();

        EditingModalGraphMouse graphMouse = new EditingModalGraphMouse(vvTmp.getRenderContext(), vertexFactory, edgeFactory);
        graphMouse.setMode(this.currentMouseMode);

        vvTmp.setGraphMouse(graphMouse);
        vvTmp.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
        vvTmp.getRenderContext().setVertexShapeTransformer(newVertexSize);
        vvTmp.getRenderContext().setVertexFillPaintTransformer(newVertexColour);
        return vvTmp;
    }

    private void updateJpanels(int currentSelectedItem, boolean refresh) {
        if ((currentIndexOfSelectedView != currentSelectedItem
                || refresh) && this.currentBidlg != null) {

            Factory<VertexBDLG> vertexFactory = new VertexFactory(this.currentBidlg);
            Factory<Edge> edgeFactory = new EdgeFactory();
            EditingModalGraphMouse graphMouseOne;
            Graph<String, String> gg;
            currentIndexOfSelectedView = currentSelectedItem;
            switch (currentSelectedItem) {
                case 0:
                    try {
                        this.vv = genrateVisualizationViewer(this.currentCVSFile);
                        if (graphJPane != null || refresh) {
                            this.remove(graphJPane);
                        }
                        graphJPane = new ScrollPane();
                        graphJPane.add(new GraphZoomScrollPane(vv));
                        graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                        this.getContentPane().add(graphJPane, BorderLayout.CENTER);
                        this.layout.setSize(this.getSize());
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(BiDynamicLineGraphGUI.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                case 1:
                    this.layoutOneMode = new KKLayout<>((Graph)this.currentBidlg);
                    this.vvOneMode = new VisualizationViewer<>(this.layoutOneMode);
                    this.vvOneMode.setSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    graphMouseOne = new EditingModalGraphMouse(this.vvOneMode.getRenderContext(), vertexFactory, edgeFactory);
                    graphMouseOne.setMode(this.currentMouseMode);
                    this.vvOneMode.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.GRAY));
                    this.vvOneMode.setGraphMouse(graphMouseOne);
                    this.remove(this.graphJPane);
                    this.vvOneMode.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
                    //this.vvOneMode.getRenderContext().setVertexFillPaintTransformer(this.newVertexColour);
                    graphJPane = new ScrollPane();
                    graphJPane.add(new GraphZoomScrollPane(this.vvOneMode));
                    this.graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    this.add(graphJPane, BorderLayout.CENTER);
                case 2:
                    gg = this.currentBidlg.getOneModeActorGraph();
                    this.layoutOneMode = new KKLayout<>(gg);
                    this.vvOneMode = new VisualizationViewer<>(this.layoutOneMode);
                    this.vvOneMode.setSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    graphMouseOne = new EditingModalGraphMouse(this.vvOneMode.getRenderContext(), vertexFactory, edgeFactory);
                    graphMouseOne.setMode(this.currentMouseMode);
                    this.vvOneMode.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.GRAY));
                    this.vvOneMode.setGraphMouse(graphMouseOne);
                    this.remove(this.graphJPane);
                    this.vvOneMode.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
                    //this.vvOneMode.getRenderContext().setVertexFillPaintTransformer(this.newVertexColour);
                    graphJPane = new ScrollPane();
                    graphJPane.add(new GraphZoomScrollPane(this.vvOneMode));
                    this.graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    this.add(graphJPane, BorderLayout.CENTER);
                    break;
                case 3:
                    gg = this.currentBidlg.getOneModeEventGraph();
                    this.layoutOneMode = new KKLayout<>(gg);
                    this.vvOneMode = new VisualizationViewer<>(this.layoutOneMode);
                    this.vvOneMode.setSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    graphMouseOne = new EditingModalGraphMouse(this.vvOneMode.getRenderContext(), vertexFactory, edgeFactory);
                    graphMouseOne.setMode(this.currentMouseMode);
                    this.vvOneMode.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.GRAY));
                    this.vvOneMode.setGraphMouse(graphMouseOne);
                    this.remove(this.graphJPane);
                    this.vvOneMode.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
                    graphJPane = new ScrollPane();
                    graphJPane.add(new GraphZoomScrollPane(this.vvOneMode));
                    this.graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    this.add(graphJPane, BorderLayout.CENTER);
                    break;
                case 4:
                    this.remove(this.graphJPane);
                    this.graphJPane = new ScrollPane();
                    if(this.kDC != null)
                        this.graphJPane.add(this.kDC.getKnowlageTableAsJTable());
                    this.graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
                    this.add(graphJPane, BorderLayout.CENTER);
                    break;
            }
            validate();
            repaint();
        }
    }

    private void updateVvMouseMode() {
        Factory<VertexBDLG> vertexFactory = new VertexFactory(this.currentBidlg);
        Factory<Edge> edgeFactory = new EdgeFactory();

        if (this.vv != null) {
            EditingModalGraphMouse graphMouse = new EditingModalGraphMouse(vv.getRenderContext(), vertexFactory, edgeFactory);
            graphMouse.setMode(this.currentMouseMode);
            vv.setGraphMouse(graphMouse);
            vv.addKeyListener(graphMouse.getModeKeyListener());
        }
        if (this.vvOneMode != null) {
            EditingModalGraphMouse graphMouseOne = new EditingModalGraphMouse(vv.getRenderContext(), vertexFactory, edgeFactory);
            graphMouseOne.setMode(this.currentMouseMode);
            vvOneMode.setGraphMouse(graphMouseOne);
            vvOneMode.addKeyListener(graphMouseOne.getModeKeyListener());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        OptionsPanel = new javax.swing.JPanel();
        refreshGraphButton = new javax.swing.JButton();
        VisulizerPicker = new javax.swing.JComboBox<>();
        jSeparator2 = new javax.swing.JSeparator();
        jTextFieldBetaKinput = new javax.swing.JTextField();
        jTextFieldAlphaKinput = new javax.swing.JTextField();
        jSeparator1 = new javax.swing.JSeparator();
        mouseModeChanger = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        maxPathLengthJField = new javax.swing.JTextField();
        runKDiffsuin = new javax.swing.JButton();
        pathFinderAlgo = new javax.swing.JComboBox();
        MainMenu = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        importcvs = new javax.swing.JMenuItem();
        export = new javax.swing.JMenuItem();
        exit = new javax.swing.JMenuItem();
        EditMenu = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Bi-Dynamic Line Graph Viewer");
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setMinimumSize(new java.awt.Dimension(800, 600));
        setName("Home Frame"); // NOI18N

        OptionsPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        OptionsPanel.setAlignmentX(0.0F);
        OptionsPanel.setAlignmentY(0.0F);
        OptionsPanel.setMaximumSize(new java.awt.Dimension(220, 32767));
        OptionsPanel.setPreferredSize(new java.awt.Dimension(235, 718));

        refreshGraphButton.setText("Refresh Current View");
        refreshGraphButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshGraphButtonActionPerformed(evt);
            }
        });

        VisulizerPicker.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "BiDynamic Grid Layout", "BiDynamic Cluster Layout", "One-Mode Actor View", "One-Mode Event View", "View Knowlage Difusion"}));
        VisulizerPicker.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                VisulizerPickerItemStateChanged(evt);
            }
        });
        VisulizerPicker.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                VisulizerPickerActionPerformed(evt);
            }
        });

        jTextFieldBetaKinput.setText("");

        jTextFieldAlphaKinput.setText("");

        mouseModeChanger.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Transforming", "Picking", "Annotation", "Editing" }));
        mouseModeChanger.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mouseModeChangerActionPerformed(evt);
            }
        });

        jLabel1.setText("Edit Mode:");

        jLabel2.setText("Alpha K:");

        jLabel3.setText("Beta  K:");

        jLabel4.setText("Max Path Length:");

        maxPathLengthJField.setText("");

        runKDiffsuin.setText("Run Knowlage Diffusion");
        runKDiffsuin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runKDiffsuinActionPerformed(evt);
            }
        });

        pathFinderAlgo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "BFS All Paths", "Shortest Parths" }));
        pathFinderAlgo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pathFinderAlgoActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout OptionsPanelLayout = new org.jdesktop.layout.GroupLayout(OptionsPanel);
        OptionsPanel.setLayout(OptionsPanelLayout);
        OptionsPanelLayout.setHorizontalGroup(
            OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(OptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(pathFinderAlgo, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, VisulizerPicker, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(OptionsPanelLayout.createSequentialGroup()
                        .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 64, Short.MAX_VALUE)
                            .add(jLabel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, jTextFieldBetaKinput)
                            .add(jTextFieldAlphaKinput)))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jSeparator1)
                    .add(mouseModeChanger, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(OptionsPanelLayout.createSequentialGroup()
                        .add(jLabel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(maxPathLengthJField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(refreshGraphButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(runKDiffsuin, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(OptionsPanelLayout.createSequentialGroup()
                        .add(jLabel1)
                        .add(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(OptionsPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .add(jSeparator2)
                    .addContainerGap()))
        );
        OptionsPanelLayout.setVerticalGroup(
            OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(OptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(VisulizerPicker, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(4, 4, 4)
                .add(refreshGraphButton)
                .add(18, 18, 18)
                .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextFieldAlphaKinput, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jTextFieldBetaKinput, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(maxPathLengthJField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(pathFinderAlgo, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(runKDiffsuin)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel1)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(mouseModeChanger, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(432, Short.MAX_VALUE))
            .add(OptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(OptionsPanelLayout.createSequentialGroup()
                    .add(70, 70, 70)
                    .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(638, Short.MAX_VALUE)))
        );

        getContentPane().add(OptionsPanel, java.awt.BorderLayout.WEST);

        FileMenu.setText("File");

        importcvs.setText("Import 2-mode CVS");
        importcvs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importcvsActionPerformed(evt);
            }
        });
        FileMenu.add(importcvs);

        export.setText("Export to png");
        export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportActionPerformed(evt);
            }
        });
        FileMenu.add(export);

        exit.setText("Exit");
        exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitActionPerformed(evt);
            }
        });
        FileMenu.add(exit);

        MainMenu.add(FileMenu);

        EditMenu.setText("Edit");
        MainMenu.add(EditMenu);

        setJMenuBar(MainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void importcvsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importcvsActionPerformed
        final JFileChooser fc = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(".csv", "csv");
        fc.setFileFilter(filter);
        fc.showOpenDialog(MainMenu);

        this.currentCVSFile = fc.getSelectedFile();
        try {
            this.layout = null;
            this.currentBidlg = null;
            this.kDC = null;
            this.vv = genrateVisualizationViewer(currentCVSFile);
            if (graphJPane != null) {
                this.remove(graphJPane);
            }
            graphJPane = new ScrollPane();
            graphJPane.add(new GraphZoomScrollPane(this.vv));
            graphJPane.setPreferredSize(new Dimension(this.getWidth() - OptionsPanel.getWidth(), this.getHeight()));
            this.getContentPane().add(graphJPane, BorderLayout.CENTER);
            this.invalidate();
            this.repaint();
            this.pack();
            System.out.println(currentCVSFile.getAbsoluteFile());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BiDynamicLineGraphGUI.class.getName()).log(Level.SEVERE, null, ex);
        }

    }//GEN-LAST:event_importcvsActionPerformed

    private void exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitActionPerformed

    private void VisulizerPickerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_VisulizerPickerActionPerformed
        int currentSelectedItem = this.VisulizerPicker.getSelectedIndex();
        this.updateJpanels(currentSelectedItem, false);
    }//GEN-LAST:event_VisulizerPickerActionPerformed

    private void VisulizerPickerItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_VisulizerPickerItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_VisulizerPickerItemStateChanged

    private void refreshGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshGraphButtonActionPerformed
        if (this.kDC != null) {
            this.kDC.updateAlphaGain(Double.parseDouble(this.jTextFieldAlphaKinput.getText()));
            this.kDC.updateBetaCoeffient(Double.parseDouble(this.jTextFieldBetaKinput.getText()));
            this.kDC.updateMaxPathLength(Integer.parseInt(this.maxPathLengthJField.getText()));
            this.kDC.refreshKnowlageDifusionValues();
            
        }
        if(this.currentBidlg != null)
            this.updateJpanels(this.VisulizerPicker.getSelectedIndex(), true);
    }//GEN-LAST:event_refreshGraphButtonActionPerformed

    private void mouseModeChangerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mouseModeChangerActionPerformed
        this.currentMouseMode = ModalGraphMouse.Mode.values()[this.mouseModeChanger.getSelectedIndex()];
        this.updateVvMouseMode();
    }//GEN-LAST:event_mouseModeChangerActionPerformed

    private void runKDiffsuinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runKDiffsuinActionPerformed
        if (this.kDC == null || this.pFType != this.kDC.getPFType()) {
            this.kDC = new KnowledgeDiffusionCalculator(this.currentBidlg, this.pFType);
            jTextFieldBetaKinput.setText(Double.toString(this.kDC.getBetaKnowlageDifussionCoeffient()));
            jTextFieldAlphaKinput.setText(Double.toString(this.kDC.getAlphaGainValue()));
            this.maxPathLengthJField.setText(Integer.toString(this.kDC.getMaxPathLength()));
        }
        if(this.currentBidlg != null)
            this.updateJpanels(this.VisulizerPicker.getSelectedIndex(), true);
    }//GEN-LAST:event_runKDiffsuinActionPerformed

    private void exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportActionPerformed
        final JFileChooser fc = new JFileChooser();
        int op = fc.showSaveDialog(this);
        if (op == JFileChooser.APPROVE_OPTION && (this.vv != null || this.vvOneMode != null)) {
            File fileToWriteTo = new File(fc.getSelectedFile().getAbsolutePath() + ".png");
            System.out.println("Save as file: " + fileToWriteTo.getAbsolutePath());
            if(currentIndexOfSelectedView == 0 || currentIndexOfSelectedView == 1)
                this.createImageOfGraph(vv, fileToWriteTo);
            else
                this.createImageOfGraph(vvOneMode, fileToWriteTo);
        }
    }//GEN-LAST:event_exportActionPerformed

    private void pathFinderAlgoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pathFinderAlgoActionPerformed
        switch(this.pathFinderAlgo.getSelectedIndex()){
            case 0:
                this.pFType = BFS_ALL_PATHS;
                break;
            case 1:
                this.pFType = SHORTEST_PATHS;
                break;
            default:
                this.pFType = BFS_ALL_PATHS;
                break;
        }
    }//GEN-LAST:event_pathFinderAlgoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu EditMenu;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenuBar MainMenu;
    private javax.swing.JPanel OptionsPanel;
    private javax.swing.JComboBox<String> VisulizerPicker;
    private javax.swing.JMenuItem exit;
    private javax.swing.JMenuItem export;
    private javax.swing.JMenuItem importcvs;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTextField jTextFieldAlphaKinput;
    private javax.swing.JTextField jTextFieldBetaKinput;
    private javax.swing.JTextField maxPathLengthJField;
    private javax.swing.JComboBox<String> mouseModeChanger;
    private javax.swing.JComboBox pathFinderAlgo;
    private javax.swing.JButton refreshGraphButton;
    private javax.swing.JButton runKDiffsuin;
    // End of variables declaration//GEN-END:variables

}
