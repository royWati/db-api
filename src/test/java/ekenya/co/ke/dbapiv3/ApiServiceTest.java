package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonArray;
import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import ekenya.co.ke.dbapiv3.services.ApiService;
import ekenya.co.ke.dbapiv3.services.DatabaseExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.*;

//  @SpringBootTest
class ApiServiceTest {

    @Autowired
    LoadConfiguration loadConfiguration;
    @Autowired private ApiService apiService;

    @Autowired private DatabaseExtractor databaseExtractor;

  //  @Test
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


         try {
             Process proc = Runtime.getRuntime().exec("/home/destino/workspace/JavaProject/listing.sh /"); //Whatever you want to execute
             BufferedReader read = new BufferedReader(new InputStreamReader(
                     proc.getInputStream()));
             try {
                 proc.waitFor();
             } catch (InterruptedException e) {
                 System.out.println(e.getMessage());
             }
             while (read.ready()) {
                 System.out.println(read.readLine());
             }
         } catch (IOException e) {
             System.out.println(e.getMessage());
         }
    }
//    @Test
    public void tester(){
        String you = "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?";

        int count = 0;
       for (char c : you.toCharArray()) if (c == '?') count++;

        System.out.println(count);
    }

 //   @Test
    public void generateQuestionMarks(){

        StringJoiner joiner = new StringJoiner(",","(",")");

        for (int i =0 ; i < 57 ; i ++){
            joiner.add("?");
        }
        System.out.println(joiner.toString());
    }

 //   @Test
    public void alignment(){

        String s = "ACCOUNTNUMBER,\n" +
                "FIRSTNAME,\n" +
                "MIDDLENAME,\n" +
                "SURNAME,\n" +
                "GENDER,\n" +
                "DOB,\n" +
                "PLACEOFBIRTH,\n" +
                "IDTYPE,\n" +
                "IDNUMBER,\n" +
                "MOBILENUMBER,\n" +
                "PROFESSION,\n" +
                "BRANCH,\n" +
                "EMAIL,\n" +
                "BRANCHNAME,\n" +
                "DSSFLEXID,\n" +
                "DSSACCOUNTNUMBER,\n" +
                "IMGPASSPORT,\n" +
                "IMGID,\n" +
                "IMGSIGNATURE,\n" +
                "ACC_CLS,\n" +
                "CUSTOMER_CATEGORY,\n" +
                "CARD_NUMBER,\n" +
                "DSSMOBILENUMBER,\n" +
                "PERMANENT_REGION,\n" +
                "PERMANENT_DISTRICT,\n" +
                "PERMANENT_WARD,\n" +
                "PERMANENT_STREET,\n" +
                "RES_DISTRICT,\n" +
                "RES_WARD,\n" +
                "RES_STREET,\n" +
                "RES_REGION,\n" +
                "POSTALADDRESS,\n" +
                "PLOTNUMBER,\n" +
                "INTRODUCTORY_LETTER,\n" +
                "TERMS_AND_CONDITIONS,\n" +
                "MARITAL_STATUS,\n" +
                "CITIZENSHIP,\n" +
                "CURRENCY,\n" +
                "EBANK_SBU_STATUS,\n" +
                "TIN,\n" +
                "CHECK_NUMBER,\n" +
                "TELEPHONE,\n" +
                "TITLE_PREFIX1,\n" +
                "TITLE_PREFIX2,\n" +
                "TITLE_PREFIX3,\n" +
                "MAIDEN_NAME,\n" +
                "LANDMARKS,\n" +
                "ID_ISSUEDATE,\n" +
                "ID_EXPIRYDATE,\n" +
                "MONTHLY_INCOME,\n" +
                "INCOME_SOURCE,\n" +
                "EBANK_SBUREG,\n" +
                "EBANK_SBUREGNARATIVE,\n" +
                "RECLASSIFY,\n" +
                "MANDATE_DECLARATION,\n" +
                "KYCREF\n";


        s = s.replace("\n","");

        System.out.println(s);
    }

    @Test
    public void splitValues(){

        String field54 = "9030010726483|16779555~9030014668771|27462~9030015853750|982928";

        String[] accounts = field54.split("~");


        List<String> values = new ArrayList<>();
        Arrays.stream(accounts).forEach(s -> {
            System.out.println(s);
            Arrays.stream(s.split("\\|")).forEach(values::add);
        });

        values.forEach(System.out::println);
    }
}