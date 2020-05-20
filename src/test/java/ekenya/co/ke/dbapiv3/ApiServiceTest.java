package ekenya.co.ke.dbapiv3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiServiceTest {

    @Autowired LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

    @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "UPDATE TB_CUSTOMER SET BLOCK_STATUS=1 , BLOCKED_BY = ? WHERE CUSTOMER_ID = ?";
        String query = "{\n" +
                "\t\"query\":\"BLOCK_USER_ACCOUNT\",\n" +
                "\t\"data\":{\n" +
                "\t\t\"CUSTOMER_ID\":\"34\",\n" +
                "\t\t\"BLOCKED_BY\": \"MUNGAI\"\n" +
                "\t}\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }
}