package ekenya.co.ke.dbapiv3;

import ekenya.co.ke.dbapiv3.configuration.LoadConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LoadConfigurationTest {

    @Autowired
    LoadConfiguration loadConfiguration;

 //   @Test
    void updateQueryTemplate() {

        loadConfiguration.updateQueryTemplate();
    }

  //  @Test
    public void viewListedIps(){
        loadConfiguration.encryptIpAddresses();
        loadConfiguration.decryptIpAddress();
    }
}