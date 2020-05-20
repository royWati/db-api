package ekenya.co.ke.dbapiv3;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApiService {

    @Value("${primarydb}")
    private String primarydb;
    @Autowired private DatabaseExtractor databaseExtractor;

    private final static Logger logger = Logger.getLogger(ApiService.class.getName());

    private JsonParser jsonParser = new JsonParser();

    public Object executeRawSqlStatement(String value){

        JsonObject jsonObject = jsonParser.parse(value).getAsJsonObject();

        String query = jsonObject.get("query").getAsString();

        JsonArray jsonArray = jsonObject.getAsJsonArray("data");

        JsonObject responseObject = new JsonObject();
        try {
            JsonArray response_array = databaseExtractor.executeSqlStatement(query, jsonArray);


            responseObject.addProperty("status", 200);
            responseObject.addProperty("message", "database execution was successful");
            responseObject.add("data",response_array);

        }catch (Exception e){
            responseObject.addProperty("status", 500);
            responseObject.addProperty("message", "error in database execution -> "+e.getMessage());
        }

        return responseObject.toString();
    }

    public Object executeStoredProcedure(String query) {
        JsonObject jsonObject = jsonParser.parse(query).getAsJsonObject();

        logger.info("sp request object : "+jsonObject);

        String procedureName = jsonObject.get("procedure").getAsString();
        JsonObject dataObject = jsonObject.get("parameters").getAsJsonObject();

        JsonObject retrieveStructure = databaseExtractor.loadStoredProcedure(procedureName);
        JsonObject responseObject = new JsonObject();

        logger.info("procedure template : "+retrieveStructure);

        final boolean[] hasMissingValues = {false};
        StringJoiner missingJoiner = new StringJoiner(",","[","]");

        StringBuilder builder = new StringBuilder();
        builder.append("Currently missing the following values ");

        if (retrieveStructure != null){
            JsonArray jsonArray = new JsonArray();
            retrieveStructure.get("parameter").getAsJsonArray().forEach(o -> {
                JsonObject object = (JsonObject) o;

                String fieldName = object.get("parameterName").getAsString();

                //    logger.info("JSON -- "+jsonObject);
                if (dataObject.has(fieldName)){
                    JsonObject obj = new JsonObject();
                    obj.addProperty("field",fieldName);
                    obj.addProperty("dataType", object.get("dataType").getAsString());
                    obj.addProperty("inOut", object.get("inOut").getAsString());

                    logger.info("field name : "+fieldName);
                    String value = dataObject.get(fieldName).getAsString();

                    obj.addProperty("value",value);

                    jsonArray.add(obj);
                }else{
                    logger.info("null value found");
                    hasMissingValues[0] = true;
                    missingJoiner.add(fieldName);
                }
            });

            if (!hasMissingValues[0]){
                try {
                    JsonArray response = databaseExtractor.executeStoredProcedure(procedureName, jsonArray);

                    if (response.size() == 1 && response.get(0).getAsJsonObject().has("FAILED_EXCEPTION")){

                        String error = response.get(0).getAsJsonObject().get("FAILED_EXCEPTION").getAsString();

                        responseObject.addProperty("status", 500);
                        responseObject.addProperty("message", "failed execution -> "+error);
                    }else{
                        responseObject.addProperty("status", 200);
                        responseObject.addProperty("message", "data set for messaging");
                        responseObject.add("data", response);
                    }

                }catch (Exception e){

                    logger.info("message "+e.getMessage());
                    logger.info("cause "+e.getCause());
                    String message = e.getLocalizedMessage();
                    jsonObject.addProperty("status",500);
                    jsonObject.addProperty("message","Failed execution -> "+message);
                }

            }else{
                responseObject.addProperty("status", 404);
                String message = builder.append(missingJoiner.toString()).toString();
                responseObject.addProperty("message", message);
            }

        }else{
            responseObject.addProperty("status",404);
            responseObject.addProperty("message", "Sp not found");
        }

        return responseObject.toString();
    }

    public Object getStoredProcedures() {
        JsonObject jsonObject = new JsonObject();
        try {
            JsonElement jsonElement = DbApiV3Application.spJsonElements;

            String jsonArrayString = jsonElement.toString();
            JsonArray jsonArray = jsonElement.getAsJsonArray();

            jsonObject.addProperty("status", 200); ;
            jsonObject.addProperty("message", "stored procedure retrieved");
            jsonObject.add("data",jsonArray);
        }catch (Exception e){
            String message = e.getLocalizedMessage();
            jsonObject.addProperty("status", 500);
            jsonObject.addProperty("message","Failed execution -> "+message);

            logger.info("message "+e.getMessage());
            logger.info("cause "+e.getCause());
        }


        return jsonObject.toString();
    }

    /**
     *
     * @param query this is the request body from the client
     * @return
     */
    public Object executeSavedSqlStatements(String query){
        JsonObject queryObject = new JsonParser().parse(query).getAsJsonObject();

        // check if the query exists
        String queryName = queryObject.get("query").getAsString();

        JsonObject templateObject = findQueryTemplate(queryName);

        if (null != templateObject){

            String crudType = templateObject.get("CRUD_TYPE").getAsString();
            JsonArray whereClause = templateObject.get("WHERE_CLAUSE").getAsJsonArray();
            JsonArray columnNames = templateObject.get("COLUMN_NAMES").getAsJsonArray();

            // validate the fields in the data payload to check if the fields exists

            JsonObject dataObject = queryObject.get("data").getAsJsonObject();


            List<String> missingValues = validateDataFields(dataObject,columnNames, whereClause, true );

            if (missingValues.size() == 0 ){ // validation was successful
                String sqlTemplate = loadSqlTemplate(crudType);

                if (null != sqlTemplate){
                    // create the preparedStatement sql statement as required
                    String prepared_statement_string = prepareSqlStatement(sqlTemplate, templateObject);

                    //logger.info(prepared_statement_string);
                    JsonArray jsonArray = new JsonArray();


                    List<String> values = validateDataFields(dataObject, columnNames, whereClause, false);

                    Gson gson = new Gson();
                    JsonElement jsonElement = gson.toJsonTree(values , new TypeToken<List<String>>(){}.getType());

                    logger.info(prepared_statement_string);
                    logger.info("data - > "+jsonElement.toString());
                    return prepared_statement_string;

                }else{
                    //TODO : RESPOND WITH  STATUS 500

                    return 500;
                }

            }else{
                // some missing values were found
                StringJoiner stringJoiner = new StringJoiner(",","[","]");
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("missing the following fields");

                missingValues.forEach(stringJoiner::add);

                stringBuilder.append(stringJoiner.toString());

                //TODO : RESPOND WITH  STATUS 404
                return stringBuilder.toString();
            }

            // find the template assosiated with the query before execution,



        }else{
            // TODO : RESPOND WITH STATUS 404
            return 404;

        }

    }


    JsonObject findQueryTemplate(String queryName){
        JsonArray jsonArray = DbApiV3Application.sqlQueries.getAsJsonArray();

        boolean found = false;

        JsonObject object = new JsonObject();

        for (JsonElement element : jsonArray){
            JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.get("QUERY_NAME").getAsString().equals(queryName)){
                found = true;
                object = jsonObject;
                break;
            }
        }

        return found ? object : null;
    }

    String loadSqlTemplate(String crudType){
        JsonArray jsonArray = DbApiV3Application.queryTemplate.getAsJsonArray();
        boolean found = false;

        String object = "";

        for (JsonElement element : jsonArray){
            JsonObject jsonObject = element.getAsJsonObject();
            if (jsonObject.get("CRUD_TYPE").getAsString().equals(crudType)){
                found = true;
                if ("oracle".equals(primarydb)){
                    object = jsonObject.get("ORACLE").getAsString();
                }else{
                    object = jsonObject.get("MSSQL").getAsString();
                }
                break;
            }
        }

        return found ? object : null;
    }

    @SuppressWarnings("Duplicates")
    List<String> validateDataFields(JsonObject dataObject, JsonArray columnNames, JsonArray whereClause,boolean validate){

        List<String> missingValues = new ArrayList<>();

        // validate the column names that require data to be field

        for (JsonElement element : columnNames) {
            String str = element.getAsString();

            String[] split_string = str.split(" ");

            for (String s : split_string) {
                if (s.contains("@")){
                    String key = s.replace("@","");

                    if (validate){
                        if (!dataObject.has(key)) missingValues.add(key);
                    }
                    else {
                        String value = dataObject.get(key).getAsString();
                        missingValues.add(value);
                    }

                }
            }

        }
        // validate the where clause

        for (JsonElement element : whereClause) {
            String str = element.getAsString();
            String[] split_string = str.split(" ");

            for (String s : split_string) {
                if (s.contains("@")){
                    String key = s.replace("@","");

                    if (validate){
                        if (!dataObject.has(key)) missingValues.add(key);
                    }else{
                        String value = dataObject.get(key).getAsString();
                        missingValues.add(value);
                    }
                }
            }

        }

        return missingValues;
    }

    /**
     *
     * @param queryStringTemplate  -- this is the sql statement stored in the query-template.json
     * @param queryObjectTemplate -- this is the object that will be used to prepare the final sql statement
     * @return
     */
    String prepareSqlStatement(String queryStringTemplate, JsonObject queryObjectTemplate){
        Matcher m = Pattern.compile("\\{(.*?)\\}").matcher(queryStringTemplate);
        while(m.find()) {
            switch (m.group(1)){
                case "TOP":
                    String top = queryObjectTemplate.get("TABLE_NAME").getAsString();
                    top = top.replace("@","");
                    queryStringTemplate = queryStringTemplate.replace("{TOP}",top);
                    break;
                case "TABLE_NAME":
                    String table_name = queryObjectTemplate.get("TABLE_NAME").getAsString();
                    queryStringTemplate = queryStringTemplate.replace("{TABLE_NAME}", table_name);
                    break;
                case "WHERE_CLAUSE":
                    JsonArray whereArray = queryObjectTemplate.get("WHERE_CLAUSE").getAsJsonArray();
                    String where_clause = constructStringFromArray(whereArray);

                    where_clause = "WHERE "+where_clause;


                    where_clause = where_clause.replace("@","");
                    queryStringTemplate = queryStringTemplate.replace("{WHERE_CLAUSE}",where_clause);
                    break;
                case "GROUP_STATEMENT":
                    String group_statement = queryObjectTemplate.get("GROUP_STATEMENT").getAsString();
                    group_statement = group_statement.replace("@","");
                    queryStringTemplate = queryStringTemplate.replace("{GROUP_STATEMENT}",
                            group_statement);
                    break;
                case "COLUMN_NAMES":
                    JsonArray columnArray = queryObjectTemplate.get("COLUMN_NAMES").getAsJsonArray();
                    String column_names = constructStringFromArray(columnArray);

                    column_names = column_names.replace("@","");
                    queryStringTemplate = queryStringTemplate.replace("{COLUMN_NAMES}",
                            column_names);
                    break;
            }
            System.out.println(m.group(1));
        }

        return queryStringTemplate;
    }

    String constructStringFromArray(JsonArray jsonArray){
        StringJoiner joiner = new StringJoiner(" , ","","");

        jsonArray.forEach(jsonElement -> joiner.add(jsonElement.getAsString()));
        return joiner.toString();
    }
}
