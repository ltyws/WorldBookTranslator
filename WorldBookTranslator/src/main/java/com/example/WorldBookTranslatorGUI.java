package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class WorldBookTranslatorGUI extends Application {
    private static final String DEFAULT_TITLE = "世界书翻译器";
    private static final String DEFAULT_BACKGROUND = "#f0f0f0";
    
    private Stage primaryStage;
    private TextField apiUrlField;
    private TextField apiKeyField;
    private TextField modelField;
    private TextArea logArea;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label titleLabel;
    private BorderPane rootPane;
    
    private File selectedFile;
    private JsonNode rootNode;
    private OpenAITranslator translator;
    private CacheManager cacheManager;
    private Set<String> processedEntries;
    private boolean isTranslating = false;
    
    private String currentTitle = DEFAULT_TITLE;
    private String currentBackground = DEFAULT_BACKGROUND;
    
    // 轮换标题列表
    private String[] titles = {
        "世界书翻译器",
        "AI酒馆世界书翻译助手",
        "世界书翻译工具",
        "AI翻译助手"
    };
    
    // 轮换标题索引
    private int titleIndex = 0;
    
    // 轮换背景图片列表
    private List<String> backgroundImages = new ArrayList<>();
    
    // 轮换背景图片索引
    private int backgroundIndex = 0;
    
    private ListView<String> entryListView;
    private TextArea commentArea;
    private TextArea contentArea;
    private TextArea keyArea;
    private Button saveButton;
    private Button backButton;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.cacheManager = new CacheManager();
        this.processedEntries = new HashSet<>();
        
        // 加载backgrounds目录中的图片
        loadBackgroundImages();
        
        primaryStage.setTitle(currentTitle);
        primaryStage.setWidth(900);
        primaryStage.setHeight(700);
        
        rootPane = new BorderPane();
        rootPane.setStyle("-fx-background-color: " + currentBackground + ";");
        
        setupMainScene();
        
        Scene scene = new Scene(rootPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    // 加载backgrounds目录中的图片
    private void loadBackgroundImages() {
        File backgroundsDir = new File("backgrounds");
        if (backgroundsDir.exists() && backgroundsDir.isDirectory()) {
            File[] files = backgroundsDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".png");
                }
            });
            if (files != null) {
                for (File file : files) {
                    backgroundImages.add(file.getAbsolutePath());
                }
                updateLog("加载了 " + backgroundImages.size() + " 张背景图片");
            }
        } else {
            // 如果backgrounds目录不存在，创建一个
            backgroundsDir.mkdir();
            updateLog("backgrounds目录不存在，已创建");
        }
    }
    
    private void setupMainScene() {
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(20));
        
        // 标题
        titleLabel = new Label(currentTitle);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-text-fill: #333333;");
        
        // API配置区域
        GridPane apiConfigPane = new GridPane();
        apiConfigPane.setHgap(10);
        apiConfigPane.setVgap(10);
        apiConfigPane.setPadding(new Insets(15));
        apiConfigPane.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        
        Label apiUrlLabel = new Label("API地址:");
        apiUrlField = new TextField();
        apiUrlField.setPromptText("https://api.openai.com/v1/chat/completions");
        apiUrlField.setPrefWidth(400);
        
        Label apiKeyLabel = new Label("API密钥:");
        apiKeyField = new TextField();
        apiKeyField.setPromptText("请输入您的API密钥");
        apiKeyField.setPrefWidth(400);
        
        Label modelLabel = new Label("模型:");
        modelField = new TextField();
        modelField.setPromptText("gpt-3.5-turbo");
        modelField.setPrefWidth(400);
        
        apiConfigPane.add(apiUrlLabel, 0, 0);
        apiConfigPane.add(apiUrlField, 1, 0);
        apiConfigPane.add(apiKeyLabel, 0, 1);
        apiConfigPane.add(apiKeyField, 1, 1);
        apiConfigPane.add(modelLabel, 0, 2);
        apiConfigPane.add(modelField, 1, 2);
        
        // 文件选择区域
        HBox fileSelectionPane = new HBox(10);
        fileSelectionPane.setPadding(new Insets(10));
        fileSelectionPane.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        
        Button selectFileButton = new Button("选择JSON文件");
        selectFileButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        selectFileButton.setOnAction(e -> selectJsonFile());
        
        Label fileLabel = new Label("未选择文件");
        fileLabel.setStyle("-fx-text-fill: #666666;");
        
        fileSelectionPane.getChildren().addAll(selectFileButton, fileLabel);
        fileSelectionPane.setAlignment(Pos.CENTER_LEFT);
        
        // 操作按钮区域
        HBox buttonPane = new HBox(10);
        buttonPane.setPadding(new Insets(10));
        buttonPane.setAlignment(Pos.CENTER);
        
        Button translateButton = new Button("开始翻译");
        translateButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        translateButton.setOnAction(e -> startTranslation());
        
        Button viewEntriesButton = new Button("查看条目");
        viewEntriesButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        viewEntriesButton.setOnAction(e -> viewEntries());
        viewEntriesButton.setDisable(true);
        
        Button settingsButton = new Button("设置");
        settingsButton.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        settingsButton.setOnAction(e -> showSettings());
        
        buttonPane.getChildren().addAll(translateButton, viewEntriesButton, settingsButton);
        
        // 进度区域
        VBox progressPane = new VBox(5);
        progressPane.setPadding(new Insets(10));
        progressPane.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        
        progressBar = new ProgressBar();
        progressBar.setProgress(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        
        progressLabel = new Label("准备就绪");
        progressLabel.setStyle("-fx-text-fill: #666666;");
        
        progressPane.getChildren().addAll(progressBar, progressLabel);
        
        // 日志区域
        VBox logPane = new VBox(5);
        logPane.setPadding(new Insets(10));
        logPane.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        
        Label logLabel = new Label("翻译日志:");
        logLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        VBox.setVgrow(logArea, Priority.ALWAYS);
        
        logPane.getChildren().addAll(logLabel, logArea);
        VBox.setVgrow(logPane, Priority.ALWAYS);
        
        // 添加到主容器
        mainContainer.getChildren().addAll(titleLabel, apiConfigPane, fileSelectionPane, buttonPane, progressPane, logPane);
        
        rootPane.setCenter(mainContainer);
    }
    
    private void selectJsonFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择世界书JSON文件");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON文件", "*.json"));
        
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            selectedFile = file;
            updateLog("已选择文件: " + file.getName());
            
            // 更新文件标签
            HBox fileSelectionPane = (HBox) ((VBox) rootPane.getCenter()).getChildren().get(2);
            Label fileLabel = (Label) fileSelectionPane.getChildren().get(1);
            fileLabel.setText(file.getName());
            
            // 启用查看条目按钮
            HBox buttonPane = (HBox) ((VBox) rootPane.getCenter()).getChildren().get(3);
            Button viewEntriesButton = (Button) buttonPane.getChildren().get(1);
            viewEntriesButton.setDisable(false);
            
            // 读取JSON文件
            try {
                ObjectMapper mapper = new ObjectMapper();
                rootNode = mapper.readTree(file);
                updateLog("文件读取成功，共 " + getEntryCount() + " 个条目");
            } catch (Exception e) {
                updateLog("错误：无法读取文件 - " + e.getMessage());
                showAlert("错误", "无法读取文件: " + e.getMessage());
            }
        }
    }
    
    private int getEntryCount() {
        if (rootNode != null && rootNode.has("entries")) {
            return rootNode.get("entries").size();
        }
        return 0;
    }
    
    private void startTranslation() {
        if (isTranslating) {
            showAlert("提示", "翻译正在进行中，请稍候...");
            return;
        }
        
        if (selectedFile == null) {
            showAlert("错误", "请先选择JSON文件");
            return;
        }
        
        String apiUrl = apiUrlField.getText().trim();
        String apiKey = apiKeyField.getText().trim();
        String model = modelField.getText().trim();
        
        if (apiUrl.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            showAlert("错误", "请填写完整的API配置信息");
            return;
        }
        
        // 初始化翻译器
        translator = new OpenAITranslator(apiUrl, apiKey, model);
        
        // 尝试加载缓存
        try {
            CacheManager.CacheData cacheData = cacheManager.loadCache();
            if (cacheData != null) {
                rootNode = cacheData.getRootNode();
                processedEntries = cacheData.getProcessedEntries();
                updateLog("从缓存加载数据，已处理 " + processedEntries.size() + " 个条目");
            } else {
                processedEntries = new HashSet<>();
            }
        } catch (Exception e) {
            updateLog("缓存加载失败，将从头开始翻译");
            processedEntries = new HashSet<>();
        }
        
        // 开始翻译
        isTranslating = true;
        updateProgress(0, "开始翻译...");
        
        new Thread(() -> {
            try {
                translateEntries();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateLog("翻译错误: " + e.getMessage());
                    showAlert("错误", "翻译过程中发生错误: " + e.getMessage());
                    isTranslating = false;
                });
            }
        }).start();
    }
    
    private void translateEntries() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        final int totalEntries = getEntryCount();
        final int[] processedCount = new int[1];
        processedCount[0] = 0;
        
        if (rootNode.has("entries")) {
            JsonNode entriesNode = rootNode.get("entries");
            Iterator<Map.Entry<String, JsonNode>> iterator = ((ObjectNode) entriesNode).fields();
            
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                String key = entry.getKey();
                JsonNode entryNode = entry.getValue();
                
                // 跳过已处理的条目
                if (processedEntries.contains(key)) {
                    processedCount[0]++;
                    updateProgress((double) processedCount[0] / totalEntries, "跳过已处理的条目 " + key);
                    continue;
                }
                
                updateLog("处理条目 " + key + "...");
                
                try {
                    // 翻译comment
                    if (entryNode.has("comment") && !entryNode.get("comment").isNull()) {
                        String originalComment = entryNode.get("comment").asText();
                        if (!originalComment.isEmpty()) {
                            updateLog("翻译comment...");
                            String translatedComment = translator.translate(originalComment);
                            ((ObjectNode) entryNode).put("comment", translatedComment);
                            updateLog("comment翻译完成");
                        }
                    }
                    
                    // 翻译content
                    if (entryNode.has("content") && !entryNode.get("content").isNull()) {
                        String originalContent = entryNode.get("content").asText();
                        if (!originalContent.isEmpty()) {
                            updateLog("翻译content...");
                            TextSplitter splitter = new TextSplitter();
                            String translatedContent = splitter.splitAndTranslate(originalContent, translator);
                            ((ObjectNode) entryNode).put("content", translatedContent);
                            updateLog("content翻译完成");
                        }
                    }
                    
                    // 翻译key
                    if (entryNode.has("key") && !entryNode.get("key").isNull() && entryNode.get("key").isArray()) {
                        updateLog("翻译key...");
                        ArrayNode keyArray = (ArrayNode) entryNode.get("key");
                        for (int i = 0; i < keyArray.size(); i++) {
                            String originalKey = keyArray.get(i).asText();
                            if (!originalKey.isEmpty()) {
                                String translatedKey = translator.translate(originalKey);
                                keyArray.set(i, mapper.valueToTree(translatedKey));
                            }
                        }
                        updateLog("key翻译完成");
                    }
                    
                    // 标记为已处理
                    processedEntries.add(key);
                    processedCount[0]++;
                    
                    // 更新进度
                    double progress = (double) processedCount[0] / totalEntries;
                    updateProgress(progress, "正在处理: " + key + " (" + processedCount[0] + "/" + totalEntries + ")");
                    
                    // 每处理5个条目保存一次缓存
                    if (processedCount[0] % 5 == 0) {
                        cacheManager.saveCache(rootNode, processedEntries);
                    }
                } catch (Exception e) {
                    updateLog("处理条目 " + key + " 时出错：" + e.getMessage());
                    // 保存当前进度
                    cacheManager.saveCache(rootNode, processedEntries);
                    throw e;
                }
            }
        }
        
        // 翻译完成
        Platform.runLater(() -> {
            updateProgress(1.0, "翻译完成！");
            updateLog("翻译完成！共处理 " + processedCount[0] + " 个条目");
            
            // 保存输出文件
            try {
                File outputFile = new File(selectedFile.getParent(), "output_" + selectedFile.getName());
                mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, rootNode);
                updateLog("输出文件已生成: " + outputFile.getName());
                showAlert("完成", "翻译完成！输出文件: " + outputFile.getName());
            } catch (Exception e) {
                updateLog("保存输出文件失败: " + e.getMessage());
                showAlert("错误", "保存输出文件失败: " + e.getMessage());
            }
            
            // 清除缓存
            cacheManager.clearCache();
            isTranslating = false;
        });
    }
    
    private void viewEntries() {
        if (rootNode == null || !rootNode.has("entries")) {
            showAlert("错误", "没有可查看的条目");
            return;
        }
        
        setupEntriesScene();
    }
    
    private void setupEntriesScene() {
        VBox mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(20));
        
        // 标题和返回按钮
        HBox topPane = new HBox(10);
        topPane.setAlignment(Pos.CENTER_LEFT);
        
        Button backButton = new Button("← 返回");
        backButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        backButton.setOnAction(e -> setupMainScene());
        
        Label titleLabel = new Label("世界书条目");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        topPane.getChildren().addAll(backButton, titleLabel);
        
        // 条目列表和详情区域
        HBox contentPane = new HBox(10);
        HBox.setHgrow(contentPane, Priority.ALWAYS);
        
        // 条目列表
        VBox listPane = new VBox(5);
        listPane.setPrefWidth(250);
        
        Label listLabel = new Label("条目列表:");
        listLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        entryListView = new ListView<>();
        entryListView.setPrefWidth(250);
        
        ObservableList<String> entries = FXCollections.observableArrayList();
        JsonNode entriesNode = rootNode.get("entries");
        Iterator<String> fieldNames = ((ObjectNode) entriesNode).fieldNames();
        while (fieldNames.hasNext()) {
            entries.add(fieldNames.next());
        }
        entryListView.setItems(entries);
        
        entryListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showEntryDetails(newVal);
            }
        });
        
        listPane.getChildren().addAll(listLabel, entryListView);
        VBox.setVgrow(entryListView, Priority.ALWAYS);
        
        // 条目详情
        VBox detailPane = new VBox(10);
        detailPane.setPadding(new Insets(10));
        detailPane.setStyle("-fx-background-color: white; -fx-background-radius: 5;");
        VBox.setVgrow(detailPane, Priority.ALWAYS);
        
        Label detailLabel = new Label("条目详情:");
        detailLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        // Comment
        Label commentLabel = new Label("Comment:");
        commentLabel.setStyle("-fx-font-weight: bold;");
        commentArea = new TextArea();
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(3);
        
        // Content
        Label contentLabel = new Label("Content:");
        contentLabel.setStyle("-fx-font-weight: bold;");
        contentArea = new TextArea();
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(10);
        
        // Key
        Label keyLabel = new Label("Key:");
        keyLabel.setStyle("-fx-font-weight: bold;");
        keyArea = new TextArea();
        keyArea.setWrapText(true);
        keyArea.setPrefRowCount(3);
        
        // 保存按钮
        HBox buttonPane = new HBox(10);
        buttonPane.setAlignment(Pos.CENTER);
        
        saveButton = new Button("保存修改");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        saveButton.setOnAction(e -> saveEntryChanges());
        saveButton.setDisable(true);
        
        buttonPane.getChildren().add(saveButton);
        
        detailPane.getChildren().addAll(detailLabel, commentLabel, commentArea, contentLabel, contentArea, keyLabel, keyArea, buttonPane);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        
        contentPane.getChildren().addAll(listPane, detailPane);
        
        // 添加到主容器
        mainContainer.getChildren().addAll(topPane, contentPane);
        
        rootPane.setCenter(mainContainer);
    }
    
    private void showEntryDetails(String entryId) {
        JsonNode entryNode = rootNode.get("entries").get(entryId);
        
        // Comment
        if (entryNode.has("comment") && !entryNode.get("comment").isNull()) {
            commentArea.setText(entryNode.get("comment").asText());
        } else {
            commentArea.setText("");
        }
        
        // Content
        if (entryNode.has("content") && !entryNode.get("content").isNull()) {
            contentArea.setText(entryNode.get("content").asText());
        } else {
            contentArea.setText("");
        }
        
        // Key
        if (entryNode.has("key") && !entryNode.get("key").isNull() && entryNode.get("key").isArray()) {
            ArrayNode keyArray = (ArrayNode) entryNode.get("key");
            StringBuilder keyText = new StringBuilder();
            for (int i = 0; i < keyArray.size(); i++) {
                if (i > 0) keyText.append(", ");
                keyText.append(keyArray.get(i).asText());
            }
            keyArea.setText(keyText.toString());
        } else {
            keyArea.setText("");
        }
        
        saveButton.setDisable(false);
    }
    
    private void saveEntryChanges() {
        String selectedEntry = entryListView.getSelectionModel().getSelectedItem();
        if (selectedEntry == null) return;
        
        try {
            JsonNode entryNode = rootNode.get("entries").get(selectedEntry);
            ObjectNode entryObj = (ObjectNode) entryNode;
            
            // 更新Comment
            entryObj.put("comment", commentArea.getText());
            
            // 更新Content
            entryObj.put("content", contentArea.getText());
            
            // 更新Key
            ObjectMapper mapper = new ObjectMapper();
            String[] keys = keyArea.getText().split(",\\s*");
            ArrayNode keyArray = mapper.createArrayNode();
            for (String key : keys) {
                if (!key.trim().isEmpty()) {
                    keyArray.add(key.trim());
                }
            }
            entryObj.set("key", keyArray);
            
            updateLog("条目 " + selectedEntry + " 已更新");
            showAlert("成功", "条目已更新");
        } catch (Exception e) {
            updateLog("更新条目失败: " + e.getMessage());
            showAlert("错误", "更新条目失败: " + e.getMessage());
        }
    }
    
    private void showSettings() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("设置");
        dialog.setHeaderText("自定义界面");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // 标题选择
        Label titleLabel = new Label("标题:");
        ComboBox<String> titleComboBox = new ComboBox<>();
        titleComboBox.getItems().addAll("随机", "世界书翻译器", "AI酒馆世界书翻译助手", "世界书翻译工具", "AI翻译助手", "自定义");
        titleComboBox.setValue(currentTitle);
        
        // 自定义标题输入框
        TextField customTitleField = new TextField();
        customTitleField.setPromptText("输入自定义标题");
        customTitleField.setDisable(!titleComboBox.getValue().equals("自定义"));
        
        // 监听标题选择变化
        titleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            customTitleField.setDisable(!newVal.equals("自定义"));
        });
        
        // 背景选择
        Label backgroundLabel = new Label("背景:");
        ComboBox<String> backgroundComboBox = new ComboBox<>();
        backgroundComboBox.getItems().add("随机图片");
        
        // 添加backgrounds目录中的图片
        if (!backgroundImages.isEmpty()) {
            for (String imagePath : backgroundImages) {
                File imageFile = new File(imagePath);
                backgroundComboBox.getItems().add(imageFile.getName());
            }
        }
        
        backgroundComboBox.getItems().addAll("默认颜色", "#f0f0f0", "#e0f2f1", "#f3e5f5");
        backgroundComboBox.setValue("默认颜色");
        
        grid.add(titleLabel, 0, 0);
        grid.add(titleComboBox, 1, 0);
        grid.add(new Label("自定义标题:"), 0, 1);
        grid.add(customTitleField, 1, 1);
        grid.add(backgroundLabel, 0, 2);
        grid.add(backgroundComboBox, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String selectedTitle = titleComboBox.getValue();
                String customTitle = customTitleField.getText().trim();
                String selectedBackground = backgroundComboBox.getValue();
                
                // 处理标题
                if (selectedTitle.equals("随机")) {
                    // 随机选择一个标题
                    titleIndex = (titleIndex + 1) % titles.length;
                    currentTitle = titles[titleIndex];
                } else if (selectedTitle.equals("自定义")) {
                    if (!customTitle.isEmpty()) {
                        currentTitle = customTitle;
                    }
                } else {
                    currentTitle = selectedTitle;
                }
                
                // 处理背景
                if (selectedBackground.equals("随机图片")) {
                    // 随机选择一个背景图片
                    if (!backgroundImages.isEmpty()) {
                        backgroundIndex = (backgroundIndex + 1) % backgroundImages.size();
                        String imagePath = backgroundImages.get(backgroundIndex);
                        // 尝试设置背景图片
                        try {
                            rootPane.setStyle("-fx-background-image: url('file:" + imagePath + "'); -fx-background-size: cover; -fx-background-position: center;");
                        } catch (Exception e) {
                            // 如果图片不存在，使用默认颜色
                            rootPane.setStyle("-fx-background-color: " + DEFAULT_BACKGROUND + ";");
                            updateLog("背景图片不存在，使用默认颜色");
                        }
                    } else {
                        // 如果没有背景图片，使用默认颜色
                        rootPane.setStyle("-fx-background-color: " + DEFAULT_BACKGROUND + ";");
                        updateLog("没有背景图片，使用默认颜色");
                    }
                } else if (selectedBackground.equals("默认颜色")) {
                    currentBackground = DEFAULT_BACKGROUND;
                    rootPane.setStyle("-fx-background-color: " + currentBackground + ";");
                } else if (selectedBackground.startsWith("#")) {
                    currentBackground = selectedBackground;
                    rootPane.setStyle("-fx-background-color: " + currentBackground + ";");
                } else {
                    // 选择了具体的背景图片
                    for (String imagePath : backgroundImages) {
                        File imageFile = new File(imagePath);
                        if (imageFile.getName().equals(selectedBackground)) {
                            try {
                                rootPane.setStyle("-fx-background-image: url('file:" + imagePath + "'); -fx-background-size: cover; -fx-background-position: center;");
                            } catch (Exception e) {
                                // 如果图片不存在，使用默认颜色
                                rootPane.setStyle("-fx-background-color: " + DEFAULT_BACKGROUND + ";");
                                updateLog("背景图片不存在，使用默认颜色");
                            }
                            break;
                        }
                    }
                }
                
                // 更新标题
                primaryStage.setTitle(currentTitle);
                if (this.titleLabel != null && rootPane.getCenter() instanceof VBox) {
                    VBox center = (VBox) rootPane.getCenter();
                    if (!center.getChildren().isEmpty() && center.getChildren().get(0) instanceof Label) {
                        Label label = (Label) center.getChildren().get(0);
                        if (label.getFont().getSize() == 24) {
                            this.titleLabel.setText(currentTitle);
                        }
                    }
                }
                
                updateLog("设置已更新");
            }
        });
    }
    
    private void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressLabel.setText(message);
        });
    }
    
    private void updateLog(String message) {
        Platform.runLater(() -> {
            String timestamp = new Date().toString();
            logArea.appendText("[" + timestamp + "] " + message + "\n");
        });
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}