package com.ukdw.rplbo.soal_ug_javafx;

import com.ukdw.rplbo.soal_ug_javafx.data.Mahasiswa_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Matakuliah_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Nilai_table;
import com.ukdw.rplbo.soal_ug_javafx.entity.Mahasiswa;
import com.ukdw.rplbo.soal_ug_javafx.entity.Matakuliah;
import com.ukdw.rplbo.soal_ug_javafx.entity.Nilai;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.*;

public class AppController {
    @FXML
    private ComboBox<String> option;
    @FXML
    private TableView<Object> table;
    @FXML
    private TableColumn<Object,String> column1;
    @FXML
    private TableColumn<Object,String> column2;
    @FXML
    private TableColumn<Object,String> column3;

    @FXML
    private BarChart<String, Number> barchart;
    @FXML
    private LineChart<String, Number> linechart;
    @FXML
    private PieChart piechart;


    Mahasiswa_table mhs_table = new Mahasiswa_table();
    Matakuliah_table mtkl_table = new Matakuliah_table();
    Nilai_table nilai_table = new Nilai_table();


    public AppController() throws SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Mahasiswa",
                "Matakuliah"
        );
        option.setItems(options);
        option.setValue("Mahasiswa");

        option.valueProperty().addListener((observable, oldValue, newValue) -> {
            table.getItems().clear();

            if ("Matakuliah".equals(newValue)) {
                linechart.setVisible(true);
                column1.setText("kode_mk");
                column1.setCellValueFactory(new PropertyValueFactory<>("kode_mk"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));


                column3.setText("sks");
                column3.setCellValueFactory(new PropertyValueFactory<>("sks"));

                table.setItems(FXCollections.observableArrayList(mtkl_table.fetch_all_matkul()));
            } else {
                linechart.setVisible(false);
                column1.setText("NIM");
                column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));

                column3.setText(" ");
                column3.setCellValueFactory(null);

                table.setItems(FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa()));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                if (newSelection instanceof Mahasiswa) {
                    Mahasiswa m = (Mahasiswa) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getNIM() + ")");

                    // -- chart --
                    update_barchart("nim",m.getNIM());
                    update_piechart("nim",m.getNIM());


                } else if (newSelection instanceof Matakuliah) {
                    Matakuliah m = (Matakuliah) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getKode_mk() + ")");

                    // -- chart --
                    update_barchart("kode_mk",m.getKode_mk());
                    update_piechart("kode_mk",m.getKode_mk());
                    update_linechart(m.getKode_mk());
                }
            }
        });

        linechart.setVisible(false);
        column1.setText("NIM");
        column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
        column2.setText("nama");
        column2.setCellValueFactory(new PropertyValueFactory<>("nama"));
        column3.setText(" ");

        ObservableList<Object> data = FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa());
        table.setItems(data);

    }

    public void update_barchart(String target_col,String val) {
        // TODO: buat barchart menampilkan seberapa banyak nilai A,A-,B+,...
        // method ini dapat di gunakan di 2 situasi yaitu nilai berdasarkan nim mahasiswa dan berdasarkan kode matakuliah
        // ambil data dari attribute nilai_table
        // tips: target_col merujuk pada nama kolom di datbase sedangkan val adalah value yang di cari dari kolom tersebut misal:
        // target_col -> nim, val -> 71200001, maka kita mencari 71200001 di kolom nim
        List<Nilai> listNilai = nilai_table.fetch_nilai_by(target_col, val);
        Map<String, Integer> mapNilai = new HashMap<>();
        for (String label: nilai_table.penilaian){
            mapNilai.put(label, 0);
        }
        for (Nilai n: listNilai){
            String grade = n.getNilai();
            mapNilai.put(grade, mapNilai.getOrDefault(grade, 0) + 1);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(target_col);

        for(String label: nilai_table.penilaian){
            series.getData().add(new XYChart.Data<>(label, mapNilai.get(label)));
        }
        barchart.getData().setAll(series);

    }

    public void update_linechart(String kode_mk) {
        // TODO: buatlah linechart yang menggambarkan nilai mean dari setiap angkatan
        // angkatan dapat di ambil dengan cara getAngkatan() pada entity Mahasiswa
        // tips: fetch dulu entity mahasiswa menggunakan fetch_mahasiswa_by_nim() di mhs_tabel menggunakan nim pada nilai_table
        List<Nilai> listNilai = nilai_table.fetch_nilai_by_kode_mk(kode_mk);
        Map<Integer, Double> totalNilai = new TreeMap<>();
        Map<Integer, Integer> jumlahMhs = new TreeMap<>();
        for (Nilai n: listNilai){
            Mahasiswa mhs = mhs_table.fetch_mahasiswa_by_nim(n.getNIM());
            if (mhs != null){
                int angkatan  = mhs.getAngkatan();
                double poin = n.get_converted_nilai();

                totalNilai.put(angkatan, totalNilai.getOrDefault(angkatan, 0.0) + poin);
                jumlahMhs.put(angkatan, jumlahMhs.getOrDefault(angkatan, 0) + 1);
            }
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rata-rata");

        for(Integer angkatan: totalNilai.keySet()){
            double mean =  totalNilai.get(angkatan) / jumlahMhs.get(angkatan);
            series.getData().add(new XYChart.Data<>(String.valueOf(angkatan), mean));
        }
        linechart.getData().setAll(series);

    }

    public void update_piechart(String target_col, String val) {
       // TODO: tampilkan banyaknya nilai A,A-,B+,... dalam bentuk piechart
        // method ini dapat di gunakan di 2 situasi yaitu nilai berdasarkan nim mahasiswa dan berdasarkan kode matakuliah
        // ambil data dari attribute nilai_table
        // tips: target_col merujuk pada nama kolom di datbase sedangkan val adalah value yang di cari dari kolom tersebut misal:
        // target_col -> nim, val -> 71200001, maka kita mencari 71200001 di kolom nim
        List<Nilai> listNilai = nilai_table.fetch_nilai_by(target_col, val);
        Map<String, Integer> mapNilai = new HashMap<>();
        for (Nilai n: listNilai){
            mapNilai.put(n.getNilai(), mapNilai.getOrDefault(n.getNilai(), 0) + 1);
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for(String label: nilai_table.penilaian){
            int count = mapNilai.getOrDefault(label, 0);
            if(count > 0){
                String labelFormat = label + " (" + count + ")";
                pieData.add(new PieChart.Data(labelFormat, count));
            }
        }
        piechart.setData(pieData);
    }
}
