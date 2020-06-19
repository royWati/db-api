package ekenya.co.ke.dbapiv3;

import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

  @SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

    @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "INSERT INTO TB_PROCESSING_CODES (CODE , CREATED_ON) VALUES(?,?)";

        finalQuery = "SELECT ID, FIELD37, DATEX FROM TB_MESSAGES_EXTERNAL WHERE FIELD3 = ? AND FIELD3 NOT IN ('150000','320000','360000') " +
                "ORDER BY ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String query = "{\n" +
                "    \"query\": \"VIEW_ALL_TRANSACTIONS\",\n" +
                "    \"data\": {\n" +
                "        \"PAGE\": \"0\",\n" +
                "        \"FIELD3\": \"310000\",\n" +
                "        \"SIZE\": \"10\"\n" +
                "    }\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }
}