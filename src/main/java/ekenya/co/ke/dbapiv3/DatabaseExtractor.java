package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface DatabaseExtractor {
    JsonArray fetchSqlProcedures() throws Exception;
    JsonArray fetchTables() throws Exception;
    void updateCurrentSpStore(JsonArray jsonArray);

    JsonElement retrieveFileContent(String fileName);

    JsonObject loadStoredProcedure(String procedureName);
    JsonArray executeStoredProcedure(String procedureName, JsonArray jsonArray);
    JsonArray executeSqlStatement(String sqlStatement, JsonArray jsonArray) throws Exception;
    Object executeSqlStatement(String sqlStatement, JsonObject jsonObject) throws Exception;
    Object executeSqlStatement(JsonObject queryTemplate, JsonObject requestObject) throws Exception;
    JsonObject queryInspector(String query , JsonArray jsonArray);




}
