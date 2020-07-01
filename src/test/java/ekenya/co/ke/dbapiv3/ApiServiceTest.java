package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonArray;
import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import ekenya.co.ke.dbapiv3.services.DatabaseExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

 // @SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

    @Autowired private DatabaseExtractor databaseExtractor;

  //  @Test
    void executeSavedSqlStatements() {
        loadConfiguration.updateQueryTemplate();

        String finalQuery = "INSERT INTO TB_PROCESSING_CODES (CODE , CREATED_ON) VALUES(?,?)";

        finalQuery = "SELECT  ID , CREATED_ON , USER_NAME , FIRST_NAME , MIDDLE_NAME , LAST_NAME ," +
                " MOBILE_NUMBER AS PHONE_NUMBER , EMAIL , LAST_LOGIN_DATE AS LAST_LOGIN ," +
                " COALESCE(TRIALS,0) AS LOGIN_TRIALS , LAST_PASSWORD_RESET , COALESCE(STATUS,0) AS STATUS ," +
                " COALESCE(LOCKED,0) AS LOCKED FROM TB_USER WHERE DELETED = 0   " +
                " ORDER BY ID DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        String query = "{\n" +
                "    \"query\": \"VIEW_ALL_USERS_SQL\",\n" +
                "    \"data\": {\n" +
                "        \"PAGE\": \"0\",\n" +
                "        \"SIZE\": \"10\"\n" +
                "    }\n" +
                "}";
        String s = String.valueOf(apiService.executeSavedSqlStatements(query)) ;
        assertEquals(finalQuery, s);
    }

 //   @Test
     void testStoredProcedures() throws Exception {
        String json_string = "[]";

        assertEquals(json_string, databaseExtractor.fetchSqlProcedures().toString());

    }
}