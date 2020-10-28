package ekenya.co.ke.dbapiv3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.regex.Pattern;

//@SpringBootTest
class LoadConfigurationTest {

    @Autowired
    LoadConfiguration loadConfiguration;

 //   @Test
    void updateQueryTemplate() {

        loadConfiguration.updateQueryTemplate();
    }

    @Test
    public void viewListedIps(){


             String phone =  "NKLJJFGJBKCM09";

             //"^[a-zA-Z0-9]{14}$"
             boolean f = Pattern.matches("^[A-Z]{14}$", phone);

             boolean t = Pattern.matches("^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{14}$",phone);

             System.out.println(t);

    }

    @Test
    public void jsonTree(){
        String tree = "{\"totalResults\":\"0\",\"status\":200,\"message\":\"database execution was successful\",\"data\":[]}";


        ObjectMapper objectMapper = new ObjectMapper();

      //  JsonObject jsonObject = new JsonParser().parse(tree).getAsJsonObject();
        try {
            Object json = objectMapper.readValue(tree , Object.class);

            String indented = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            System.out.println(indented);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



    }
}