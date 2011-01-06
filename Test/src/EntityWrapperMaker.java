/**
 * @(#) EntityWrapperMaker.java 2008/04/10
 * Copyright 1999-2000 by  LG-EDS Systems, Inc.,
 * Information Technology Group, Application Architecture Team, 
 * Application Intrastructure Part.
 * 236-1, Hyosung-2dong, Kyeyang-gu, Inchun, 407-042, KOREA.
 * All rights reserved.
 * 
 * NOTICE !      You can copy or redistribute this code freely, 
 * but you should not remove the information about the copyright notice 
 * and the author.
 * 
 * @author  WonYoung Lee, wyounglee@lgeds.lg.co.kr.
 * @modifier SeokYoung Shin,dogfly@hanmail.com 2001-01-16
 */

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * Spring Application Generator
 * 
 * @version v1.00 2008/04/07
 * @modifier YoSeph Baek
 */
public class EntityWrapperMaker {

    /** db driver */
    public static final String driver = "oracle.jdbc.driver.OracleDriver";

    /** db url */
    public static final String url = "jdbc:oracle:thin:@203.242.137.163:1521:VEGA";

    /** db user */
    public static final String user = "dts";

    /** db password */
    public static final String password = "dts000";

    /** packagename */
    public static final String packagename = "";

    /** savedir */
    public static final String savedir = "C:/test";

    /** tablenames */
    public static String [] tablenames = new String[]{
    	"AS2_MESSAGE"};

    /** entityNames  */
    public static String [] entityNames = new String[]{
    	"AS2_MESSAGE"};
    
    /** entityNamesLower  */
    public static String [] entityNamesLower = new String[]{
    	"as2_message"};


    /**
     * main method
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            java.util.Enumeration e = System.getProperties().propertyNames();
            String obj = null;
            while (e.hasMoreElements()) {
                obj = (String) e.nextElement();
                System.out.print(obj + " ===> ");
                System.out.println(System.getProperty(obj));
            }
            System.out.println("===============================================");

            for(int i = 0 ; i < tablenames.length; i++){
            	performTask(tablenames[i], entityNames[i], entityNamesLower[i]);
            }
            System.out.println("See the " + savedir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * do gen
     * 
     * @throws IOException
     */
    public static void performTask(String tablename, String entityName, String entityNameLower) throws IOException {
        StringBuffer modelStr = new StringBuffer();
        StringBuffer sqlStr = new StringBuffer();
        StringBuffer daoStr = new StringBuffer();
        StringBuffer daoImplStr = new StringBuffer();
        StringBuffer svrStr = new StringBuffer();
        StringBuffer svrImplStr = new StringBuffer();
        StringBuffer controllerStr = new StringBuffer();
        StringBuffer formControllerStr = new StringBuffer();
        StringBuffer beanStr = new StringBuffer();
        
        String strFirstColumn = "";
        String strFirstAttr = "";

        Connection conn = null;

        boolean isDb2 = true; // db2?
        try {

            Class.forName(driver);

            if (driver.indexOf("db2", 0) == -1)
                isDb2 = false;

            conn = java.sql.DriverManager.getConnection(url, user, password);

            tablename = tablename.toUpperCase();

            if (entityName == null) {
                entityName = tablename + "DbEntity";
            }

            Vector columns = new Vector();
            if (!packagename.equals(""))
                modelStr.append("\npackage " + packagename + ".model;\n");

            String jdkver = ((System.getProperty("java.version")).replaceAll("[.]", "")).replaceAll("[_]", "");

            modelStr.append("\nimport org.apache.commons.lang.builder.ToStringBuilder;\n");
            modelStr.append("import org.apache.commons.lang.builder.ToStringStyle;\n\n");
            modelStr.append("import "+packagename+".common.model.SearchModel;\n\n");
            modelStr.append("/*\n");
            modelStr.append(" * "+tablename+"'s Value-Object\n");
            modelStr.append(" */\n");
            modelStr.append("public class " + entityName + "Model extends SearchModel implements java.io.Serializable, Cloneable{\n");
            modelStr.append("\n");
            modelStr.append("    private static final long serialVersionUID = " + jdkver + "L;");
            modelStr.append("\n\n");
            DatabaseMetaData meta = null;
            ResultSet rs = null;

            try {
                meta = conn.getMetaData();
                rs = meta.getColumns(null, null, tablename, "%");
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME").toLowerCase();

                    System.out.println("COLUMN_NAME = " + name);
                    String model_name = null;
                    int _position = name.indexOf("_");
                    if (_position > 0) {
                        StringBuffer temp = new StringBuffer();
                        String str = null;
                        String str_one = null;
                        String str_etc = "";
                        StringTokenizer st = new StringTokenizer(name, "_");
                        temp.append(st.nextToken());
                        while (st.hasMoreTokens()) {
                            str = st.nextToken();
                            if (str.length() == 1) {
                                str_one = str.toUpperCase();
                            } else if (str.length() > 1) {
                                str_one = str.substring(0, 1).toUpperCase();
                                str_etc = str.substring(1, str.length());
                            } else {
                                str_one = "";
                            }
                            temp.append(str_one).append(str_etc);
                        }
                        System.out.println("Model_NAME = " + temp.toString());
                        model_name = temp.toString();
                    } else {
                        model_name = name;
                    }

                    String dbtype = rs.getString("TYPE_NAME");
                    int length = rs.getInt("COLUMN_SIZE");
                    int digits = rs.getInt("DECIMAL_DIGITS");
                    String nullable = rs.getString("IS_NULLABLE");
                    String type = null; 
                    String gettype = null; 
                    if (dbtype.indexOf("CHAR") == 0 || dbtype.indexOf("DATE") == 0 || dbtype.indexOf("VARCHAR") == 0
                        || dbtype.indexOf("LONG VARCHAR") == 0 || dbtype.indexOf("CLOB") == 0) {
                        type = "String";
                        gettype = type;
                    } else if (dbtype.indexOf("NUMBER") == 0 || dbtype.indexOf("DEC") == 0
                        || dbtype.indexOf("INTeger") == 0) {
                        if (digits > 0) {
                            type = "double";
                            gettype = "Double";
                        } else {
                            if (length > 18) {// NOTE : 9223372036854775807 + 1 = -9223372036854775808
                                type = "java.math.BigDecimal";
                                gettype = "String";
                            } else if (length > 9 && length <= 18) {// NOTE : 2,147,483,647 + 1 = -2147483648
                                type = "long";
                                gettype = "Long";
                            } else {
                                type = "int";
                                gettype = "Int";
                            }
                        }
                    } else if (dbtype.indexOf("BLOB") == 0) {
                        type = "byte[]";
                        gettype = type;                       
                    } else {
                        type = dbtype;
                        gettype = dbtype;
                    }

                    Hashtable column = new Hashtable();
                    column.put("name", name);
                    column.put("model_name", model_name);
                    column.put("dbtype", dbtype);
                    column.put("type", type);
                    column.put("gettype", gettype);
                    column.put("digits", new Integer(digits));
                    columns.addElement(column);

                    String entityDef = "    private " + type + " " + model_name + "; // " + dbtype + "(" + length;
                    if (digits > 0) {
                        entityDef += "," + digits;
                    }
                    entityDef += ")";
                    if (nullable.equals("NO")) {
                        entityDef += ": NOT NULL";
                    }

                    modelStr.append(entityDef + "\n\n");
                }

                if (columns.size() == 0){
                    throw new Exception("TABLE " + tablename + " is invalid");
                } else {
                    Enumeration cols = columns.elements();

                    while (cols.hasMoreElements()) {
                        Hashtable column = (Hashtable) cols.nextElement();
                        String model_name = (String) column.get("model_name");
                        String model_name_firtstUpper = model_name.toUpperCase().substring(0,1) + model_name.substring(1, model_name.length());
                        String type = (String) column.get("type");

						modelStr.append("    public "+type+" get"+model_name_firtstUpper+"() {\n");
						modelStr.append("        return "+model_name+";\n");
						modelStr.append("    }\n");
						modelStr.append("\n");
						modelStr.append("    public void set"+model_name_firtstUpper+"("+type+" "+model_name+") {\n");
						modelStr.append("        this."+model_name+" = "+model_name+";\n");
						modelStr.append("    }\n\n");          
                    }
                }
            } finally {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            modelStr.append("    public String toString() {\n");
            modelStr.append("        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);\n");
            modelStr.append("    } \n");
            modelStr.append("}");

            // ---------- private key -------------------------------------
            Vector pk = new Vector();
            try {
                meta = conn.getMetaData();
                if (isDb2) {
                    rs = meta.getBestRowIdentifier("", "", tablename, DatabaseMetaData.bestRowTemporary, true);
                } else {
                    rs = meta.getPrimaryKeys(null, null, tablename);
                }

                while (rs.next()) {
                    String column_name = rs.getString("COLUMN_NAME").toLowerCase();

                    //String type = null;
                    Enumeration cols = columns.elements();

                    while (cols.hasMoreElements()) {
                        Hashtable column = (Hashtable) cols.nextElement();
                        String name = (String) column.get("name");

                        if (name.equals(column_name)) {
                            pk.addElement(column);
                            break;
                        }
                    }
                }
            } finally {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }

            // ---------- result object ------------------------------
			sqlStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

			sqlStr.append("<!DOCTYPE sqlMap\n");
			sqlStr.append("    PUBLIC \"-//ibatis.apache.org//DTD SQL Map 2.0//EN\"\n");
			sqlStr.append("    \"http://ibatis.apache.org/dtd/sql-map-2.dtd\">\n");

			sqlStr.append("<sqlMap namespace=\""+entityNameLower+"\">\n");
			sqlStr.append("\n  <!-- resultMap //-->");
            Enumeration e = pk.elements();
            boolean first = true;

            sqlStr.append("\n  <resultMap id=\"" + entityNameLower + "Model\" class=\"" + packagename + ".model." + entityName
                + "Model\">");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                String dbtype = (String) col.get("dbtype");

                System.out.println("COLUMN_NAME = " + name);
                System.out.println("COLUMN_TYPE = " + dbtype);
                System.out.println("===========================================");

                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }
                
                String dbtype2 = dbtype;
                if ("VARCHAR2".equalsIgnoreCase(dbtype2)){
                	dbtype2 = "VARCHAR";
                }

                sqlStr.append("\n    <result property=\"" + model_name + "\" column=\"" + name + "\" jdbcType=\""+dbtype2.toUpperCase()+"\"/>");
            }
            sqlStr.append("\n  </resultMap>");
            sqlStr.append("\n  <!-- resultMap //-->");
            sqlStr.append("\n\n");
            
