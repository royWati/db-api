package ekenya.co.ke.dbapiv3.configuration;

import com.google.gson.JsonArray;

import com.google.gson.JsonParser;
import ekenya.co.ke.dbapiv3.DbApiV3Application;
import ekenya.co.ke.dbapiv3.services.DatabaseExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

//@Configuration
@Component
public class LoadConfiguration {

    private final static Logger logger = Logger.getLogger(LoadConfiguration.class.getName());
    @Autowired
    private DatabaseExtractor databaseExtractor;

    @Resource
    private Environment environment;

    @Value("${whitelist-ip}")
    public String[] listedIps;
    @Value("${whitelist-ip-encrypted}")
    public String[] encryptedIps;

    private static final String KEY = "864dba779da64d43bf51f05f5282cc08";

    public void updateSpListStore() throws Exception {
        JsonArray jsonArray = databaseExtractor.fetchSqlProcedures();
        databaseExtractor.updateCurrentSpStore(jsonArray);
        String spLocation = environment.getProperty("file-storage.sp-location");
        logger.info("sp file location -- "+spLocation);
        DbApiV3Application.spJsonElements = databaseExtractor.retrieveFileContent(spLocation);
        logger.info("primary sp cache load updated...");
    }

    public void updateQueryTemplate(){
         String fileName = environment.getProperty("file-storage.query-template");
         String queryData = environment.getProperty("file-storage.sql-queries");
         String injectionData = environment.getProperty("file-storage.injections");

         DbApiV3Application.queryTemplate = databaseExtractor.retrieveFileContent(fileName);
         DbApiV3Application.sqlQueries = databaseExtractor.retrieveFileContent(queryData);
         DbApiV3Application.injectionChecks = databaseExtractor.retrieveFileContent(injectionData);

   //     logger.info(DbApiV3Application.queryTemplate.toString());
   //     logger.info(DbApiV3Application.sqlQueries.toString());

    }

    public void updateInjectionList(){
        String fileName  = environment.getProperty("file-storage.injections");
        DbApiV3Application.injectionChecks = databaseExtractor.retrieveFileContent(fileName);
    }

    public void readWhiteListedIPs(){
        for (String s : listedIps) System.out.println(s);

        System.out.println(UUID.randomUUID().toString());
    }

    public void encryptIpAddresses(){
        String initVector = "encryptionIntVec";
        for (String s : listedIps){
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(StandardCharsets.UTF_8));
            SecretKeySpec secretKeySpec = new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8),"AES");

            Cipher cipher = null;
            try {
                cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
                byte[] encrypted = cipher.doFinal(s.getBytes());

                String finalValue = Base64.getEncoder().encodeToString(encrypted);
                String str = new String(encrypted);
                System.out.println(finalValue);
                System.out.println(str);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void decryptIpAddress(){
        String initVector = "encryptionIntVec";
        for (String s : encryptedIps){
            try {
                IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
                SecretKeySpec skeySpec = new SecretKeySpec(KEY.getBytes("UTF-8"), "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
                byte[] original = cipher.doFinal(Base64.getDecoder().decode(s));

                String decode = new String(original);

                System.out.println(decode);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


    @Bean
    JsonParser getJsonParser(){
        return new JsonParser();
    }
}
