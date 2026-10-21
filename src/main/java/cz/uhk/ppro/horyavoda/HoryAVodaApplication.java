package cz.uhk.ppro.horyavoda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Informační systém půjčovny sportovního vybavení Hory a voda.
 * <p>
 * Třívrstvá architektura (závislosti směřují shora dolů):
 * {@code presentation} (REST API) → {@code application} (služby) → {@code infrastructure} (persistence, security)
 * nad doménovým modelem {@code domain}.
 */
@SpringBootApplication
public class HoryAVodaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoryAVodaApplication.class, args);
    }
}
