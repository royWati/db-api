package ekenya.co.ke.dbapiv3.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ekenya.co.ke.dbapiv3.DbApiV3Application;
import oracle.jdbc.OracleTypes;
import oracle.jdbc.internal.OracleClob;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.stereotype.Service;

import javax.sql.rowset.serial.SerialClob;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
//import java.util.logging.//logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class DatabaseExtractorImpl implements DatabaseExtractor {
    @Autowired
    private JdbcTemplate jdbcTemplate;
 //   private final static //logger //logger = //logger.get//logger(DatabaseExtractorImpl.class.getName());

    @Value("${primarydb}")
    private String primarydb;
    @Value("${schema-name}")
    private String schemaName;
    @Value("${file-storage.sp-location}")
    private String spFileLocation;

    @Override
    public JsonArray fetchSqlProcedures() throws Exception {
        String sql = "";
        String sql_parameters = "";



        // switch the query based on the selected database
        switch (primarydb){
            case "mssql":
                sql = "select routine_name as procedure_name from information_schema.routines where " +
                        "routine_schema not in ('sys', 'information_schema',\n" +
                        " 'mysql', 'performance_schema')";

                sql_parameters = "SELECT ORDINAL_POSITION as parameterPosition,\n" +
                        "DATA_TYPE as dataType,\n" +
                        "PARAMETER_NAME as parameterName,\n" +
                        "PARAMETER_MODE as inOut \n" +
                        "FROM information_schema.parameters \n" +
                        "WHERE SPECIFIC_NAME = ? ";
                break;

            case "mysql":
                sql = "select routine_name as procedure_name from information_schema.routines where " +
                        "routine_schema not in ('sys', 'information_schema',\n" +
                        " 'mysql', 'performance_schema')";

                sql_parameters = "SELECT ORDINAL_POSITION as parameterPosition,\n" +
                        "DATA_TYPE as dataType,\n" +
                        "PARAMETER_NAME as parameterName,\n" +
                        "PARAMETER_MODE as inOut \n" +
                        "FROM information_schema.parameters \n" +
                        "WHERE SPECIFIC_NAME = ? ";
                break;

            case "mariadb":
                sql = "select routine_name as procedure_name from information_schema.routines where " +
                        "routine_schema not in ('sys', 'information_schema',\n" +
                        " 'mysql', 'performance_schema')";

                sql_parameters = "SELECT ORDINAL_POSITION as parameterPosition,\n" +
                        "DATA_TYPE as dataType,\n" +
                        "PARAMETER_NAME as parameterName,\n" +
                        "PARAMETER_MODE as in_Out \n" +
                        "FROM information_schema.parameters \n" +
                        "WHERE SPECIFIC_NAME = ? ";
                break;

            case "oracle":
                sql = "select proc.object_name as procedure_name\n" +
                        "from sys.ALL_PROCEDURES proc \n" +
                        "where proc.owner not in ('ANONYMOUS','CTXSYS','DBSNMP','EXFSYS',\n" +
                        "          'MDSYS', 'MGMT_VIEW','OLAPSYS','OWBSYS','ORDPLUGINS', 'ORDSYS',\n" +
                        "          'OUTLN', 'SI_INFORMTN_SCHEMA','SYS','SYSMAN','SYSTEM', 'TSMSYS',\n" +
                        "          'WK_TEST', 'WKSYS', 'WKPROXY','WMSYS','XDB','APEX_040000', \n" +
                        "          'APEX_PUBLIC_USER','DIP', 'FLOWS_30000','FLOWS_FILES','MDDATA',\n" +
                        "          'ORACLE_OCM', 'XS$NULL', 'SPATIAL_CSW_ADMIN_USR', 'LBACSYS',\n" +
                        "          'SPATIAL_WFS_ADMIN_USR', 'PUBLIC', 'APEX_040200')\n" +
                        "      and object_type = 'PROCEDURE' and proc.owner = ? \n" +
                        "group by proc.owner,  proc.object_name ";

                sql_parameters = "select \n" +
                        "       args.argument_name as parameterName,\n" +
                        "       args.in_out,\n" +
                        "       args.data_type as dataType,\n" +
                        "       args.position as parameterPosition\n" +
                        "from sys.all_procedures proc\n" +
                        "left join sys.all_arguments args\n" +
                        "    on proc.object_id = args.object_id\n" +
                        "where  object_type = 'PROCEDURE' AND proc.object_name = ? AND proc.owner = ? " +
                        "order by procedure_name,\n" +
                        "         args.position ";
                break;
        }

        // prepare the statement for execution
        String finalSql = sql;

        //logger.info("PROCEDURE : "+finalSql);
        //logger.info("PARAMETERS : "+sql_parameters);

        //jdbcTemplate.queryForList(finalSql).forEach(System.out::println);
        PreparedStatementCreator preparedStatementCreator = connection -> {

            PreparedStatement statement = connection.prepareStatement(finalSql);

            if ("oracle".equals(primarydb)){
                //logger.info("SCHEMA NAME ..."+schemaName);
                statement.setString(1, schemaName);
            }
            return statement;
        };

        boolean executeUpdate = false;

        // fetch records from the provided database
        JsonArray jsonArray = new JsonArray();

        ExecuteJdbcQuery(jsonArray, preparedStatementCreator,1, executeUpdate);

        //logger.info("first results ... "+jsonArray.toString());
        //logger.info("total results ... "+jsonArray.size());

        JsonArray updatedArray = new JsonArray();

        String finalSql_parameters = sql_parameters;


        jsonArray.forEach(o -> {
            JsonObject object= (JsonObject) o;
            JsonArray parameter_array = new JsonArray();

            PreparedStatementCreator preparedStatementCreator1 =null;
            preparedStatementCreator1 = connection -> {
                PreparedStatement statement = connection.prepareStatement(finalSql_parameters);

                String procedure_name = "";
                switch (primarydb){
                    case "oracle":
                        procedure_name = "PROCEDURE_NAME";
                        break;
                    case "mssql":
                    case "mysql":
                        procedure_name = "procedure_name";
                        break;
                }


                String value = object.get(procedure_name).getAsString();

                if ("oracle".equals(primarydb)){
                    object.remove("PROCEDURE_NAME");
                    object.addProperty("procedure_name",value);
                    statement.setString(2, schemaName);
                }
                //logger.info("value "+value);
                statement.setString(1, value);
                return statement;
            };

            try {
                ExecuteJdbcQuery(parameter_array, preparedStatementCreator1,1, executeUpdate);

                //logger.info("param array"+parameter_array);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if("oracle".equals(primarydb)){
                JsonArray oracle_updated_array = new JsonArray();
                parameter_array.forEach(o1 -> {
                    JsonObject jsonObject = (JsonObject) o1;

                    //logger.info("object from parameter "+jsonObject.toString());
                    JsonObject oracleObject = new JsonObject();
                    oracleObject.addProperty("parameterName",jsonObject.get("PARAMETERNAME").getAsString());
                    oracleObject.addProperty("dataType",jsonObject.get("DATATYPE").getAsString());
                    oracleObject.addProperty("inOut",jsonObject.get("IN_OUT").getAsString());
                    if (jsonObject.get("PARAMETERPOSITION").getAsString() != "null")
                        oracleObject.addProperty("parameterPosition",jsonObject.get("PARAMETERPOSITION").getAsLong());
                    else
                        oracleObject.addProperty("parameterPosition",0);

                    oracle_updated_array.add(oracleObject);
                });

                object.add("parameter",oracle_updated_array);
            }else if ("mariadb".equals(primarydb)){
                JsonArray maria_db_array = new JsonArray();
                parameter_array.forEach(o1 -> {
                    JsonObject jsonObject = (JsonObject) o1;

                    //logger.info("object from parameter "+jsonObject.toString());
                    JsonObject oracleObject = new JsonObject();
                    oracleObject.addProperty("parameterName",jsonObject.get("parameterName").getAsString());
                    oracleObject.addProperty("dataType",jsonObject.get("dataType").getAsString());
                    oracleObject.addProperty("inOut",jsonObject.get("parameterName").getAsString());

                    maria_db_array.add(oracleObject);
                });

                object.add("parameter",maria_db_array);

            }else{
                object.add("parameter",parameter_array);
            }

            updatedArray.add(object);

        });

        return updatedArray;
    }

    /**
     * this current implementation only contains the oracle implementation
     * @return
     */
    @Override
    public JsonArray fetchTables() throws Exception {

        String sql_statement = "SELECT  TABLE_NAME\n" +
                "FROM\n" +
                "  user_tables ";

        // prepare the statement for execution
        String finalSql = sql_statement;

        //jdbcTemplate.queryForList(finalSql).forEach(System.out::println);
        PreparedStatementCreator preparedStatementCreator = connection -> {

            PreparedStatement statement = connection.prepareStatement(finalSql);

            return statement;
        };
        boolean executeUpdate = false;

        // fetch records from the provided database
        JsonArray jsonArray = new JsonArray();

        ExecuteJdbcQuery(jsonArray, preparedStatementCreator,1, executeUpdate);

        //logger.info("first results ... "+jsonArray.toString());
        //logger.info("total results ... "+jsonArray.size());


        return null;
    }

    @Override
    public void updateCurrentSpStore(JsonArray jsonArray) {
        FileWriter fileWriter = null;

        //logger.info("file location "+spFileLocation);
        //logger.info("file data "+jsonArray);

        try {
            String dataTemplate = jsonArray.toString();
            System.out.println(dataTemplate);
            fileWriter = new FileWriter(spFileLocation);
            fileWriter.write(dataTemplate);
            fileWriter.flush();
            System.out.println("data template updated successfully...");
        } catch (Exception e) {
            //logger.info(e.getMessage());
        } finally {
            try {
                fileWriter.close();
            } catch (Exception e) {
                //logger.info(e.getMessage());
            }
        }
    }

    @Override
    public JsonElement retrieveFileContent(String fileName) {

        //logger.info("file name : "+fileName);
        InputStream is = null;
        JsonElement jsonElement = null;
        try {
            is = new FileInputStream(fileName);

            JsonParser jsonParser = new JsonParser();
            jsonElement = jsonParser.parse(new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return jsonElement;
    }

    @Override
    public JsonObject loadStoredProcedure(String procedureName) {
        //logger.info("procedure name : "+procedureName);
        JsonObject jsonObject = new JsonObject();

        boolean found = false;

    //    //logger.info("all sps : "+DbApiV3Application.spJsonElements.toString());

        JsonArray jsonArray = DbApiV3Application.spJsonElements.getAsJsonArray();

        for (int i = 0; i< jsonArray.size() ; i++){
            JsonObject object = jsonArray.get(i).getAsJsonObject();
       //     //logger.info("object - "+object);

            if (procedureName.equals(object.get("procedure_name").getAsString())){
                jsonObject = object;
                found = true;
                break;
            }
        }
        return found ? jsonObject : null;
    }

    @Override
    public JsonArray executeStoredProcedure(String procedureName, JsonArray jsonArray) {
        StringBuilder stringBuilder = new StringBuilder();
        StringJoiner parameterJoiner = new StringJoiner(",","(",")");

        jsonArray.forEach(o -> parameterJoiner.add("?"));

        //logger.info("JSON ARRAY "+jsonArray);
        stringBuilder.append("{CALL ").append(procedureName)
                .append(parameterJoiner.toString()).append("}");

        //logger.info(stringBuilder.toString());
        String query = stringBuilder.toString();

        List<SqlParameter> parameterList = new ArrayList<>();

        JsonArray resultArray = new JsonArray();

        ResultSet resultSet = null;

        Connection connection = null;
        CallableStatement callableStatement = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();

            callableStatement = connection.prepareCall(query);


            boolean hasRegisteredOutParameter = false;

            List<String> outParameterList = new ArrayList<>();

            for (int i = 0; i < jsonArray.size() ; i ++){
                JsonObject object = jsonArray.get(i).getAsJsonObject();
                //logger.info("object to create --"+i+"---- "+object.toString());
                String field = object.get("field").getAsString();
                String value = object.get("value").getAsString();
                String dataType = object.get("dataType").getAsString();
                String in_out_value = object.get("inOut").getAsString();

                Reader reader = null;
                try {
                    switch (dataType.toLowerCase()){
                        case "clob":
                            System.out.println("clob object found...");
                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value)){
                                callableStatement.registerOutParameter(field,OracleTypes.CLOB );
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);
                            }else{
                                callableStatement.setObject(field, value);

                                reader = new StringReader(value);

                                 callableStatement.setClob(field, reader, value.length());

                            //    Clob clob = new SerialClob(value.toCharArray());
                            //    callableStatement.setClob(field, clob);
                            }
                            break;
                        case "nvarchar":
                        case "nvarchar2":

                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value)){
                                callableStatement.registerOutParameter(field, OracleTypes.NVARCHAR);
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);
                            }else{
                                callableStatement.setString(field, value);
                            }
                            break;
                        case "varchar":
                        case "date":
                        case "varchar2":

                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value) ){
                                callableStatement.registerOutParameter(field,OracleTypes.VARCHAR);
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);
                            }else{
                                callableStatement.setString(field, value);
                            }

                            break;
                        case "integer":
                        case "number":

                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value)){
                                callableStatement.registerOutParameter(field,OracleTypes.NUMBER);
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);
                            }else{

                                // handling NumberFormatException error

                                if(!"".equals(value)){
                                    if(value.contains("."))
                                        callableStatement.setDouble(field, Double.parseDouble(value));
                                    else
                                    callableStatement.setInt(field, Integer.parseInt(value));
                                }else{
                                    callableStatement.setInt(field, 0);
                                }
                            }
                            break;
                        case "bigint":

                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value)){
                                callableStatement.registerOutParameter(field,OracleTypes.NUMBER);
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);

                                //logger.info("OUT PARAMETER FOUND : "+field);
                            }else{
                                callableStatement.setLong(field, Long.parseLong(value));
                            }
                            break;
                        case "ref cursor":

                            //logger.info("OUT VALUE : "+in_out_value);

                            //
                            if ("IN/OUT".equals(in_out_value) || "OUT".equals(in_out_value)){
                                if ("oracle".equals(primarydb)){
                                    //logger.info("oracle db found...");
                                //    callableStatement.registerOutParameter();
                                    callableStatement.registerOutParameter(field, OracleTypes.CURSOR);
                                }else{
                                    callableStatement.registerOutParameter(field, Types.REF_CURSOR);
                                }
                                hasRegisteredOutParameter = true;
                                outParameterList.add(field);
                            }else{
                                callableStatement.setString(field, value);
                            }
                            //logger.info("OUT PARAMETER FOUND : "+field);
                            break;
                    }
                    //logger.info("value +"+i+" added successfully");

                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }finally {
                    if (reader != null) reader.close();
                }
                int j = i+1;
                System.out.println("parameters added  = "+ j);
            }

            //boolean hasResultSet =


            //
            callableStatement.execute();



            if (hasRegisteredOutParameter){

                String s = outParameterList.get(0);
                resultSet = (ResultSet) callableStatement.getObject(s);
                GenerateJsonArrayFromResultSet(resultArray, resultSet, 0);
                //logger.info("sp results -- "+resultSet.getFetchSize());

            }else{
                resultSet = callableStatement.getResultSet();
                GenerateJsonArrayFromResultSet(resultArray, resultSet, 0);
                //logger.info("sp results -- "+resultSet.getFetchSize());
            }

