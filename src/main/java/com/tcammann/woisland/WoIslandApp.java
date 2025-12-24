package com.tcammann.woisland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class WoIslandApp {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(WoIslandApp.class, args);

        // keep Spring alive until the process is stopped
        var latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));
        latch.await();
    }
}
