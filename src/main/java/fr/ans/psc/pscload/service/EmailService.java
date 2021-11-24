package fr.ans.psc.pscload.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Value("${pscload.mail.receiver}")
    private String receiver;

    public void sendProcessEndingConfirmationMail(String subject, Map<String, File> latestTxtAndSer) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(receiver);
        message.setSubject(subject);
        message.setText(getEmailMessage(latestTxtAndSer));

        emailSender.send(message);
    }

    private String getEmailMessage(Map<String, File> latestTxtAndSer) {
        String latestTxt = latestTxtAndSer.get("txt").getName();
        String latestSer = latestTxtAndSer.get("ser").getName();

        return "Le process pscload s'est terminé, le fichier " + latestSer + " a été généré à partir du fichier " + latestTxt + ".";
    }
}
