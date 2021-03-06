package net.ahwater.tender.db;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Reeye on 2017/11/28.
 * Nothing is true but improving yourself.
 */
public class OracleEntityCreator {

    private DataSource dataSource;
    private String classPath;
    private String packageName;
    private List<String> intros;
    private List<String> classNames;
    private List<String> tables;
    private boolean getter;
    private boolean setter;
    private boolean noArgsConstructor;
    private boolean allArgsConstructor;
    private boolean toString;
    private String lineSeparator = System.getProperty("line.separator");

    private Connection conn;
    private PreparedStatement ps;

    private List<Column> getColumns(String tableName) {
        String sql = "select a.column_name, a.data_type, a.data_precision, a.data_scale, b.comments " +
                "from user_tab_cols a, all_col_comments b " +
                "where a.column_name=b.column_name " +
                "and a.table_name=b.table_name " +
                "and a.table_name=? " +
                "order by a.internal_column_id";
        List<Column> list = new ArrayList<>();
        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, tableName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                BigDecimal precision = (BigDecimal) rs.getObject(3);
                BigDecimal scale = (BigDecimal) rs.getObject(4);
                Integer p = null;
                Integer s = null;
                if (precision != null) {
                    p = new Integer(precision.intValue());
                }
                if (scale != null) {
                    s = new Integer(scale.intValue());
                }
                Column col = new Column(
                        rs.getString(1),
                        rs.getString(2),
                        p,
                        s,
                        rs.getString(5));
                list.add(col);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (conn !=null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    private void createEntityFile(int index, List<Column> entityColumns) {
        if (classNames.get(index) == null || entityColumns == null || entityColumns.size() < 1) {
            return;
        }
        File dir = new File(classPath/* + File.separator + (packageName.replaceAll("\\.", File.separator))*/);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try {
            FileWriter fw = new FileWriter(dir.getAbsolutePath() + File.separator + classNames.get(index) + ".java");
            if (packageName != null && !packageName.equals("")) {
                fw.write("package " + packageName + ";" + lineSeparator);
                fw.write(lineSeparator);
            }

            if (entityColumns.stream().map(Column::getType).anyMatch("DATE"::equals)) {
                fw.write("import java.util.Date;" + lineSeparator);
                fw.write(lineSeparator);
            }

            fw.write("/**" + lineSeparator);
            fw.write(" * Autogenerated by Reeye's OracleEntityCreator on " + LocalDate.now() + "." + lineSeparator);
            fw.write(" * Nothing is true but improving yourself." + lineSeparator);
            if (intros.get(index) != null && intros.get(index).length() > 0) {
                fw.write(" * " + intros.get(index) + lineSeparator);
            }
            fw.write(" */" + lineSeparator);

            fw.write("public class " + classNames.get(index) + " {" + lineSeparator);
            fw.write(lineSeparator);

            for (int i = 0; i < entityColumns.size(); i++) {
                if (entityColumns.get(i).getComments() != null) {
                    fw.write("    // " + entityColumns.get(i).getComments() + lineSeparator);
                }
                fw.write("    private " + typeMapper(entityColumns.get(i)) + " " + entityColumns.get(i).getName() + ";" + lineSeparator);
            }
            fw.write(lineSeparator);

            if (noArgsConstructor) {
                fw.write("    public " + classNames.get(index) + "() { }" + lineSeparator);
                fw.write(lineSeparator);
            }

            if (allArgsConstructor) {
                fw.write("    public " + classNames.get(index) + "(");
                for (int i = 0; i < entityColumns.size(); i++) {
                    fw.write(typeMapper(entityColumns.get(i)) + " " + entityColumns.get(i).getName()
                            + ((i < entityColumns.size() - 1) ? ", " : (") {" + lineSeparator)));
                }
                for (int i = 0; i < entityColumns.size(); i++) {
                    fw.write("        this." + entityColumns.get(i).getName() + " = " + entityColumns.get(i).getName() + ";" + lineSeparator);
                }
                fw.write("    }" + lineSeparator);
                fw.write(lineSeparator);
            }

            for (int i = 0; i < entityColumns.size(); i++) {
                if (getter) {
                    fw.write("    public " + typeMapper(entityColumns.get(i)) + " get" + firstLetter2UpperCase(entityColumns.get(i).getName()) + "() {" + lineSeparator);
                    fw.write("        return " + entityColumns.get(i).getName() + ";" + lineSeparator);
                    fw.write("    }" + lineSeparator);
                    fw.write(lineSeparator);
                }

                if (setter) {
                    fw.write("    public void set" + firstLetter2UpperCase(entityColumns.get(i).getName())
                            + "(" + typeMapper(entityColumns.get(i)) + " " + entityColumns.get(i).getName()
                            + ") {" + lineSeparator);
                    fw.write("        this." + entityColumns.get(i).getName() + " = " + entityColumns.get(i).getName() + ";" + lineSeparator);
                    fw.write("    }" + lineSeparator);
                    fw.write(lineSeparator);
                }

            }

            if (toString) {
                fw.write("    @Override" + lineSeparator);
                fw.write("    public String toString() {" + lineSeparator);
                fw.write("        return \"" + classNames.get(index) + "{\" +" + lineSeparator);
                for (int i = 0; i < entityColumns.size(); i++) {
                    if (i == 0) {
                        if (Arrays.asList("Integer", "Long", "Float", "Double").contains(typeMapper(entityColumns.get(i)))) {
                            fw.write("                \"" + entityColumns.get(i).getName() + "=\" + " + entityColumns.get(i).getName() + " +" + lineSeparator);
                        } else {
                            fw.write("                \"" + entityColumns.get(i).getName() + "='\" + " + entityColumns.get(i).getName() + " + '\\'\' +" + lineSeparator);
                        }
                    } else {
                        if (Arrays.asList("Integer", "Long", "Float", "Double").contains(typeMapper(entityColumns.get(i)))) {
                            fw.write("                \", " + entityColumns.get(i).getName() + "=\" + " + entityColumns.get(i).getName() + " +" + lineSeparator);
                        } else {
                            fw.write("                \", " + entityColumns.get(i).getName() + "='\" + " + entityColumns.get(i).getName() + " + '\\'\' +" + lineSeparator);
                        }
                    }
                }
                fw.write("                '}';" + lineSeparator);
                fw.write("    }" + lineSeparator);
                fw.write(lineSeparator);
            }
            fw.write("}");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void create() {
        for (int i = 0; i < tables.size(); i++) {
            createEntityFile(i, getColumns(tables.get(i)));
        }
        String os = System.getProperty("os.name");
        String cmd = "";
        if (os.contains("Mac")) {
            cmd = "open " + classPath;
        } else if (os.contains("Windows")) {
            cmd = "explorer " + classPath;
        }
        try {
            if (cmd != null && cmd.length() > 0) {
                Runtime.getRuntime().exec(cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String typeMapper(Column col) {
        switch (col.getType()) {
            case "NUMBER":
                if (col.getScale() != null) {
                    if (col.getScale() > 0) {
                        return "Double";
                    } else {
                        if (col.getPrecision() != null && col.getPrecision() <= 9) {
                            return "Integer";
                        } else {
                            return "Long";
                        }
                    }
                } else {
                    if (col.getPrecision() != null) {
                        if (col.getPrecision() <= 9) {
                            return "Integer";
                        } else {
                            return "Long";
                        }
                    } else {
                        return "Double";
                    }
                }
            case "FLOAT":
                return "Float";
            case "DATE":
                return "Date";
        }
        return "String";
    }

    private static String firstLetter2UpperCase(String name) {
        if (name == null || name.trim().equals("")) {
            return null;
        }
        if (name.length() == 1) {
            return name.toUpperCase();
        } else {
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
    }

    private OracleEntityCreator(Builder builder) {
        this.dataSource = builder.dataSource;
        this.classPath = builder.classPath;
        this.packageName = builder.packageName;
        this.intros = builder.intros;
        this.classNames = builder.classNames;
        this.tables = builder.tables;
        this.setter = builder.setter;
        this.getter = builder.getter;
        this.noArgsConstructor = builder.noArgsConstructor;
        this.allArgsConstructor = builder.allArgsConstructor;
        this.toString = builder.toString;
    }

    public static class Builder {

        private DataSource dataSource;
        private String classPath = System.getProperty("user.dir");
        private String packageName = "entity";
        private List<String> intros;
        private List<String> classNames = new ArrayList<>();
        private List<String> tables = new ArrayList<>();
        private boolean getter = true;
        private boolean setter = true;
        private boolean noArgsConstructor = true;
        private boolean allArgsConstructor = true;
        private boolean toString = true;

        public Builder(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        public Builder classPath(String classPath) {
            this.classPath = classPath;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder intros(List<String> intros) {
            this.intros = intros;
            return this;
        }

        public Builder classNames(List<String> classNames) {
            this.classNames = classNames;
            return this;
        }

        public Builder tables(List<String> tables) {
            this.tables = tables;
            return this;
        }

        public Builder getter(boolean getter) {
            this.getter = getter;
            return this;
        }

        public Builder setter(boolean setter) {
            this.setter = setter;
            return this;
        }

        public Builder noArgsConstructor(boolean noArgsConstructor) {
            this.noArgsConstructor = noArgsConstructor;
            return this;
        }

        public Builder allArgsConstructor(boolean allArgsConstructor) {
            this.allArgsConstructor = allArgsConstructor;
            return this;
        }

        public Builder toString(boolean toString) {
            this.toString = toString;
            return this;
        }

        public OracleEntityCreator build() {
            if (classNames.size() != tables.size()) {
                System.err.println("????????????????????????????????????!");
                return null;
            }
            return new OracleEntityCreator(this);
        }

    }

    private static class Column {

        private String name;
        private String type;
        private Integer precision;
        private Integer scale;
        private String comments;

        public Column() {
        }

        public Column(String name, String type, Integer precision, Integer scale, String comments) {
            this.name = name;
            this.type = type;
            this.precision = precision;
            this.scale = scale;
            this.comments = comments;
        }

        private String underline2Camel(String str) {
            return Arrays.stream(str.toLowerCase().split("_")).reduce("", (pre, curr) -> pre + (pre.equals("") ? curr : curr.substring(0 ,1).toUpperCase() + curr.substring(1, curr.length())));
        }

        @Override
        public String toString() {
            return "Column{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", precision=" + precision +
                    ", scale=" + scale +
                    ", comments='" + comments + '\'' +
                    '}';
        }

        public String getName() {
            return name != null ? underline2Camel(name) : null;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getPrecision() {
            return precision;
        }

        public void setPrecision(Integer precision) {
            this.precision = precision;
        }

        public Integer getScale() {
            return scale;
        }

        public void setScale(Integer scale) {
            this.scale = scale;
        }

        public String getComments() {
            return comments;
        }

        public void setComments(String comments) {
            this.comments = comments;
        }

    }

}
