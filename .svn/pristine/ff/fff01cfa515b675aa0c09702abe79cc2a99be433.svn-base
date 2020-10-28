package ekenya.co.ke.dbapiv3.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import ekenya.co.ke.dbapiv3.DbApiV3Application;
import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.exceptions.SqlInjectionException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
//import java.util.logging.//logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApiService {

    @Value("${primarydb}")
    private String primarydb;
    @Autowired private DatabaseExtractor databaseExtractor;

    @Autowired
    LoadConfiguration loadConfiguration;

  //  private final static //logger //logger = //logger.get//logger(ApiService.class.getName());

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

   //     //logger.info("sp request object : "+jsonObject);

        String procedureName = jsonObject.get("procedure").getAsString();
        JsonObject dataObject = jsonObject.get("parameters").getAsJsonObject();

        JsonObject retrieveStructure = databaseExtractor.loadStoredProcedure(procedureName);
        JsonObject responseObject = new JsonObject();

  //      //logger.info("procedure template : "+retrieveStructure);

        final boolean[] hasMissingValues = {false};
        StringJoiner missingJoiner = new StringJoiner(",","[","]");

        StringBuilder builder = new StringBuilder();
        builder.append("Currently missing the following values ");

        if (retrieveStructure != null){
            JsonArray jsonArray = new JsonArray();
            retrieveStructure.get("parameter").getAsJsonArray().forEach(o -> {
                JsonObject object = (JsonObject) o;

                String fieldName = object.get("parameterName").getAsString();

                //    //logger.info("JSON -- "+jsonObject);
                if (dataObject.has(fieldName)){
                    JsonObject obj = new JsonObject();
                    obj.addProperty("field",fieldName);
                    obj.addProperty("dataType", object.get("dataType").getAsString());
                    obj.addProperty("inOut", object.get("inOut").getAsString());

   //                 //logger.info("field name : "+fieldName);
                    String value = dataObject.get(fieldName).getAsString();

                    obj.addProperty("value",value);

                    jsonArray.add(obj);
                }else{
                    //logger.info("null value found");
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

                    //logger.info("message "+e.getMessage());
                    //logger.info("cause "+e.getCause());
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

    public Object getStoredProcedures() throws Exception {

        // update the application to refresh the database application before returning the results
        loadConfiguration.updateSpListStore();

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

            //logger.info("message "+e.getMessage());
            //logger.info("cause "+e.getCause());
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

        JsonObject responseObject = new JsonObject();

        // check if the query exists
        String queryName = queryObject.get("query").getAsString();

        JsonObject templateObject = findQueryTemplate(queryName);

        if (null != templateObject){

            //logger.info("template : "+templateObject);

            String crudType = templateObject.get("CRUD_TYPE").getAsString();
            JsonArray whereClause = templateObject.get("WHERE_CLAUSE").getAsJsonArray();
            JsonArray columnNames = templateObject.get("COLUMN_NAMES").getAsJsonArray();

            // validate the fields in the data payload to check if the fields exists

            JsonObject dataObject = queryObject.get("data").getAsJsonObject();

            List<String> missingValues = new ArrayList<>();

            // all select values contain page and size in order to limit huge datacall to the database
            if ("SELECT".equals(crudType)){
                String orderBy = templateObject.get("ORDER_STATEMENT").getAsString();
                missingValues = validateDataFields(dataObject,columnNames, whereClause, true, orderBy );
            }else if("SEARCH".equals(crudType)){

                missingValues = likeValues(dataObject, templateObject.get("ORDER_STATEMENT").getAsString(),true);

            }else {
                 missingValues = validateDataFields(dataObject,columnNames, whereClause, true , null);
            }


            if (missingValues.size() == 0 ){ // validation was successful
                String sqlTemplate = loadSqlTemplate(crudType);

                if (null != sqlTemplate){
                    // create the preparedStatement sql statement as required
                    String prepared_statement_string = prepareSqlStatement(sqlTemplate, templateObject, crudType, dataObject);

                    ////logger.info(prepared_statement_string);

                    // create a page request query for a search

                    String page = "";

                    if ("SEARCH".equals(crudType) || "SELECT".equals(crudType))
                    page = createPageQuery(templateObject.getAsJsonArray("COLUMN_NAMES"),crudType);

                    //logger.info("PAGE QUERY FOR "+crudType+" "+page);

                    String pageQuery = prepareSqlStatement(page, templateObject, crudType, dataObject);

                    List<String> values = new ArrayList<>();

                    List<String> pageValues = new ArrayList<>();

                    if ("SELECT".equals(crudType)){
                        String orderBy = templateObject.get("ORDER_STATEMENT").getAsString();
                        values = validateDataFields(dataObject,columnNames, whereClause, false, orderBy );
                        pageValues = validateDataFields(dataObject,columnNames, whereClause, false , null);
                    }else {
                        values = validateDataFields(dataObject,columnNames, whereClause, false , null);
                        pageValues = validateDataFields(dataObject,columnNames, whereClause, false , null);
                    }

                    if ("SEARCH".equals(crudType) ){
                        values = likeValues(dataObject, templateObject.get("ORDER_STATEMENT").getAsString(),false);
                    }

                    // perform check for sql injection on the values
                    values.forEach(s -> {
                        //logger.info("validating --- "+s);
                        if (checkSqlInjection(s)) throw new SqlInjectionException("sql injection detected");
                    });


                    Gson gson = new Gson();
                    JsonElement jsonElement = gson.toJsonTree(values , new TypeToken<List<String>>(){}.getType());
                    JsonElement pageElement = gson.toJsonTree(pageValues , new TypeToken<List<String>>(){}.getType());

                    //logger.info("PAGE Q :"+pageQuery);
                    //logger.info(prepared_statement_string);
                    //logger.info("data - > "+jsonElement.toString());
                    //logger.info("page data - > "+pageElement.toString());

                    try {
                        JsonArray response_array = databaseExtractor.executeSqlStatement(prepared_statement_string,
                                jsonElement.getAsJsonArray());

                        // GET PAGE COUNT
                        //logger.info("CRUD TYPE ;;"+crudType);
                        if ("SEARCH".equals(crudType) || "SELECT".equals(crudType)){
                            JsonArray pageResults = databaseExtractor.executeSqlStatement(pageQuery, pageElement.getAsJsonArray());

                            JsonObject pageObject = new JsonObject();
                            responseObject.addProperty("totalResults",
                                    pageResults.get(0).getAsJsonObject().get("TOTAL_RESULTS").getAsString());
                            //logger.info("PAGE RESULTS "+pageResults);

                        }

                        responseObject.addProperty("status", 200);
                        responseObject.addProperty("message", "database execution was successful");
                        responseObject.add("data",response_array);

                    }catch (Exception e){
                        responseObject.addProperty("status", 500);
                        responseObject.addProperty("message", "error in database execution -> "+e.getMessage());
                    }

                    return responseObject.toString();

                }else{
                    //TODO : RESPOND WITH  STATUS 500

                    responseObject.addProperty("status", 500);
                    responseObject.addProperty("message", "error in database execution -> sql template not found");

                    return responseObject.toString();
                }

            }else{
                // some missing values were found
                StringJoiner stringJoiner = new StringJoiner(",","[","]");
                StringBuilder stringBuilder = new StringBuilder();

                if ("SEARCH".equals(crudType)){
                    stringBuilder.append("search operation requires atleast one or more of the following fields");
                }else{
                    stringBuilder.append("missing the following fields");
                }

                missingValues.forEach(stringJoiner::add);

                stringBuilder.append(stringJoiner.toString());

                responseObject.addProperty("status", 404);
                responseObject.addProperty("message", stringBuilder.toString());


                //TODO : RESPOND WITH  STATUS 404
                return responseObject.toString();
            }
            // find the template assosiated with the query before execution,
        }else{
            responseObject.addProperty("status", 404);
            responseObject.addProperty("message", "query not found");

            //TODO : RESPOND WITH  STATUS 404
            return responseObject.toString();
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

    // validate the fields for search
    private boolean validateDataFields(JsonObject dataObject, JsonArray likeArray) {
        boolean fieldFound = false;

        for (JsonElement element : likeArray ){
            String str = element.getAsString();

            String[] split_str = str.split(":");
            String key = split_str[1];

            if (dataObject.has(key)) fieldFound = true;
        }

        return fieldFound;
    }

    @SuppressWarnings("Duplicates")
    List<String> validateDataFields(JsonObject dataObject, JsonArray columnNames, JsonArray whereClause
            , boolean validate, @Nullable String orderBy){

        List<String> missingValues = new ArrayList<>();

        // validate the column names that require data to be field

        for (JsonElement element : columnNames) {
            String str = element.getAsString();

            String[] split_string = str.split(" ");

            //logger.info("FIRST VALUE : "+split_string[0]);

            String s = split_string[0];

                if (s.contains("@")){
                    String key = s.replace("@","");

                    if (validate){
                        if (!dataObject.has(key)) missingValues.add(key);
                    }
                    else {
                        String value = dataObject.get(key).getAsString();
                        missingValues.add(value);
                    }

                }else if (s.contains("OPTIONAL")){
                    String key = s.replace("OPTIONAL:","");

                    if (!validate){


                        // check if the optional value is present in the data object
                        if (dataObject.has(key)){
                            String value = dataObject.get(key).getAsString();
                            missingValues.add(value);
                        }
                    }
                }
        }
        // validate the where clause

        for (JsonElement element : whereClause) {
            String str = element.getAsString();

            boolean complexClauseExistance = false;
            if (str.contains("(")){
                complexClauseExistance = true;
            }

            String[] split_string = str.split(" ");

            //logger.info("FIRST VALUE : "+split_string[0]);

            String s = split_string[0];

            if (complexClauseExistance){

                //logger.info("COMPLEX QUERY FOUND");
                for (String value : split_string){
                    if (value.contains("@")) s = value;
                }
            }

                //required field search
                if (s.contains("@")){
                    String key = s.replace("@","");

                    if (validate){
                        if (!dataObject.has(key)) missingValues.add(key);
                    }else{
                        String value = dataObject.get(key).getAsString();
                        missingValues.add(value);
                    }
                }else if (s.contains("OPTIONAL:")){ // optional field found


                    String key = s.replace("OPTIONAL:","");

                    //logger.info("OPTIONAL VALUE FOUND : "+key);

                    // validation does not take place on optional values hence next set of logic only applies to adding
                    // values to be used by the prepared statement
                    /**
                     * check if the optional value maps to a relational column e.g mapping to
                     * dates columns : OPTIONAL:FROM->CREATED_ON > ? , OPTIONAL:TO->CREATED_ON < ?
                     *
                     */
                    //logger.info("value key "+key);
                    key = key.split("->")[0];

                    //logger.info("key -> "+key);

                    if (!validate){
                        // check if the optional value is present in the data object
                        if (dataObject.has(key)){
                            String value = dataObject.get(key).getAsString();
                            missingValues.add(value);
                        }
                    }
                }
        }
        // inspecting the offset elements of the code
        if (null != orderBy){
            Matcher findOffsets = Pattern.compile("\\((.*?)\\)").matcher(orderBy);

            while(findOffsets.find()) {
                switch (findOffsets.group(1)){
                    case "@PAGE":
                        if(validate){
                            if (!dataObject.has("PAGE")) missingValues.add("PAGE");
                        }else{

                            String size = dataObject.get("SIZE").getAsString();
                            String value = dataObject.get("PAGE").getAsString();
                            int new_page = Integer.parseInt(size)*Integer.parseInt(value);


                            missingValues.add(String.valueOf(new_page));
                        }
                        break;
                    case "@SIZE":
                        if(validate){
                            if (!dataObject.has("SIZE")) missingValues.add("SIZE");
                        }else{
                            String value = dataObject.get("SIZE").getAsString();
                            missingValues.add(value);
                        }
                        break;
                }
            }
        }

        return missingValues;
    }

    public List<String> likeValues (JsonObject dataObject, String orderyBy, boolean validate){
        List<String> list = new ArrayList<>();

        Matcher findOffsets = Pattern.compile("\\((.*?)\\)").matcher(orderyBy);

        while(findOffsets.find()) {
            switch (findOffsets.group(1)){
                case "@PAGE":

                    if (validate){
                        if (!dataObject.has("PAGE")) list.add("PAGE");
                    }else{
                        String size = dataObject.get("SIZE").getAsString();
                        String value = dataObject.get("PAGE").getAsString();
                        int new_page = Integer.parseInt(size)*Integer.parseInt(value);
                        list.add(String.valueOf(new_page));
                    }

                    break;
                case "@SIZE":
                        if (validate){
                            if (!dataObject.has("SIZE")) list.add("SIZE");
                        }else{
                            String a = dataObject.get("SIZE").getAsString();
                            list.add(a);
                        }
                    break;
            }
        }
        return list;
    }

    /**
     *
     * @param queryStringTemplate  -- this is the sql statement stored in the query-template.json
     * @param queryObjectTemplate -- this is the object that will be used to prepare the final sql statement
     * @param crudType
     * @param dataObject -- this will be used to validate the request for optional values
     * @return
     */
    public String prepareSqlStatement(String queryStringTemplate, JsonObject queryObjectTemplate,
                                      String crudType, JsonObject dataObject){
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
                    String where_clause = constructStringFromArray(whereArray, dataObject," AND ");

                    //logger.info("CONSTRUCTED WHERE CLAUSE : "+where_clause);

                    /**
                     * check if the where_clause contains an '=' sign, this is to justify the existance of
                     * a where clause in the string
                     *
                     * also check for existance of in
                     */
                    if (where_clause.contains("=") || where_clause.contains(" IN ")){
                        where_clause = "WHERE "+where_clause;
                    }

                    // replace the required fields
                    where_clause = where_clause.replace("@","");

                    // replacing the optional fields
                    where_clause = where_clause.replace("OPTIONAL:","");
                    queryStringTemplate = queryStringTemplate.replace("{WHERE_CLAUSE}",where_clause);
                    break;
                case "GROUP_STATEMENT":
                    JsonArray group_array = queryObjectTemplate.get("GROUP_STATEMENT").getAsJsonArray();
                    String groups = constructStringFromArray(group_array,",");
                    groups = groups.replace("@","");

                    if (group_array.size() > 0){
                        groups = "GROUP BY "+groups;
                    }
                    queryStringTemplate = queryStringTemplate.replace("{GROUP_STATEMENT}",
                            groups);
                    break;
                case "COLUMN_NAMES":
                    JsonArray columnArray = queryObjectTemplate.get("COLUMN_NAMES").getAsJsonArray();
                    String column_names = constructStringFromArray(columnArray, dataObject," , ");

                    //logger.info("COLUMNS -> "+column_names);

                    column_names = column_names.replace("@","");
                    column_names = column_names.replace("OPTIONAL:","");

               //     column_names = column_names.replace("OPTIONAL:","");
                    queryStringTemplate = queryStringTemplate.replace("{COLUMN_NAMES}",
                            column_names);
                    break;

                case "VALUES":
                    String value = queryObjectTemplate.get("VALUES").getAsString();
                    queryStringTemplate = queryStringTemplate.replace("{VALUES}", value);
                    break;

                case "ORDER_STATEMENT":
                    String order_value = queryObjectTemplate.get("ORDER_STATEMENT").getAsString();

                    order_value = order_value.replace("(@PAGE)","?");
                    order_value = order_value.replace("(@SIZE)","?");

                    queryStringTemplate = queryStringTemplate.replace("{ORDER_STATEMENT}", order_value);
                    break;

                case "LIKE":
                    JsonArray like_array = queryObjectTemplate.get("LIKE").getAsJsonArray();

                    String liker = create_like_structure(like_array, dataObject);

                    queryStringTemplate = queryStringTemplate.replace("{LIKE}", liker);
                    break;
            }
            System.out.println(m.group(1));
        }

        return queryStringTemplate;
    }

    private String create_like_structure(JsonArray like_array, JsonObject dataObject) {

        AtomicReference<String> s = new AtomicReference<>("");

        like_array.forEach(jsonElement -> {
            String key = jsonElement.getAsString().split(":")[1];
            if (dataObject.has(key)){
                String v = key+" LIKE '%"+dataObject.get(key).getAsString()+"%'";
                s.set(v);
            }
        });

        return s.get();
    }

    private String constructStringFromArray(JsonArray whereArray, JsonObject dataObject, String delimeter) {
        StringJoiner joiner = new StringJoiner(delimeter,"","");

        // " AND "
        //logger.info("clause data: "+dataObject);
        whereArray.forEach(jsonElement ->{
            // check if the value contains optional string
            String s = jsonElement.getAsString();

            //logger.info("clause : "+s);

            if (s.contains("OPTIONAL")){
                String fieldChecker = s.split(":")[1];

                boolean hasMoreMapping = false;  // this is to be used to map the correct value

                if (fieldChecker.contains("->")){
                    fieldChecker = fieldChecker.split("->")[0];
                    hasMoreMapping = true;
                }

                //logger.info("field checker : "+fieldChecker);
                if (dataObject.has(fieldChecker.split(" ")[0])){  // the split is to enable get the first value

                    if (hasMoreMapping){
                        joiner.add(s.split("->")[1]);
                    }else{
                        joiner.add(s);
                    }

                }
            }else{
                joiner.add(s);
            }
        });

        return joiner.toString();
    }

    String constructStringFromArray(JsonArray jsonArray, String delimeter){

        StringJoiner joiner = new StringJoiner(delimeter," "," ");

        jsonArray.forEach(jsonElement -> joiner.add(jsonElement.getAsString()));
        return joiner.toString();
    }

    String constructStringFromArray_Columns(JsonArray jsonArray, JsonObject dataObject){
        StringJoiner joiner = new StringJoiner(" , ","","");

        //logger.info("clause data: "+dataObject);
        //logger.info("json array data: "+jsonArray);

        jsonArray.forEach(jsonElement ->{
            // check if the value contains optional string
            String s = jsonElement.getAsString();

            //logger.info("clause : "+s);

            if (s.contains("OPTIONAL")){
                String fieldChecker = s.split(":")[1];

                //logger.info("field checker : "+fieldChecker);
                if (dataObject.has(fieldChecker.split(" ")[0])){
                    joiner.add(s);
                }
            }else{
                joiner.add(s);
            }
        });

        return joiner.toString();
    }


    public Object fetchDatabaseOperations() throws JsonProcessingException {

        // refresh the application list cache in order to have the latest update of the list

        loadConfiguration.updateQueryTemplate();

        JsonArray dataArray = new JsonArray();
        List<QueryDocumentation> queryDocumentations = new ArrayList<>();

        for (JsonElement jsonElement :DbApiV3Application.sqlQueries.getAsJsonArray()){
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            QueryDocumentation documentation = new QueryDocumentation();
            documentation.setDoc(jsonObject.get("DOCUMENTATION").getAsString());
            documentation.setQuery(jsonObject.get("QUERY_NAME").getAsString());

            if (jsonObject.has("FILTERS")){
                JsonArray jsonArray = jsonObject.get("FILTERS").getAsJsonArray();

                List<String> list =  new ArrayList<>();
                jsonArray.forEach(jsonElement1 -> list.add(jsonElement1.getAsString()));
                documentation.setFilters(list);
            }else {
                documentation.setFilters(new ArrayList<>());
            }

            queryDocumentations.add(documentation);

        }
        String doc = new ObjectMapper().writeValueAsString(queryDocumentations);
        dataArray = new JsonParser().parse(doc).getAsJsonArray();

        JsonObject responseObject = new JsonObject();
        responseObject.addProperty("status",200);
        responseObject.addProperty("message","database operations retrieved successfully");
        responseObject.add("data",dataArray);

        return responseObject.toString();
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    class QueryDocumentation{
        private String query;
        private String doc;
        private List<String> filters;
    }



    public boolean checkSqlInjection(String value){

        JsonObject injectionCheks  = DbApiV3Application.injectionChecks.getAsJsonObject();

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);

        injectionCheks.get("special_characters").getAsJsonArray().forEach(jsonElement -> {
            String check = jsonElement.getAsString();

            if (value.toLowerCase().contains(check)) atomicBoolean.set(true);
        });

        if (!atomicBoolean.get()){
            injectionCheks.get("special_words").getAsJsonArray().forEach(jsonElement -> {
                String check = jsonElement.getAsString();
                if (value.contains(check.toLowerCase())) atomicBoolean.set(true);
            });

            return atomicBoolean.get();
        }else{
            return true;
        }
    }

    String createPageQuery(JsonArray columns, String crudType){
        return "SELECT".equals(crudType) ?
        "SELECT COUNT("+columns.get(0)+") as " +
                "TOTAL_RESULTS FROM {TABLE_NAME} {WHERE_CLAUSE} {GROUP_STATEMENT}" :
                "SELECT COUNT("+columns.get(0)+") as " +
                        "TOTAL_RESULTS FROM {TABLE_NAME} WHERE {LIKE}"    ;
    }


}


