package application;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gui.util.ControllerUtils;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import util.FindFile;
import util.IniFile;
import util.MyFile;
import util.MyString;
import util.Sounds;

public class Main extends Application {

	/*
	 * REGRAS:
	 * A raiz de tudo é o Stage.
	 * Todo Stage tem uma Scene.
	 * Toda Scene tem seus Nodes (Root e Leaf)
	 * Root Nodes são nodes que contém Branch que levam a outros Nodes ou Leaves.
	 * As Leaves são os elementos da tela (Botões, textos, etc)
	 *  
	 */
	
	static Group root;
	Scene scene;
	private FlowPane flowPane;
	private ScrollPane scrollPane;
	private VBox vBox;
	private HBox hBox;
	private Button btnAdd, btnDel, btnRefresh, bntStopAllSounds;
	private ComboBox<CBItens> comboBox;
	private IniFile iniFile;
	
	@Override
	public void start(Stage stage) {
		iniFile = IniFile.getNewIniFileInstance(".\\config.ini");
		root = new Group();
		scene = new Scene(root, 800, 600, Color.DARKSLATEGRAY);
		stage.setTitle("SoundBoard");
		stage.setScene(scene);
		flowPane = new FlowPane();
		flowPane.setHgap(5);
		flowPane.setVgap(5);
		flowPane.setAlignment(Pos.CENTER);
		scrollPane = new ScrollPane(flowPane);
		comboBox = new ComboBox<>();
		comboBox.setOnAction(e -> reloadButtons());
		ControllerUtils.changeHowComboBoxDisplayItens(comboBox, e -> e.getDisplayName());
		btnRefresh = new Button("Atualizar");
		btnRefresh.setOnAction(e -> refresh());
		bntStopAllSounds = new Button("Parar tudo");
		bntStopAllSounds.setOnAction(e -> {
			Sounds.stopAllMp3();
			Sounds.stopAllWaves();
		});
		btnAdd = new Button("Adicionar");
		btnAdd.setOnAction(e -> {
			File file = MyFile.selectDir("Selecione a pasta contendo arquivos de som para adicionar");
			if (file == null)
				return;
			int n = 1;
			for (; iniFile.isItem("SOUNDDIRS", "" + n); n++);
			iniFile.write("SOUNDDIRS", "" + n, file.getAbsolutePath());
			CBItens cbitem = new CBItens(file.getName(), file);
			comboBox.getItems().add(cbitem);
			comboBox.getSelectionModel().select(cbitem);
			reloadButtons();
    });
		btnDel = new Button("Remover");
		btnDel.setOnAction(e -> {
			Map<String, String> result = new HashMap<>();
			int n = 1;
			for (String item : iniFile.getItemList("SOUNDDIRS"))
				if (!iniFile.read("SOUNDDIRS", item).equals(comboBox.getSelectionModel().getSelectedItem().getFile().getAbsolutePath()))
					result.put("" + (n++), iniFile.read("SOUNDDIRS", item));
			iniFile.remove("SOUNDDIRS");
			for (String item : result.keySet())
				iniFile.write("SOUNDDIRS", item, result.get(item));
			refresh();
		});
		hBox = new HBox(comboBox);
		hBox.setSpacing(10);
		hBox.getChildren().addAll(btnAdd, btnDel, btnRefresh, bntStopAllSounds);
		vBox = new VBox(hBox);
		vBox.setSpacing(10);
		vBox.getChildren().add(scrollPane);
		vBox.prefWidthProperty().bind(stage.widthProperty().add(-16));
		vBox.prefHeightProperty().bind(stage.heightProperty().add(-38));
		flowPane.prefWidthProperty().bind(vBox.widthProperty());
		flowPane.prefHeightProperty().bind(vBox.heightProperty());
		root.getChildren().add(vBox);
		
		refresh();
		stage.show();
		
	}
	
	public static void main(String[] args) {
		launch(args);
		Sounds.stopAllMp3();
		Sounds.stopAllWaves();
	}
	
	private void refresh() {
		comboBox.setOnAction(null);
		comboBox.getItems().clear();
		for (String s : iniFile.getItemList("SOUNDDIRS")) {
			File file = new File(iniFile.read("SOUNDDIRS", s));
			comboBox.getItems().add(new CBItens(file.getName(), file));
		}
		if (!comboBox.getItems().isEmpty()) {
			comboBox.getSelectionModel().select(0);
			reloadButtons();
			comboBox.setOnAction(e -> reloadButtons());
		}
		comboBox.setDisable(comboBox.getItems().isEmpty());
		scrollPane.setDisable(comboBox.getItems().isEmpty());
	}

	private void reloadButtons() {
		flowPane.getChildren().clear();
		int p;
		double w = 0, h = 0;
		for (String fileType : Arrays.asList(".wav", ".mp3", "*.ogg")) {
			String path = comboBox.getSelectionModel().getSelectedItem().getFile().getAbsolutePath();
			List<File> files = FindFile.findFile(path, "*" + fileType);
			files.sort((f1, f2) -> 
				MyString.ignoraAcentos(f1.getAbsoluteFile().toString()).toLowerCase()
					.compareTo(MyString.ignoraAcentos(f2.getAbsoluteFile().toString()).toLowerCase()));
			
			for (File file : files) {
	      Button bt = new Button();
				Text btText = new Text();
				btText.setStyle("-fx-font-weight: bold; -fx-font-family: Lucida COnsole");
				btText.setTextAlignment(TextAlignment.CENTER);
				String txt = file.getName().replace(fileType, "");
				List<String> txts = new ArrayList<>();
				txts.add("");
				for (String s : txt.split(" ")) {
					p = txts.size() - 1;
					btText.setText(txts.get(p) + " " + s);
					if (btText.getLayoutBounds().getWidth() >= 100) {
						txts.add("");
						p++;
					}
					txts.set(p,(txts.get(p).isEmpty() ? s : txts.get(p) + " " + s));
				}
				txt = "";
				for (String s : txts)
					txt = txt.isEmpty() ? s : txt + "\n" + s; 
				btText.setText(txt);
	      bt.setGraphic(btText);
	      if (btText.getLayoutBounds().getWidth() > w)
	      	w = btText.getLayoutBounds().getWidth();
	      if (btText.getLayoutBounds().getHeight() > h)
	      	h = btText.getLayoutBounds().getHeight();
				flowPane.getChildren().add(bt);
				if (fileType.equals(".wav"))
					bt.setOnAction(e -> Sounds.playWav(file.getAbsolutePath().toString(), true));
				else
					bt.setOnAction(e -> Sounds.playMp3(file.getAbsolutePath().toString(), true));
			}
		}
		w += 20;
		h += 10;
		for (int n = 0; n < flowPane.getChildren().size(); n++) {
			Button bt = (Button) flowPane.getChildren().get(n);
			bt.setPrefSize(w, h);
			bt.setMinSize(w, h);
			bt.setMaxSize(w, h);
			flowPane.getChildren().set(n, bt);
		}
	}
	
}

class CBItens {
	
	private String displayName;
	private File file;

	public CBItens(String displayName, File file) {
		this.displayName = displayName;
		this.file = file;
	}

	public String getDisplayName()
		{ return displayName; }

	public File getFile()
		{ return file; }
	
}