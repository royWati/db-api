package ekenya.co.ke.dbapiv3;

import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

  //  @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "INSERT INTO TB_PROCESSING_CODES (CODE , CREATED_ON) VALUES(?,?)";
        String query = "{\n" +
                "\t\"query\":\"CREATE_PROCESSING_CODE\",\n" +
                "\t\"data\":{\n" +
                "\t\t\"CODE\":\"32000\",\n" +
                "\t\t\"CREATED_ON\": \"MUNGAI\"\n" +
                "\t}\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }
}