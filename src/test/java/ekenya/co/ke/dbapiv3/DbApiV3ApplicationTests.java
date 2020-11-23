package ekenya.co.ke.dbapiv3;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.text.DateFormatter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

//@SpringBootTest
class DbApiV3ApplicationTests {

    @Test
    void contextLoads() throws ParseException {
      String date = "860628";

      String t = date.substring(0,2);
      DateFormat sdfp = new SimpleDateFormat("yy");
      Date d = sdfp.parse(t);
      DateFormat sdff = new SimpleDateFormat("yyyy");
      String dt = sdff.format(d);

      System.out.println(dt);

      String new_date = dt+date.substring(2);

      System.out.println("new date | "+new_date);

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

      LocalDate localDate = LocalDate.parse(new_date , formatter);

      System.out.println("date | "+localDate.toString());
    }

    @Test
  void contextLoads2(){

      String currentDate = "28061986";

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddMMyyyy");

      LocalDate localDate = LocalDate.parse(currentDate, formatter);

      String newDate = localDate.toString().replace("-","");

      System.out.println(newDate);
    }

}