            // ---------- select key-------------------------------------
            String alias = entityName.substring(0,2).toUpperCase();
            sqlStr.append("\n  <!-- SELECT KEY //-->");
            sqlStr.append("\n  <select id=\"get" + entityName
                + "Serial\" resultClass=\"java.lang.String\">");
            sqlStr.append("\n    SELECT\n");            
            sqlStr.append("       TO_CHAR(SYSDATE,'YYYYMMDD')||'"+alias+"'||LPAD("+entityNameLower+"_seq.NEXTVAL, 10, '0') as SEQ FROM DUAL");
            sqlStr.append("\n  </select>");
            sqlStr.append("\n  <!-- SELECT KEY //-->");
            sqlStr.append("\n\n");            

            // ---------- select one-------------------------------------
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n  <select id=\"get" + entityName
                + "\" resultMap=\"" + entityNameLower + "Model\" parameterClass=\""+packagename + ".model." + entityName+"Model\">");
            sqlStr.append("\n    SELECT\n");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                String dbtype = (String) col.get("dbtype");
                
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }                

                if (first) {
                	strFirstColumn = name;
                	strFirstAttr = model_name;
                    if (dbtype.equals("TIMESTAMP(6)")) {
                        sqlStr.append("        to_char(" + name + ", 'YYYYMMDDHH24MISS') " + name);
                        sqlStr.append("\n");
                    } else {
                        sqlStr.append("        " + name);
                        sqlStr.append("\n");
                    }
                } else {
                    if (dbtype.equals("TIMESTAMP(6)")) {
                        sqlStr.append("        , to_char(" + name + ", 'YYYYMMDDHH24MISS') " + name);
                        sqlStr.append("\n");
                    } else {
                        sqlStr.append("        , " + name);
                        sqlStr.append("\n");
                    }
                }

                if (first) {
                    first = false;
                }
            }
            sqlStr.append("    FROM  " + tablename.toLowerCase());
            sqlStr.append("\n    WHERE " + strFirstColumn + " = #"+strFirstAttr+"#");
            sqlStr.append("\n  </select>");
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n\n");
            
            // ---------- select list -------------------------------------
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n  <select id=\"get" + entityName
                + "List\" resultMap=\"" + entityNameLower + "Model\" parameterClass=\""+packagename + ".model." + entityName+"Model\">\n");
			sqlStr.append("    SELECT\n");
			sqlStr.append("           B.*\n");
			sqlStr.append("    FROM (\n");
			sqlStr.append("          SELECT\n");
			sqlStr.append("                 ROWNUM AS RNUM,\n");
			sqlStr.append("                 A.*\n");
			sqlStr.append("          FROM (\n");            
            sqlStr.append("                SELECT\n");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                String dbtype = (String) col.get("dbtype");
                
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }                

                if (first) {
                	strFirstColumn = name;
                	strFirstAttr = model_name;                	
                    if (dbtype.equals("TIMESTAMP(6)")) {
                        sqlStr.append("                       to_char(" + name + ", 'YYYYMMDDHH24MISS') " + name);
                        sqlStr.append("\n");
                    } else {
                        sqlStr.append("                       " + name);
                        sqlStr.append("\n");
                    }
                } else {
                    if (dbtype.equals("TIMESTAMP(6)")) {
                        sqlStr.append("                       , to_char(" + name + ", 'YYYYMMDDHH24MISS') " + name);
                        sqlStr.append("\n");
                    } else {
                        sqlStr.append("                       , " + name);
                        sqlStr.append("\n");
                    }
                }

                if (first) {
                    first = false;
                }
            }
            sqlStr.append("                FROM  " + tablename.toLowerCase());
            sqlStr.append("\n                WHERE 1 = 1");
            sqlStr.append("\n                <dynamic prepend=\"AND\">");
			sqlStr.append("\n                    <isNotNull property=\"searchField\">");
			sqlStr.append("\n                        <isEqual property=\"searchField\" compareValue=\"subject\">");
			sqlStr.append("\n                            subject like '%'||#searchText#||'%'");
			sqlStr.append("\n                        </isEqual>");
			sqlStr.append("\n                        <isEqual property=\"searchField\" compareValue=\"name\">");
			sqlStr.append("\n                            reg_user like '%'||#searchText#||'%'");
			sqlStr.append("\n                        </isEqual>");
			sqlStr.append("\n                    </isNotNull>");                                                                                    
			sqlStr.append("\n                </dynamic>");            
            sqlStr.append("\n                ORDER BY " + strFirstColumn + " DESC ) A");
			sqlStr.append("\n          WHERE ROWNUM &lt;= #endIndex# ) B");
			sqlStr.append("\n    WHERE RNUM &gt;= #startIndex# ");           
            sqlStr.append("\n  </select>");
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n\n");  
            
            // ---------- select list count -------------------------------------
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n  <select id=\"get" + entityName
                + "ListCount\" resultClass=\"int\" parameterClass=\""+packagename + ".model." + entityName+"Model\">");
            sqlStr.append("\n    SELECT\n");
            sqlStr.append("           COUNT(*) AS CNT\n");
            
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                String dbtype = (String) col.get("dbtype");
                
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }                

                if (first) {
                	strFirstColumn = name;
                	strFirstAttr = model_name;                    	
                }

                if (first) {
                    first = false;
                }
            }
            sqlStr.append("    FROM  " + tablename.toLowerCase());
            sqlStr.append("\n    WHERE 1 = 1");
            sqlStr.append("\n    <dynamic prepend=\"AND\">");
			sqlStr.append("\n        <isNotNull property=\"searchField\">");
			sqlStr.append("\n            <isEqual property=\"searchField\" compareValue=\"subject\">");
			sqlStr.append("\n                subject like '%'||#searchText#||'%'");
			sqlStr.append("\n            </isEqual>");
			sqlStr.append("\n            <isEqual property=\"searchField\" compareValue=\"name\">");
			sqlStr.append("\n                reg_user like '%'||#searchText#||'%'");
			sqlStr.append("\n            </isEqual>");
			sqlStr.append("\n        </isNotNull>");                                                                                    
			sqlStr.append("\n    </dynamic>"); 
            sqlStr.append("\n  </select>");
            sqlStr.append("\n  <!-- SELECT SQL //-->");
            sqlStr.append("\n\n");              

            // ---------- insert -------------------------------------
            sqlStr.append("\n  <!-- INSERT SQL //-->");
            sqlStr.append("\n  <update id=\"insert" + entityName
                + "\" parameterClass=\""+packagename + ".model." + entityName+"Model\">");
            
			//sqlStr.append("\n    <selectKey resultClass=\"int\" keyProperty=\"new_id\" >");
			//sqlStr.append("\n        SELECT "+entityNameLower+"_seq.NEXTVAL AS NEW_ID FROM DUAL");
			//sqlStr.append("\n    </selectKey>\n");
            
            sqlStr.append("\n    INSERT INTO " + tablename.toLowerCase() + "( \n");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }                

                if (first) {
                    sqlStr.append("       " + name);
                    sqlStr.append("\n");
                } else {
                	sqlStr.append("    <isNotNull property=\""+model_name+"\">\n"); 
                    sqlStr.append("       , " + name);
                    sqlStr.append("\n");
                    sqlStr.append("    </isNotNull> \n");                    
                }

                if (first) {
                    first = false;
                }
            }
            sqlStr.append("      ) VALUES ( \n");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");
                //String dbtype = (String) col.get("dbtype");
                
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }                

                if (first) {
                    sqlStr.append("       #"+model_name+"#");
                    sqlStr.append("\n");
                } else {
                    sqlStr.append("    <isNotNull property=\"" + model_name + "\">\n");                    
                    sqlStr.append("       , #" + model_name + "#");
                    sqlStr.append("\n");
                    sqlStr.append("    </isNotNull> \n");                      
                }

                if (first) {
                    first = false;
                }
            }
            sqlStr.append("      )");
            sqlStr.append("\n  </update>");
            sqlStr.append("\n  <!-- INSERT SQL //-->");
            sqlStr.append("\n\n");

            // ---------- update -------------------------------------
            sqlStr.append("\n  <!-- UPDATE SQL //-->");
            sqlStr.append("\n  <update id=\"update" + entityName
                + "\" parameterClass=\""+packagename + ".model." + entityName+"Model\">");
            sqlStr.append("\n    UPDATE " + tablename.toLowerCase() + " SET \n");
            e = columns.elements();
            first = true;
            while (e.hasMoreElements()) {
                Hashtable col = (Hashtable) e.nextElement();
                String name = (String) col.get("name");

                System.out.println("COLUMN_NAME = " + name);
                String model_name = null;
                int _position = name.indexOf("_");
                if (_position > 0) {
                    StringBuffer temp = new StringBuffer();
                    String str = null;
                    String str_one = null;
                    String str_etc = "";
                    StringTokenizer st = new StringTokenizer(name, "_");
                    temp.append(st.nextToken());
                    while (st.hasMoreTokens()) {
                        str = st.nextToken();
                        if (str.length() == 1) {
                            str_one = str.toUpperCase();
                        } else if (str.length() > 1) {
                            str_one = str.substring(0, 1).toUpperCase();
                            str_etc = str.substring(1, str.length());
                        } else {
                            str_one = "";
                        }
                        temp.append(str_one).append(str_etc);
                    }
                    System.out.println("Model_NAME = " + temp.toString());
                    model_name = temp.toString();
                } else {
                    model_name = name;
                }

                //String dbtype = (String) col.get("dbtype");

                if (first) {
                	strFirstColumn = name;
                	strFirstAttr = model_name;                    	
                    sqlStr.append("      " + name + " = #" + model_name + "#");
                    sqlStr.append("\n");                    
                } else {
                    sqlStr.append("    <isNotNull property=\"" + model_name + "\">\n");                       
                    sqlStr.append("      , " + name + " = #" + model_name + "#");
                    sqlStr.append("\n");  
                    sqlStr.append("    </isNotNull> \n");                     
                    
                }

                if (first) {
                    first = false;
                }


            }
            sqlStr.append("    WHERE  " + strFirstColumn + " = #"+strFirstAttr+"#");
            sqlStr.append("\n  </update>");
            sqlStr.append("\n  <!-- UPDATE SQL //-->");
            sqlStr.append("\n\n");

            // ---------- delete -------------------------------------
            sqlStr.append("\n  <!-- DELETE SQL //-->");
            sqlStr.append("\n  <update id=\"delete" + entityName
                + "\" parameterClass=\""+packagename + ".model." + entityName+"Model\">");
            sqlStr.append("\n    DELETE FROM " + tablename.toLowerCase());
            sqlStr.append("\n    WHERE  " + strFirstColumn + " = #"+strFirstAttr+"#");
            sqlStr.append("\n  </update>");
            sqlStr.append("\n  <!-- nDELETE SQL //-->");
            sqlStr.append("\n\n");
            sqlStr.append("</sqlMap>");
            
