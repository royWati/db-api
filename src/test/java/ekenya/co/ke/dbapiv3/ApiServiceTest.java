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

        finalQuery = "SELECT ID, DATE_OF_BIRTH, CREATED_ON FROM VW_ALLCUSTOMERS WHERE ID = ? AND ACCOUNT_NO = ? " +
                "CREATED_ON > ? AND CREATED_ON < ?" +
                "ORDER BY ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String query = "{\n" +
                "    \"query\": \"VIEW_CUSTOMERS_TEST\",\n" +
                "    \"data\": {\n" +
                "        \"PAGE\": \"0\",\n" +
                "        \"SIZE\": \"20\",\n" +
                "        \"ACCOUNT_NO\": \"13456768\",\n" +
                "        \"FROM\": \"2020-09-20\",\n" +
                "        \"TO\": \"2020-10-20\",\n" +
                "        \"ID\": \"1\"\n" +
                "    }\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }
}