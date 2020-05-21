package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
class DatabaseExtractorImplTest {

    @Autowired DatabaseExtractor databaseExtractor;
    @Autowired LoadConfiguration loadConfiguration;
    @Autowired ApiService apiService;
   // @Test
    void executeSqlStatement() {
        loadConfiguration.updateQueryTemplate();
        String finalQuery = "UPDATE TB_CUSTOMER SET BLOCK_STATUS=1 WHERE @CUSTOMER_ID=?";
        String query = "UPDATE {TABLE_NAME} SET {COLUMN_NAMES} {WHERE_CLAUSE}";
        JsonObject requestObject = new JsonObject();
        String str_template = "{\n" +
                "    \"QUERY_NAME\": \"BLOCK_USER_ACCOUNT\",\n" +
                "    \"CRUD_TYPE\": \"UPDATE\",\n" +
                "    \"TABLE_NAME\": \"TB_CUSTOMER\",\n" +
                "    \"COLUMN_NAMES\": \"BLOCK_STATUS=1\",\n" +
                "    \"WHERE_CLAUSE\": \"WHERE @CUSTOMER_ID=?\"\n" +
                "  }";
        JsonObject templateObject = new JsonParser().parse(str_template).getAsJsonObject();
        String template = "";
        try {
            template = apiService.prepareSqlStatement(query, templateObject, "INSERT");
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(finalQuery, template);

    }
}