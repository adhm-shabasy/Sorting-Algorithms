import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainApp extends Application {

    private TabPane tabPane;

    //Comparison tab
    private ObservableList<ComparisonResult> comparisonData = FXCollections.observableArrayList();
    private CheckBox[] algoCheckboxes;
    private final String[] ALGO_NAMES = {"Selection Sort","Insertion Sort","Bubble Sort",
                                          "Merge Sort","Heap Sort","Quick Sort"};
    private RadioButton rbRandom, rbSorted, rbInverseSorted, rbFile;
    private TextField sizeField, runsField, boundField;
    private Label fileLabel;
    private String selectedFilePath = null;
    private ProgressBar compProgressBar;
    private Label compStatusLabel;
    private Button runCompBtn;

    //Visualization tab
    private ComboBox<String> vizAlgoCombo;
    private RadioButton vizRbRandom, vizRbSorted, vizRbInverseSorted, vizRbFile;
    private TextField vizSizeField;
    private Label vizFileLabel, vizStatusLabel, vizComparisonsLabel, vizInterchangesLabel;
    private String vizFilePath = null;
    private Slider speedSlider;
    private CheckBox soundCheckBox;
    private Button playPauseBtn;

    private List<int[]> vizSteps       = new ArrayList<>();
    private List<int[]> highlightPairs = new ArrayList<>();
    private List<Boolean> isCompStep   = new ArrayList<>();
    private int vizStepIndex = 0;
    private boolean isPlaying = false;
    private AnimationTimer animTimer;
    private long lastFrameNanos = 0;
    private Canvas canvas;
    private int[] currentHighlight = null;

    //Dual Visualization tab
    private ComboBox<String> dualAlgoComboA, dualAlgoComboB;
    private RadioButton dualRbRandom, dualRbSorted, dualRbInverseSorted, dualRbFile;
    private TextField dualSizeField;
    private Label dualFileLabel;
    private String dualFilePath = null;
    private Slider dualSpeedSlider;
    private CheckBox dualSoundCheckBox;
    private Button dualPlayPauseBtn;

    private List<int[]>   dualStepsA = new ArrayList<>(), dualStepsB = new ArrayList<>();
    private List<int[]>   dualHighlightA = new ArrayList<>(), dualHighlightB = new ArrayList<>();
    private List<Boolean> dualIsCompA = new ArrayList<>(), dualIsCompB = new ArrayList<>();
    private int dualStepIndexA = 0, dualStepIndexB = 0;
    private boolean dualIsPlaying = false;
    private AnimationTimer dualAnimTimer;
    private long dualLastFrameNanos = 0;
    private Canvas canvasA, canvasB;
    private Label dualStatusLabelA, dualStatusLabelB;
    private Label dualCompLabelA, dualCompLabelB, dualInterLabelA, dualInterLabelB;

    // audio
    private SourceDataLine audioLine;
    private ExecutorService audioPool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audio"); t.setDaemon(true); return t;
    });
    private volatile boolean audioShouldStop = false;

    @Override
    public void start(Stage stage) {
        initAudio();
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.getTabs().addAll(
            new Tab("Sorting Comparison",    buildComparisonTab()),
            new Tab("Sorting Visualization", buildVisualizationTab()),
            new Tab("Dual Visualization",    buildDualVisualizationTab())
        );
        Scene scene = new Scene(tabPane, 1300, 720);
        stage.setTitle("Sorting Algorithms – CSE224");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> { audioPool.shutdownNow(); if (audioLine!=null) audioLine.close(); });
        stage.show();
    }

    //  COMPARISON TAB
    private Pane buildComparisonTab() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        VBox controls = new VBox(10);
        controls.setPrefWidth(260);
        controls.setPadding(new Insets(0,12,0,0));

        TitledPane algoPane = new TitledPane();
        algoPane.setText("Algorithms"); algoPane.setCollapsible(false);
        VBox algoBox = new VBox(4);
        algoCheckboxes = new CheckBox[ALGO_NAMES.length];
        for (int i=0;i<ALGO_NAMES.length;i++) {
            algoCheckboxes[i] = new CheckBox(ALGO_NAMES[i]);
            algoCheckboxes[i].setSelected(true);
            algoBox.getChildren().add(algoCheckboxes[i]);
        }
        algoPane.setContent(algoBox);

        TitledPane inputPane = new TitledPane();
        inputPane.setText("Input"); inputPane.setCollapsible(false);
        VBox inputBox = new VBox(6);
        ToggleGroup tg = new ToggleGroup();
        rbRandom        = rb("Random",          tg, true);
        rbSorted        = rb("Sorted",           tg, false);
        rbInverseSorted = rb("Inversely Sorted", tg, false);
        rbFile          = rb("From File",        tg, false);
        sizeField  = new TextField("1000");
        boundField = new TextField("1000");
        runsField  = new TextField("5");
        fileLabel  = new Label("No file selected");
        fileLabel.setStyle("-fx-text-fill:gray;-fx-font-size:11;");
        Button chooseFileBtn = new Button("Choose File…");
        chooseFileBtn.disableProperty().bind(rbFile.selectedProperty().not());
        chooseFileBtn.setOnAction(e -> chooseCompFile());
        inputBox.getChildren().addAll(rbRandom,rbSorted,rbInverseSorted,rbFile,
            lf("Size (max 10000):",sizeField), lf("Bound:",boundField),
            lf("Runs:",runsField), chooseFileBtn, fileLabel);
        inputPane.setContent(inputBox);

        runCompBtn = new Button("▶  Run Comparison");
        runCompBtn.setMaxWidth(Double.MAX_VALUE);
        runCompBtn.setStyle("-fx-font-size:13;-fx-font-weight:bold;");
        runCompBtn.setOnAction(e -> runComparison());

        compProgressBar = new ProgressBar(0); compProgressBar.setMaxWidth(Double.MAX_VALUE);
        compStatusLabel = new Label("Ready"); compStatusLabel.setStyle("-fx-text-fill:gray;");
        Button clearBtn = new Button("Clear Results"); clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> comparisonData.clear());

        controls.getChildren().addAll(algoPane, inputPane, runCompBtn,
                compProgressBar, compStatusLabel, clearBtn);

        TableView<ComparisonResult> table = new TableView<>(comparisonData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().addAll(
            col("Algorithm","algorithm",120), col("Array Size","arraySize",80),
            col("Input Type","inputType",110), col("Runs","runs",50),
            col("Avg Time (ms)","avgTime",95), col("Min Time (ms)","minTime",95),
            col("Max Time (ms)","maxTime",95), col("Comparisons","comparisons",100),
            col("Interchanges","interchanges",100)
        );
        root.setLeft(controls); root.setCenter(table);
        return root;
    }

    private void runComparison() {
        List<String> selected = new ArrayList<>();
        for (int i=0;i<algoCheckboxes.length;i++)
            if (algoCheckboxes[i].isSelected()) selected.add(ALGO_NAMES[i]);
        if (selected.isEmpty()) { alert("No algorithm selected."); return; }
        int size,runs,bound;
        try {
            size  = Integer.parseInt(sizeField.getText().trim());
            runs  = Integer.parseInt(runsField.getText().trim());
            bound = Integer.parseInt(boundField.getText().trim());
            if (size<1||runs<1||bound<1) throw new NumberFormatException();
        } catch (NumberFormatException ex) { alert("Invalid numeric input."); return; }
        if (rbFile.isSelected() && selectedFilePath==null) { alert("Please choose a file."); return; }
        runCompBtn.setDisable(true);
        compProgressBar.setProgress(0);
        compStatusLabel.setText("Running…");
        int total = selected.size();
        int[] done = {0};
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(total,6));
        for (String algoName : selected) {
            Task<ComparisonResult> task = new Task<>() {
                @Override protected ComparisonResult call() throws Exception {
                    Sort sorter = createSorter(algoName);
                    int[] baseArray = buildArray(rbFile.isSelected()?selectedFilePath:null,
                        size, bound, rbSorted.isSelected()?1:rbInverseSorted.isSelected()?2:0);
                    long[] times = new long[runs];
                    long totalComp=0, totalInter=0;
                    for (int r=0;r<runs;r++) {
                        int[] arr = Arrays.copyOf(baseArray,baseArray.length);
                        sorter.reset();
                        long t0 = System.nanoTime();
                        sorter.sort(arr);
                        times[r] = System.nanoTime()-t0;
                        totalComp  += sorter.comparisons;
                        totalInter += sorter.interchanges;
                    }
                    long min = Arrays.stream(times).min().getAsLong();
                    long max = Arrays.stream(times).max().getAsLong();
                    double avg = Arrays.stream(times).average().getAsDouble();
                    String inputType = rbFile.isSelected() ? new File(selectedFilePath).getName()
                        : rbSorted.isSelected() ? "Sorted"
                        : rbInverseSorted.isSelected() ? "Inversely Sorted" : "Random";
                    return new ComparisonResult(algoName, String.valueOf(baseArray.length),
                        inputType, String.valueOf(runs),
                        String.format("%.4f", avg/1_000_000.0),
                        String.format("%.4f", min/1_000_000.0),
                        String.format("%.4f", max/1_000_000.0),
                        String.valueOf(totalComp/runs), String.valueOf(totalInter/runs));
                }
            };
            task.setOnSucceeded(e -> Platform.runLater(() -> {
                comparisonData.add(task.getValue());
                done[0]++;
                compProgressBar.setProgress((double)done[0]/total);
                if (done[0]==total) { compStatusLabel.setText("Done."); runCompBtn.setDisable(false); pool.shutdown(); }
            }));
            task.setOnFailed(e -> Platform.runLater(() -> {
                done[0]++;
                compStatusLabel.setText("Error: "+task.getException().getMessage());
                compProgressBar.setProgress((double)done[0]/total);
                if (done[0]==total) { runCompBtn.setDisable(false); pool.shutdown(); }
            }));
            pool.submit(task);
        }
    }

    //  VISUALIZATION TAB
    private Pane buildVisualizationTab() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));
        VBox controls = new VBox(10);
        controls.setPrefWidth(230);
        controls.setPadding(new Insets(0,12,0,0));

        vizAlgoCombo = new ComboBox<>(FXCollections.observableArrayList(ALGO_NAMES));
        vizAlgoCombo.getSelectionModel().selectFirst();
        controls.getChildren().addAll(new Label("Algorithm:"), vizAlgoCombo);

        ToggleGroup vtg = new ToggleGroup();
        vizRbRandom        = rb("Random",          vtg, true);
        vizRbSorted        = rb("Sorted",           vtg, false);
        vizRbInverseSorted = rb("Inversely Sorted", vtg, false);
        vizRbFile          = rb("From File",        vtg, false);
        vizSizeField = new TextField("80");
        vizFileLabel = new Label("No file selected");
        vizFileLabel.setStyle("-fx-text-fill:gray;-fx-font-size:11;");
        Button vizChooseFile = new Button("Choose File…");
        vizChooseFile.disableProperty().bind(vizRbFile.selectedProperty().not());
        vizChooseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(tabPane.getScene().getWindow());
            if (f!=null) { vizFilePath=f.getAbsolutePath(); vizFileLabel.setText(f.getName()); }
        });
        controls.getChildren().addAll(vizRbRandom,vizRbSorted,vizRbInverseSorted,vizRbFile,
            lf("Size (max 100):",vizSizeField), vizChooseFile, vizFileLabel);

        Button generateBtn = new Button("⚙  Generate & Load");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> prepareVisualization());

        controls.getChildren().add(new Label("Speed:"));
        speedSlider = new Slider(1, 500, 50);
        speedSlider.setShowTickLabels(true);
        speedSlider.setMajorTickUnit(150);
        speedSlider.setPrefWidth(200);
        speedSlider.valueProperty().addListener((o,ov,nv) -> { if (isPlaying) { stopPlay(); startPlay(); } });
        controls.getChildren().add(speedSlider);

        soundCheckBox = new CheckBox("Sound");
        soundCheckBox.setSelected(true);
        controls.getChildren().add(soundCheckBox);

        playPauseBtn = new Button("▶  Play");
        playPauseBtn.setMaxWidth(Double.MAX_VALUE);
        playPauseBtn.setDisable(true);
        playPauseBtn.setOnAction(e -> togglePlay());

        Button stepFwdBtn  = new Button("⏭  Step Fwd");
        Button stepBckBtn  = new Button("⏮  Step Back");
        Button resetVizBtn = new Button("↺  Reset");
        for (Button b : new Button[]{stepFwdBtn,stepBckBtn,resetVizBtn}) b.setMaxWidth(Double.MAX_VALUE);
        stepFwdBtn.setOnAction(e  -> { stopPlay(); stepForward(); });
        stepBckBtn.setOnAction(e  -> { stopPlay(); stepBackward(); });
        resetVizBtn.setOnAction(e -> { stopPlay(); resetViz(); });

        vizStatusLabel       = new Label("Step: 0 / 0");
        vizComparisonsLabel  = new Label("Comparisons: 0");
        vizInterchangesLabel = new Label("Interchanges: 0");
        Label legend = new Label("Yellow = Comparison   Red = Swap");
        legend.setStyle("-fx-font-size:11;");

        controls.getChildren().addAll(generateBtn, playPauseBtn,
            stepFwdBtn, stepBckBtn, resetVizBtn,
            vizStatusLabel, vizComparisonsLabel, vizInterchangesLabel, legend);

        canvas = new Canvas(800, 600);
        Pane canvasPane = new Pane(canvas);
        canvasPane.setStyle("-fx-background-color:#1e1e2e;");
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener(o -> redraw());
        canvas.heightProperty().addListener(o -> redraw());

        root.setLeft(controls);
        root.setCenter(canvasPane);
        return root;
    }

    private void prepareVisualization() {
        stopPlay();
        int size;
        try {
            size = Integer.parseInt(vizSizeField.getText().trim());
            if (size<1||size>100) throw new NumberFormatException();
        } catch (NumberFormatException ex) { alert("Size must be 1–100."); return; }
        if (vizRbFile.isSelected()&&vizFilePath==null) { alert("Please choose a file."); return; }
        int[] base;
        try {
            base = buildArray(vizRbFile.isSelected()?vizFilePath:null, size, 1000,
                vizRbSorted.isSelected()?1:vizRbInverseSorted.isSelected()?2:0);
        } catch (Exception ex) { alert("Error: "+ex.getMessage()); return; }
        if (base.length>100) base = Arrays.copyOf(base,100);
        base = shiftToPositive(base);

        vizSteps       = new ArrayList<>();
        highlightPairs = new ArrayList<>();
        isCompStep     = new ArrayList<>();
        vizSteps.add(Arrays.copyOf(base,base.length));
        highlightPairs.add(null);
        isCompStep.add(false);

        long[] compCount = {0};
        Sort sorter = buildCapturingSorter(vizAlgoCombo.getValue(), vizSteps, highlightPairs, isCompStep, compCount);
        sorter.sort(Arrays.copyOf(base,base.length));

        vizStepIndex = 0;
        currentHighlight = null;
        playPauseBtn.setDisable(false);
        redraw();
        updateVizLabels();
    }

    private void togglePlay() { if (isPlaying) stopPlay(); else startPlay(); }

    private void startPlay() {
        if (vizSteps.isEmpty() || vizStepIndex >= vizSteps.size()-1) return;
        isPlaying = true;
        audioShouldStop = false;
        playPauseBtn.setText("⏸  Pause");
        long intervalNanos = (long)(speedSlider.getValue() * 1_000_000L);
        lastFrameNanos = 0;
        animTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastFrameNanos==0) { lastFrameNanos=now; return; }
                long elapsed = now - lastFrameNanos;
                int steps = (int)(elapsed / intervalNanos);
                if (steps<1) return;
                lastFrameNanos += steps * intervalNanos;
                for (int i=0;i<steps;i++) {
                    if (vizStepIndex >= vizSteps.size()-1) { stopPlay(); return; }
                    vizStepIndex++;
                }
                currentHighlight = highlightPairs.get(vizStepIndex);
                redraw(); updateVizLabels(); playSound();
            }
        };
        animTimer.start();
    }

    private void stopPlay() {
        if (animTimer!=null) animTimer.stop();
        isPlaying = false;
        playPauseBtn.setText("▶  Play");
        stopAudio();
    }

    private void stopAudio() {
        audioShouldStop = true;
        if (audioLine != null) audioLine.flush();
    }

    private void stepForward() {
        if (vizSteps.isEmpty()||vizStepIndex>=vizSteps.size()-1) return;
        vizStepIndex++;
        currentHighlight = highlightPairs.get(vizStepIndex);
        redraw(); updateVizLabels(); playSound();
    }
    private void stepBackward() {
        if (vizSteps.isEmpty()||vizStepIndex<=0) return;
        vizStepIndex--;
        currentHighlight = highlightPairs.get(vizStepIndex);
        redraw(); updateVizLabels();
    }
    private void resetViz() {
        vizStepIndex=0; currentHighlight=null;
        if (!vizSteps.isEmpty()) { redraw(); updateVizLabels(); }
    }

    private void redraw() {
        if (vizSteps==null||vizSteps.isEmpty()) return;
        drawOnCanvas(canvas, vizSteps.get(vizStepIndex), currentHighlight, isCompStep.get(vizStepIndex));
    }

    private void drawOnCanvas(Canvas c, int[] arr, int[] highlight, boolean isComparison) {
        GraphicsContext gc = c.getGraphicsContext2D();
        double w = c.getWidth(), h = c.getHeight();
        gc.setFill(Color.web("#1e1e2e"));
        gc.fillRect(0,0,w,h);
        int n = arr.length;
        int max = Arrays.stream(arr).max().orElse(1);
        if (max==0) max=1;
        double barW = w / n;
        double padding = barW > 4 ? 1 : 0;
        for (int i=0;i<n;i++) {
            double barH = ((double)arr[i]/max) * (h-4);
            boolean hi = highlight!=null && (i==highlight[0]||i==highlight[1]);
            if (hi) {
                gc.setFill(isComparison ? Color.web("#f1fa8c") : Color.web("#ff5555"));
            } else {
                gc.setFill(Color.hsb(200+((double)arr[i]/max)*140, 0.85, 0.95));
            }
            gc.fillRect(i*barW+padding, h-barH, barW-padding*2, barH);
        }
    }

    private void updateVizLabels() {
        vizStatusLabel.setText("Step: "+vizStepIndex+" / "+(vizSteps.size()-1));
        long comps=0, swaps=0;
        for (int i=0;i<=vizStepIndex;i++) {
            if (isCompStep.get(i)) comps++;
            else if (highlightPairs.get(i)!=null) swaps++;
        }
        vizComparisonsLabel.setText("Comparisons: "+comps);
        vizInterchangesLabel.setText("Interchanges: "+swaps);
    }

    private void initAudio() {
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(fmt, 2048); // small buffer = less lag on stop
            audioLine.start();
        } catch (Exception e) { audioLine = null; }
    }

    private void playSound() {
        if (!soundCheckBox.isSelected() || audioLine==null) return;
        int[] arr = vizSteps.get(vizStepIndex);
        int[] pair = highlightPairs.get(vizStepIndex);
        if (pair==null) return;
        int max = Arrays.stream(arr).max().orElse(1);
        double freq = 200 + ((double)arr[pair[0]]/max) * 800;
        audioPool.submit(() -> { if (!audioShouldStop) beep(freq, 30); });
    }

    private void beep(double frequency, int durationMs) {
        if (audioLine==null || audioShouldStop) return;
        int sampleRate = 44100;
        int samples = (int)(sampleRate * durationMs / 1000.0);
        byte[] buf = new byte[samples*2];
        for (int i=0;i<samples;i++) {
            if (audioShouldStop) break;
            double angle = 2.0*Math.PI*i*frequency/sampleRate;
            double env = 1.0 - (double)i/samples;
            short s = (short)(Math.sin(angle) * env * 16000);
            buf[i*2]   = (byte)(s & 0xFF);
            buf[i*2+1] = (byte)((s>>8) & 0xFF);
        }
        if (!audioShouldStop) audioLine.write(buf, 0, buf.length);
    }

    //  DUAL VISUALIZATION TAB
    private Pane buildDualVisualizationTab() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        VBox controls = new VBox(8);
        controls.setPrefWidth(240);
        controls.setPadding(new Insets(0,12,0,0));

        dualAlgoComboA = new ComboBox<>(FXCollections.observableArrayList(ALGO_NAMES));
        dualAlgoComboA.getSelectionModel().selectFirst();
        dualAlgoComboB = new ComboBox<>(FXCollections.observableArrayList(ALGO_NAMES));
        dualAlgoComboB.getSelectionModel().select(1);
        controls.getChildren().addAll(
            new Label("Algorithm A:"), dualAlgoComboA,
            new Label("Algorithm B:"), dualAlgoComboB);

        ToggleGroup dtg = new ToggleGroup();
        dualRbRandom        = rb("Random",          dtg, true);
        dualRbSorted        = rb("Sorted",           dtg, false);
        dualRbInverseSorted = rb("Inversely Sorted", dtg, false);
        dualRbFile          = rb("From File",        dtg, false);
        dualSizeField = new TextField("60");
        dualFileLabel = new Label("No file selected");
        dualFileLabel.setStyle("-fx-text-fill:gray;-fx-font-size:11;");
        Button dualChooseFile = new Button("Choose File…");
        dualChooseFile.disableProperty().bind(dualRbFile.selectedProperty().not());
        dualChooseFile.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            File f = fc.showOpenDialog(tabPane.getScene().getWindow());
            if (f!=null) { dualFilePath=f.getAbsolutePath(); dualFileLabel.setText(f.getName()); }
        });
        controls.getChildren().addAll(dualRbRandom,dualRbSorted,dualRbInverseSorted,dualRbFile,
            lf("Size (max 100):",dualSizeField), dualChooseFile, dualFileLabel);

        Button dualGenerateBtn = new Button("⚙  Generate & Load Both");
        dualGenerateBtn.setMaxWidth(Double.MAX_VALUE);
        dualGenerateBtn.setOnAction(e -> prepareDualVisualization());

        controls.getChildren().add(new Label("Speed:"));
        dualSpeedSlider = new Slider(1, 500, 50);
        dualSpeedSlider.setShowTickLabels(true);
        dualSpeedSlider.setMajorTickUnit(150);
        dualSpeedSlider.setPrefWidth(200);
        dualSpeedSlider.valueProperty().addListener((o,ov,nv) -> { if (dualIsPlaying) { stopDualPlay(); startDualPlay(); } });
        controls.getChildren().add(dualSpeedSlider);

        dualSoundCheckBox = new CheckBox("Sound");
        dualSoundCheckBox.setSelected(true);
        controls.getChildren().add(dualSoundCheckBox);

        dualPlayPauseBtn = new Button("▶  Play Both");
        dualPlayPauseBtn.setMaxWidth(Double.MAX_VALUE);
        dualPlayPauseBtn.setDisable(true);
        dualPlayPauseBtn.setOnAction(e -> toggleDualPlay());

        Button dualStepFwd = new Button("⏭  Step Fwd");
        Button dualStepBck = new Button("⏮  Step Back");
        Button dualReset   = new Button("↺  Reset Both");
        for (Button b : new Button[]{dualStepFwd,dualStepBck,dualReset}) b.setMaxWidth(Double.MAX_VALUE);
        dualStepFwd.setOnAction(e -> { stopDualPlay(); dualStepForward(); });
        dualStepBck.setOnAction(e -> { stopDualPlay(); dualStepBackward(); });
        dualReset.setOnAction(e   -> { stopDualPlay(); resetDualViz(); });

        Label dualLegend = new Label("Yellow = Comparison   Red = Swap");
        dualLegend.setStyle("-fx-font-size:11;");
        controls.getChildren().addAll(dualGenerateBtn, dualPlayPauseBtn,
            dualStepFwd, dualStepBck, dualReset, dualLegend);

        canvasA = new Canvas(); canvasB = new Canvas();
        dualStatusLabelA = new Label("Step: 0 / 0"); dualStatusLabelB = new Label("Step: 0 / 0");
        dualCompLabelA   = new Label("Comparisons: 0"); dualCompLabelB = new Label("Comparisons: 0");
        dualInterLabelA  = new Label("Interchanges: 0"); dualInterLabelB = new Label("Interchanges: 0");

        Pane paneA = new Pane(canvasA); paneA.setStyle("-fx-background-color:#1e1e2e;");
        canvasA.widthProperty().bind(paneA.widthProperty());
        canvasA.heightProperty().bind(paneA.heightProperty());
        canvasA.widthProperty().addListener(o -> redrawDual());
        canvasA.heightProperty().addListener(o -> redrawDual());

        Pane paneB = new Pane(canvasB); paneB.setStyle("-fx-background-color:#1e1e2e;");
        canvasB.widthProperty().bind(paneB.widthProperty());
        canvasB.heightProperty().bind(paneB.heightProperty());
        canvasB.widthProperty().addListener(o -> redrawDual());
        canvasB.heightProperty().addListener(o -> redrawDual());

        Label titleA = new Label("Algorithm A"); titleA.setStyle("-fx-font-weight:bold;-fx-font-size:13;");
        Label titleB = new Label("Algorithm B"); titleB.setStyle("-fx-font-weight:bold;-fx-font-size:13;");

        VBox boxA = new VBox(4, titleA, paneA, dualStatusLabelA, dualCompLabelA, dualInterLabelA);
        VBox boxB = new VBox(4, titleB, paneB, dualStatusLabelB, dualCompLabelB, dualInterLabelB);
        VBox.setVgrow(paneA, Priority.ALWAYS);
        VBox.setVgrow(paneB, Priority.ALWAYS);

        HBox canvasArea = new HBox(8, boxA, boxB);
        HBox.setHgrow(boxA, Priority.ALWAYS);
        HBox.setHgrow(boxB, Priority.ALWAYS);

        root.setLeft(controls);
        root.setCenter(canvasArea);
        return root;
    }

    private void prepareDualVisualization() {
        stopDualPlay();
        int size;
        try {
            size = Integer.parseInt(dualSizeField.getText().trim());
            if (size<1||size>100) throw new NumberFormatException();
        } catch (NumberFormatException ex) { alert("Size must be 1–100."); return; }
        if (dualRbFile.isSelected()&&dualFilePath==null) { alert("Please choose a file."); return; }
        int[] base;
        try {
            base = buildArray(dualRbFile.isSelected()?dualFilePath:null, size, 1000,
                dualRbSorted.isSelected()?1:dualRbInverseSorted.isSelected()?2:0);
        } catch (Exception ex) { alert("Error: "+ex.getMessage()); return; }
        if (base.length>100) base = Arrays.copyOf(base,100);
        base = shiftToPositive(base);

        dualStepsA=new ArrayList<>(); dualHighlightA=new ArrayList<>(); dualIsCompA=new ArrayList<>();
        dualStepsA.add(Arrays.copyOf(base,base.length)); dualHighlightA.add(null); dualIsCompA.add(false);
        long[] cA={0};
        buildCapturingSorter(dualAlgoComboA.getValue(), dualStepsA, dualHighlightA, dualIsCompA, cA)
            .sort(Arrays.copyOf(base,base.length));

        dualStepsB=new ArrayList<>(); dualHighlightB=new ArrayList<>(); dualIsCompB=new ArrayList<>();
        dualStepsB.add(Arrays.copyOf(base,base.length)); dualHighlightB.add(null); dualIsCompB.add(false);
        long[] cB={0};
        buildCapturingSorter(dualAlgoComboB.getValue(), dualStepsB, dualHighlightB, dualIsCompB, cB)
            .sort(Arrays.copyOf(base,base.length));

        dualStepIndexA=0; dualStepIndexB=0;
        dualPlayPauseBtn.setDisable(false);
        redrawDual(); updateDualLabels();
    }

    private void toggleDualPlay() { if (dualIsPlaying) stopDualPlay(); else startDualPlay(); }

    private void startDualPlay() {
        boolean aDone = dualStepsA.isEmpty() || dualStepIndexA >= dualStepsA.size()-1;
        boolean bDone = dualStepsB.isEmpty() || dualStepIndexB >= dualStepsB.size()-1;
        if (aDone && bDone) return;
        dualIsPlaying = true;
        audioShouldStop = false;
        dualPlayPauseBtn.setText("⏸  Pause Both");
        long intervalNanos = (long)(dualSpeedSlider.getValue() * 1_000_000L);
        dualLastFrameNanos = 0;
        dualAnimTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (dualLastFrameNanos==0) { dualLastFrameNanos=now; return; }
                int steps = (int)((now-dualLastFrameNanos)/intervalNanos);
                if (steps<1) return;
                dualLastFrameNanos += steps*intervalNanos;
                for (int i=0;i<steps;i++) {
                    if (dualStepIndexA < dualStepsA.size()-1) dualStepIndexA++;
                    if (dualStepIndexB < dualStepsB.size()-1) dualStepIndexB++;
                }
                redrawDual(); updateDualLabels();
                if (dualSoundCheckBox.isSelected()) playDualSound();
                if (dualStepIndexA>=dualStepsA.size()-1 && dualStepIndexB>=dualStepsB.size()-1) {
                    stopDualPlay();
                }
            }
        };
        dualAnimTimer.start();
    }

    private void stopDualPlay() {
        if (dualAnimTimer!=null) dualAnimTimer.stop();
        dualIsPlaying = false;
        dualPlayPauseBtn.setText("▶  Play Both");
        stopAudio();
    }

    private void dualStepForward() {
        if (!dualStepsA.isEmpty() && dualStepIndexA<dualStepsA.size()-1) dualStepIndexA++;
        if (!dualStepsB.isEmpty() && dualStepIndexB<dualStepsB.size()-1) dualStepIndexB++;
        redrawDual(); updateDualLabels();
    }
    private void dualStepBackward() {
        if (!dualStepsA.isEmpty() && dualStepIndexA>0) dualStepIndexA--;
        if (!dualStepsB.isEmpty() && dualStepIndexB>0) dualStepIndexB--;
        redrawDual(); updateDualLabels();
    }
    private void resetDualViz() {
        dualStepIndexA=0; dualStepIndexB=0;
        if (!dualStepsA.isEmpty()) { redrawDual(); updateDualLabels(); }
    }

    private void redrawDual() {
        if (!dualStepsA.isEmpty())
            drawOnCanvas(canvasA, dualStepsA.get(dualStepIndexA), dualHighlightA.get(dualStepIndexA), dualIsCompA.get(dualStepIndexA));
        if (!dualStepsB.isEmpty())
            drawOnCanvas(canvasB, dualStepsB.get(dualStepIndexB), dualHighlightB.get(dualStepIndexB), dualIsCompB.get(dualStepIndexB));
    }

    private void updateDualLabels() {
        updatePanelLabels(dualStepsA, dualHighlightA, dualIsCompA, dualStepIndexA,
            dualStatusLabelA, dualCompLabelA, dualInterLabelA);
        updatePanelLabels(dualStepsB, dualHighlightB, dualIsCompB, dualStepIndexB,
            dualStatusLabelB, dualCompLabelB, dualInterLabelB);
    }
    private void updatePanelLabels(List<int[]> steps, List<int[]> hi, List<Boolean> comp,
            int idx, Label status, Label compL, Label interL) {
        if (steps.isEmpty()) return;
        status.setText("Step: "+idx+" / "+(steps.size()-1));
        long c=0, s=0;
        for (int i=0;i<=idx;i++) {
            if (comp.get(i)) c++;
            else if (hi.get(i)!=null) s++;
        }
        compL.setText("Comparisons: "+c);
        interL.setText("Interchanges: "+s);
    }

    private void playDualSound() {
        if (audioLine==null || dualStepsA.isEmpty()) return;
        int[] arr = dualStepsA.get(dualStepIndexA);
        int[] pair = dualHighlightA.get(dualStepIndexA);
        if (pair==null) return;
        int max = Arrays.stream(arr).max().orElse(1);
        double freq = 200 + ((double)arr[pair[0]]/max)*800;
        audioPool.submit(() -> { if (!audioShouldStop) beep(freq, 30); });
    }

    private RadioButton rb(String text, ToggleGroup tg, boolean selected) {
        RadioButton rb = new RadioButton(text); rb.setToggleGroup(tg); rb.setSelected(selected); return rb;
    }
    private HBox lf(String label, TextField tf) {
        tf.setPrefWidth(90);
        HBox hb = new HBox(6, new Label(label), tf);
        hb.setAlignment(Pos.CENTER_LEFT); return hb;
    }
    @SuppressWarnings("unchecked")
    private <S> TableColumn<S,String> col(String title, String prop, double w) {
        TableColumn<S,String> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(prop));
        c.setPrefWidth(w); return c;
    }
    private void chooseCompFile() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files","*.txt","*.csv","*.*"));
        File f = fc.showOpenDialog(tabPane.getScene().getWindow());
        if (f!=null) { selectedFilePath=f.getAbsolutePath(); fileLabel.setText(f.getName()); }
    }
    private int[] buildArray(String filePath, int size, int bound, int op) throws IOException {
        if (filePath!=null) {
            return new InputReader().readIntegerFile(filePath).stream().mapToInt(Integer::intValue).toArray();
        }
        return new Generator().generate(size, op, bound);
    }
    private Sort createSorter(String name) {
        return switch(name) {
            case "Selection Sort" -> new SelectionSort();
            case "Insertion Sort" -> new InsertionSort();
            case "Bubble Sort"    -> new BubbleSort();
            case "Merge Sort"     -> new MergeSort();
            case "Heap Sort"      -> new HeapSort();
            case "Quick Sort"     -> new QuickSort();
            default -> throw new IllegalArgumentException("Unknown: "+name);
        };
    }
    private void alert(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    private int[] shiftToPositive(int[] array) {
        int min = Arrays.stream(array).min().orElse(0);
        int shift = min - 1; // subtract this to make min become 1
        int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) result[i] = array[i] - shift;
        return result;
    }

    private static Sort buildCapturingSorter(String name,
            List<int[]> steps, List<int[]> pairs, List<Boolean> compFlags, long[] compCount) {
        return switch(name) {

            case "Selection Sort" -> new SelectionSort() {
                @Override public void swap(int[] a,int i,int j) {
                    super.swap(a,i,j);
                    steps.add(Arrays.copyOf(a,a.length)); pairs.add(new int[]{i,j}); compFlags.add(false);
                }
                @Override public void sort(int[] array) {
                    for (int i=0;i<array.length;i++) {
                        int min=array[i], index=i;
                        for (int j=i;j<array.length;j++) {
                            comparisons++; compCount[0]++;
                            steps.add(Arrays.copyOf(array,array.length));
                            pairs.add(new int[]{index,j}); compFlags.add(true);
                            if (array[j]<min) { min=array[j]; index=j; }
                        }
                        if (index!=i) { swap(array,i,index); interchanges++; }
                    }
                }
            };

            case "Insertion Sort" -> new InsertionSort() {
                @Override public void swap(int[] a,int i,int j) {
                    super.swap(a,i,j);
                    steps.add(Arrays.copyOf(a,a.length)); pairs.add(new int[]{i,j}); compFlags.add(false);
                }
                @Override public void sort(int[] array) {
                    for (int i=1;i<array.length;i++) {
                        int temp=i;
                        for (int j=temp-1;j>=0;j--) {
                            comparisons++; compCount[0]++;
                            steps.add(Arrays.copyOf(array,array.length));
                            pairs.add(new int[]{j,temp}); compFlags.add(true);
                            if (array[j]>array[temp]) { swap(array,j,temp); interchanges++; temp--; }
                            else break;
                        }
                    }
                }
            };

            case "Bubble Sort" -> new BubbleSort() {
                @Override public void swap(int[] a,int i,int j) {
                    super.swap(a,i,j);
                    steps.add(Arrays.copyOf(a,a.length)); pairs.add(new int[]{i,j}); compFlags.add(false);
                }
                @Override public void sort(int[] array) {
                    for (int i=0;i+1<array.length;i++) {
                        for (int j=0;j+1<array.length;j++) {
                            comparisons++; compCount[0]++;
                            steps.add(Arrays.copyOf(array,array.length));
                            pairs.add(new int[]{j,j+1}); compFlags.add(true);
                            if (array[j]>array[j+1]) { swap(array,j,j+1); interchanges++; }
                        }
                    }
                }
            };

            case "Merge Sort" -> new Sort() {
                int[] full;
                @Override public void sort(int[] array) {
                    full = array;
                    mergeSort(0, array.length - 1);
                    compCount[0] = comparisons;
                }
                void mergeSort(int lo, int hi) {
                    if (lo >= hi) return;
                    int mid = (lo + hi) / 2;
                    mergeSort(lo, mid);
                    mergeSort(mid + 1, hi);
                    merge(lo, mid, hi);
                }
                void merge(int lo, int mid, int hi) {
                    int[] left  = Arrays.copyOfRange(full, lo, mid + 1);
                    int[] right = Arrays.copyOfRange(full, mid + 1, hi + 1);
                    int i = 0, j = 0, k = lo;
                    while (i < left.length && j < right.length) {
                        comparisons++; compCount[0]++;
                        steps.add(Arrays.copyOf(full, full.length));
                        pairs.add(new int[]{lo + i, mid + 1 + j}); compFlags.add(true);
                        if (left[i] <= right[j]) full[k++] = left[i++];
                        else                     full[k++] = right[j++];
                        interchanges++;
                        steps.add(Arrays.copyOf(full, full.length));
                        pairs.add(new int[]{k - 1, k - 1}); compFlags.add(false);
                    }
                    while (i < left.length)  {
                        full[k] = left[i++]; interchanges++;
                        steps.add(Arrays.copyOf(full, full.length));
                        pairs.add(new int[]{k, k}); compFlags.add(false); k++;
                    }
                    while (j < right.length) {
                        full[k] = right[j++]; interchanges++;
                        steps.add(Arrays.copyOf(full, full.length));
                        pairs.add(new int[]{k, k}); compFlags.add(false); k++;
                    }
                }
            };

            case "Heap Sort" -> new Sort() {
                int heapLength;
                @Override public void sort(int[] array) {
                    heapLength = array.length;
                    buildMaxHeap(array);
                    int end = array.length - 1;
                    while (heapLength != 0 && end >= 0) {
                        swap(array, 0, end); interchanges++;
                        end--; heapLength--;
                        maxHeapify(0, array);
                    }
                    compCount[0] = comparisons;
                }
                @Override public void swap(int[] a, int i, int j) {
                    super.swap(a, i, j);
                    steps.add(Arrays.copyOf(a, a.length)); pairs.add(new int[]{i, j}); compFlags.add(false);
                }
                void buildMaxHeap(int[] array) {
                    for (int i = array.length / 2; i >= 0; i--) maxHeapify(i, array);
                }
                void maxHeapify(int i, int[] array) {
                    int l = (2 * i) + 1, r = (2 * i) + 2, largest = i;
                    if (r < heapLength) {
                        comparisons++; compCount[0]++;
                        steps.add(Arrays.copyOf(array, array.length));
                        pairs.add(new int[]{r, i}); compFlags.add(true);
                        if (array[r] > array[i]) largest = r;
                    }
                    if (l < heapLength) {
                        comparisons++; compCount[0]++;
                        steps.add(Arrays.copyOf(array, array.length));
                        pairs.add(new int[]{l, largest}); compFlags.add(true);
                        if (array[l] > array[largest]) largest = l;
                    }
                    if (largest != i) { swap(array, i, largest); interchanges++; maxHeapify(largest, array); }
                }
            };

            case "Quick Sort" -> new Sort() {
                @Override public void sort(int[] array) {
                    partition(array, 0, array.length - 1);
                    compCount[0] = comparisons;
                }
                @Override public void swap(int[] a, int i, int j) {
                    super.swap(a, i, j);
                    steps.add(Arrays.copyOf(a, a.length)); pairs.add(new int[]{i, j}); compFlags.add(false);
                }
                void partition(int[] array, int l, int h) {
                    if (l >= h) return;
                    int pivotIndex = l + (h - l) / 2;
                    int p = array[pivotIndex];
                    swap(array, pivotIndex, l);
                    int i = l, j = l + 1;
                    while (j <= h) {
                        comparisons++; compCount[0]++;
                        steps.add(Arrays.copyOf(array, array.length));
                        pairs.add(new int[]{j, l}); compFlags.add(true);
                        if (array[j] >= p) { j++; }
                        else { i++; swap(array, i, j); j++; interchanges++; }
                    }
                    if (i != l) { swap(array, i, l); interchanges++; }
                    partition(array, l, i - 1);
                    partition(array, i + 1, h);
                }
            };

            default -> throw new IllegalArgumentException("Unknown: "+name);
        };
    }

    public static class ComparisonResult {
        private final SimpleStringProperty algorithm,arraySize,inputType,runs,
            avgTime,minTime,maxTime,comparisons,interchanges;
        public ComparisonResult(String a,String b,String c,String d,String e,
                                String f,String g,String h,String ii){
            algorithm=new SimpleStringProperty(a); arraySize=new SimpleStringProperty(b);
            inputType=new SimpleStringProperty(c); runs=new SimpleStringProperty(d);
            avgTime=new SimpleStringProperty(e);   minTime=new SimpleStringProperty(f);
            maxTime=new SimpleStringProperty(g);   comparisons=new SimpleStringProperty(h);
            interchanges=new SimpleStringProperty(ii);
        }
        public String getAlgorithm()    { return algorithm.get(); }
        public String getArraySize()    { return arraySize.get(); }
        public String getInputType()    { return inputType.get(); }
        public String getRuns()         { return runs.get(); }
        public String getAvgTime()      { return avgTime.get(); }
        public String getMinTime()      { return minTime.get(); }
        public String getMaxTime()      { return maxTime.get(); }
        public String getComparisons()  { return comparisons.get(); }
        public String getInterchanges() { return interchanges.get(); }
    }

    public static void main(String[] args) { launch(args); }
}
