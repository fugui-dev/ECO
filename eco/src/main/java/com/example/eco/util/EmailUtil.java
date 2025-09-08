package com.example.eco.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Component
public class EmailUtil {

    /**
     * 生成验证码
     *
     * @return
     */
    public String getVerificationCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    }

    /**
     * 生成邮件内容
     *
     * @param verificationCode
     * @return
     */
    public String getEmailContent(String verificationCode) {
        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<p>Hotel Reservation Verification Code</p>" +
                "<p>Dear User</p>" +
                "<p>Thank you for choosing our service! To ensure the security of your reservation, please verify your booking using the code below within the next 5 minutes:</p>" +
                "<p>Verification Code: <span style='font-size: 24px; font-weight: bold; color: #333;'>" + verificationCode + "</span></p>" +
                "<p>Simply enter this code on the platform to complete your verification. If you did not request this, please contact our support team immediately.</p>" +
                "<p>We will do our utmost to ensure your extraordinary journey!</p>" +
                "<p>Warm regards,<br>" +
                "Support Team<br>" +
                "[reservation@odysseyglobal.io | Contact Team | Website Link]</p>" +
                "</div>" +
                "</body></html>";

        return content;
    }

    public String getOrderExaminePassContent(String hotelName) {

        String content = "Dear user, your hotel [" + hotelName + "] reservation has been approved. Please wait for the administrator to make a reservation for you.";

        return content;
    }

    public String getOrderExamineRejectContent(String hotelName, String reason) {

        if (StringUtils.hasLength(reason)) {
            String content = "Dear user, we are very sorry to inform you that your hotel [" + hotelName + "] order has not been approved," +
                    " The reason is [" + reason + "], if you have any questions, please contact us.";
            return content;
        } else {
            String content = "Dear user, we are very sorry to inform you that your hotel [" + hotelName + "] order has not been approved," +
                    " if you have any questions, please contact us.";
            return content;
        }
    }


    public String getOrderFinishContent(String hotelName) {

        String content = "Dear user, congratulations! Your hotel [" + hotelName + "]reservation has been successfully booked.";

        return content;
    }


    public String getOrderFailContent(String hotelName, String reason) {

        if (StringUtils.hasLength(reason)) {
            String content = "Dear user, we are very sorry to inform you that your hotel [" + hotelName + "]reservation was not successfully booked." +
                    " The reason is [" + reason + "]. we apologize again. If there are any problems, please contact us.";
            return content;
        } else {
            String content = "Dear user, we are very sorry to inform you that your hotel [" + hotelName + "]reservation was not successfully booked." +
                    " we apologize again. If there are any problems, please contact us.";
            return content;
        }
    }

    /**
     * 验证邮箱格式
     *
     * @param email
     * @return
     */
    public Boolean isEmail(String email) {
        if (email == null || email.length() < 1 || email.length() > 256) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$");
        return pattern.matcher(email).matches();
    }

    public String getOrderSuccessContent(String reservationId, String checkInDate, String checkOutDate, String guestName, String hotelName) {
        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2>Your Reservation is Confirmed! 🎉</h2>" +
                "<p>Dear " + guestName + ",</p>" +
                "<p>Thank you for choosing our service! We are delighted to inform you that your reservation has been successfully processed.</p>" +
                "<h3>Reservation Details:</h3>" +
                "<ul style='list-style: none; padding: 0;'>" +
                "<li><strong>Reservation ID:</strong> " + reservationId + "</li>" +
                "<li><strong>Check-in Date:</strong> " + checkInDate + "</li>" +
                "<li><strong>Check-out Date:</strong> " + checkOutDate + "</li>" +
                "<li><strong>Guest Wallet Address:</strong> " + guestName + "</li>" +
                "<li><strong>Hotel Name:</strong> " + hotelName + "</li>" +
                "</ul>" +
                "<p>If you have any questions or need further assistance, feel free to reach out to our support team. We are here to help!</p>" +
                "<p>Looking forward to being part of your unforgettable journey.</p>" +
                "<p>Warm regards,<br>" +
                "Support Team<br>" +
                "[reservation@odysseyglobal.io | Contact Team | Website Link]</p>" +
                "</div>" +
                "</body></html>";

        return content;
    }

    public String getOrderFailureContent(String reservationId, String attemptDate, String hotelName, String guestName, String reason) {
        String content = "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2>Important: Your Reservation Could Not Be Processed ⚠️</h2>" +
                "<p>Dear " + guestName + ",</p>" +
                "<p>We regret to inform you that your reservation attempt was not successful.</p>" +
                "<h3>Details of the Failed Transaction:</h3>" +
                "<ul style='list-style: none; padding: 0;'>" +
                "<li><strong>Attempted Reservation ID:</strong> " + reservationId + "</li>" +
                "<li><strong>Date of Attempt:</strong> " + attemptDate + "</li>" +
                "<li><strong>Hotel Name:</strong> " + hotelName + "</li>" +
                "</ul>" +
                "<h3>Reason for Failure:</h3>" +
                "<p>" + (StringUtils.hasLength(reason) ? reason : "Technical issues occurred during the booking process.") + "</p>" +
                "<h3>Recommended Actions:</h3>" +
                "<ul>" +
                "<li>Verify your payment details</li>" +
                "<li>Try to rebook the room via our website</li>" +
                "<li>Contact our support team for assistance</li>" +
                "</ul>" +
                "<p>We apologize for any inconvenience caused and are committed to helping you secure your booking as soon as possible.</p>" +
                "<p>Warm regards,<br>" +
                "Support Team<br>" +
                "[reservation@odysseyglobal.io | Contact Team | Website Link]</p>" +
                "</div>" +
                "</body></html>";

        return content;
    }

}
