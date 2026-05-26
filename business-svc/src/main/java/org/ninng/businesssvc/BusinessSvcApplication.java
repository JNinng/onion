package org.ninng.businesssvc;

import org.babyfish.jimmer.client.EnableImplicitApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableImplicitApi
public class BusinessSvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusinessSvcApplication.class, args);
    }

}