//          StringBuffer daoStr = new StringBuffer();
//          StringBuffer daoImplStr = new StringBuffer();
//          StringBuffer svrStr = new StringBuffer();
//          StringBuffer svrImplStr = new StringBuffer();    
            
            // ----------- dao ------------------------
            if (!packagename.equals(""))
            	daoStr.append("package " + packagename + ".dao;\n");            
            daoStr.append("\n");
            daoStr.append("import java.util.List;\n");
            daoStr.append("\n");
            daoStr.append("import org.springframework.dao.DataAccessException;\n");
            daoStr.append("\n");            
            daoStr.append("import "+packagename+".model."+entityName+"Model;\n");  
            daoStr.append("\n");  
            daoStr.append("/*\n");
            daoStr.append(" * DAO \n");
            daoStr.append(" */\n");
            daoStr.append("public interface " + entityName + "DAO {\n");
            daoStr.append("    public String get" + entityName + "Serial() throws DataAccessException;\n");
            daoStr.append("    public "+entityName+"Model get" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("    public List<"+entityName+"Model> get" + entityName + "List("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("    public int get" + entityName + "ListCount("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("    public int insert" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("    public int update" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("    public int delete" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException;\n");
            daoStr.append("}\n");
            daoStr.append("\n");       
            
            
            // ----------- daoImpl ------------------------
            if (!packagename.equals(""))
            	daoImplStr.append("package " + packagename + ".dao;\n");            
            daoImplStr.append("\n");
            daoImplStr.append("import java.util.List;\n");
            daoImplStr.append("\n");
            daoImplStr.append("import org.apache.log4j.Logger;\n");
            daoImplStr.append("import org.springframework.dao.DataAccessException;\n");
            daoImplStr.append("import org.springframework.orm.ibatis.support.SqlMapClientDaoSupport;\n");
            daoImplStr.append("\n");            
            daoImplStr.append("import "+packagename+".model."+entityName+"Model;\n");  
            daoImplStr.append("\n");  
            daoImplStr.append("/*\n");
            daoImplStr.append(" * DAOImpl \n");
            daoImplStr.append(" */\n");
            daoImplStr.append("public class " + entityName + "DAOImpl extends SqlMapClientDaoSupport implements " + entityName + "DAO {\n");
            daoImplStr.append("    Logger logger = Logger.getLogger(getClass());\n\n");
            daoImplStr.append("    public String get" + entityName + "Serial() throws DataAccessException{\n");
            daoImplStr.append("        return (String)getSqlMapClientTemplate()\n");
            daoImplStr.append("            .queryForObject(\""+entityNameLower+".get"+entityName+"Serial\");\n");    
            daoImplStr.append("    }\n\n");            
            daoImplStr.append("    public "+entityName+"Model get" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");
            daoImplStr.append("        return ("+entityName+"Model)getSqlMapClientTemplate()\n");
            daoImplStr.append("            .queryForObject(\""+entityNameLower+".get"+entityName+"\", "+entityNameLower+"Model);\n");    
            daoImplStr.append("    }\n\n");
            daoImplStr.append("    public List<"+entityName+"Model> get" + entityName + "List("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            daoImplStr.append("        return (List)getSqlMapClientTemplate()\n");
            daoImplStr.append("            .queryForList(\""+entityNameLower+".get"+entityName+"List\", "+entityNameLower+"Model);\n");              
            daoImplStr.append("    }\n\n");
            daoImplStr.append("    public int get" + entityName + "ListCount("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            daoImplStr.append("        return (Integer)getSqlMapClientTemplate()\n");
            daoImplStr.append("            .queryForObject(\""+entityNameLower+".get"+entityName+"ListCount\", "+entityNameLower+"Model);\n");              
            daoImplStr.append("    }\n\n");            
            daoImplStr.append("    public int insert" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            daoImplStr.append("        return getSqlMapClientTemplate()\n");
            daoImplStr.append("            .update(\""+entityNameLower+".insert"+entityName+"\", "+entityNameLower+"Model);\n");              
            daoImplStr.append("    }\n\n");
            daoImplStr.append("    public int update" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            daoImplStr.append("        return getSqlMapClientTemplate()\n");
            daoImplStr.append("            .update(\""+entityNameLower+".update"+entityName+"\", "+entityNameLower+"Model);\n");                
            daoImplStr.append("    }\n\n");
            daoImplStr.append("    public int delete" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws DataAccessException{\n");
            daoImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            daoImplStr.append("        return getSqlMapClientTemplate()\n");
            daoImplStr.append("            .update(\""+entityNameLower+".delete"+entityName+"\", "+entityNameLower+"Model);\n");                
            daoImplStr.append("    }\n\n");
            daoImplStr.append("}\n");
            daoImplStr.append("\n");                
            
            
            // ----------- service ------------------------
            if (!packagename.equals(""))
            	svrStr.append("package " + packagename + ".service;\n");            
            svrStr.append("\n");
            svrStr.append("import java.util.List;\n");
            svrStr.append("\n");            
            svrStr.append("import "+packagename+".model."+entityName+"Model;\n");  
            svrStr.append("\n");  
            svrStr.append("/*\n");
            svrStr.append(" * Service \n");
            svrStr.append(" */\n");
            svrStr.append("public interface " + entityName + "Service {\n");
            svrStr.append("    public String get" + entityName + "Serial() throws Exception;\n");
            svrStr.append("    public "+entityName+"Model get" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("    public List<"+entityName+"Model> get" + entityName + "List("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("    public int get" + entityName + "ListCount("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("    public int insert" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("    public int update" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("    public int delete" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception;\n");
            svrStr.append("}\n");
            svrStr.append("\n");              
            
            // ----------- serviceImpl ------------------------
            if (!packagename.equals(""))
            	svrImplStr.append("package " + packagename + ".service;\n");            
            svrImplStr.append("\n");
            svrImplStr.append("import java.util.List;\n");
            svrImplStr.append("\n");
            svrImplStr.append("import org.apache.log4j.Logger;\n");
            svrImplStr.append("\n");
            svrImplStr.append("import "+packagename+".dao."+entityName+"DAO;\n");              
            svrImplStr.append("import "+packagename+".model."+entityName+"Model;\n");  
            svrImplStr.append("\n");  
            svrImplStr.append("/*\n");
            svrImplStr.append(" * ServiceImpl\n");
            svrImplStr.append(" */\n");
            svrImplStr.append("public class " + entityName + "ServiceImpl implements " + entityName + "Service {\n");
            svrImplStr.append("    Logger logger = Logger.getLogger(getClass());\n\n");            
            svrImplStr.append("    private "+entityName+"DAO "+entityNameLower+"DAO;\n");
			svrImplStr.append("\n");
			svrImplStr.append("    public void set"+entityName+"DAO("+entityName+"DAO "+entityNameLower+"DAO) {\n");
			svrImplStr.append("        this."+entityNameLower+"DAO = "+entityNameLower+"DAO;\n");
			svrImplStr.append("    }\n\n"); 
            svrImplStr.append("    public String get" + entityName + "Serial() throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");
            svrImplStr.append("        return "+entityNameLower+"DAO.get"+entityName+"Serial();\n");    
            svrImplStr.append("    }\n\n");			
            svrImplStr.append("    public "+entityName+"Model get" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");
            svrImplStr.append("        return "+entityNameLower+"DAO.get"+entityName+"("+entityNameLower+"Model);\n");    
            svrImplStr.append("    }\n\n");
            svrImplStr.append("    public List<"+entityName+"Model> get" + entityName + "List("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            svrImplStr.append("        return "+entityNameLower+"DAO.get"+entityName+"List("+entityNameLower+"Model);\n");              
            svrImplStr.append("    }\n\n");
            svrImplStr.append("    public int get" + entityName + "ListCount("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            svrImplStr.append("        return "+entityNameLower+"DAO.get"+entityName+"ListCount("+entityNameLower+"Model);\n");              
            svrImplStr.append("    }\n\n");            
            svrImplStr.append("    public int insert" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            svrImplStr.append("        return "+entityNameLower+"DAO.insert"+entityName+"("+entityNameLower+"Model);\n");              
            svrImplStr.append("    }\n\n");
            svrImplStr.append("    public int update" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            svrImplStr.append("        return "+entityNameLower+"DAO.update"+entityName+"("+entityNameLower+"Model);\n");                
            svrImplStr.append("    }\n\n");
            svrImplStr.append("    public int delete" + entityName + "("+entityName+"Model "+entityNameLower+"Model) throws Exception{\n");
            //svrImplStr.append("        logger.debug("+entityNameLower+"Model);\n");            
            svrImplStr.append("        return "+entityNameLower+"DAO.delete"+entityName+"("+entityNameLower+"Model);\n");                
            svrImplStr.append("    }\n\n");
            svrImplStr.append("}\n");
            svrImplStr.append("\n");    
            
            
            // ----------- Controller ------------------------
            if (!packagename.equals(""))
            	controllerStr.append("package " + packagename + ".controller;\n");            
            controllerStr.append("\n");
            controllerStr.append("import javax.servlet.http.HttpServletRequest;\n");
			controllerStr.append("import javax.servlet.http.HttpServletResponse;\n");
            controllerStr.append("\n");
			controllerStr.append("import org.springframework.web.servlet.ModelAndView;\n");
			controllerStr.append("import org.springframework.web.servlet.mvc.multiaction.MultiActionController;\n");       
            controllerStr.append("\n");            
            controllerStr.append("import "+packagename+".model."+entityName+"Model;\n"); 
            controllerStr.append("import "+packagename+".service."+entityName+"Service;\n");              
            controllerStr.append("\n");  
            controllerStr.append("/*\n");
            controllerStr.append(" * Controller\n");
            controllerStr.append(" */\n");
            controllerStr.append("public class " + entityName + "Controller extends MultiActionController {\n\n");
            controllerStr.append("    private " + entityName + "Service " + entityNameLower + "Service = null;\n\n");
			controllerStr.append("    public void set" + entityName + "Service(" + entityName + "Service " + entityNameLower + "Service) {\n");
			controllerStr.append("	      this." + entityNameLower + "Service = " + entityNameLower + "Service;\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");    
			controllerStr.append("    public ModelAndView list(HttpServletRequest request, HttpServletResponse response) throws Exception {\n");            
			controllerStr.append("\n");
			controllerStr.append("        // pageSize  Default: 10 \n");
			controllerStr.append("        int pageSize = Integer.parseInt(request.getParameter(\"pageSize\") == null ? \"10\" : request.getParameter(\"pageSize\"));\n");
			controllerStr.append("\n");
			controllerStr.append("        // nowPage  Default: 1 \n");
			controllerStr.append("        int nowPage = Integer.parseInt(request.getParameter(\"nowPage\") == null ? \"1\" : request.getParameter(\"nowPage\"));\n");
			controllerStr.append("\n");
			controllerStr.append("        int startIndex = ( nowPage - 1 ) * pageSize + 1;\n");
			controllerStr.append("        int endIndex = ( nowPage * pageSize );\n");
			controllerStr.append("\n");
			controllerStr.append("        String searchField = request.getParameter(\"searchField\") == null ? \"\" : request.getParameter(\"searchField\");\n");
			controllerStr.append("        String searchText = request.getParameter(\"searchText\") == null ? \"\" : request.getParameter(\"searchText\");\n");
			controllerStr.append("\n");
			controllerStr.append("        " + entityName + "Model " + entityNameLower + " = new " + entityName + "Model();\n");
			controllerStr.append("\n");
			controllerStr.append("        " + entityNameLower + ".setPageSize(pageSize);\n");
			controllerStr.append("        " + entityNameLower + ".setNowPage(nowPage);\n");
			controllerStr.append("        " + entityNameLower + ".setStartIndex(startIndex);\n");
			controllerStr.append("        " + entityNameLower + ".setEndIndex(endIndex);\n");
			controllerStr.append("        " + entityNameLower + ".setSearchField(searchField);\n");
			controllerStr.append("        " + entityNameLower + ".setSearchText(searchText);\n");
			controllerStr.append("\n");
			controllerStr.append("        ModelAndView modelAndView = null;\n");
			controllerStr.append("        String viewName = \"/" + entityNameLower + "/list\";\n");
			controllerStr.append("        modelAndView = new ModelAndView(viewName);\n");
			controllerStr.append("\n");
			controllerStr.append("        modelAndView.addObject(\"" + entityNameLower + "List\", " + entityNameLower + "Service.get" + entityName + "List(" + entityNameLower + "));\n");
			controllerStr.append("        modelAndView.addObject(\"" + entityNameLower + "Count\", " + entityNameLower + "Service.get" + entityName + "ListCount(" + entityNameLower + "));\n");
			controllerStr.append("\n");
			controllerStr.append("        return modelAndView;\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");    
			controllerStr.append("    public ModelAndView view(HttpServletRequest request, HttpServletResponse response) throws Exception {\n");
			controllerStr.append("\n");            
			controllerStr.append("        String " + entityNameLower + "Serial = request.getParameter(\"" + entityNameLower + "Serial\") == null ? \"-1\" : request.getParameter(\"" + entityNameLower + "Serial\");\n");
			controllerStr.append("        String password = request.getParameter(\"password\");\n");
			controllerStr.append("        " + entityName + "Model " + entityNameLower + " = new " + entityName + "Model();\n");
			controllerStr.append("\n");            
			controllerStr.append("        //TODO " + entityNameLower + ".set" + entityName + "Serial(" + entityNameLower + "Serial);\n");
			controllerStr.append("\n");            
			controllerStr.append("\n");            
			controllerStr.append("\n");        
			controllerStr.append("        String viewName = \"/" + entityNameLower + "/view\";\n");
			controllerStr.append("        String errorMsg = null;\n");
			controllerStr.append("\n");        
			controllerStr.append("        " + entityName + "Model " + entityNameLower + "Model = " + entityNameLower + "Service.get" + entityName + "(" + entityNameLower + ");\n");
			controllerStr.append("        if(" + entityNameLower + "Model == null){\n");
			controllerStr.append("            throw new Exception(\"No data.\");\n");
			controllerStr.append("        }\n");
			controllerStr.append("\n");            
			controllerStr.append("        ModelAndView modelAndView = null;\n");
			controllerStr.append("        modelAndView = new ModelAndView(viewName);\n");
			controllerStr.append("        modelAndView.addObject(\"errorMsg\", errorMsg);\n");
			controllerStr.append("        modelAndView.addObject(\"" + entityNameLower + "View\", " + entityNameLower + "Model);\n");
			controllerStr.append("\n");        
			controllerStr.append("        return modelAndView;\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");        
			controllerStr.append("    public ModelAndView delete(HttpServletRequest request, HttpServletResponse response) throws Exception {\n");
			controllerStr.append("\n");            
			controllerStr.append("        String " + entityNameLower + "Serial = request.getParameter(\"" + entityNameLower + "Serial\") == null ? \"-1\" : request.getParameter(\"" + entityNameLower + "Serial\");\n");
			controllerStr.append("        String password = request.getParameter(\"password\");\n");
			controllerStr.append("        " + entityName + "Model " + entityNameLower + " = new " + entityName + "Model();\n");
			controllerStr.append("\n");            
			controllerStr.append("        //TODO " + entityNameLower + ".set" + entityName + "Serial(" + entityNameLower + "Serial);\n");
			controllerStr.append("\n");        
			controllerStr.append("        String viewName = \"/" + entityNameLower + "/list\";\n");
			controllerStr.append("        String errorMsg = null;\n");
			controllerStr.append("\n");        
			controllerStr.append("        " + entityName + "Model " + entityNameLower + "Model = " + entityNameLower + "Service.get" + entityName + "(" + entityNameLower + ");\n");
			controllerStr.append("        if(" + entityNameLower + "Model == null){\n");
			controllerStr.append("            throw new Exception(\"No data.\");\n");
			controllerStr.append("        }\n");
			controllerStr.append("\n");            
			controllerStr.append("        ModelAndView modelAndView = null;\n");
			controllerStr.append("        modelAndView = new ModelAndView(viewName);\n");			
			controllerStr.append("\n");                
			controllerStr.append("        return modelAndView;\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");        
			controllerStr.append("    public ModelAndView writeForm(HttpServletRequest request, HttpServletResponse response) throws Exception {\n");
			controllerStr.append("        return new ModelAndView(\"/" + entityNameLower + "/writeForm\");\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");        
			controllerStr.append("    public ModelAndView modifyForm(HttpServletRequest request, HttpServletResponse response) throws Exception {\n");
			controllerStr.append("        String " + entityNameLower + "Serial = request.getParameter(\"" + entityNameLower + "Serial\") == null ? \"-1\" : request.getParameter(\"" + entityNameLower + "Serial\");\n");
			controllerStr.append("        String password = request.getParameter(\"password\");\n");
			controllerStr.append("        " + entityName + "Model " + entityNameLower + " = new " + entityName + "Model();\n");
			controllerStr.append("\n");            
			controllerStr.append("        //TODO " + entityNameLower + ".set" + entityName + "Serial(" + entityNameLower + "Serial);\n");
			controllerStr.append("\n");        
			controllerStr.append("        String viewName = \"/" + entityNameLower + "/modifyForm\";\n");
			controllerStr.append("        String errorMsg = null;\n");
			controllerStr.append("\n");        
			controllerStr.append("        " + entityName + "Model " + entityNameLower + "Model = " + entityNameLower + "Service.get" + entityName + "(" + entityNameLower + ");\n");
			controllerStr.append("        if(" + entityNameLower + "Model == null){\n");
			controllerStr.append("            throw new Exception(\"No data.\");\n");
			controllerStr.append("        }\n");
			controllerStr.append("\n");        
			controllerStr.append("        if(password == null){//TODO more check\n");
			controllerStr.append("            viewName = \"/" + entityNameLower + "/confirm\";\n");
			controllerStr.append("\n");                
			controllerStr.append("            if(password != null && !password.equals(\"\")){\n");
			controllerStr.append("                errorMsg = \"Invalid passwd.\";\n");
			controllerStr.append("            }\n");
			controllerStr.append("\n");        
			controllerStr.append("            ModelAndView modelAndView = null;\n");
			controllerStr.append("            modelAndView = new ModelAndView(viewName);\n");
			controllerStr.append("            modelAndView.addObject(\"errorMsg\", errorMsg);\n");
			controllerStr.append("            modelAndView.addObject(\"" + entityNameLower + "View\", " + entityNameLower + ");\n");
			controllerStr.append("\n");                
			controllerStr.append("            return modelAndView;\n");
			controllerStr.append("        }else{\n");
			controllerStr.append("\n");                
			controllerStr.append("            ModelAndView modelAndView = null;\n");
			controllerStr.append("            modelAndView = new ModelAndView(viewName);\n");
			controllerStr.append("            modelAndView.addObject(\"" + entityNameLower + "View\", " + entityNameLower + "Model);\n");
			controllerStr.append("\n");                
			controllerStr.append("            return modelAndView;\n");
			controllerStr.append("        }\n");
			controllerStr.append("    }\n");
			controllerStr.append("\n");        
            controllerStr.append("}\n");
            controllerStr.append("\n");               
            
            // ----------- FormController ------------------------
            if (!packagename.equals(""))
            	formControllerStr.append("package " + packagename + ".controller;\n");            
            formControllerStr.append("\n");
            formControllerStr.append("import java.io.File;\n");
            formControllerStr.append("\n");
            formControllerStr.append("import javax.servlet.ServletException;\n");
            formControllerStr.append("import javax.servlet.http.HttpServletRequest;\n");
            formControllerStr.append("import javax.servlet.http.HttpServletResponse;\n");
            formControllerStr.append("\n");
            formControllerStr.append("import org.springframework.validation.BindException;\n");
            formControllerStr.append("import org.springframework.web.bind.ServletRequestDataBinder;\n");
            formControllerStr.append("import org.springframework.web.multipart.MultipartFile;\n");
            formControllerStr.append("import org.springframework.web.multipart.MultipartHttpServletRequest;\n");
            formControllerStr.append("import org.springframework.web.multipart.support.StringMultipartFileEditor;\n");
            formControllerStr.append("import org.springframework.web.servlet.ModelAndView;\n");
            formControllerStr.append("import org.springframework.web.servlet.mvc.SimpleFormController;\n");
            formControllerStr.append("\n");
            formControllerStr.append("import " + packagename + ".model."+entityName+"Model;\n");
            formControllerStr.append("import " + packagename + ".service."+entityName+"Service;\n");
            formControllerStr.append("\n");
            formControllerStr.append("public class "+entityName+"FormController extends SimpleFormController {\n");
            formControllerStr.append("\n");
            formControllerStr.append("    private "+entityName+"Service " + entityNameLower + "Service = null;\n");
            formControllerStr.append("\n");
            formControllerStr.append("    public void set"+entityName+"Service("+entityName+"Service " + entityNameLower + "Service) {\n");
            formControllerStr.append("        this." + entityNameLower + "Service = " + entityNameLower + "Service;\n");
            formControllerStr.append("    }\n");
            formControllerStr.append("\n");
            formControllerStr.append("    public "+entityName+"FormController() {\n");
            formControllerStr.append("        setCommandName(\"command\");\n");
            formControllerStr.append("        setCommandClass("+entityName+"Model.class);\n");
            formControllerStr.append("    }\n");
            formControllerStr.append("\n");
            formControllerStr.append("    protected ModelAndView onSubmit(HttpServletRequest request,\n");
            formControllerStr.append("            HttpServletResponse response, Object command,\n");
            formControllerStr.append("            BindException exception) throws Exception {\n");
            formControllerStr.append("\n");
            formControllerStr.append("        "+entityName+"Model " + entityNameLower + " = ("+entityName+"Model) command;\n");
            formControllerStr.append("\n");
            formControllerStr.append("        return new ModelAndView(getSuccessView());\n");
            formControllerStr.append("    }\n");
            formControllerStr.append("	\n");
            formControllerStr.append("	protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws ServletException {\n");
            formControllerStr.append("		binder.registerCustomEditor(String.class, new StringMultipartFileEditor());\n");
            formControllerStr.append("	}\n");  
            formControllerStr.append("\n");
            formControllerStr.append("}\n");
            
            // ----------- spring bean ------------------------
            beanStr.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            beanStr.append("\n");
            beanStr.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"\n");
            beanStr.append("	xmlns:p=\"http://www.springframework.org/schema/p\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
            beanStr.append("	xmlns:aop=\"http://www.springframework.org/schema/aop\" xmlns:tx=\"http://www.springframework.org/schema/tx\"\n");
            beanStr.append("	xsi:schemaLocation=\"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-2.5.xsd\n");
            beanStr.append("            http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop-2.5.xsd\n");
            beanStr.append("            http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx-2.5.xsd\">\n\n");
            
			beanStr.append("    <bean id=\"" + entityNameLower + "DAO\" class=\""+packagename+".dao."+entityName+"DAOImpl\">\n");     
			beanStr.append("    	<property name=\"sqlMapClientTemplate\" ref=\"sqlMapClientTemplate\" />\n");     
			beanStr.append("    </bean>\n\n"); 
			                    
			beanStr.append("    <bean id=\"" + entityNameLower + "Service\" class=\""+packagename+".service."+entityName+"ServiceImpl\">\n");     
			beanStr.append("    	<property name=\"" + entityNameLower + "DAO\" ref=\"" + entityNameLower + "DAO\" />\n");     
			beanStr.append("    </bean>\n\n");
			                    
			beanStr.append("    <bean id=\"" + entityNameLower + "Controller\" class=\""+packagename+".controller."+entityName+"Controller\" p:methodNameResolver-ref=\"" + entityNameLower + "Resolver\">\n");
			beanStr.append("    	<property name=\"" + entityNameLower + "Service\" ref=\"" + entityNameLower + "Service\" />\n");
			beanStr.append("    </bean>\n\n");	
			                    
			beanStr.append("    <bean id=\"" + entityNameLower + "FormController\" class=\""+packagename+".controller."+entityName+"FormController\" >\n");
			beanStr.append("    	<property name=\"" + entityNameLower + "Service\" ref=\"" + entityNameLower + "Service\" />\n");
			beanStr.append("    	<property name=\"formView\" value=\"/" + entityNameLower + "/writeForm\" />\n");
			beanStr.append("    	<property name=\"successView\" value=\"redirect:/" + entityNameLower + "/list.do\" />\n");
			beanStr.append("    </bean>\n\n");	
			                    
			beanStr.append("    <bean class=\"org.springframework.web.servlet.handler.SimpleUrlHandlerMapping\">\n");
			beanStr.append("    	<property name=\"alwaysUseFullPath\" value=\"true\"></property>\n");
			beanStr.append("    	<property name=\"mappings\">\n");
			beanStr.append("    		<props>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/list.do\">" + entityNameLower + "Controller</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/view.do\">" + entityNameLower + "Controller</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/delete.do\">" + entityNameLower + "Controller</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/writeForm.do\">" + entityNameLower + "Controller</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/write.do\">" + entityNameLower + "FormController</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/modifyForm.do\">" + entityNameLower + "Controller</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/modify.do\">" + entityNameLower + "FormController</prop>\n");
			beanStr.append("    		</props>\n");
			beanStr.append("    	</property>\n");
			beanStr.append("    	<property name=\"interceptors\">\n");
			beanStr.append("    		<list>\n");
			beanStr.append("    			<ref bean=\"paaInterceptor\"/>\n");
			beanStr.append("    		</list>\n");
			beanStr.append("    	</property>\n");    			
			beanStr.append("    </bean>\n\n");				
			                    
			beanStr.append("    <bean id=\"" + entityNameLower + "Resolver\" class=\"org.springframework.web.servlet.mvc.multiaction.PropertiesMethodNameResolver\">\n");
			beanStr.append("    	<property name=\"mappings\">\n");
			beanStr.append("    		<props>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/list.do\">list</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/view.do\">view</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/delete.do\">delete</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/writeForm.do\">writeForm</prop>\n");
			beanStr.append("    			<prop key=\"/" + entityNameLower + "/modifyForm.do\">modifyForm</prop>\n");
			beanStr.append("    		</props>\n");
			beanStr.append("    	</property>\n");
			beanStr.append("    </bean>\n\n");	
			
			beanStr.append("</beans>");	

            writeFile(savedir + "/model", entityName+"Model.java", modelStr.toString());
            writeFile(savedir + "/sql", entityName+".xml", sqlStr.toString());
            writeFile(savedir + "/dao", entityName+"DAO.java", daoStr.toString());
            writeFile(savedir + "/dao", entityName+"DAOImpl.java", daoImplStr.toString());
            writeFile(savedir + "/service", entityName+"Service.java", svrStr.toString());
            writeFile(savedir + "/service", entityName+"ServiceImpl.java", svrImplStr.toString());    
            writeFile(savedir + "/controller", entityName+"Controller.java", controllerStr.toString());  
            writeFile(savedir + "/controller", entityName+"FormController.java", formControllerStr.toString());
            writeFile(savedir + "/spring", "applicationContext"+ entityName+".xml", beanStr.toString());

        } catch (java.io.FileNotFoundException e) {
            System.out.println("wrong path -> " + savedir);
        } catch (Exception e) {
            if (e instanceof SQLException) {
                SQLException ex = (SQLException) e;
                System.out.println("SQLException Error Code : " + ex.getErrorCode());
            }
            System.out.close();
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * File Writing
     * 
     * @param dir
     * @param filename
     * @param msg
     * @throws java.io.FileNotFoundException
     */
    public static void writeFile(String dir, String filename, String msg) throws java.io.FileNotFoundException {
    	
        File file = new File(dir);
        if (!file.exists()){
        	file.mkdir();
        }
    	
        if (!dir.endsWith("/")) {
            dir += "/";
        }

        try {
            PrintWriter out =
                new PrintWriter(new BufferedOutputStream(new FileOutputStream(dir + filename, false)));
            out.print(new String(msg.getBytes("UTF-8")));
            out.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
