package ekenya.co.ke.dbapiv3;

import com.google.gson.JsonElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class DbApiV3Application {

    public static JsonElement spJsonElements;
    public static JsonElement queryTemplate;
    public static JsonElement sqlQueries;

    @Autowired private LoadConfiguration loadConfiguration;

    public static void main(String[] args) {
        SpringApplication.run(DbApiV3Application.class, args);
    }

    @PostConstruct
    void configureApplication() throws Exception {
       // loadConfiguration.updateSpListStore();
    }

}
