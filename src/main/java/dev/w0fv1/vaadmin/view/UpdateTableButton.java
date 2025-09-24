package dev.w0fv1.vaadmin.view;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 上传表格并解析，全部在内存完成，不落磁盘
 *
 * onSuccess: 上传完成
 * onComplete: 解析完成
 * onError: 任何异常
 */
@Slf4j
public class UpdateTableButton extends Composite<Upload> {
    @FunctionalInterface
    public interface UploadSuccess {
        void handle(String fileName, byte[] rawBytes);
    }

    @FunctionalInterface
    public interface UploadComplete {
        void handle(Table table);
    }

    @FunctionalInterface
    public interface UploadError {
        void handle(Throwable throwable);
    }

    // =======================================================
    // Constructor
    // =======================================================

    public UpdateTableButton(UploadSuccess onSuccess,
                             UploadComplete onComplete,
                             UploadError onError) {

        // Memory‑only receiver: once uploaded we have the bytes
        InMemoryUploadHandler handler = UploadHandler.inMemory(
                (metadata, bytes) -> {
                    String fileName = metadata.fileName();
                    try {
                        // 1️⃣ notify upload finished
                        onSuccess.handle(fileName, bytes);

                        // 2️⃣ parse spreadsheet → Table
                        List<Map<String, String>> rows = parseSpreadsheet(fileName, bytes);
                        Table table = Table.from(fileName, rows);

                        // 3️⃣ notify parsing finished
                        onComplete.handle(table);
                    } catch (Exception ex) {
                        log.warn("Failed to parse spreadsheet: fileName={}, size={}", fileName, bytes.length, ex);
                        onError.handle(ex);
                    }
                });

        Upload upload = getContent();
        upload.setUploadHandler(handler);
        upload.setAcceptedFileTypes(".csv", ".xls", ".xlsx");
        upload.setDropAllowed(true);
        upload.setAutoUpload(true);
    }

    // =======================================================
    // Internal helpers
    // =======================================================

    /** Parse the uploaded bytes into a List<Map<column, value>> */
    private List<Map<String, String>> parseSpreadsheet(String name, byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            if (name.endsWith(".csv")) {
                CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                        .setHeader()               // first row as header
                        .setSkipHeaderRecord(true)
                        .get();
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    Iterable<CSVRecord> records = csvFormat.parse(reader);
                    return csv2Map(records);
                }
            } else { // .xls / .xlsx
                return excel2Map(in);
            }
        }
    }

    private List<Map<String, String>> csv2Map(Iterable<CSVRecord> records) {
        List<Map<String, String>> list = new ArrayList<>();
        for (CSVRecord r : records) {
            list.add(new LinkedHashMap<>(r.toMap()));
        }
        return list;
    }

    private List<Map<String, String>> excel2Map(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) { // supports xls / xlsx
            DataFormatter fmt = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            Sheet sheet = Optional.ofNullable(wb.getSheetAt(0))
                    .orElseThrow(() -> new IllegalStateException("Empty sheet"));

            // 1️⃣ header row
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) throw new IllegalStateException("Missing header row");

            List<String> columns = new ArrayList<>();
            headerRow.forEach(c -> columns.add(fmt.formatCellValue(c, evaluator)));

            // 2️⃣ data rows
            List<Map<String, String>> rows = new ArrayList<>();
            for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue; // skip blank rows

                Map<String, String> map = new LinkedHashMap<>();
                for (int j = 0; j < columns.size(); j++) {
                    String key = columns.get(j);
                    String val = fmt.formatCellValue(row.getCell(j), evaluator);
                    map.put(key, val);
                }
                rows.add(map);
            }
            return rows;
        }
    }


    @Getter
    public static class Table {

        /** Original file name or sheet name */
        @Getter
        private final String name;

        /** Ordered column headers */
        private final List<String> columns;

        /** Row data */
        private final List<Row> rows;


        public Table(String name, List<String> columns, List<Row> rows) {
            this.name = name;
            this.columns = new ArrayList<>(columns);
            this.rows = new ArrayList<>(rows);
        }

        // Convenience factory: convert from the List<Map<>> produced by UpdateTableButton
        public static Table from(String name, List<Map<String, String>> maps) {
            if (maps == null) throw new IllegalArgumentException("rows must not be null");
            List<String> columns = maps.isEmpty() ? List.of() : new ArrayList<>(maps.get(0).keySet());
            List<Row> rows = new ArrayList<>();
            for (Map<String, String> map : maps) {
                rows.add(new Row(map));
            }
            return new Table(name, columns, rows);
        }

        // ======================= helpers =======================

        /** Returns the first row that matches the given predicate or {@code null} */
        public Row findRow(java.util.function.Predicate<Row> predicate) {
            for (Row r : rows) if (predicate.test(r)) return r;
            return null;
        }

        @Override
        public String toString() {
            return "Table{" +
                    "name='" + name + '\'' +
                    ", columns=" + columns +
                    ", rows=" + rows.size() +
                    '}';
        }
        /** Total number of rows */
        public int getRowCount() {
            return rows.size();
        }

        /** Total number of columns */
        public int getColumnCount() {
            return columns.size();
        }
        // =======================================================
        // Inner Row class
        // =======================================================

        /**
         * Represents a single record in the table: column‑name → value.
         */
        @Getter
        public static class Row {
            private final Map<String, String> data;

            public Row() {
                this.data = new LinkedHashMap<>();
            }

            public Row(Map<String, String> data) {
                this.data = new LinkedHashMap<>(data);
            }

            public String get(String column) {
                return data.get(column);
            }

            public void put(String column, String value) {
                data.put(column, value);
            }

            public Map<String, String> asMap() {
                return Collections.unmodifiableMap(data);
            }

            @Override
            public String toString() {
                return data.toString();
            }
        }
    }

}
