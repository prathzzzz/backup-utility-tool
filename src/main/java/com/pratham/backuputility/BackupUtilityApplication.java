package com.pratham.backuputility;

import com.pratham.backuputility.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackupUtilityApplication implements CommandLineRunner {

    @Autowired
    private TransferService transferService;

    public static void main(String[] args) {
        SpringApplication.run(BackupUtilityApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize the transfer service on startup
        transferService.initialize();
    }
}