//            if (hasResultSet){
//
//
//            }else{
//
//                //logger.info("does not have a result set");
//            }
            //resultSet = callableStatement.executeQuery();

        }catch (Exception e){
            e.printStackTrace();
            //   throw new Exception(e.getMessage());
            String error = e.getLocalizedMessage();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("FAILED_EXCEPTION",error);

            resultArray.add(jsonObject);

        }finally {
            try {
                if (!connection.isClosed()){
                    connection.close();
                    //logger.info("connection closed...");
                }
                if (!resultSet.isClosed()){
                    resultSet.close();
                    //logger.info("sp result set closed...");
                }

                if (!callableStatement.isClosed()){
                    callableStatement.close();
                    //logger.info("callableStatement set closed...");
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return resultArray;
    }

    @Override
    public JsonArray executeSqlStatement(String sqlStatement, JsonArray jsonArray) throws Exception {
        //logger.info(sqlStatement);

        boolean executeUpdate = false;
        // check if the statement contains update

        String execute_type = sqlStatement.toLowerCase().split(" ")[0];

        if("update".equals(execute_type) || "insert".equals(execute_type)){
            executeUpdate = true;
        }

        //logger.info("execute update : "+executeUpdate);
        PreparedStatementCreator preparedStatementCreator = connection -> {
            PreparedStatement statement = connection.prepareStatement(sqlStatement);

            for (int i = 0; i < jsonArray.size(); i++){
                String initial_object = jsonArray.get(i).getAsString();
                int parameter_index = i+1;

                if ("oracle".equals(primarydb)){

                    if (initial_object.matches("[0-9]")){
                        statement.setInt(parameter_index,Integer.parseInt(initial_object));
                    }else {
                        statement.setString(parameter_index, initial_object);
                    }
                }else{

                    /**
                     * this update affects order by values, since the order by values threw an exception,
                     * since order by values appear as last values in the array, we check for  values and convert them to
                     * integers, all other values in the clauses will be treated as strings.
                     */

                    if (parameter_index == jsonArray.size()-1 || parameter_index == jsonArray.size()){

                        // check if the code is a string or a number coz of the esb processing

                        if (NumberUtils.isCreatable(initial_object)){
                            statement.setInt(parameter_index, Integer.parseInt(initial_object));
                            //logger.info("number found to be working "+initial_object);
                        }else{
                            statement.setString(parameter_index, initial_object);
                        }

                    }else{
                        statement.setString(parameter_index, initial_object);
                    }
                }


            }
            return statement;
        };

        JsonArray responseArray = new JsonArray();

        ExecuteJdbcQuery(responseArray, preparedStatementCreator,1,executeUpdate);

        return responseArray;
    }

    @Override
    public Object executeSqlStatement(String sqlStatement, JsonObject jsonObject) throws Exception {

        // check for the existence of the sqlStatement
        return null;
    }

    @Override
    public Object executeSqlStatement(JsonObject queryTemplate, JsonObject requestObject) throws Exception {

        JsonArray jsonArray = DbApiV3Application.queryTemplate.getAsJsonArray();
        String template = "";
        for (JsonElement jsonElement : jsonArray) {
            JsonObject o = jsonElement.getAsJsonObject();

            //logger.info(o.toString());
            //logger.info(o.get("CRUD_TYPE").getAsString());

            if (o.get("CRUD_TYPE").getAsString().equals("UPDATE")){
                if ("oracle".equals(primarydb)){
                    template = o.get("ORACLE").getAsString();
                }else{
                    template = o.get("MSSQL").getAsString();
                }
            }

        }


        //logger.info(template);

        Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(template);
        while(m.find()) {
            switch (m.group(1)){
                case "TOP":
                    String top = queryTemplate.get("TABLE_NAME").getAsString();
                    template = template.replace("{TOP}",top);
                    break;
                case "TABLE_NAME":
                    String table_name = queryTemplate.get("TABLE_NAME").getAsString();
                    template = template.replace("{TABLE_NAME}", table_name);
                    break;
                case "WHERE_CLAUSE":
                    String where_clause = queryTemplate.get("WHERE_CLAUSE").getAsString();
                    template = template.replace("{WHERE_CLAUSE}",where_clause);
                    break;
                case "GROUP_STATEMENT":
                    String group_statement = queryTemplate.get("GROUP_STATEMENT").getAsString();
                    template = template.replace("{GROUP_STATEMENT}",
                            group_statement);
                    break;
                case "COLUMN_NAMES":
                    String column_names = queryTemplate.get("COLUMN_NAMES").getAsString();
                    template = template.replace("{COLUMN_NAMES}",
                            column_names);
                    break;
            }
            System.out.println(m.group(1));
        }

        //logger.info(template);

        return template;
    }

    @Override
    public JsonObject queryInspector(String query, JsonArray jsonArray) {
        return null;
    }

    private void ExecuteJdbcQuery(JsonArray parameter_array, PreparedStatementCreator preparedStatementCreator1, int flag,
                                  boolean executeUpdate) throws Exception {
        if (executeUpdate){
            try {
                int affected_rows = jdbcTemplate.update(preparedStatementCreator1);

                parameter_array.add(affected_rows);

            }catch (Exception e){
                //logger.info("ERROR IN EXECUTING UPDATE : "+e.getMessage());
                throw new Exception(e.getMessage());
            }

        }else{
            jdbcTemplate.query(preparedStatementCreator1,resultSet -> {
                if (!resultSet.isClosed()){
                    try {

                        //logger.info("result set size : "+resultSet.getFetchSize());
                        GenerateJsonArrayFromResultSet(parameter_array, resultSet,flag);

                        //logger.info("total results..."+parameter_array.size());

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.add("content", parameter_array);
                        //   future.complete(jsonObject);
                    }catch (Exception e){
                        e.printStackTrace();
                        //    future.failed();
                        //logger.info("empty result set");
                    }
                }else{
                    //logger.info("result set is closed");
                    //    future.failed();
                }
            });
        }


    }

    private void GenerateJsonArrayFromResultSet(JsonArray jsonArray, ResultSet resultSet, int flag) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int column = metaData.getColumnCount();

            //logger.info("columns.."+column);
            //logger.info("flag status.."+flag);

        if (flag == 0){
                BuildResultSetObjectForValues(jsonArray, resultSet, metaData, column);
        }else{
            BuildResultSetObject(jsonArray, resultSet, metaData, column);
        }

    }

    private void BuildResultSetObject(JsonArray jsonArray, ResultSet resultSet,
                                      ResultSetMetaData metaData, int column) throws SQLException {
        //logger.info("building result set..."+resultSet.getRow());

        if (resultSet.getRow() > 0 ){

            do {
                JsonObject dataObject = new JsonObject();
                for (int i = 0; i < column; i++){

                    int columnIndex = i+1;
                    String columnName = metaData.getColumnLabel(columnIndex);
                    String value = resultSet.getString(columnName);

                    RowToJsonObjectMapper(value, dataObject, columnName);
                }
                //logger.info("object details ... "+dataObject.toString());
                jsonArray.add(dataObject);

            }while (resultSet.next());

        }else {
            //logger.info("empty result set found....");
        }
    }

    private void RowToJsonObjectMapper(String  value, JsonObject dataObject, String columnName) throws SQLException {
   //     String value = resultSet.getString(columnName);

  //      //logger.info("value"+value);
        if (value != null ){


            if (value.matches("[0-9]")){
                dataObject.addProperty(columnName,Long.parseLong(value));
            }else{

               try{
                   JsonElement element = new JsonParser().parse(value);

                   if (element instanceof JsonArray || element instanceof JsonObject) dataObject.add(columnName, element);
                   else dataObject.addProperty(columnName, value);
               }catch (Exception e ){
                   dataObject.addProperty(columnName,value);
               }

            }
        }else{
            dataObject.addProperty(columnName, "null");
        }
    }

    private void BuildResultSetObjectForValues(JsonArray jsonArray, ResultSet resultSet,
                                               ResultSetMetaData metaData, int column) throws SQLException {

    //    int i = 0;


//        while (resultSet.next()){
//            JsonObject dataObject = new JsonObject();
//
//                int columnIndex = i+1;
//                String columnName = metaData.getColumnLabel(columnIndex);
//                //logger.info("column name --- "+columnName);
//                String value = resultSet.getString(columnName);
//                RowToJsonObjectMapper(value, dataObject, columnName);
//
//            jsonArray.add(dataObject);
//            i++;
//        }


        while (resultSet.next()){
            JsonObject dataObject = new JsonObject();
            for (int i = 0; i < column; i++){

                int columnIndex = i+1;
                String columnName = metaData.getColumnLabel(columnIndex);
         //       //logger.info("column name --- "+columnName);
                String value = resultSet.getString(columnName);

                RowToJsonObjectMapper(value, dataObject, columnName);
            }


            //     //logger.info("object details ... "+dataObject.toString());
            jsonArray.add(dataObject);
        }

    }

}
