package in.gov.sih.sih26135;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
public class Sih26135Application {

  public static void main(String[] args) {
    SpringApplication.run(Sih26135Application.class, args);
  }
}
