package ekenya.co.ke.dbapiv3;

import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

//  @SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

  //  @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "INSERT INTO TB_PROCESSING_CODES (CODE , CREATED_ON) VALUES(?,?)";

        finalQuery = "SELECT ID, ACCOUNT_TYPE_ID FROM VW_ALLCUSTOMERS WHERE ACCOUNT_NO = ? ORDER BY ID OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String query = "{\n" +
                "    \"query\": \"VIEW_CUSTOMERS_TEST\",\n" +
                "    \"data\": {\n" +
                "        \"PAGE\": \"0\",\n" +
                "        \"SIZE\": \"20\",\n" +
                "        \"ACCOUNT_NO\": \"13456768\",\n" +
                "        \"ID\": \"1\"\n" +
                "    }\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }
}