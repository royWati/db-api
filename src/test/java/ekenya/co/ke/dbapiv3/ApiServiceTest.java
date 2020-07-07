package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonArray;
import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import ekenya.co.ke.dbapiv3.services.DatabaseExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

  @SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

    @Autowired private DatabaseExtractor databaseExtractor;

    @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "INSERT INTO TB_PROCESSING_CODES (CODE , CREATED_ON) VALUES(?,?)";

        finalQuery = "update tb_biller_requests set biller_status = ?, biller_status_desc = ?, PG_REQUEST_ID = ? where esbref = ?";
        String query = "{\n" +
                "    \"query\": \"UPDATE_BILLER_REQUEST\",\n" +
                "    \"data\": {\n" +
                "        \"BILLER_STATUS\": \"0\",\n" +
                "        \"PG_REQUEST_ID\": \"0\",\n" +
                "        \"ESBREF\": \"0\",\n" +
                "        \"BILLER_STATUS_DESC\": \"10\"\n" +
                "    }\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery.toUpperCase(), s);
    }

 //   @Test
     void testStoredProcedures() throws Exception {
        String json_string = "[]";

        assertEquals(json_string, databaseExtractor.fetchSqlProcedures().toString());

    }
}