package com.toni.FoodApp.email_notification.services;

import com.toni.FoodApp.auth_users.entity.User;
import com.toni.FoodApp.email_notification.dtos.AccountSetupDto;
import com.toni.FoodApp.email_notification.dtos.NotificationDTO;
import com.toni.FoodApp.email_notification.entity.Notification;
import com.toni.FoodApp.email_notification.repository.NotificationRepository;
import com.toni.FoodApp.enums.NotificationType;
import com.toni.FoodApp.enums.RoleName;
import com.toni.FoodApp.order.dtos.OrderDTO;
import com.toni.FoodApp.order.dtos.OrderItemDTO;
import com.toni.FoodApp.response.Response;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService{
    private final JavaMailSender javaMailSender;
    private final NotificationRepository notificationRepository;
    @Value("${base.payment.link}")
    private String basePaymentLink;
    private final TemplateEngine templateEngine;

    private void sendEmail(NotificationDTO notificationDTO) {
        log.info("Inside SendEmail()");
        try{
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,MimeMessageHelper.MULTIPART_MODE_MIXED,
                    StandardCharsets.UTF_8.name());
            helper.setTo(notificationDTO.getRecipient());
            helper.setSubject(notificationDTO.getSubject());
            helper.setText(notificationDTO.getBody(), notificationDTO.isHtml());
            javaMailSender.send(mimeMessage);
            //save to db

            Notification notificationToSave = Notification.builder()
                    .recipient(notificationDTO.getRecipient())
                    .subject(notificationDTO.getSubject())
                    .body(notificationDTO.getBody())
                    .type(NotificationType.EMAIL)
                    .build();
            notificationRepository.save(notificationToSave);
            log.info("Email saved to DB successfully.");
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    @Override
    public void sendAccountSetupEmail(AccountSetupDto accountSetupDto, RoleName role) {
        log.info("Preparing account setup email for: {}", accountSetupDto.getEmail());

        String subject = "Welcome to the Team - Account Credentials";

        String htmlBody = String.format(
                "<html>" +
                        "<body>" +
                        "<h3>Welcome Aboard!</h3>" +
                        "<p>Your %s account has been created. Please use the credentials below to log in:</p>" +
                        "<p><b>Email:</b> %s</p>" +
                        "<p><b>Temporary Password:</b> %s</p>" +
                        "<br>" +
                        "<p><i>Please log in immediately and change your password.</i></p>" +
                        "</body>" +
                        "</html>",
                role.name().toLowerCase(),
                accountSetupDto.getEmail(),
                accountSetupDto.getPassword()
        );

        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(accountSetupDto.getEmail())
                .subject(subject)
                .body(htmlBody)
                .isHtml(true)
                .build();

        sendEmail(notificationDTO);
    }


    @Override
    public void sendOrderConfirmationEmail(User customer, OrderDTO orderDTO, Long branchId) {
        String subject = "Your Order Confirmation - Order #" + orderDTO.getId();
        Context context = new Context(Locale.getDefault());
        context.setVariable("customerName", customer.getName());
        //orderId, amount, paymentDate
        context.setVariable("orderId", String.valueOf(orderDTO.getId()));
        context.setVariable("totalAmount", orderDTO.getTotalAmount());
        context.setVariable("orderDate", orderDTO.getOrderDate());
        String deliveryAddress = orderDTO.getUser().getAddress();
        context.setVariable("deliveryAddress", deliveryAddress);
        context.setVariable("currentYear", java.time.Year.now());


        StringBuilder orderItemsHtml = new StringBuilder();
        for (OrderItemDTO item: orderDTO.getOrderItems()) {
            orderItemsHtml.append("<div class=\"order-item\">")
                    .append("<p>").append(item.getMenu().getName()).append(" x ").append(item.getQuantity()).append(" ").append("</p>")
                    .append("<p>").append(item.getSubTotal()).append("</p>")
                    .append("</div>");
        }

        context.setVariable("orderItemsHtml", orderItemsHtml.toString());
        context.setVariable("totalItems", orderDTO.getOrderItems().size());

        String paymentLink = basePaymentLink + orderDTO.getId() + "&amount=" + orderDTO.getTotalAmount();
        context.setVariable("paymentLink", paymentLink);

        String EmailBody = templateEngine.process("order-confirmation", context);
        sendEmail(NotificationDTO.builder()
                .recipient(customer.getEmail())
                .subject(subject)
                .body(EmailBody)
                .isHtml(true)
                .build());

    }

    @Override
    public void deactivateAccount(User user) {
        log.info("Inside deactivateOwnAccount()");
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your account has been deactivated")
                .body("Dear " + user.getName() + ",\n\n" +
                        "Your account has been successfully deactivated. " +
                        "If you didn't request this action or want to reactivate your account, " +
                        "please contact our support team.\n\n" +
                        "Best regards,\nFood App Team")
                .build();
        sendEmail(notificationDTO);
    }

    @Override
    public void restoreAccount(User user) {
        log.info("Inside restoreAccount()");
        NotificationDTO notificationDTO = NotificationDTO.builder()
                .recipient(user.getEmail())
                .subject("Your account has been restored")
                .body("Dear " + user.getName() + ",\n\n" +
                        "Your account has been restored by a manager or administrator. " +
                        "If you didn't request this action or want to deactivate your account, " +
                        "please contact our support team.\n\n" +
                        "Best regards,\nFood App Team")
                .build();
        sendEmail(notificationDTO);
    }
}
